// TwoGtp.java

package net.sf.gogui.tools.twogtp;

import java.util.ArrayList;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.BlackWhiteSet;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** GTP adapter for playing games between two Go programs. */
public class TwoGtp
    extends GtpEngine
{
    /** Constructor.
        @param komi The fixed komi. See TwoGtp documentation for option
        -komi */
    public TwoGtp(Program black, Program white, Program referee,
                  String observer, int size, Komi komi, int numberGames,
                  boolean alternate, String filePrefix, boolean verbose,
                  Openings openings, TimeSettings timeSettings,
                  ResultFile resultFile)
        throws Exception
    {
        super(null);
        assert size > 0;
        assert size <= GoPoint.MAX_SIZE;
        assert komi != null;
        m_filePrefix = filePrefix;
        m_allPrograms = new ArrayList<Program>();
        m_black = black;
        m_allPrograms.add(m_black);
        m_white = white;
        m_allPrograms.add(m_white);
        m_referee = referee;
        if (m_referee != null)
            m_allPrograms.add(m_referee);
        if (observer.equals(""))
            m_observer = null;
        else
        {
            m_observer = new Program(observer, "Observer", "O", verbose);
            m_allPrograms.add(m_observer);
        }
        for (Program program : m_allPrograms)
            program.setLabel(m_allPrograms);
        m_size = size;
        m_komi = komi;
        m_alternate = alternate;
        m_numberGames = numberGames;
        m_openings = openings;
        m_verbose = verbose;
        m_timeSettings = timeSettings;
        m_resultFile = resultFile;
        initGame(size);
    }

    public void autoPlay() throws Exception
    {
        StringBuilder response = new StringBuilder(256);
        while (true)
        {
            try
            {
                newGame(m_size);
                while (! gameOver())
                {
                    response.setLength(0);
                    sendGenmove(getToMove(), response);
                }
            }
            catch (GtpError e)
            {
                if (m_gameIndex == -1)
                    break;
                handleEndOfGame(true, e.getMessage());
            }
        }
        if (m_black.isProgramDead())
            throw new ErrorMessage("Black program died");
        if (m_white.isProgramDead())
            throw new ErrorMessage("White program died");
    }

    public void close()
    {
        for (Program program : m_allPrograms)
            program.close();
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
        else if (command.equals("gogui-interrupt"))
            ;
        else if (command.equals("gogui-title"))
            cmd.setResponse(getTitle());
        else if (command.equals("gogui-twogtp-black"))
            twogtpColor(m_black, cmd);
        else if (command.equals("gogui-twogtp-white"))
            twogtpColor(m_white, cmd);
        else if (command.equals("gogui-twogtp-referee"))
            twogtpReferee(cmd);
        else if (command.equals("gogui-twogtp-observer"))
            twogtpObserver(cmd);
        else if (command.equals("quit"))
        {
            close();
            setQuit();
        }
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
            cmd.setResponse("gogui-twogtp");
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
                            "gogui-interrupt\n" +
                            "gogui-title\n" +
                            "komi\n" +
                            "list_commands\n" +
                            "name\n" +
                            "play\n" +
                            "quit\n" +
                            "scoring_system\n" +
                            "time_settings\n" +
                            "gogui-twogtp-black\n" +
                            "gogui-twogtp-observer\n" +
                            "gogui-twogtp-referee\n" +
                            "gogui-twogtp-white\n" +
                            "undo\n" +
                            "version\n");
        else if (GtpUtil.isStateChangingCommand(command))
            throw new GtpError("unknown command");
        else if (command.equals("time_settings"))
            sendIfSupported(command, cmd.getLine());
        else
        {
            boolean isExtCommandBlack = m_black.isSupported(command);
            boolean isExtCommandWhite = m_white.isSupported(command);
            boolean isExtCommandReferee = false;
            if (m_referee != null)
                isExtCommandReferee = m_referee.isSupported(command);
            boolean isExtCommandObserver = false;
            if (m_observer != null)
                isExtCommandObserver = m_observer.isSupported(command);
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
            throw new GtpError("use gogui-twogtp-black/white/referee/observer");
        }
    }

    public void interruptCommand()
    {
        for (Program program : m_allPrograms)
            program.interruptProgram();
    }

    /** Store stderr of programs during move generation in SGF comments. */
    public void setDebugToComment(boolean enable)
    {
        m_black.setIOCallback(null);
        m_white.setIOCallback(null);
        m_debugToComment = enable;
        if (m_debugToComment)
        {
            m_black.setIOCallback(new GtpClient.IOCallback()
                {
                    public void receivedInvalidResponse(String s) { }

                    public void receivedResponse(boolean error, String s) { }

                    public void receivedStdErr(String s)
                    {
                        appendDebugToCommentBuffer(BLACK, s);
                    }

                    public void sentCommand(String s) { }
                });
            m_white.setIOCallback(new GtpClient.IOCallback()
                {
                    public void receivedInvalidResponse(String s) { }

                    public void receivedResponse(boolean error, String s) { }

                    public void receivedStdErr(String s)
                    {
                        appendDebugToCommentBuffer(WHITE, s);
                    }

                    public void sentCommand(String s) { }
                });
        }
    }

    /** Limit number of moves.
        @param maxMoves Maximum number of moves after which genmove will fail,
        -1 for no limit. */
    public void setMaxMoves(int maxMoves)
    {
        m_maxMoves = maxMoves;
    }

    private final boolean m_alternate;

    private boolean m_gameSaved;

    private boolean m_debugToComment;

    private int m_maxMoves = 1000;

    private int m_gameIndex;

    private boolean m_resigned;

    private final boolean m_verbose;

    private final int m_numberGames;

    private final int m_size;

    /** Fixed komi. */
    private final Komi m_komi;

    private Game m_game;

    private GoColor m_resignColor;

    private final Openings m_openings;

    private final Program m_black;

    private final Program m_white;

    private final Program m_referee;

    private final Program m_observer;

    private final ArrayList<Program> m_allPrograms;

    private final BlackWhiteSet<Double> m_realTime =
        new BlackWhiteSet<Double>(0., 0.);

    private String m_openingFile;

    private final String m_filePrefix;

    private final ArrayList<ArrayList<Compare.Placement>> m_games
        = new ArrayList<ArrayList<Compare.Placement>>(100);

    private ResultFile m_resultFile;

    private final TimeSettings m_timeSettings;

    private ConstNode m_lastOpeningNode;

    /** Buffers for stderr of programs if setDebugToComment() is used.
        This member is used by two threads. Access only through synchronized
        functions. */
    private BlackWhiteSet<StringBuilder> m_debugToCommentBuffer =
        new BlackWhiteSet<StringBuilder>(new StringBuilder(),
                                         new StringBuilder());

    private synchronized void appendDebugToCommentBuffer(GoColor c, String s)
    {
        m_debugToCommentBuffer.get(c).append(s);
    }

    private void checkInconsistentState() throws GtpError
    {
        for (Program program : m_allPrograms)
            if (program.isOutOfSync())
                throw new GtpError("Inconsistent state");
    }

    private synchronized void clearDebugToCommentBuffers()
    {
        m_debugToCommentBuffer.get(BLACK).setLength(0);
        m_debugToCommentBuffer.get(WHITE).setLength(0);
    }

    private void cmdBoardSize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAX_SIZE);
        if (size != m_size)
            throw new GtpError("Size must be " + m_size);
    }

    private void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        newGame(m_size);
    }

    private void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        try
        {
            sendGenmove(cmd.getColorArg(), cmd.getResponse());
        }
        catch (ErrorMessage e)
        {
            throw new GtpError(e.getMessage());
        }
    }

    private void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        checkInconsistentState();
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_size);
        Move move = Move.get(color, point);
        m_game.play(move);
        synchronize();
    }

    private void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        int moveNumber = m_game.getMoveNumber();
        if (moveNumber == 0)
            throw new GtpError("cannot undo");
        m_game.gotoNode(getCurrentNode().getFatherConst());
        assert m_game.getMoveNumber() == moveNumber - 1;
        synchronize();
    }

    private void finalStatusCommand(GtpCommand cmd) throws GtpError
    {
        checkInconsistentState();
        if (m_referee != null)
            forward(m_referee, cmd);
        else if (m_black.isSupported("final_status"))
            forward(m_black, cmd);
        else if (m_white.isSupported("final_status"))
            forward(m_white, cmd);
        else
            throw new GtpError("neither player supports final_status");
    }

    private void forward(Program program, GtpCommand cmd) throws GtpError
    {
        cmd.setResponse(program.send(cmd.getLine()));
    }

    private boolean gameOver()
    {
        return (getBoard().bothPassed() || m_resigned);
    }

    private ConstBoard getBoard()
    {
        return m_game.getBoard();
    }

    private ConstNode getCurrentNode()
    {
        return m_game.getCurrentNode();
    }

    private synchronized String getDebugToCommentBuffer(GoColor color)
    {
        return m_debugToCommentBuffer.get(color).toString();
    }

    private GoColor getToMove()
    {
        return m_game.getToMove();
    }

    private ConstGameTree getTree()
    {
        return m_game.getTree();
    }

    private String getTitle()
    {
        StringBuilder buffer = new StringBuilder();
        String nameBlack = m_black.getLabel();
        String nameWhite = m_white.getLabel();
        if (isAlternated())
        {
            String tmpName = nameBlack;
            nameBlack = nameWhite;
            nameWhite = tmpName;
        }
        buffer.append(nameWhite);
        buffer.append(" vs ");
        buffer.append(nameBlack);
        buffer.append(" (B)");
        if (! m_filePrefix.equals(""))
        {
            buffer.append(" (");
            buffer.append(m_gameIndex + 1);
            buffer.append(')');
        }
        return buffer.toString();
    }

    private void handleEndOfGame(boolean error, String errorMessage)
        throws ErrorMessage
    {
        String resultBlack;
        String resultWhite;
        String resultReferee;
        if (m_resigned)
        {
            String result = (m_resignColor == BLACK ? "W" : "B");
            result = result + "+R";
            resultBlack = result;
            resultWhite = result;
            resultReferee = result;
        }
        else
        {
            resultBlack = m_black.getResult();
            resultWhite = m_white.getResult();
            resultReferee = "?";
            if (m_referee != null)
                resultReferee = m_referee.getResult();
        }
        double cpuTimeBlack = m_black.getAndClearCpuTime();
        double cpuTimeWhite = m_white.getAndClearCpuTime();
        double realTimeBlack = m_realTime.get(BLACK);
        double realTimeWhite = m_realTime.get(WHITE);
        if (isAlternated())
        {
            resultBlack = inverseResult(resultBlack);
            resultWhite = inverseResult(resultWhite);
            resultReferee = inverseResult(resultReferee);
            realTimeBlack = m_realTime.get(WHITE);
            realTimeWhite = m_realTime.get(BLACK);
        }
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
                assert false;
            }
        }

        String nameBlack = m_black.getLabel();
        String nameWhite = m_white.getLabel();
        String blackCommand = m_black.getProgramCommand();
        String whiteCommand = m_white.getProgramCommand();
        String blackVersion = m_black.getVersion();
        String whiteVersion = m_white.getVersion();
        if (isAlternated())
        {
            nameBlack = m_white.getLabel();
            nameWhite = m_black.getLabel();
            blackCommand = m_white.getProgramCommand();
            whiteCommand = m_black.getProgramCommand();
            blackVersion = m_white.getVersion();
            whiteVersion = m_black.getVersion();
        }
        m_game.setPlayer(BLACK, nameBlack);
        m_game.setPlayer(WHITE, nameWhite);
        if (m_referee != null)
            m_game.setResult(resultReferee);
        else if (resultBlack.equals(resultWhite) && ! resultBlack.equals("?"))
            m_game.setResult(resultBlack);
        String host = Platform.getHostInfo();
        StringBuilder comment = new StringBuilder();
        comment.append("Black command: ");
        comment.append(blackCommand);
        comment.append("\nWhite command: ");
        comment.append(whiteCommand);
        comment.append("\nBlack version: ");
        comment.append(blackVersion);
        comment.append("\nWhite version: ");
        comment.append(whiteVersion);
        if (m_openings != null)
        {
            comment.append("\nOpening: ");
            comment.append(m_openingFile);
        }
        comment.append("\nResult[Black]: ");
        comment.append(resultBlack);
        comment.append("\nResult[White]: ");
        comment.append(resultWhite);
        if (m_referee != null)
        {
            comment.append("\nReferee: ");
            comment.append(m_referee.getProgramCommand());
            comment.append("\nResult[Referee]: ");
            comment.append(resultReferee);
        }
        comment.append("\nHost: ");
        comment.append(host);
        comment.append("\nDate: ");
        comment.append(StringUtil.getDate());
        m_game.setComment(comment.toString(), getTree().getRootConst());
        int moveNumber = NodeUtil.getMoveNumber(getCurrentNode());
        if (m_resultFile != null)
            m_resultFile.addResult(m_gameIndex, m_game, resultBlack,
                                   resultWhite, resultReferee, isAlternated(),
                                   moveNumber, error, errorMessage,
                                   realTimeBlack, realTimeWhite, cpuTimeBlack,
                                   cpuTimeWhite);
    }

    private void initGame(int size) throws GtpError
    {
        m_game = new Game(size, m_komi, null, null, null);
        m_realTime.set(BLACK, 0.);
        m_realTime.set(WHITE, 0.);
        // Clock is not needed
        m_game.haltClock();
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
            if (m_openings.getBoardSize() != size)
                throw new GtpError("Wrong board size: " + m_openingFile);
            m_game.init(m_openings.getTree());
            m_game.setKomi(m_komi);
            m_lastOpeningNode = NodeUtil.getLast(getTree().getRootConst());
            // TODO: Check that root node contains no setup stones, if
            // TwoGtp is run as a GTP engine, see also comment in sendGenmove()
        }
        else
            m_lastOpeningNode = null;
        synchronizeInit();
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

    private boolean isInOpening()
    {
        if (m_lastOpeningNode == null)
            return false;
        for (ConstNode node = getCurrentNode().getChildConst(); node != null;
             node = node.getChildConst())
            if (node == m_lastOpeningNode)
                return true;
        return false;
    }

    private void komi(GtpCommand cmd) throws GtpError
    {
        String arg = cmd.getArg();
        try
        {
            Komi komi = Komi.parseKomi(arg);
            if (! ObjectUtil.equals(komi, m_komi))
                throw new GtpError("komi is fixed at " + m_komi);
        }
        catch (InvalidKomiException e)
        {
            throw new GtpError("invalid komi: " + arg);
        }
    }

    private void newGame(int size) throws GtpError
    {
        if (m_resultFile != null)
            m_gameIndex = m_resultFile.getNextGameIndex();
        else
        {
            ++m_gameIndex;
            if (m_numberGames > 0 && m_gameIndex > m_numberGames)
                m_gameIndex = -1;
        }
        if (m_gameIndex == -1)
            throw new GtpError("maximum number of games reached");
        if (m_verbose)
        {
            System.err.println("============================================");
            System.err.println("Game " + m_gameIndex);
            System.err.println("============================================");
        }
        m_black.getAndClearCpuTime();
        m_white.getAndClearCpuTime();
        initGame(size);
        m_gameSaved = false;
        if (m_timeSettings != null)
            sendIfSupported("time_settings",
                            GtpUtil.getTimeSettingsCommand(m_timeSettings));
    }

    private void sendGenmove(GoColor color, StringBuilder response)
        throws GtpError, ErrorMessage
    {
        checkInconsistentState();
        int moveNumber = m_game.getMoveNumber();
        if (m_maxMoves >= 0 && moveNumber > m_maxMoves)
            throw new GtpError("move limit exceeded");
        if (isInOpening())
        {
            // TODO: Check that node contains no setup stones or fully support
            // openings with setup stones and non-alternating moves in GTP
            // engine mode again (by transforming the opening file into a
            // sequence of alternating moves, replacing setup stones by moves
            // and filling in passes). See also comment in initGame() and
            // doc/manual/xml/reference-twogtp.xml
            ConstNode child = getCurrentNode().getChildConst();
            Move move = child.getMove();
            if (move.getColor() != color)
                throw new GtpError("next opening move is " + move);
            m_game.gotoNode(child);
            synchronize();
            response.append(GoPoint.toString(move.getPoint()));
            return;
        }
        Program program;
        boolean exchangeColors =
            (color == BLACK && isAlternated())
            || (color == WHITE && ! isAlternated());
        if (exchangeColors)
            program = m_white;
        else
            program = m_black;
        clearDebugToCommentBuffers();
        long timeMillis = System.currentTimeMillis();
        String responseGenmove = program.sendCommandGenmove(color);
        double time = (System.currentTimeMillis() - timeMillis) / 1000.;
        m_realTime.set(color, m_realTime.get(color) + time);
        if (responseGenmove.equalsIgnoreCase("resign"))
        {
            response.append("resign");
            m_resigned = true;
            m_resignColor = color;
        }
        else
        {
            ConstBoard board = getBoard();
            GoPoint point = null;
            try
            {
                point = GtpUtil.parsePoint(responseGenmove, board.getSize());
            }
            catch (GtpResponseFormatError e)
            {
                throw new GtpError(program.getLabel()
                                   + " played invalid move: "
                                   + responseGenmove);
            }
            Move move = Move.get(color, point);
            m_game.play(move);
            program.updateAfterGenmove(board);
            synchronize();
            response.append(GoPoint.toString(move.getPoint()));
            if (m_debugToComment)
            {
                // All stderr that was written by the program before the
                // response to genmove should have been received by now, but
                // maybe the IO callback thread had no chance to run yet, so we
                // wait for an extra 10 milliseconds
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException e)
                {
                }
                m_game.setComment(getDebugToCommentBuffer(color));
            }
        }
        if (gameOver() && ! m_gameSaved)
        {
            handleEndOfGame(false, "");
            m_gameSaved = true;
        }
    }

    private void sendIfSupported(String cmd, String cmdLine)
    {
        for (Program program : m_allPrograms)
            program.sendIfSupported(cmd, cmdLine);
    }

    private void synchronize() throws GtpError
    {
        for (Program program : m_allPrograms)
            program.synchronize(m_game);
    }

    private void synchronizeInit() throws GtpError
    {
        for (Program program : m_allPrograms)
            program.synchronizeInit(m_game);
    }

    private void twogtpColor(Program program, GtpCommand cmd) throws GtpError
    {
        cmd.setResponse(program.send(cmd.getArgLine()));
    }

    private void twogtpObserver(GtpCommand cmd) throws GtpError
    {
        if (m_observer == null)
            throw new GtpError("no observer enabled");
        twogtpColor(m_observer, cmd);
    }

    private void twogtpReferee(GtpCommand cmd) throws GtpError
    {
        if (m_referee == null)
            throw new GtpError("no referee enabled");
        twogtpColor(m_referee, cmd);
    }
}
