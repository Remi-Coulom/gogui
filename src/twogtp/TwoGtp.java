//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package twogtp;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import game.*;
import go.*;
import gtp.*;
import utils.*;
import version.*;

//----------------------------------------------------------------------------

public class TwoGtp
    extends GtpServer
{
    public TwoGtp(InputStream in, OutputStream out, String black, String white,
                  String referee, int size, Float komi, int numberGames,
                  boolean alternate, String sgfFile, boolean force,
                  boolean verbose, boolean estimateScore)
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
        m_size = size;
        m_komi = komi;
        m_alternate = alternate;
        m_estimateScore = estimateScore;
        m_numberGames = numberGames;
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
                while (! m_board.bothPassed())
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
        m_black.waitForExit();
        m_white.waitForExit();
        if (m_referee != null)
        {
            m_referee.close();
            m_referee.waitForExit();
        }
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
            komi(cmdLine);
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
            if (isExtCommandBlack
                && ! isExtCommandWhite && ! isExtCommandReferee)
                return sendSingle(m_black, cmdLine, response);
            if (isExtCommandWhite
                && ! isExtCommandBlack && ! isExtCommandReferee)
                return sendSingle(m_white, cmdLine, response);
            if (isExtCommandReferee
                && ! isExtCommandBlack && ! isExtCommandWhite)
                return sendSingle(m_referee, cmdLine, response);
            if (! isExtCommandReferee
                && ! isExtCommandBlack
                && ! isExtCommandWhite)
            {
                response.append("unknown command");
                return false;
            }
            response.append("use twogtp_black/white/referee");
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
        catch (Gtp.Error e)
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
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "alternate",
                "analyze:",
                "auto",
                "black:",
                "compare",
                "config:",
                "estimate-score",
                "force",
                "games:",
                "help",
                "komi:",
                "referee:",
                "sgffile:",
                "size:",
                "verbose",
                "version",
                "white:"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar twogtp.jar [options]\n" +
                    "\n" +
                    "-alternate      alternate colors\n" +
                    "-analyze file   analyze result file\n" +
                    "-auto           autoplay games\n" +
                    "-black          command for black program\n" +
                    "-compare        compare list of sgf files\n" +
                    "-config         config file\n" +
                    "-estimate-score send estimate_score to programs\n" +
                    "-force          overwrite existing files\n" +
                    "-games          number of games (0=unlimited)\n" +
                    "-help           display this help and exit\n" +
                    "-komi           komi\n" +
                    "-referee        command for referee program\n" +
                    "-sgffile        filename prefix\n" +
                    "-size           board size for autoplay (default 19)\n" +
                    "-verbose        log GTP streams to stderr\n" +
                    "-version        print version and exit\n" +
                    "-white          command for white program\n";
                System.out.print(helpText);
                System.exit(0);
            }
            boolean compare = opt.isSet("compare");
            if (compare)
            {
                compare(opt.getArguments());
                System.exit(0);
            }
            boolean force = opt.isSet("force");
            if (opt.isSet("version"))
            {
                System.out.println("TwoGtp " + Version.get());
                System.exit(0);
            }
            if (opt.contains("analyze"))
            {
                String filename = opt.getString("analyze");
                new Analyze(filename, force);
                return;
            }                
            boolean alternate = opt.isSet("alternate");
            boolean estimateScore = opt.isSet("estimate-score");
            boolean auto = opt.isSet("auto");
            boolean verbose = opt.isSet("verbose");
            String black = opt.getString("black", "");
            String white = opt.getString("white", "");
            String referee = opt.getString("referee", "");
            int size = opt.getInteger("size", 0, 0);
            Float komi = new Float(0f);
            if (opt.isSet("komi"))
                komi = new Float(opt.getFloat("komi"));
            else if (auto)
                komi = new Float(6.5);
            int defaultGames = (auto ? 1 : 0);
            int games = opt.getInteger("games", defaultGames, 0);
            String sgfFile = opt.getString("sgffile", "");
            if (opt.isSet("games") && sgfFile.equals(""))
                throw new Exception("Use option -sgffile with -games.");
            TwoGtp twoGtp =
                new TwoGtp(System.in, System.out, black, white, referee, size,
                           komi, games, alternate, sgfFile, force, verbose,
                           estimateScore);
            if (auto)
            {
                if (twoGtp.gamesLeft() == 0)
                    System.err.println("Already " + games + " games played.");
                twoGtp.autoPlay();
            }
            else
                twoGtp.mainLoop();
            twoGtp.close();
        }
        catch (Error e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            System.err.println(StringUtils.formatException(t));
            System.exit(-1);
        }
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

    private boolean m_refereeIsDisabled;

    private int m_gameIndex;

    private int m_numberGames;

    private int m_size;

    private double m_cpuTimeBlack;

    private double m_cpuTimeWhite;

    private Board m_board;

    private GameTree m_gameTree;

    private Node m_currentNode;

    private String m_blackName;

    private String m_blackVersion;

    private String m_refereeCommand;

    private String m_refereeName;

    private String m_refereeVersion;

    private String m_sgfFile;

    private String m_whiteName;

    private String m_whiteVersion;

    private HashMap m_scoreEstimates = new HashMap();

    private Float m_komi;

    private Vector m_games = new Vector(100, 100);;

    private Gtp m_black;

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

    private static String checkDuplicate(Board board, Vector moves,
                                         Vector games)
    {
        String result = "-";
        for (int numberGame = 0; numberGame < games.size(); ++numberGame)
        {
            Vector gameMoves = (Vector)games.get(numberGame);
            for (int rot = 0; rot < Board.NUMBER_ROTATIONS; ++rot)
            {
                int numberDifferent = 0;
                int moveNumber = moves.size();
                final int maxDifferent = moveNumber / 5;
                if (gameMoves.size() != moveNumber)
                {
                    numberDifferent = Math.abs(gameMoves.size() - moveNumber);
                    moveNumber = Math.min(gameMoves.size(), moveNumber);
                }
                for (int i = 0;
                     numberDifferent <= maxDifferent && i < moveNumber; ++i)
                {
                    Move move = (Move)moves.get(i);
                    Point point = move.getPoint();
                    Color color = move.getColor();
                    Move gameMove = (Move)gameMoves.get(i);
                    Point gamePoint = board.rotate(rot, gameMove.getPoint());
                    Color gameColor = gameMove.getColor();                    
                    if (! color.equals(gameColor)
                        || ! Point.equals(point, gamePoint))
                        ++numberDifferent;
                }
                if (numberDifferent == 0)
                    return Integer.toString(numberGame);
                else if (numberDifferent < maxDifferent)
                    result = Integer.toString(numberGame) + "?";
            }
        }
        return result;
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
            point = Gtp.parsePoint(cmdArray[1], m_board.getSize());
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }
        Move move = new Move(point, color);
        String cmdBlack = m_black.getCommandPlay(move);
        String cmdWhite = m_white.getCommandPlay(move);
        String cmdReferee = null;
        if (m_referee != null)
            cmdReferee = m_referee.getCommandPlay(move);
        boolean status =
            send(m_black, m_white, cmdBlack, cmdWhite, cmdReferee, response,
                 true, true);
        if (status)
            play(move);
        return status;
    }

    private static void compare(Vector filenames) throws Exception
    {
        Board board = null;
        Vector games = new Vector();
        for (int gameNumber = 0; gameNumber < filenames.size(); ++gameNumber)
        {
            String filename = (String)filenames.get(gameNumber);
            File file = new File(filename);
            sgf.Reader reader =
                new sgf.Reader(new FileReader(file), file.toString());
            GameTree gameTree = reader.getGameTree();
            GameInformation gameInformation = gameTree.getGameInformation();
            int size = gameInformation.m_boardSize;
            if (board == null)
                board = new Board(size);
            else if (size != board.getSize())
                throw new Exception("Board size in " + filename +
                                    " does not match other games");
            Vector moves = getMoves(gameTree.getRoot(), filename);
            String duplicate = checkDuplicate(board, moves, games);
            System.out.println(Integer.toString(gameNumber) + " " +
                               filename + " " + duplicate);
            games.add(moves);
        }
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
        else
            return sendEither(cmdLine, response);
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

    private double getCpuTime(Gtp gtp)
    {
        double result = 0;
        try
        {
            if (gtp.isCpuTimeSupported())
                result = gtp.getCpuTime();
        }
        catch (Gtp.Error e)
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

    private static Vector getMoves(Node root, String filename)
    {
        Vector moves = new Vector();
        Node node = root;
        while (node != null)
        {
            if (node.getNumberAddBlack() + node.getNumberAddWhite() > 0)
                throw new RuntimeException("File " + filename +
                                           " contains setup stones");
            if (node.getMove() != null)
                moves.add(node.getMove());
            node = node.getChild();
        }
        return moves;
    }

    private static String getName(Gtp gtp)
    {
        try
        {
            String name = gtp.sendCommand("name");
            if (! name.trim().equals(""))
                return name;
        }
        catch (Gtp.Error e)
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
        catch (Gtp.Error e)
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
        catch (Gtp.Error e)
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
            String resultBlack = getResult(m_black);
            String resultWhite = getResult(m_white);
            String resultReferee = "?";
            if (m_referee != null && ! m_refereeIsDisabled)
                resultReferee = getResult(m_referee);
            double cpuTimeBlack = getCpuTime(m_black) - m_cpuTimeBlack;
            m_cpuTimeBlack = cpuTimeBlack;
            double cpuTimeWhite = getCpuTime(m_white) - m_cpuTimeWhite;
            m_cpuTimeWhite = cpuTimeWhite;
            if (isAlternated())
            {
                resultBlack = inverseResult(resultBlack);
                resultWhite = inverseResult(resultWhite);
                resultReferee = inverseResult(resultReferee);
            }
            Vector moves = getMoves(m_gameTree.getRoot(), null);
            String duplicate = checkDuplicate(m_board, moves, m_games);
            saveResult(resultBlack, resultWhite, resultReferee, isAlternated(),
                       duplicate, moves.size(), error, errorMessage,
                       cpuTimeBlack, cpuTimeWhite);
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

    private void initGame(int size)
    {
        m_board = new Board(size);
        m_gameTree = new GameTree(size, m_komi.floatValue(), null, null);
        m_currentNode = m_gameTree.getRoot();
        m_scoreEstimates.clear();
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

    private void komi(String cmdLine)
    {
        if (m_komi != null)
            return;
        sendIfSupported("komi", cmdLine);
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
        catch (Gtp.Error e)
        {
            response.append("B: " + e.getMessage());
            return false;
        }
        try
        {
            m_white.sendCommandBoardsize(size);
            m_white.sendCommandClearBoard(size);
        }
        catch (Gtp.Error e)
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
        m_inconsistentState = false;
        initGame(size);
        m_gameSaved = false;
        if (m_komi != null)
            sendIfSupported("komi", "komi " + m_komi.floatValue());
        return true;
    }

    private void play(Move move)
    {
        m_board.play(move);
        Node node = new Node(move);
        m_currentNode.append(node);
        m_currentNode = node;
        estimateScore();
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
                sgf.Reader reader =
                    new sgf.Reader(new FileReader(file), file.toString());
                m_games.add(getMoves(reader.getGameTree().getRoot(),
                                     file.toString()));
            }
            catch (sgf.Reader.Error e)
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
            "\nWhite: " + whiteCommand +
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
                    + (alternated ? "1" : "0" ) + "\t" + duplicate + "\t"
                    + numberMoves + "\t" + format.format(cpuTimeBlack) + "\t" +
                    format.format(cpuTimeWhite) + "\t" +
                    (error ? "1" : "0" ) + "\t" + errorMessage);
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
            ScoreEstimate estimate = (ScoreEstimate)m_scoreEstimates.get(node);
            if (estimate == null)
                continue;
            out.println(node.getMoveNumber() + "\t" + estimate.m_black + "\t"
                        + estimate.m_white  + "\t" + estimate.m_referee);
        }
        out.close();
    }

    private boolean send(Gtp gtp1, Gtp gtp2, String command1, String command2,
                         String commandReferee, StringBuffer response,
                         boolean changesState, boolean tryUndo)
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
        catch (Gtp.Error e)
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
        catch (Gtp.Error e)
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
                    catch (Gtp.Error errorUndo)
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
        return status;
    }

    private boolean sendBoth(String command, StringBuffer response,
                             boolean changesState, boolean tryUndo)
    {
        return send(m_black, m_white, command, command, command, response,
                    changesState, tryUndo);
    }

    private boolean sendEither(String command, StringBuffer response)
    {
        if (sendSingle(m_black, command, response))
            return true;
        response.setLength(0);
        return sendSingle(m_white, command, response);
    }

    private boolean sendGenmove(Color color, StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
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
        catch (Gtp.Error e)
        {
            response1 = e.getMessage();
            mergeResponse(response, response1, response2, prefix1,
                          prefix2);
            return false;
        }
        Point point = null;
        try
        {
            point = Gtp.parsePoint(response1, m_board.getSize());
        }
        catch (Gtp.Error e)
        {
            response.append(prefix1 + " played invalid move");
            m_inconsistentState = true;
            return false;
        }
        command2 = command2 + " " + response1;
        try
        {
            response2 = gtp2.sendCommand(command2);
        }
        catch (Gtp.Error e)
        {
            response2 = e.getMessage();
            try
            {
                gtp1.sendCommand("undo");
            }
            catch (Gtp.Error errorUndo)
            {
                m_inconsistentState = true;
            }
            mergeResponse(response, response1, response2, prefix1, prefix2);
            return false;
        }
        response.append(response1);
        if (m_referee != null && ! m_refereeIsDisabled)
            sendToReferee(m_referee.getCommandPlay(color) + " " + response1);
        play(new Move(point, color));
        if (m_board.bothPassed() && ! m_gameSaved)
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
    }

    private void sendIfSupported(Gtp gtp, String cmd, String cmdLine)
    {
        if (gtp.isCommandSupported(cmd))
        {
            StringBuffer response = new StringBuffer();
            sendSingle(gtp, cmdLine, response);
        }
    }

    private boolean sendSingle(Gtp gtp, String command, StringBuffer response)
    {
        try
        {
            response.append(gtp.sendCommand(command));
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }        
        return true;
    }

    private void sendToReferee(String command)
    {
        if (m_referee == null || m_refereeIsDisabled)
            return;
        try
        {
            m_referee.sendCommand(command);
        }
        catch (Gtp.Error e)
        {
            System.err.println("Referee denied " + command + " command: "
                               + e.getMessage() + "\n" +
                               "Disabling referee for this game.");
            m_refereeIsDisabled = true;
        }        
    }

    private boolean twogtpColor(Gtp gtp, String command, StringBuffer response)
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
            m_currentNode = m_currentNode.getFather();
        }
        return status;
    }
}

//----------------------------------------------------------------------------
