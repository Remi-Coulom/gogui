
//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.ConstGameInformation;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.util.Table;
import net.sf.gogui.version.Version;

/** GTP adapter for playing games between two Go programs. */
public class TwoGtp
    extends GtpEngine
{
    public TwoGtp(String black, String white, String referee, String observer,
                  int size, Komi komi, int numberGames, boolean alternate,
                  String sgfFile, boolean force, boolean verbose,
                  Openings openings, TimeSettings timeSettings)
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
        m_black = new GtpClient(black, verbose, null);
        m_black.setLogPrefix("B");
        m_synchronizerBlack = new GtpSynchronizer(m_black);
        m_white = new GtpClient(white, verbose, null);
        m_white.setLogPrefix("W");
        m_synchronizerWhite = new GtpSynchronizer(m_white);
        m_refereeCommand = referee;
        if (! referee.equals(""))
        {
            m_referee = new GtpClient(referee, verbose, null);
            m_referee.setLogPrefix("R");
            m_synchronizerReferee = new GtpSynchronizer(m_referee);
        }
        else
        {
            m_referee = null;
            m_synchronizerReferee = null;
        }
        if (! observer.equals(""))
        {
            m_observer = new GtpClient(observer, verbose, null);
            m_observer.setLogPrefix("O");
            m_synchronizerObserver = new GtpSynchronizer(m_observer);
        }
        else
        {
            m_observer = null;
            m_synchronizerObserver = null;
        }
        m_black.queryProtocolVersion();
        m_white.queryProtocolVersion();
        m_nameBlack = getName(m_black, "Black");
        m_nameWhite = getName(m_white, "White");
        m_blackVersion = getVersion(m_black);
        m_whiteVersion = getVersion(m_white);        
        m_black.querySupportedCommands();
        m_white.querySupportedCommands();
        m_black.queryInterruptSupport();
        m_white.queryInterruptSupport();
        if (m_referee != null)
        {
            m_referee.queryProtocolVersion();
            m_nameReferee = getName(m_referee, "Referee");        
            m_refereeVersion = getVersion(m_referee);        
            m_referee.querySupportedCommands();
            m_referee.queryInterruptSupport();
        }
        else
        {
            m_nameReferee = null;
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
        m_alternate = alternate;
        m_numberGames = numberGames;
        m_openings = openings;
        m_verbose = verbose;
        m_timeSettings = timeSettings;
        initTable();
        readGames();
        initGame(size);
    }

    public void autoPlay() throws Exception
    {
        try
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
                        sendGenmove(getToMove(), response);
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
        }
        finally
        {
            close();
        }
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
        else if (command.equals("gogui-interrupt"))
            ;
        else if (command.equals("gogui-title"))
            cmd.setResponse(getTitle());
        else if (command.equals("twogtp-black"))
            twogtpColor(m_black, cmd);
        else if (command.equals("twogtp-white"))
            twogtpColor(m_white, cmd);
        else if (command.equals("twogtp-referee"))
            twogtpReferee(cmd);
        else if (command.equals("twogtp-observer"))
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
                            "gogui-interrupt\n" +
                            "gogui-title\n" +
                            "komi\n" +
                            "list_commands\n" +
                            "name\n" +
                            "play\n" +
                            "quit\n" +
                            "scoring_system\n" +
                            "time_settings\n" +
                            "twogtp-black\n" +
                            "twogtp-observer\n" +
                            "twogtp-referee\n" +
                            "twogtp-white\n" +
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
            throw new GtpError("use twogtp-black/white/referee/observer");
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
        if (m_referee != null)
            interruptProgram(m_referee);
        if (m_observer != null)
            interruptProgram(m_observer);
    }

    /** Limit number of moves.
        @param maxMoves Maximum number of moves after which genmove will fail,
        -1 for no limit.
    */
    public void setMaxMoves(int maxMoves)
    {
        m_maxMoves = maxMoves;
    }

    private final boolean m_alternate;

    private boolean m_gameSaved;

    private int m_maxMoves = 1000;

    private boolean m_resigned;

    private final boolean m_verbose;

    private int m_gameIndex;

    private final int m_numberGames;

    private final int m_size;

    /** Enforced komi.
        Contains komi if komi is enforced by command line option, null
        otherwise.
    */
    private Komi m_komi;

    private double m_cpuTimeBlack;

    private double m_cpuTimeWhite;

    private Game m_game;

    private GoColor m_resignColor;

    private final Openings m_openings;

    private final String m_nameBlack;

    private final String m_nameReferee;

    private final String m_nameWhite;

    private final String m_blackVersion;

    private String m_openingFile;

    private final String m_refereeCommand;

    private final String m_refereeVersion;

    private final String m_sgfFile;

    private final String m_whiteVersion;

    private final ArrayList m_games = new ArrayList(100);

    private final GtpClient m_black;

    private GtpClient m_observer;

    private GtpClient m_referee;

    private final GtpClient m_white;

    private final GtpSynchronizer m_synchronizerBlack;

    private final GtpSynchronizer m_synchronizerWhite;

    private final GtpSynchronizer m_synchronizerReferee;

    private final GtpSynchronizer m_synchronizerObserver;

    private Table m_table;

    private final TimeSettings m_timeSettings;

    private ConstNode m_lastOpeningNode;

    private void checkInconsistentState() throws GtpError
    {
        if (m_synchronizerBlack.isOutOfSync()
            || m_synchronizerWhite.isOutOfSync()
            || (m_referee != null && m_synchronizerReferee.isOutOfSync())
            || (m_observer != null && m_synchronizerObserver.isOutOfSync()))
            throw new GtpError("Inconsistent state");
    }

    public void close()
    {
        close(m_black);
        close(m_white);
        close(m_referee);
        close(m_observer);
    }    

    private void close(GtpClient gtp)
    {
        if (gtp == null)
            return;
        // Some programs don't handle closing input stream well, so
        // we send an explicit quit
        try
        {
            gtp.send("quit");
        }
        catch (GtpError e)
        {
        }
        gtp.close();
        gtp.waitForExit();
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
        assert(m_game.getMoveNumber() == moveNumber - 1);
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

    private void initTable() throws ErrorMessage
    {
        File file = getResultFile();
        if (file.exists())
        {            
            m_table = new Table();
            try
            {
                m_table.read(getResultFile());
                int lastRowIndex = m_table.getNumberRows() - 1;
                m_gameIndex =
                    Integer.parseInt(m_table.get("GAME", lastRowIndex)) + 1;
                if (m_gameIndex < 0)
                    throw new ErrorMessage("Invalid file format: " + file);
            }                        
            catch (NumberFormatException e)
            {
                throw new ErrorMessage("Invalid file format: " + file);
            }
            catch (FileNotFoundException e)
            {
                throw new ErrorMessage(e.getMessage());
            }
            catch (IOException e)
            {
                throw new ErrorMessage("Read error: " + file);
            }
            return;
        }
        ArrayList columns = new ArrayList();
        columns.add("GAME");
        columns.add("RES_B");
        columns.add("RES_W");
        columns.add("RES_R");
        columns.add("ALT");
        columns.add("DUP");
        columns.add("LEN");
        columns.add("CPU_B");
        columns.add("CPU_W");
        columns.add("ERR");
        columns.add("ERR_MSG");
        m_table = new Table(columns);
        String nameBlack = m_nameBlack;
        String nameWhite = m_nameWhite;
        if (! m_blackVersion.equals(""))
            nameBlack = m_nameBlack + ":" + m_blackVersion;
        if (! m_whiteVersion.equals(""))
            nameWhite = m_nameWhite + ":" + m_whiteVersion;
        String nameReferee = "-";
        if (m_referee != null)
            nameReferee = m_nameReferee + ":" + m_refereeVersion;
        m_table.setProperty("Black", nameBlack);
        m_table.setProperty("White", nameWhite);
        m_table.setProperty("Referee", nameReferee);
        m_table.setProperty("BlackCommand", m_black.getProgramCommand());
        m_table.setProperty("WhiteCommand", m_white.getProgramCommand());
        m_table.setProperty("RefereeCommand", m_refereeCommand);
        m_table.setProperty("Size", Integer.toString(m_size));
        m_table.setProperty("Komi", m_komi.toString());
        if (m_openings != null)
            m_table.setProperty("Openings",
                                m_openings.getDirectory() + " ("
                                + m_openings.getNumber() + " files)");
        m_table.setProperty("Date", StringUtil.getDate());
        m_table.setProperty("Host", Platform.getHostInfo());
    }

    private void forward(GtpClient gtp, GtpCommand cmd) throws GtpError
    {
        cmd.setResponse(gtp.send(cmd.getLine()));
    }

    private boolean gameOver()
    {
        return (getBoard().bothPassed() || m_resigned);
    }

    private ConstBoard getBoard()
    {
        return m_game.getBoard();
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

    private ConstNode getCurrentNode()
    {
        return m_game.getCurrentNode();
    }

    private File getFile(int gameIndex)
    {
        return new File(m_sgfFile + "-" + gameIndex + ".sgf");
    }

    private static String getName(GtpClient gtp, String defaultName)
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
        return defaultName;
    }

    private GoColor getToMove()
    {
        return m_game.getToMove();
    }

    private ConstGameTree getTree()
    {
        return m_game.getTree();
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
        String nameBlack = m_nameBlack;
        String nameWhite = m_nameWhite;
        if (nameBlack.equals(nameWhite))
        {
            if (! m_blackVersion.equals(""))
                nameBlack = m_nameBlack + ":" + m_blackVersion;
            if (! m_whiteVersion.equals(""))
                nameWhite = m_nameWhite + ":" + m_whiteVersion;
        }
        if (isAlternated())
        {
            String tmpName = nameBlack;
            nameBlack = nameWhite;
            nameWhite = tmpName;
        }
        nameBlack = StringUtil.capitalize(nameBlack);
        nameWhite = StringUtil.capitalize(nameWhite);
        buffer.append(nameWhite);
        buffer.append(" vs ");
        buffer.append(nameBlack);
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
        throws ErrorMessage
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
                if (m_referee != null)
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
            ArrayList moves = Compare.getAllAsMoves(getTree().getRootConst());
            String duplicate =
                Compare.checkDuplicate(getBoard(), moves, m_games,
                                       m_alternate, isAlternated());
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
            int moveNumber = NodeUtil.getMoveNumber(getCurrentNode());
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
        m_game = new Game(size, m_komi, null, null, null);
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
                throw new GtpError("Komi " + m_komi
                                   + " is fixed by command line option");
            m_game.setKomi(komi);
            sendIfSupported("komi", "komi " + komi);
        }
        catch (Komi.InvalidKomi e)
        {
            throw new GtpError("invalid komi: " + arg);
        }
    }

    private void newGame(int size) throws GtpError
    {
        m_cpuTimeBlack = getCpuTime(m_black);
        m_cpuTimeWhite = getCpuTime(m_white);
        initGame(size);
        m_gameSaved = false;
        sendIfSupported("komi", "komi " + m_komi);
        if (m_timeSettings != null)
            sendIfSupported("time_settings",
                            GtpUtil.getTimeSettingsCommand(m_timeSettings));
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
                    new SgfReader(fileStream, file, null, 0);
                ConstNode root = reader.getTree().getRoot();
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
        String nameBlack = m_nameBlack;
        if (! m_blackVersion.equals(""))
            nameBlack = nameBlack + ":" + m_blackVersion;
        String nameWhite = m_nameWhite;
        if (! m_whiteVersion.equals(""))
            nameWhite = nameWhite + ":" + m_whiteVersion;
        String blackCommand = m_black.getProgramCommand();
        String whiteCommand = m_white.getProgramCommand();
        if (isAlternated())
        {
            nameBlack = m_nameWhite;
            if (! m_whiteVersion.equals(""))
                nameBlack = nameBlack + ":" + m_whiteVersion;
            nameWhite = m_nameBlack;
            if (! m_blackVersion.equals(""))
                nameWhite = nameWhite + ":" + m_blackVersion;
            blackCommand = m_white.getProgramCommand();
            whiteCommand = m_black.getProgramCommand();
            String resultTmp = inverseResult(resultWhite);
            resultWhite = inverseResult(resultBlack);
            resultBlack = resultTmp;
            resultReferee = inverseResult(resultReferee);
        }
        m_game.setPlayer(GoColor.BLACK, nameBlack);
        m_game.setPlayer(GoColor.WHITE, nameWhite);
        if (m_referee != null)
            m_game.setResult(resultReferee);
        else if (resultBlack.equals(resultWhite) && ! resultBlack.equals("?"))
            m_game.setResult(resultBlack);
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
            "\nDate: " + StringUtil.getDate();
        m_game.setComment(gameComment, getTree().getRootConst());
        File file = getFile(m_gameIndex);
        if (m_verbose)
            System.err.println("Saving " + file);
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, getTree(), "TwoGtp", Version.get());
    }

    private void saveResult(String resultBlack, String resultWhite,
                            String resultReferee, boolean alternated,
                            String duplicate, int numberMoves, boolean error,
                            String errorMessage, double cpuTimeBlack,
                            double cpuTimeWhite)
         throws ErrorMessage
    {
        if (m_sgfFile.equals(""))
            return;
        NumberFormat format = StringUtil.getNumberFormat(1);
        m_table.startRow();
        m_table.set("GAME", Integer.toString(m_gameIndex));
        m_table.set("RES_B", resultBlack);
        m_table.set("RES_W", resultWhite);
        m_table.set("RES_R", resultReferee);
        m_table.set("ALT", alternated ? "1" : "0");
        m_table.set("DUP", duplicate);
        m_table.set("LEN", numberMoves);
        m_table.set("CPU_B", format.format(cpuTimeBlack));
        m_table.set("CPU_W", format.format(cpuTimeWhite));
        m_table.set("ERR", error ? "1" : "0");
        m_table.set("ERR_MSG", errorMessage);
        try
        {
            m_table.save(getResultFile());
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Could not write to: " + getResultFile());
        }
    }

    private void sendGenmove(GoColor color, StringBuffer response)
        throws GtpError, ErrorMessage
    {
        checkInconsistentState();
        int moveNumber = m_game.getMoveNumber();
        if (m_maxMoves >= 0 && moveNumber > m_maxMoves)
            throw new GtpError("move limit exceeded");
        if (isInOpening())
        {
            ConstNode child = getCurrentNode().getChildConst();
            Move move = child.getMove();
            if (move.getColor() != color)
                throw new GtpError("next opening move is " + move);
            m_game.gotoNode(child);
            synchronize();
            response.append(GoPoint.toString(move.getPoint()));            
            return;
        }
        GtpClient gtp;
        String name;
        String command;
        GtpSynchronizer synchronizer;
        boolean exchangeColors =
            (color == GoColor.BLACK && isAlternated())
            || (color == GoColor.WHITE && ! isAlternated());
        if (exchangeColors)
        {
            gtp = m_white;
            name = m_nameWhite;
            synchronizer = m_synchronizerWhite;
            command = m_white.getCommandGenmove(color);
        }
        else
        {
            gtp = m_black;
            name = m_nameBlack;
            synchronizer = m_synchronizerBlack;
            command = m_black.getCommandGenmove(color);
        }
        String responseGenmove = gtp.send(command);
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
            catch (GtpError e)
            {
                throw new GtpError(name + " played invalid move: "
                                   + responseGenmove);
            }
            Move move = Move.get(color, point);
            m_game.play(move);
            synchronizer.updateAfterGenmove(board);
            synchronize();
            response.append(GoPoint.toString(move.getPoint()));            
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
        if (m_referee != null)
            sendIfSupported(m_referee, cmd, cmdLine);
        if (m_observer != null)
            sendIfSupported(m_observer, cmd, cmdLine);
    }

    private void sendIfSupported(GtpClient gtp, String cmd, String cmdLine)
    {
        if (! gtp.isSupported(cmd))
            return;
        try
        {
            gtp.send(cmdLine);
        }
        catch (GtpError e)
        {
        }
    }

    private void synchronize() throws GtpError
    {
        synchronize(m_synchronizerBlack, m_nameBlack);
        synchronize(m_synchronizerWhite, m_nameWhite);
        synchronize(m_synchronizerReferee, m_nameReferee);
        synchronize(m_synchronizerObserver, "Observer");
    }

    private void synchronize(GtpSynchronizer synchronizer,
                             String name) throws GtpError
    {
        if (synchronizer == null)
            return;
        try
        {
            ConstNode node = m_game.getGameInformationNode();
            ConstGameInformation info = m_game.getGameInformation(node);
            synchronizer.synchronize(getBoard(), info.getKomi(),
                                     info.getTimeSettings());
        }
        catch (GtpError e)
        {
            throw new GtpError(name + ": " + e.getMessage());
        }
    }

    private void synchronizeInit() throws GtpError
    {
        synchronizeInit(m_synchronizerBlack, m_nameBlack);
        synchronizeInit(m_synchronizerWhite, m_nameWhite);
        synchronizeInit(m_synchronizerReferee, m_nameReferee);
        synchronizeInit(m_synchronizerObserver, "Observer");
    }

    private void synchronizeInit(GtpSynchronizer synchronizer,
                                 String name) throws GtpError
    {
        if (synchronizer == null)
            return;
        try
        {
            ConstNode node = m_game.getGameInformationNode();
            ConstGameInformation info = m_game.getGameInformation(node);
            synchronizer.init(getBoard(), info.getKomi(),
                              info.getTimeSettings());
        }
        catch (GtpError e)
        {
            throw new GtpError(name + ": " + e.getMessage());
        }
    }

    private void twogtpColor(GtpClient gtp, GtpCommand cmd) throws GtpError
    {
        cmd.setResponse(gtp.send(cmd.getArgLine()));
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

