//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package twogtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import game.GameInformation;
import game.GameTree;
import game.Node;
import game.NodeUtils;
import go.Board;
import go.Color;
import go.Move;
import go.Point;
import gtp.Gtp;
import gtp.GtpError;
import gtp.GtpUtils;
import gtp.GtpServer;
import utils.StringUtils;
import version.Version;

//----------------------------------------------------------------------------

public class TwoGtp
    extends GtpServer
{
    public TwoGtp(InputStream in, OutputStream out, String black,
                  String white, String referee, String observer, int size,
                  double komi, boolean isKomiFixed, int numberGames,
                  boolean alternate, String sgfFile, boolean force,
                  boolean verbose, boolean estimateScore, Openings openings,
                  boolean loadsgf)
        throws Exception
    {
        super(in, out, null);
        if (black.equals(""))
            throw new Exception("No black program set.");
        if (white.equals(""))
            throw new Exception("No white program set.");
        m_sgfFile = sgfFile;
        if (force)
            getResultFile().delete();
        findInitialGameIndex();
        readGames();
        m_black = new Gtp(black, verbose, null);
        m_black.setLogPrefix("B");
        m_white = new Gtp(white, verbose, null);
        m_white.setLogPrefix("W");
        m_refereeCommand = referee;
        if (! referee.equals(""))
        {
            m_referee = new Gtp(referee, verbose, null);
            m_referee.setLogPrefix("R");
        }
        if (! observer.equals(""))
        {
            m_observer = new Gtp(observer, verbose, null);
            m_observer.setLogPrefix("O");
        }
        m_inconsistentState = false;
        m_black.queryProtocolVersion();
        m_white.queryProtocolVersion();
        m_blackName = getName(m_black);
        m_whiteName = getName(m_white);        
        m_blackVersion = getVersion(m_black);
        m_whiteVersion = getVersion(m_white);        
        m_black.querySupportedCommands();
        m_white.querySupportedCommands();
        m_black.queryInterruptSupport();
        m_white.queryInterruptSupport();
        if (m_referee != null)
        {
            m_referee.queryProtocolVersion();
            m_refereeName = getName(m_referee);        
            m_refereeVersion = getVersion(m_referee);        
            m_referee.querySupportedCommands();
            m_referee.queryInterruptSupport();
        }        
        if (m_observer != null)
        {
            m_observer.queryProtocolVersion();
            m_observer.querySupportedCommands();
            m_observer.queryInterruptSupport();
        }        
        m_size = size;
        m_komi = komi;
        m_isKomiFixed = isKomiFixed;
        m_alternate = alternate;
        m_estimateScore = estimateScore;
        m_numberGames = numberGames;
        m_openings = openings;
        m_verbose = verbose;
        m_loadsgf = loadsgf;
        initGame(size < 1 ? 19 : size);
    }

    public void autoPlay() throws Exception
    {
        System.in.close();
        StringBuffer response = new StringBuffer(256);
        while (m_gameIndex < m_numberGames)
        {
            boolean error = false;
            response.setLength(0);
            if (newGame(m_size > 0 ? m_size : 19, response))
            {
                while (! gameOver())
                {
                    response.setLength(0);
                    if (! sendGenmove(m_board.getToMove(), response))
                    {
                        error = true;
                        break;
                    }
                }
            }
            else
                error = true;
            if (error)
                handleEndOfGame(true, response.toString());
            if (m_black.isProgramDead())
                throw new Exception("Black program died");
            if (m_white.isProgramDead())
                throw new Exception("White program died");
        }
        m_black.sendCommand("quit");
        m_white.sendCommand("quit");
        close();
    }

    public void close()
    {
        m_black.close();
        m_white.close();
        if (m_referee != null)
            m_referee.close();
        if (m_observer != null)
            m_observer.close();
        m_black.waitForExit();
        m_white.waitForExit();
        if (m_referee != null)
            m_referee.waitForExit();
        if (m_observer != null)
            m_observer.waitForExit();
    }    

    /** Returns number of games left of -1 if no maximum set. */
    public int gamesLeft()
    {
        if (m_numberGames <= 0)
            return -1;
        return m_numberGames - m_gameIndex;
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (cmd.equals("final_score"))
            return finalStatusCommand(cmdLine, response);
        else if (cmd.equals("final_status"))
            return finalStatusCommand(cmdLine, response);
        else if (cmd.equals("final_status_list"))
            return finalStatusCommand(cmdLine, response);
        else if (cmd.equals("gogui_interrupt"))
            ;
        else if (cmd.equals("gogui_title"))
            response.append(getTitle());
        else if (cmd.equals("loadsgf"))
            return sendBoth(cmdLine, response, true, false);
        else if (cmd.equals("twogtp_black"))
            status = twogtpColor(m_black, cmdLine, response);
        else if (cmd.equals("twogtp_white"))
            status = twogtpColor(m_white, cmdLine, response);
        else if (cmd.equals("twogtp_referee"))
            status = twogtpReferee(cmdLine, response);
        else if (cmd.equals("twogtp_observer"))
            status = twogtpObserver(cmdLine, response);
        else if (cmd.equals("quit"))
            status = sendBoth(cmdLine, response, false, false);
        else if (cmd.equals("black"))
            status = cmdPlay(Color.BLACK, cmdArray, response);
        else if (cmd.equals("white"))
            status = cmdPlay(Color.WHITE, cmdArray, response);
        else if (cmd.equals("undo"))
            status = undo(response);
        else if (cmd.equals("genmove_black"))
            status = sendGenmove(Color.BLACK, response);
        else if (cmd.equals("genmove_white"))
            status = sendGenmove(Color.WHITE, response);
        else if (cmd.equals("boardsize"))
            status = boardsize(cmdArray, response);
        else if (cmd.equals("komi"))
            komi(cmdArray, response);
        else if (cmd.equals("scoring_system"))
            sendIfSupported(cmd, cmdLine);
        else if (cmd.equals("name"))
            response.append("TwoGtp");
        else if (cmd.equals("version"))
            response.append(Version.get());
        else if (cmd.equals("protocol_version"))
            response.append("1");
        else if (cmd.equals("help"))
            response.append("boardsize\n" +
                            "black\n" +
                            "final_score\n" +
                            "final_status\n" +
                            "final_status_list\n" +
                            "genmove_black\n" +
                            "genmove_white\n" +
                            "gogui_interrupt\n" +
                            "gogui_title\n" +
                            "help\n" +
                            "komi\n" +
                            "loadsgf\n" +
                            "name\n" +
                            "quit\n" +
                            "scoring_system\n" +
                            "time_settings\n" +
                            "twogtp_black\n" +
                            "twogtp_observer\n" +
                            "twogtp_referee\n" +
                            "twogtp_white\n" +
                            "undo\n" +
                            "version\n" +
                            "white\n");
        else if (cmd.equals("genmove"))
        {
            response.append("command not supported in protocol version 1");
            status = false;
        }
        else if (cmd.equals("time_settings"))
            sendIfSupported(cmd, cmdLine);
        else
        {
            boolean isExtCommandBlack = m_black.isCommandSupported(cmd);
            boolean isExtCommandWhite = m_white.isCommandSupported(cmd);
            boolean isExtCommandReferee = false;
            if (m_referee != null && ! m_refereeIsDisabled)
                isExtCommandReferee = m_referee.isCommandSupported(cmd);
            boolean isExtCommandObserver = false;
            if (m_observer != null && ! m_observerIsDisabled)
                isExtCommandObserver = m_observer.isCommandSupported(cmd);
            if (isExtCommandBlack && ! isExtCommandObserver
                && ! isExtCommandWhite && ! isExtCommandReferee)
                return sendSingle(m_black, cmdLine, response);
            if (isExtCommandWhite && ! isExtCommandObserver
                && ! isExtCommandBlack && ! isExtCommandReferee)
                return sendSingle(m_white, cmdLine, response);
            if (isExtCommandReferee && ! isExtCommandObserver
                && ! isExtCommandBlack && ! isExtCommandWhite)
                return sendSingle(m_referee, cmdLine, response);
            if (isExtCommandObserver && ! isExtCommandReferee
                && ! isExtCommandBlack && ! isExtCommandWhite)
                return sendSingle(m_observer, cmdLine, response);
            if (! isExtCommandReferee
                && ! isExtCommandBlack
                && ! isExtCommandObserver
                && ! isExtCommandWhite)
            {
                response.append("unknown command");
                return false;
            }
            response.append("use twogtp_black/white/referee/observer");
            return false;
        }
        return status;
    }

    public void interruptProgram(Gtp gtp)
    {
        try
        {
            if (gtp.isInterruptSupported())
                gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            System.err.println(e);
        }
    }

    public void interruptCommand()
    {
        interruptProgram(m_black);
        interruptProgram(m_white);
        if (m_referee != null && ! m_refereeIsDisabled)
            interruptProgram(m_referee);
        if (m_observer != null && ! m_observerIsDisabled)
            interruptProgram(m_observer);
    }

    private static class ScoreEstimate
    {
        double m_black;

        double m_white;

        double m_referee;
    }

    private boolean m_alternate;

    private boolean m_estimateScore;

    private boolean m_gameSaved;

    private boolean m_inconsistentState;

    private boolean m_isKomiFixed;

    private boolean m_loadsgf;

    private boolean m_observerIsDisabled;

    private boolean m_refereeIsDisabled;

    private boolean m_resigned;

    private boolean m_verbose;

    private int m_gameIndex;

    private int m_numberGames;

    private int m_openingMovesIndex;

    private int m_size;

    private double m_komi;

    private double m_cpuTimeBlack;

    private double m_cpuTimeWhite;

    private Board m_board;

    private Color m_resignColor;

    private GameTree m_gameTree;

    private Node m_currentNode;

    private Openings m_openings;

    private String m_blackName;

    private String m_blackVersion;

    private String m_openingFile;

    private String m_refereeCommand;

    private String m_refereeName;

    private String m_refereeVersion;

    private String m_sgfFile;

    private String m_whiteName;

    private String m_whiteVersion;

    private HashMap m_scoreEstimates = new HashMap();

    private Vector m_games = new Vector(100, 100);;

    private Vector m_openingMoves;

    private Gtp m_black;

    private Gtp m_observer;

    private Gtp m_referee;

    private Gtp m_white;

    private boolean boardsize(String[] cmdArray, StringBuffer response)
    {
        if (gamesLeft() == 0)
        {
            response.append("Maximum number of " + m_numberGames +
                            " games reached");
            return false;
        }
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (argument.m_integer < 1)
        {
            response.append("Invalid argument");
            return false;
        }
        if (m_size > 0 && argument.m_integer != m_size)
        {
            response.append("Size must be ");
            response.append(m_size);
            return false;
        }
        return newGame(argument.m_integer, response);
    }

    private boolean checkInconsistentState(StringBuffer response)
    {
        if (m_inconsistentState)
            response.append("Inconsistent state");
        return m_inconsistentState;
    }

    private boolean cmdPlay(Color color, String[] cmdArray,
                            StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        if (cmdArray.length < 2)
        {
            response.append("Missing argument");
            return false;
        }
        Point point = null;
        try
        {
            point = GtpUtils.parsePoint(cmdArray[1], m_board.getSize());
        }
        catch (GtpError e)
        {
            response.append(e.getMessage());
            return false;
        }
        Move move = new Move(point, color);
        if (m_openings != null)
        {
            if (! move.equals((Move)m_openingMoves.get(m_openingMovesIndex)))
            {
                response.append("Move not allowed if openings are used");
                return false;
            }
        }
        return sendMove(move, response);
    }

    private void estimateScore()
    {
        if (! m_estimateScore)
            return;
        ScoreEstimate estimate = new ScoreEstimate();
        estimate.m_black = estimateScore(m_black);
        estimate.m_white = estimateScore(m_white);
        if (m_referee != null && ! m_refereeIsDisabled)
            estimate.m_referee = estimateScore(m_referee);
        m_scoreEstimates.put(m_currentNode, estimate);
    }

    private double estimateScore(Gtp gtp)
    {
        if (! gtp.isCommandSupported("estimate_score"))
            return 0.0;
        StringBuffer response = new StringBuffer();
        if (! sendSingle(gtp, "estimate_score", response))
            return 0.0;
        String score = StringUtils.tokenize(response.toString())[0];
        try
        {
            if (score.indexOf("B+") >= 0)
                return Double.parseDouble(score.substring(2));
            else if (score.indexOf("W+") >= 0)
                return Double.parseDouble("-" + score.substring(2));
        }
        catch (NumberFormatException e)
        {            
        }
        return 0.0;
    }

    private boolean finalStatusCommand(String cmdLine, StringBuffer response)
    {
        if (m_referee != null && ! m_refereeIsDisabled)
            return sendSingle(m_referee, cmdLine, response);
        if (m_black.isCommandSupported("final_status"))
            return sendSingle(m_black, cmdLine, response);
        if (m_white.isCommandSupported("final_status"))
            return sendSingle(m_white, cmdLine, response);
        response.append("Neither player supports final_status command");
        return false;
    }

    private void findInitialGameIndex()
    {
        m_gameIndex = 0;
        try
        {
            InputStream inputStream = new FileInputStream(getResultFile());
            Reader reader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while (true)
            {
                line = bufferedReader.readLine();
                if (line == null)
                    break;
                if (line.trim().startsWith("#"))
                    continue;
                String[] elements = line.split("\\t");
                int gameIndex = Integer.parseInt(elements[0]);
                if (gameIndex + 1 > m_gameIndex)
                    m_gameIndex = gameIndex + 1;
            }
            bufferedReader.close();
        }
        catch (FileNotFoundException e)
        {
        }
        catch (IOException e)
        {
        }
    }

    private boolean gameOver()
    {
        return (m_board.bothPassed() || m_resigned);
    }

    private double getCpuTime(Gtp gtp)
    {
        double result = 0;
        try
        {
            if (gtp.isCpuTimeSupported())
                result = gtp.getCpuTime();
        }
        catch (GtpError e)
        {
        }
        return result;
    }

    private File getFile(int gameIndex)
    {
        return new File(m_sgfFile + "-" + gameIndex + ".sgf");
    }

    private static String getHost()
    {
        String host = "?";
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
        }
        return host;
    }

    private static String getName(Gtp gtp)
    {
        try
        {
            String name = gtp.sendCommand("name");
            if (! name.trim().equals(""))
                return name;
        }
        catch (GtpError e)
        {
        }
        return "Unknown";
    }

    private static String getVersion(Gtp gtp)
    {
        try
        {
            String version = gtp.sendCommand("version");
            if (! version.trim().equals(""))
                return version;
        }
        catch (GtpError e)
        {
        }
        return "";
    }

    private static String getResult(Gtp gtp)
    {
        try
        {
            return gtp.sendCommand("final_score");
        }
        catch (GtpError e)
        {
            return "?";
        }
    }

    private File getResultFile()
    {
        return new File(m_sgfFile + ".dat");
    }

    private String getTitle()
    {
        StringBuffer buffer = new StringBuffer();
        String blackName = m_blackName;
        String whiteName = m_whiteName;
        if (blackName.equals(whiteName))
        {
            if (! m_blackVersion.equals(""))
                blackName = m_blackName + ":" + m_blackVersion;
            if (! m_whiteVersion.equals(""))
                whiteName = m_whiteName + ":" + m_whiteVersion;
        }
        if (isAlternated())
        {
            String tmpName = blackName;
            blackName = whiteName;
            whiteName = tmpName;
        }
        blackName = StringUtils.capitalize(blackName);
        whiteName = StringUtils.capitalize(whiteName);
        buffer.append(blackName);
        buffer.append(" vs ");
        buffer.append(whiteName);
        if (! m_sgfFile.equals(""))
        {
            buffer.append(" (");
            buffer.append(m_gameIndex + 1);
            buffer.append(")");
        }
        return buffer.toString();
    }

    private void handleEndOfGame(boolean error, String errorMessage)
    {
        try
        {
            String resultBlack;
            String resultWhite;
            String resultReferee;
            if (m_resigned)
            {
                String result = (m_resignColor == Color.BLACK ? "W" : "B");
                result = result + "+R";
                resultBlack = result;
                resultWhite = result;
                resultReferee = result;
            }
            else
            {
                resultBlack = getResult(m_black);
                resultWhite = getResult(m_white);
                resultReferee = "?";
                if (m_referee != null && ! m_refereeIsDisabled)
                    resultReferee = getResult(m_referee);
            }
            double cpuTimeBlack = getCpuTime(m_black) - m_cpuTimeBlack;
            if (cpuTimeBlack < 0)
                cpuTimeBlack = 0;
            m_cpuTimeBlack = cpuTimeBlack;
            double cpuTimeWhite = getCpuTime(m_white) - m_cpuTimeWhite;
            if (cpuTimeWhite < 0)
                cpuTimeWhite = 0;
            m_cpuTimeWhite = cpuTimeWhite;
            if (isAlternated())
            {
                resultBlack = inverseResult(resultBlack);
                resultWhite = inverseResult(resultWhite);
                resultReferee = inverseResult(resultReferee);
            }
            Vector moves = Compare.getAllAsMoves(m_gameTree.getRoot());
            String duplicate =
                Compare.checkDuplicate(m_board, moves, m_games, m_alternate,
                                       isAlternated());
            saveResult(resultBlack, resultWhite, resultReferee,
                       isAlternated(), duplicate, moves.size(), error,
                       errorMessage, cpuTimeBlack, cpuTimeWhite);
            saveGame(resultBlack, resultWhite, resultReferee);
            saveScoreEstimates();
            ++m_gameIndex;
            m_games.add(moves);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Could not save game: " + e.getMessage());
        }
    }

    private void initGame(int size) throws Exception
    {
        m_board = new Board(size);
        m_gameTree = new GameTree(size, m_komi, null, null);
        m_currentNode = m_gameTree.getRoot();
        m_scoreEstimates.clear();
        m_resigned = false;
        if (m_openings != null)
        {
            int openingFileIndex;
            if (m_alternate)
                openingFileIndex = (m_gameIndex / 2) % m_openings.getNumber();
            else
                openingFileIndex = m_gameIndex % m_openings.getNumber();
            m_openings.loadFile(openingFileIndex);
            m_openingFile = m_openings.getFilename();
            if (m_verbose)
                System.err.println("Loaded opening " + m_openingFile);
            if (m_openings.getGameInformation().m_boardSize != size)
                throw new Exception("Wrong board size: " + m_openingFile);
            m_gameTree = m_openings.getGameTree();
            m_openingMoves = Compare.getAllAsMoves(m_gameTree.getRoot());
            m_openingMovesIndex = 0;
            Node root = m_gameTree.getRoot();
            m_currentNode = NodeUtils.getLast(root);
            if (m_loadsgf)
            {
                String command = "loadsgf " + m_openingFile;
                StringBuffer response = new StringBuffer(64);
                if (! send(m_black, m_white, command, command, command,
                           command, response, true, false))
                    throw new Exception("Loadsgf command failed: "
                                        + response);
                m_openingMovesIndex = m_openingMoves.size();
            }
        }
    }

    private String inverseResult(String result)
    {
        if (result.indexOf("B") >= 0)
            return result.replaceAll("B", "W");
        else if (result.indexOf("W") >= 0)
            return result.replaceAll("W", "B");
        else
            return result;
    }

    private boolean isAlternated()
    {
        return (m_alternate && m_gameIndex % 2 != 0);
    }

    private boolean komi(String[] cmdArray, StringBuffer response)
    {
        if (m_isKomiFixed)
        {
            response.append("Komi " + m_komi
                            + " is fixed by command line option");
            return false;
        }
        DoubleArgument doubleArgument
            = parseDoubleArgument(cmdArray, response);
        if (doubleArgument == null)
            return false;
        m_komi = doubleArgument.m_double;
        m_gameTree.getGameInformation().m_komi = m_komi;
        sendIfSupported("komi", "komi " + m_komi);
        return true;
    }

    private void mergeResponse(StringBuffer response,
                               String response1, String response2,
                               String prefix1, String prefix2)
    {
        boolean empty1 = (response1 == null || response1.equals(""));
        boolean empty2 = (response2 == null || response2.equals(""));        
        if (empty1 && empty2)
            return;
        boolean isMultiLine =
            ((response1 != null && response1.indexOf('\n') >= 0)
             || (response2 != null && response2.indexOf('\n') >= 0));
        if (! empty1)
        {
            response.append(prefix1);
            if (isMultiLine)
                response.append(":\n");
            else
                response.append(": ");
            response.append(response1);
            if (! empty2)
            {
                if (isMultiLine)
                    response.append("\n");
                else
                    response.append("  ");
            }
        }
        if (! empty2)
        {
            response.append(prefix2);
            if (isMultiLine)
                response.append(":\n");
            else
                response.append(": ");
            response.append(response2);
        }
        assert response.indexOf("\n\n") < 0;
    }

    private boolean newGame(int size, StringBuffer response)
    {
        m_cpuTimeBlack = getCpuTime(m_black);
        m_cpuTimeWhite = getCpuTime(m_white);
        try
        {
            m_black.sendCommandBoardsize(size);
            m_black.sendCommandClearBoard(size);
        }
        catch (GtpError e)
        {
            response.append("B: " + e.getMessage());
            return false;
        }
        try
        {
            m_white.sendCommandBoardsize(size);
            m_white.sendCommandClearBoard(size);
        }
        catch (GtpError e)
        {
            response.append("W: " + e.getMessage());
            m_inconsistentState = true;
            return false;
        }
        if (m_referee != null)
        {
            String commandBoardsize = m_referee.getCommandBoardsize(size);
            if (commandBoardsize != null)
                sendToReferee(commandBoardsize);
            sendToReferee(m_referee.getCommandClearBoard(size));
            m_refereeIsDisabled = false;
        }
        if (m_observer != null)
        {
            String commandBoardsize = m_observer.getCommandBoardsize(size);
            if (commandBoardsize != null)
                sendToObserver(commandBoardsize);
            sendToObserver(m_observer.getCommandClearBoard(size));
            m_observerIsDisabled = false;
        }
        m_inconsistentState = false;
        try
        {
            initGame(size);
        }
        catch (Exception e)
        {
            response.append(e.getMessage());
            return false;
        }
        m_gameSaved = false;
        sendIfSupported("komi", "komi " + m_komi);
        return true;
    }

    private void play(Move move)
    {
        m_board.play(move);        
        if (m_openings != null)
            ++m_openingMovesIndex;
        if (m_openings == null
            || m_openingMovesIndex > m_openingMoves.size())
        {
            Node node = new Node(move);
            m_currentNode.append(node);
            m_currentNode = node;
            estimateScore();
        }
    }

    private void readGames()
    {
        for (int n = 0; n < m_gameIndex; ++n)
        {
            File file = getFile(n);
            if (! file.exists())
            {
                System.err.println("Game " + file + " not found.");
                continue;
            }
            if (! file.exists())
                return;
            try
            {
                FileInputStream fileStream = new FileInputStream(file);
                sgf.Reader reader =
                    new sgf.Reader(fileStream, file.toString(), null, 0);
                Node root = reader.getGameTree().getRoot();
                m_games.add(Compare.getAllAsMoves(root));
            }
            catch (sgf.Reader.SgfError e)
            {
                System.err.println("Error reading " + file + ": " +
                                   e.getMessage());
            }
            catch (Exception e)
            {
                System.err.println("Error reading " + file + ": " +
                                   e.getMessage());
            }
        }
    }

    private void saveGame(String resultBlack, String resultWhite,
                          String resultReferee)
        throws FileNotFoundException
    {
        if (m_sgfFile.equals(""))
            return;
        String blackName = m_blackName;
        if (! m_blackVersion.equals(""))
            blackName = blackName + ":" + m_blackVersion;
        String whiteName = m_whiteName;
        if (! m_whiteVersion.equals(""))
            whiteName = whiteName + ":" + m_whiteVersion;
        String blackCommand = m_black.getProgramCommand();
        String whiteCommand = m_white.getProgramCommand();
        if (isAlternated())
        {
            blackName = m_whiteName;
            if (! m_whiteVersion.equals(""))
                blackName = blackName + ":" + m_whiteVersion;
            whiteName = m_blackName;
            if (! m_blackVersion.equals(""))
                whiteName = whiteName + ":" + m_blackVersion;
            blackCommand = m_white.getProgramCommand();
            whiteCommand = m_black.getProgramCommand();
            String resultTmp = inverseResult(resultWhite);
            resultWhite = inverseResult(resultBlack);
            resultBlack = resultTmp;
        }
        GameInformation gameInformation = m_gameTree.getGameInformation();
        gameInformation.m_playerBlack = blackName;
        gameInformation.m_playerWhite = whiteName;
        if (m_referee != null)
            gameInformation.m_result = resultReferee;
        else if (resultBlack.equals(resultWhite) && ! resultBlack.equals("?"))
            gameInformation.m_result = resultBlack;
        String host = getHost();
        String gameComment =
            "Black: " + blackCommand +
            "\nWhite: " + whiteCommand;
        if (m_openings != null)
            gameComment = gameComment +
                "\nOpening: " + m_openingFile;
        gameComment = gameComment +
            "\nResult according to Black: " + resultBlack +
            "\nResult according to White: " + resultWhite;
        if (m_referee != null)
            gameComment = gameComment +
                "\nReferee: " + m_refereeCommand +
                "\nResult according to referee: " + resultReferee;
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                           DateFormat.FULL);
        gameComment = gameComment +
            "\nHost: " + host +
            "\nDate: " + format.format(Calendar.getInstance().getTime());
        m_gameTree.getRoot().setComment(gameComment);
        File file = getFile(m_gameIndex);
        if (m_verbose)
            System.err.println("Saving " + file);
        OutputStream out = new FileOutputStream(file);
        new sgf.Writer(out, m_gameTree, file, "TwoGtp", Version.get());
    }

    private void saveResult(String resultBlack, String resultWhite,
                            String resultReferee, boolean alternated,
                            String duplicate, int numberMoves, boolean error,
                            String errorMessage, double cpuTimeBlack,
                            double cpuTimeWhite)
         throws FileNotFoundException
    {
        if (m_sgfFile.equals(""))
            return;
        File file = getResultFile();
        if (! file.exists())
        {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            PrintStream out = new PrintStream(fileOutputStream);
            String blackName = m_blackName;
            String whiteName = m_whiteName;
            if (! m_blackVersion.equals(""))
                blackName = m_blackName + ":" + m_blackVersion;
            if (! m_whiteVersion.equals(""))
                whiteName = m_whiteName + ":" + m_whiteVersion;
            String refereeName = "-";
            if (m_referee != null)
                refereeName = m_refereeName + ":" + m_refereeVersion;
            DateFormat format =
                DateFormat.getDateTimeInstance(DateFormat.FULL,
                                               DateFormat.FULL);
            Date date = Calendar.getInstance().getTime();
            out.println("# Black: " + blackName);
            out.println("# White: " + whiteName);
            out.println("# Referee: " + refereeName);
            out.println("# BlackCommand: " + m_black.getProgramCommand());
            out.println("# WhiteCommand: " + m_white.getProgramCommand());
            out.println("# RefereeCommand: " + m_refereeCommand);
            out.println("# Size: " + m_size);
            out.println("# Komi: " + m_komi);
            if (m_openings != null)
                out.println("# Openings: " + m_openings.getDirectory()
                            + " (" + m_openings.getNumber() + " files)");
            out.println("# Date: " + format.format(date));
            out.println("# Host: " + getHost());
            out.println("#GAME\tRES_B\tRES_W\tRES_R\tALT\tDUP\tLEN\tCPU_B\t"
                        + "CPU_W\tERR\tERR_MSG");
            out.close();
        }
        NumberFormat format = StringUtils.getNumberFormat(1);
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        PrintStream out = new PrintStream(fileOutputStream);
        out.println(Integer.toString(m_gameIndex) + "\t" + resultBlack + "\t"
                    + resultWhite + "\t"  + resultReferee + "\t"
                    + (alternated ? "1" : "0") + "\t" + duplicate + "\t"
                    + numberMoves + "\t" + format.format(cpuTimeBlack) + "\t"
                    + format.format(cpuTimeWhite) + "\t" +
                    (error ? "1" : "0") + "\t" + errorMessage);
        out.close();
    }

    private void saveScoreEstimates()
        throws FileNotFoundException
    {
        if (! m_estimateScore)
            return;
        File file = new File(m_sgfFile + "-" + m_gameIndex + "-score.dat");
        OutputStream fileOutputStream = new FileOutputStream(file);
        PrintStream out = new PrintStream(fileOutputStream);
        out.println("#MOVE\tBLACK\tWHITE\tREFEREE");
        for (Node node = m_gameTree.getRoot(); node != null;
             node = node.getChild())
        {
            ScoreEstimate estimate =
                (ScoreEstimate)m_scoreEstimates.get(node);
            if (estimate == null)
                continue;
            out.println(NodeUtils.getMoveNumber(node) + "\t"
                        + estimate.m_black + "\t"
                        + estimate.m_white  + "\t"
                        + estimate.m_referee);
        }
        out.close();
    }

    private boolean send(Gtp gtp1, Gtp gtp2, String command1, String command2,
                         String commandReferee, String commandObserver,
                         StringBuffer response, boolean changesState,
                         boolean tryUndo)
    {
        assert((gtp1 == m_black && gtp2 == m_white)
               || (gtp1 == m_white && gtp2 == m_black));
        if (changesState && checkInconsistentState(response))
            return false;
        String prefix1 = (gtp1 == m_black ? "B" : "W");
        String prefix2 = (gtp2 == m_black ? "B" : "W");
        String response1 = null;
        String response2 = null;
        boolean status = true;
        try
        {
            response1 = gtp1.sendCommand(command1);
        }
        catch (GtpError e)
        {
            response1 = e.getMessage();
            status = false;
            if (changesState)
            {
                mergeResponse(response, response1, response2, prefix1,
                              prefix2);
                return status;
            }
        }
        try
        {
            response2 = gtp2.sendCommand(command2);
        }
        catch (GtpError e)
        {
            response2 = e.getMessage();
            if (changesState && status)
            {
                if (tryUndo)
                {
                    try
                    {
                        gtp1.sendCommand("undo");
                    }
                    catch (GtpError errorUndo)
                    {
                        m_inconsistentState = true;
                    }
                }
                else
                    m_inconsistentState = true;
            }
            status = false;
        }
        mergeResponse(response, response1, response2, prefix1, prefix2);
        sendToReferee(commandReferee);
        sendToObserver(commandObserver);
        return status;
    }

    private boolean sendBoth(String command, StringBuffer response,
                             boolean changesState, boolean tryUndo)
    {
        return send(m_black, m_white, command, command, command, command,
                    response, changesState, tryUndo);
    }

    private boolean sendGenmove(Color color, StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        if (m_openings != null && m_openingMovesIndex < m_openingMoves.size()
            && ! m_loadsgf)
        {
            Move move = (Move)m_openingMoves.get(m_openingMovesIndex);
            if (move.getColor() != color)
            {
                response.append("Not allowed if openings are used");
                return false;
            }
            if (! sendMove(move, response))
                return false;
            response.append(Point.toString(move.getPoint()));
            return true;
        }
        Gtp gtp1;
        Gtp gtp2;
        String prefix1;
        String prefix2;
        String command;
        String command2;
        boolean alternate = isAlternated();
        if ((color == Color.BLACK && ! alternate)
            || (color == Color.WHITE && alternate))
        {
            gtp1 = m_black;
            gtp2 = m_white;
            prefix1 = "B";
            prefix2 = "W";
            command = m_black.getCommandGenmove(color);
            command2 = m_white.getCommandPlay(color);
        }
        else
        {
            gtp1 = m_white;
            gtp2 = m_black;
            prefix1 = "W";
            prefix2 = "B";
            command = m_white.getCommandGenmove(color);
            command2 = m_black.getCommandPlay(color);
        }
        String response1 = null;
        String response2 = null;
        try
        {
            response1 = gtp1.sendCommand(command);
        }
        catch (GtpError e)
        {
            response1 = e.getMessage();
            mergeResponse(response, response1, response2, prefix1, prefix2);
            return false;
        }
        if (response1.toLowerCase().equals("resign"))
        {
            response.append("resign");
            m_resigned = true;
            m_resignColor = color;
        }
        else
        {
            Point point = null;
            try
            {
                point = GtpUtils.parsePoint(response1, m_board.getSize());
            }
            catch (GtpError e)
            {
                response.append(prefix1 + " played invalid move");
                m_inconsistentState = true;
                return false;
            }
            command2 = command2 + " " + response1;
            try
            {
                gtp2.sendCommand(command2);
            }
            catch (GtpError e)
            {
                response2 = e.getMessage();
                try
                {
                    gtp1.sendCommand("undo");
                }
                catch (GtpError errorUndo)
                {
                    m_inconsistentState = true;
                }
                mergeResponse(response, response1, response2, prefix1,
                              prefix2);
                return false;
            }
            response.append(response1);
            if (m_referee != null && ! m_refereeIsDisabled)
                sendToReferee(m_referee.getCommandPlay(color) + " "
                              + response1);
            if (m_observer != null && ! m_observerIsDisabled)
                sendToObserver(m_observer.getCommandPlay(color) + " "
                              + response1);
            play(new Move(point, color));
        }
        if (gameOver() && ! m_gameSaved)
        {
            handleEndOfGame(false, "");
            m_gameSaved = true;
        }
        return true;
    }

    private void sendIfSupported(String cmd, String cmdLine)
    {
        sendIfSupported(m_black, cmd, cmdLine);
        sendIfSupported(m_white, cmd, cmdLine);
        if (m_referee != null && ! m_refereeIsDisabled)
            sendIfSupported(m_referee, cmd, cmdLine);
        if (m_observer != null && ! m_observerIsDisabled)
            sendIfSupported(m_observer, cmd, cmdLine);
    }

    private void sendIfSupported(Gtp gtp, String cmd, String cmdLine)
    {
        if (gtp.isCommandSupported(cmd))
        {
            StringBuffer response = new StringBuffer();
            sendSingle(gtp, cmdLine, response);
        }
    }

    private boolean sendMove(Move move, StringBuffer response)
    {
        String cmdBlack = m_black.getCommandPlay(move);
        String cmdWhite = m_white.getCommandPlay(move);
        String cmdReferee = null;
        if (m_referee != null)
            cmdReferee = m_referee.getCommandPlay(move);
        String cmdObserver = null;
        if (m_observer != null)
            cmdObserver = m_observer.getCommandPlay(move);
        boolean status = send(m_black, m_white, cmdBlack, cmdWhite,
                              cmdReferee, cmdObserver, response, true, true);
        if (status)
            play(move);
        return status;
    }

    private boolean sendSingle(Gtp gtp, String command, StringBuffer response)
    {
        try
        {
            response.append(gtp.sendCommand(command));
        }
        catch (GtpError e)
        {
            response.append(e.getMessage());
            return false;
        }        
        return true;
    }

    private void sendToObserver(String command)
    {
        if (m_observer == null || m_observerIsDisabled)
            return;
        try
        {
            m_observer.sendCommand(command);
        }
        catch (GtpError e)
        {
            System.err.println("Observer denied " + command + " command: "
                               + e.getMessage() + "\n" +
                               "Disabling observer for this game.");
            m_observerIsDisabled = true;
        }        
    }

    private void sendToReferee(String command)
    {
        if (m_referee == null || m_refereeIsDisabled)
            return;
        try
        {
            m_referee.sendCommand(command);
        }
        catch (GtpError e)
        {
            System.err.println("Referee denied " + command + " command: "
                               + e.getMessage() + "\n" +
                               "Disabling referee for this game.");
            m_refereeIsDisabled = true;
        }        
    }

    private boolean twogtpColor(Gtp gtp, String command,
                                StringBuffer response)
    {
        int index = command.indexOf(' ');
        if (index < 0)
        {
            response.append("Missing argument");
            return false;
        }
        command = command.substring(index).trim();
        return sendSingle(gtp, command, response);
    }

    private boolean twogtpObserver(String command, StringBuffer response)
    {
        if (m_observer == null)
        {
            response.append("No observer enabled");
            return false;
        }
        if (m_observerIsDisabled)
        {
            response.append("Observer disabled for this game");
            return false;
        }
        return twogtpColor(m_observer, command, response);
    }

    private boolean twogtpReferee(String command, StringBuffer response)
    {
        if (m_referee == null)
        {
            response.append("No referee enabled");
            return false;
        }
        if (m_refereeIsDisabled)
        {
            response.append("Referee disabled for this game");
            return false;
        }
        return twogtpColor(m_referee, command, response);
    }

    private boolean undo(StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        boolean status = sendBoth("undo", response, true, false);
        if (status)
        {
            m_board.undo();
            if (m_openings == null
                || m_openingMovesIndex > m_openingMoves.size())
                m_currentNode = m_currentNode.getFather();
            if (m_openings != null)
            {
                --m_openingMovesIndex;
                // Shouldn't happen if programs don't accept undo w/o moves
                if (m_openingMovesIndex < 0)
                    m_openingMovesIndex = 0;
            }
        }
        return status;
    }
}

//----------------------------------------------------------------------------
