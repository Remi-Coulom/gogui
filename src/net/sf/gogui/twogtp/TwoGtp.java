//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

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
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GTP adapter for playing games between two Go programs. */
public class TwoGtp
    extends GtpEngine
{
    public TwoGtp(String black, String white, String referee, String observer,
                  int size, double komi, boolean isKomiFixed, int numberGames,
                  boolean alternate, String sgfFile, boolean force,
                  boolean verbose, Openings openings, boolean loadsgf,
                  TimeSettings timeSettings)
        throws Exception
    {
        super(null);
        assert(size > 0);
        assert(size <= GoPoint.MAXSIZE);
        if (black.equals(""))
            throw new ErrorMessage("No black program set");
        if (white.equals(""))
            throw new ErrorMessage("No white program set");
        m_sgfFile = sgfFile;
        if (force)
            getResultFile().delete();
        findInitialGameIndex();
        readGames();
        m_black = new GtpClient(black, verbose, null);
        m_black.setLogPrefix("B");
        m_white = new GtpClient(white, verbose, null);
        m_white.setLogPrefix("W");
        m_refereeCommand = referee;
        if (! referee.equals(""))
        {
            m_referee = new GtpClient(referee, verbose, null);
            m_referee.setLogPrefix("R");
        }
        else
            m_referee = null;
        if (! observer.equals(""))
        {
            m_observer = new GtpClient(observer, verbose, null);
            m_observer.setLogPrefix("O");
        }
        else
            m_observer = null;
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
        else
        {
            m_refereeName = null;
            m_refereeVersion = null;
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
        m_numberGames = numberGames;
        m_openings = openings;
        m_verbose = verbose;
        m_loadsgf = loadsgf;
        m_timeSettings = timeSettings;
        initGame(size);
    }

    public void autoPlay() throws Exception
    {
        System.in.close();
        StringBuffer response = new StringBuffer(256);
        while (m_gameIndex < m_numberGames)
        {
            try
            {
                newGame(m_size);
                while (! gameOver())
                {
                    response.setLength(0);
                    sendGenmove(m_board.getToMove(), response);
                }
            }
            catch (GtpError e)
            {
                handleEndOfGame(true, e.getMessage());
            }
            if (m_black.isProgramDead())
                throw new ErrorMessage("Black program died");
            if (m_white.isProgramDead())
                throw new ErrorMessage("White program died");
        }
        m_black.send("quit");
        m_white.send("quit");
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

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        String command = cmd.getCommand();
        if (command.equals("boardsize"))
            cmdBoardSize(cmd);
        else if (command.equals("clear_board"))
            cmdClearBoard(cmd);
        else if (command.equals("final_score"))
            finalStatusCommand(cmd);
        else if (command.equals("final_status"))
            finalStatusCommand(cmd);
        else if (command.equals("final_status_list"))
            finalStatusCommand(cmd);
        else if (command.equals("gogui_interrupt"))
            ;
        else if (command.equals("gogui_title"))
            cmd.setResponse(getTitle());
        else if (command.equals("loadsgf"))
            sendBoth(cmd.getLine(), true);
        else if (command.equals("twogtp_black"))
            twogtpColor(m_black, cmd);
        else if (command.equals("twogtp_white"))
            twogtpColor(m_white, cmd);
        else if (command.equals("twogtp_referee"))
            twogtpReferee(cmd);
        else if (command.equals("twogtp_observer"))
            twogtpObserver(cmd);
        else if (command.equals("quit"))
            sendBoth("quit", false);
        else if (command.equals("play"))
            cmdPlay(cmd);
        else if (command.equals("undo"))
            cmdUndo(cmd);
        else if (command.equals("genmove"))
            cmdGenmove(cmd);
        else if (command.equals("komi"))
            komi(cmd);
        else if (command.equals("scoring_system"))
            sendIfSupported(command, cmd.getLine());
        else if (command.equals("name"))
            cmd.setResponse("TwoGtp");
        else if (command.equals("version"))
            cmd.setResponse(Version.get());
        else if (command.equals("protocol_version"))
            cmd.setResponse("2");
        else if (command.equals("list_commands"))
            cmd.setResponse("boardsize\n" +
                            "clear_board\n" +
                            "final_score\n" +
                            "final_status\n" +
                            "final_status_list\n" +
                            "genmove\n" +
                            "gogui_interrupt\n" +
                            "gogui_title\n" +
                            "komi\n" +
                            "list_commands\n" +
                            "loadsgf\n" +
                            "name\n" +
                            "play\n" +
                            "quit\n" +
                            "scoring_system\n" +
                            "time_settings\n" +
                            "twogtp_black\n" +
                            "twogtp_observer\n" +
                            "twogtp_referee\n" +
                            "twogtp_white\n" +
                            "undo\n" +
                            "version\n");
        else if (command.equals("genmove_black")
                 || command.equals("genmove_white")
                 || command.equals("black")
                 || command.equals("white")
                 || command.equals("kgs-genmove_cleanup")
                 || command.equals("genmove_cleanup"))
            throw new GtpError("unknown command");
        else if (command.equals("time_settings"))
            sendIfSupported(command, cmd.getLine());
        else
        {
            boolean isExtCommandBlack = m_black.isCommandSupported(command);
            boolean isExtCommandWhite = m_white.isCommandSupported(command);
            boolean isExtCommandReferee = false;
            if (m_referee != null && ! m_refereeIsDisabled)
                isExtCommandReferee = m_referee.isCommandSupported(command);
            boolean isExtCommandObserver = false;
            if (m_observer != null && ! m_observerIsDisabled)
                isExtCommandObserver = m_observer.isCommandSupported(command);
            if (isExtCommandBlack && ! isExtCommandObserver
                && ! isExtCommandWhite && ! isExtCommandReferee)
                forward(m_black, cmd);
            if (isExtCommandWhite && ! isExtCommandObserver
                && ! isExtCommandBlack && ! isExtCommandReferee)
                forward(m_white, cmd);
            if (isExtCommandReferee && ! isExtCommandObserver
                && ! isExtCommandBlack && ! isExtCommandWhite)
                forward(m_referee, cmd);
            if (isExtCommandObserver && ! isExtCommandReferee
                && ! isExtCommandBlack && ! isExtCommandWhite)
                forward(m_observer, cmd);
            if (! isExtCommandReferee
                && ! isExtCommandBlack
                && ! isExtCommandObserver
                && ! isExtCommandWhite)
                throw new GtpError("unknown command");
            throw new GtpError("use twogtp_black/white/referee/observer");
        }
    }

    public void interruptProgram(GtpClient gtp)
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

    private final boolean m_alternate;

    private boolean m_gameSaved;

    private boolean m_inconsistentState;

    private final boolean m_isKomiFixed;

    private final boolean m_loadsgf;

    private boolean m_observerIsDisabled;

    private boolean m_refereeIsDisabled;

    private boolean m_resigned;

    private final boolean m_verbose;

    private int m_gameIndex;

    private final int m_numberGames;

    private int m_openingMovesIndex;

    private final int m_size;

    private double m_komi;

    private double m_cpuTimeBlack;

    private double m_cpuTimeWhite;

    private Board m_board;

    private GoColor m_resignColor;

    private GameTree m_gameTree;

    private Node m_currentNode;

    private final Openings m_openings;

    private final String m_blackName;

    private final String m_blackVersion;

    private String m_openingFile;

    private final String m_refereeCommand;

    private final String m_refereeName;

    private final String m_refereeVersion;

    private final String m_sgfFile;

    private final String m_whiteName;

    private final String m_whiteVersion;

    private final ArrayList m_games = new ArrayList(100);

    private ArrayList m_openingMoves;

    private final GtpClient m_black;

    private final GtpClient m_observer;

    private final GtpClient m_referee;

    private final GtpClient m_white;

    private final TimeSettings m_timeSettings;

    private void checkInconsistentState() throws GtpError
    {
        if (m_inconsistentState)
            throw new GtpError("Inconsistent state");
    }

    private void cmdBoardSize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAXSIZE);
        if (size != m_size)
            throw new GtpError("Size must be " + m_size);
    }

    private void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        if (gamesLeft() == 0)
            throw new GtpError("Maximum number of " + m_numberGames +
                               " games reached");
        newGame(m_size);
    }

    private void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        sendGenmove(cmd.getColorArg(), cmd.getResponse());
    }

    private void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        checkInconsistentState();
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_size);
        Move move = Move.get(point, color);
        if (m_openings != null)
        {
            if (m_openingMovesIndex < m_openingMoves.size()
                && ! move.equals(getOpeningMove(m_openingMovesIndex)))
                throw new GtpError("Move not allowed if openings are used");
        }
        sendMove(move);
    }

    private void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        checkInconsistentState();
        sendBoth("undo", true);
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

    private void finalStatusCommand(GtpCommand cmd) throws GtpError
    {
        if (m_referee != null && ! m_refereeIsDisabled)
            forward(m_referee, cmd);
        else if (m_black.isCommandSupported("final_status"))
            forward(m_black, cmd);
        else if (m_white.isCommandSupported("final_status"))
            forward(m_white, cmd);
        else
            throw new GtpError("neither player supports final_status");
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

    private void forward(GtpClient gtp, GtpCommand cmd) throws GtpError
    {
        cmd.setResponse(gtp.send(cmd.getLine()));
    }

    private boolean gameOver()
    {
        return (m_board.bothPassed() || m_resigned);
    }

    private double getCpuTime(GtpClient gtp)
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

    private static String getName(GtpClient gtp)
    {
        try
        {
            String name = gtp.send("name");
            if (! name.trim().equals(""))
                return name;
        }
        catch (GtpError e)
        {
        }
        return "Unknown";
    }

    private Move getOpeningMove(int i)
    {
        return (Move)m_openingMoves.get(i);
    }

    private static String getVersion(GtpClient gtp)
    {
        try
        {
            String version = gtp.send("version");
            if (! version.trim().equals(""))
                return version;
        }
        catch (GtpError e)
        {
        }
        return "";
    }

    private static String getResult(GtpClient gtp)
    {
        try
        {
            return gtp.send("final_score");
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
        buffer.append(whiteName);
        buffer.append(" vs ");
        buffer.append(blackName);
        buffer.append(" (B)");
        if (! m_sgfFile.equals(""))
        {
            buffer.append(" (");
            buffer.append(m_gameIndex + 1);
            buffer.append(')');
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
                String result = (m_resignColor == GoColor.BLACK ? "W" : "B");
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
            double cpuTimeBlack
                = Math.max(0, getCpuTime(m_black) - m_cpuTimeBlack);
            double cpuTimeWhite
                = Math.max(0, getCpuTime(m_white) - m_cpuTimeWhite);
            m_cpuTimeBlack = cpuTimeBlack;
            m_cpuTimeWhite = cpuTimeWhite;
            if (isAlternated())
            {
                resultBlack = inverseResult(resultBlack);
                resultWhite = inverseResult(resultWhite);
                resultReferee = inverseResult(resultReferee);
            }
            ArrayList moves = Compare.getAllAsMoves(m_gameTree.getRoot());
            String duplicate =
                Compare.checkDuplicate(m_board, moves, m_games, m_alternate,
                                       isAlternated());
            // If a program is dead we wait for a few seconds, because it
            // could be because the TwoGtp process was killed and we don't
            // want to write a result in this case.
            if (m_black.isProgramDead() || m_white.isProgramDead())
            {
                try
                {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e)
                {
                    assert(false);
                }
            }
            int moveNumber = NodeUtils.getMoveNumber(m_currentNode);
            saveResult(resultBlack, resultWhite, resultReferee,
                       isAlternated(), duplicate, moveNumber, error,
                       errorMessage, cpuTimeBlack, cpuTimeWhite);
            saveGame(resultBlack, resultWhite, resultReferee);
            ++m_gameIndex;
            m_games.add(moves);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Could not save game: " + e.getMessage());
        }
    }

    private void initGame(int size) throws GtpError
    {
        m_board = new Board(size);
        m_gameTree = new GameTree(size, m_komi, null, null, null);
        m_currentNode = m_gameTree.getRoot();
        m_resigned = false;
        if (m_openings != null)
        {
            int openingFileIndex;
            if (m_alternate)
                openingFileIndex = (m_gameIndex / 2) % m_openings.getNumber();
            else
                openingFileIndex = m_gameIndex % m_openings.getNumber();
            try
            {
                m_openings.loadFile(openingFileIndex);
            }
            catch (Exception e)
            {
                throw new GtpError(e.getMessage());
            }
            m_openingFile = m_openings.getFilename();
            if (m_verbose)
                System.err.println("Loaded opening " + m_openingFile);
            if (m_openings.getGameInformation().m_boardSize != size)
                throw new GtpError("Wrong board size: " + m_openingFile);
            m_gameTree = m_openings.getGameTree();
            m_openingMoves = Compare.getAllAsMoves(m_gameTree.getRoot());
            m_openingMovesIndex = 0;
            Node root = m_gameTree.getRoot();
            m_currentNode = NodeUtils.getLast(root);
            if (m_loadsgf)
            {
                String command = "loadsgf " + m_openingFile;
                send(m_black, m_white, command, command, command,
                     command, true);
                m_openingMovesIndex = m_openingMoves.size();
            }
        }
    }

    private String inverseResult(String result)
    {
        if (result.indexOf('B') >= 0)
            return result.replaceAll("B", "W");
        else if (result.indexOf('W') >= 0)
            return result.replaceAll("W", "B");
        else
            return result;
    }

    private boolean isAlternated()
    {
        return (m_alternate && m_gameIndex % 2 != 0);
    }

    private void komi(GtpCommand cmd) throws GtpError
    {
        if (m_isKomiFixed)
            throw new GtpError("Komi " + m_komi
                               + " is fixed by command line option");
        double komi = cmd.getDoubleArg();
        m_komi = komi;
        m_gameTree.getGameInformation().m_komi = m_komi;
        sendIfSupported("komi", "komi " + m_komi);
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

    private void newGame(int size) throws GtpError
    {
        m_cpuTimeBlack = getCpuTime(m_black);
        m_cpuTimeWhite = getCpuTime(m_white);
        m_black.sendBoardsize(size);
        m_black.sendClearBoard(size);
        try
        {
            m_white.sendBoardsize(size);
            m_white.sendClearBoard(size);
        }
        catch (GtpError e)
        {
            m_inconsistentState = true;
            throw e;
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
        initGame(size);
        m_gameSaved = false;
        sendIfSupported("komi", "komi " + m_komi);
        if (m_timeSettings != null)
            sendIfSupported("time_settings",
                            GtpUtils.getTimeSettingsCommand(m_timeSettings));
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
        }
    }

    private void readGames()
    {
        for (int n = 0; n < m_gameIndex; ++n)
        {
            File file = getFile(n);
            if (! file.exists())
            {
                System.err.println("Game " + file + " not found");
                continue;
            }
            if (! file.exists())
                return;
            try
            {
                FileInputStream fileStream = new FileInputStream(file);
                SgfReader reader =
                    new SgfReader(fileStream, file.toString(), null, 0);
                Node root = reader.getGameTree().getRoot();
                m_games.add(Compare.getAllAsMoves(root));
            }
            catch (SgfReader.SgfError e)
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
            resultReferee = inverseResult(resultReferee);
        }
        GameInformation gameInformation = m_gameTree.getGameInformation();
        gameInformation.m_playerBlack = blackName;
        gameInformation.m_playerWhite = whiteName;
        if (m_referee != null)
            gameInformation.m_result = resultReferee;
        else if (resultBlack.equals(resultWhite) && ! resultBlack.equals("?"))
            gameInformation.m_result = resultBlack;
        String host = Platform.getHostInfo();
        String gameComment =
            "Black: " + blackCommand +
            "\nWhite: " + whiteCommand;
        if (m_openings != null)
            gameComment = gameComment +
                "\nOpening: " + m_openingFile;
        gameComment = gameComment +
            "\nResult[Black]: " + resultBlack +
            "\nResult[White]: " + resultWhite;
        if (m_referee != null)
            gameComment = gameComment +
                "\nReferee: " + m_refereeCommand +
                "\nResult[Referee]: " + resultReferee;
        gameComment = gameComment +
            "\nHost: " + host +
            "\nDate: " + StringUtils.getDate();
        m_gameTree.getRoot().setComment(gameComment);
        File file = getFile(m_gameIndex);
        if (m_verbose)
            System.err.println("Saving " + file);
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, m_gameTree, "TwoGtp", Version.get());
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
            out.println("# Date: " + StringUtils.getDate());
            out.println("# Host: " + Platform.getHostInfo());
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

    private void send(GtpClient gtp1, GtpClient gtp2, String command1,
                      String command2, String commandReferee,
                      String commandObserver, boolean changesState)
        throws GtpError
    {
        assert((gtp1 == m_black && gtp2 == m_white)
               || (gtp1 == m_white && gtp2 == m_black));
        if (changesState)
            checkInconsistentState();
        try
        {
            gtp1.send(command1);
        }
        catch (GtpError e)
        {
            if (changesState)
                throw e;
        }
        try
        {
            gtp2.send(command2);
        }
        catch (GtpError e)
        {
            if (changesState)
                m_inconsistentState = true;
            throw e;
        }
        sendToReferee(commandReferee);
        sendToObserver(commandObserver);
    }

    private void sendBoth(String command, boolean changesState)
        throws GtpError
    {
        send(m_black, m_white, command, command, command, command,
             changesState);
    }

    private void sendGenmove(GoColor color, StringBuffer response)
        throws GtpError
    {
        checkInconsistentState();
        if (m_openings != null && m_openingMovesIndex < m_openingMoves.size()
            && ! m_loadsgf)
        {
            Move move = getOpeningMove(m_openingMovesIndex);
            if (move.getColor() != color)
                throw new GtpError("Not allowed if openings are used");
            sendMove(move);
            response.append(GoPoint.toString(move.getPoint()));
            return;
        }
        GtpClient gtp1;
        GtpClient gtp2;
        String prefix1;
        String prefix2;
        String command;
        boolean exchangeColors =
            (color == GoColor.BLACK && isAlternated())
            || (color == GoColor.WHITE && ! isAlternated());
        if (exchangeColors)
        {
            gtp1 = m_white;
            gtp2 = m_black;
            prefix1 = "W";
            prefix2 = "B";
            command = m_white.getCommandGenmove(color);
        }
        else
        {
            gtp1 = m_black;
            gtp2 = m_white;
            prefix1 = "B";
            prefix2 = "W";
            command = m_black.getCommandGenmove(color);
        }
        String response1 = null;
        String response2 = null;
        response1 = gtp1.send(command);
        if (response1.equalsIgnoreCase("resign"))
        {
            response.append("resign");
            m_resigned = true;
            m_resignColor = color;
        }
        else
        {
            GoPoint point = null;
            try
            {
                point = GtpUtils.parsePoint(response1, m_board.getSize());
            }
            catch (GtpError e)
            {
                m_inconsistentState = true;
                throw new GtpError(prefix1 + " played invalid move");
            }
            Move move = Move.get(point, color);
            String command2 = gtp2.getCommandPlay(move);
            try
            {
                gtp2.send(command2);
            }
            catch (GtpError e)
            {
                response2 = e.getMessage();
                try
                {
                    gtp1.send("undo");
                }
                catch (GtpError errorUndo)
                {
                    m_inconsistentState = true;
                }
                mergeResponse(response, response1, response2, prefix1,
                              prefix2);
                throw new GtpError(response.toString());
            }
            response.append(response1);
            if (m_referee != null && ! m_refereeIsDisabled)
                sendToReferee(m_referee.getCommandPlay(move));
            if (m_observer != null && ! m_observerIsDisabled)
                sendToObserver(m_observer.getCommandPlay(move));
            play(move);
        }
        if (gameOver() && ! m_gameSaved)
        {
            handleEndOfGame(false, "");
            m_gameSaved = true;
        }
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

    private void sendIfSupported(GtpClient gtp, String cmd, String cmdLine)
    {
        if (! gtp.isCommandSupported(cmd))
            return;
        try
        {
            gtp.send(cmdLine);
        }
        catch (GtpError e)
        {
        }
    }

    private void sendMove(Move move) throws GtpError
    {
        String cmdBlack = m_black.getCommandPlay(move);
        String cmdWhite = m_white.getCommandPlay(move);
        String cmdReferee = null;
        if (m_referee != null)
            cmdReferee = m_referee.getCommandPlay(move);
        String cmdObserver = null;
        if (m_observer != null)
            cmdObserver = m_observer.getCommandPlay(move);
        send(m_black, m_white, cmdBlack, cmdWhite, cmdReferee, cmdObserver,
             true);
        play(move);
    }

    private void sendToObserver(String command)
    {
        if (m_observer == null || m_observerIsDisabled)
            return;
        try
        {
            m_observer.send(command);
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
            m_referee.send(command);
        }
        catch (GtpError e)
        {
            System.err.println("Referee denied " + command + " command: "
                               + e.getMessage() + "\n" +
                               "Disabling referee for this game.");
            m_refereeIsDisabled = true;
        }        
    }

    private void twogtpColor(GtpClient gtp, GtpCommand cmd) throws GtpError
    {
        cmd.setResponse(gtp.send(cmd.getArgLine()));
    }

    private void twogtpObserver(GtpCommand cmd) throws GtpError
    {
        if (m_observer == null)
            throw new GtpError("No observer enabled");
        if (m_observerIsDisabled)
            throw new GtpError("Observer disabled for this game");
        twogtpColor(m_observer, cmd);
    }

    private void twogtpReferee(GtpCommand cmd) throws GtpError
    {
        if (m_referee == null)
            throw new GtpError("No referee enabled");
        if (m_refereeIsDisabled)
            throw new GtpError("Referee disabled for this game");
        twogtpColor(m_referee, cmd);
    }
}

//----------------------------------------------------------------------------
