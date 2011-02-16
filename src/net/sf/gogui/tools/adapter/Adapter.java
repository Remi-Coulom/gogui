// Adapter.java

package net.sf.gogui.tools.adapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.gamefile.GameFileUtil;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtil;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.gtp.GtpCallback;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.util.ErrorMessage;

/** GTP adapter for logging or protocol translations. */
public class Adapter
    extends GtpEngine
{
    /** Constructor.
        @param program Command line for executing the actual GTP engine.
        @param log Stream to log commands and responses of the adapter.
        @param gtpFile File with GTP commands to send engine at startup.
        @param verbose Enable logging of commands sent from the adapter to
        the actual GTP engine to standard error.
        @param noScore Hide final_score and final_status_list commands, even
        if the angine supports them.
        @param version1 Whether the adapter reports and implements GTP version
        1 commands.
        @param fillPasses Fill moves of non-alternating colors with pass
        moves.
        @param lowerCase Translate move commands to the engine to lower-case.
        @param size Board size at startup. */
    public Adapter(String program, PrintStream log, String gtpFile,
                   boolean verbose, boolean noScore, boolean version1,
                   boolean fillPasses, boolean lowerCase, int size)
        throws Exception
    {
        super(log);
        if (program.equals(""))
            throw new Exception("No program is set.");
        m_gtp = new GtpClient(program, null, verbose, null);
        if (lowerCase)
            m_gtp.setLowerCase();
        m_synchronizer = new GtpSynchronizer(m_gtp, null, fillPasses);
        if (gtpFile != null)
            sendGtpFile(gtpFile);
        init(noScore, version1, size);
    }

    /** Construct with existing GtpClientBase.
        For testing this class. */
    public Adapter(GtpClientBase gtp, PrintStream log, boolean noScore,
                   boolean version1, boolean lowerCase, int size)
        throws GtpError
    {
        super(log);
        m_gtp = gtp;
        if (lowerCase)
            m_gtp.setLowerCase();
        m_synchronizer = new GtpSynchronizer(m_gtp, null, false);
        init(noScore, version1, size);
    }

    public void close()
    {
        m_gtp.close();
        m_gtp.waitForExit();
    }

    public void cmdBlack(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        play(BLACK, getPointArg(cmd, 0));
    }

    public void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAX_SIZE);
        m_board.init(size);
        synchronize();
    }

    public void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        m_board.init(m_board.getSize());
        synchronize();
    }

    public void cmdForward(GtpCommand cmd) throws GtpError
    {
        send(cmd.getLine(), cmd.getResponse());
    }

    public void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        GoColor c = cmd.getColorArg();
        cmdGenmove(c, cmd, m_gtp.getCommandGenmove(c));
    }

    public void cmdGenmoveCleanup(GtpCommand cmd) throws GtpError
    {
        GoColor c = cmd.getColorArg();
        cmdGenmove(c, cmd, "kgs-genmove_cleanup " + c.getUppercaseLetter());
    }

    public void cmdGenmoveBlack(GtpCommand cmd) throws GtpError
    {
        cmdGenmove(BLACK, cmd, m_gtp.getCommandGenmove(BLACK));
    }

    public void cmdGenmoveWhite(GtpCommand cmd) throws GtpError
    {
        cmdGenmove(WHITE, cmd, m_gtp.getCommandGenmove(WHITE));
    }

    public void cmdGGUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(1);
        int n = 1;
        if (cmd.getNuArg() == 1)
            n = cmd.getIntArg(0, 1, m_board.getNumberMoves());
        m_board.undo(n);
        synchronize();
    }

    public void cmdGoGuiAnalyzeCommands(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        String response =
            "string/Adapter ShowBoard/gogui-adapter-showboard\n";
        String command = null;
        if (m_gtp.isSupported("gogui-analyze_commands"))
            command = "gogui-analyze_commands";
        else if (m_gtp.isSupported("gogui_analyze_commands"))
            command = "gogui_analyze_commands"; // deprecated
        if (command != null)
            response += send(command);
        cmd.setResponse(response);
    }

    public void cmdAdapterShowBoard(GtpCommand cmd) throws GtpError
    {
        cmd.getResponse().append("\n");
        cmd.getResponse().append(BoardUtil.toString(m_board, true, false));
    }

    public void cmdKomi(GtpCommand cmd) throws GtpError
    {
        try
        {
            m_komi = Komi.parseKomi(cmd.getArg());
        }
        catch (InvalidKomiException e)
        {
            throw new GtpError("invalid komi");
        }
    }

    public void cmdLoad(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(2);
        File file = new File(cmd.getArg(0));
        int maxMove = -1;
        if (cmd.getNuArg() == 2)
            maxMove = cmd.getIntArg(1);
        try
        {
            BoardUtil.copy(m_board, GameFileUtil.load(file, maxMove));
        }
        catch (ErrorMessage e)
        {
            throw new GtpError(e.getMessage());
        }
        synchronize();
    }

    public void cmdPlaceFreeHandicap(GtpCommand cmd) throws GtpError
    {
        ConstPointList stones;
        if (m_gtp.isSupported("place_free_handicap"))
        {
            String response = send(cmd.getLine());
            try
            {
                stones = GtpUtil.parsePointList(response, m_board.getSize());
            }
            catch (GtpResponseFormatError e)
            {
                throw new GtpError(e.getMessage());
            }
        }
        else
        {
            int n = cmd.getIntArg();
            stones = Board.getHandicapStones(m_board.getSize(), n);
            if  (stones == null)
                throw new GtpError("Invalid number of handicap stones");
        }
        StringBuilder response = new StringBuilder(128);
        PointList points = new PointList();
        for (GoPoint p : stones)
        {
            points.add(p);
            if (response.length() > 0)
                response.append(' ');
            response.append(p);
        }
        m_board.setup(points, null, null);
        cmd.setResponse(response.toString());
        synchronize();
    }

    public void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = getPointArg(cmd, 1);
        if (point != null && m_board.getColor(point) != EMPTY)
            throw new GtpError("point is occupied");
        play(color, point);
    }

    public void cmdProtocolVersion1(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        cmd.setResponse("1");
    }

    public void cmdQuit(GtpCommand cmd) throws GtpError
    {
        send("quit");
        super.cmdQuit(cmd);
    }

    public void cmdSetFreeHandicap(GtpCommand cmd) throws GtpError
    {
        PointList points = new PointList();
        for (int i = 0; i < cmd.getNuArg(); ++i)
            points.add(getPointArg(cmd, i));
        m_board.setup(points, null, null);
        synchronize();
    }

    public void cmdTimeSettings(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(3);
        long mainTime = cmd.getIntArg(0, 0, Integer.MAX_VALUE) * 1000L;
        long byoyomiTime = cmd.getIntArg(0, 0, Integer.MAX_VALUE) * 1000L;
        int byoyomiStones = cmd.getIntArg(0, 0, Integer.MAX_VALUE);
        if (byoyomiTime == 0)
            m_timeSettings = new TimeSettings(mainTime);
        else if (byoyomiStones == 0)
            m_timeSettings = null;
        else
            m_timeSettings =
                new TimeSettings(mainTime, byoyomiTime, byoyomiStones);
    }

    public void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        m_board.undo();
        synchronize();
    }

    public void cmdWhite(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        play(WHITE, getPointArg(cmd, 0));
    }

    public void interruptCommand()
    {
        try
        {
            if (m_gtp.isInterruptSupported())
                m_gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            System.err.println(e);
        }
    }

    public void setName(String name)
    {
        if (name == null)
        {
            register("name", m_callbackForward);
            register("version", m_callbackForward);
            return;
        }
        int index = name.indexOf(':');
        if (index < 0)
        {
            super.setName(name);
            super.setVersion("");
        }
        else
        {
            super.setName(name.substring(0, index));
            super.setVersion(name.substring(index + 1));
        }
        register("name", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdName(cmd); } });
        register("version", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdVersion(cmd); } });
    }

    private Board m_board;

    private final GtpCallback m_callbackForward = new GtpCallback() {
            public void run(GtpCommand cmd) throws GtpError {
                cmdForward(cmd); } };

    private final GtpClientBase m_gtp;

    private final GtpSynchronizer m_synchronizer;

    private Komi m_komi;

    private TimeSettings m_timeSettings;

    private void cmdGenmove(GoColor color, GtpCommand cmd, String command)
        throws GtpError
    {
        String response = send(command);
        if (response.toLowerCase(Locale.ENGLISH).trim().equals("resign"))
        {
            cmd.setResponse("resign");
            return;
        }
        try
        {
            GoPoint point = GtpUtil.parsePoint(response, m_board.getSize());
            m_board.play(color, point);
            m_synchronizer.updateAfterGenmove(m_board);
            cmd.setResponse(response);
        }
        catch (GtpResponseFormatError e)
        {
            throw new GtpError(e.getMessage());
        }
    }

    private GoPoint getPointArg(GtpCommand cmd, int i) throws GtpError
    {
        return cmd.getPointArg(i, m_board.getSize());
    }

    private void init(boolean noScore, boolean version1, int size)
        throws GtpError
    {
        m_gtp.queryProtocolVersion();
        m_gtp.querySupportedCommands();
        m_board = new Board(size);
        registerCommands(noScore, version1);
        synchronize();
    }

    private void play(GoColor color, GoPoint point) throws GtpError
    {
        Move move = Move.get(color, point);
        m_board.play(move);
        synchronize();
    }

    private void registerCommands(boolean noScore, boolean version1)
    {
        ArrayList<String> commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
        {
            String command = commands.get(i);
            if (! GtpUtil.isStateChangingCommand(command))
                register(command, m_callbackForward);
        }
        if (m_gtp.isSupported("kgs-genmove_cleanup"))
            register("kgs-genmove_cleanup", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmoveCleanup(cmd); } });
        register("boardsize", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdBoardsize(cmd); } });
        register("gogui-analyze_commands", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGoGuiAnalyzeCommands(cmd); } });
        register("gogui-adapter-showboard", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdAdapterShowBoard(cmd); } });
        register("komi", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdKomi(cmd); } });
        register("loadsgf", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdLoad(cmd); } });
        register("loadxml", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdLoad(cmd); } });
        setName(null);
        register("place_free_handicap", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdPlaceFreeHandicap(cmd); } });
        if (version1)
            register("protocol_version", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdProtocolVersion1(cmd); } });
        register("set_free_handicap", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdSetFreeHandicap(cmd); } });
        register("time_settings", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdTimeSettings(cmd); } });
        if (noScore)
        {
            unregister("final_score");
            unregister("final_status_list");
        }
        if (version1)
        {
            register("black", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdBlack(cmd); } });
            register("genmove_black", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmoveBlack(cmd); } });
            register("genmove_white", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmoveWhite(cmd); } });
            register("help", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdListCommands(cmd); } });
            unregister("list_commands");
            register("white", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdWhite(cmd); } });
        }
        else
        {
            register("clear_board", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdClearBoard(cmd); } });
            register("genmove", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmove(cmd); } });
            unregister("help");
            register("known_command", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdKnownCommand(cmd); } });
            register("list_commands", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdListCommands(cmd); } });
            register("play", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdPlay(cmd); } });
        }
        register("undo", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdUndo(cmd); } });
        register("gg-undo", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGGUndo(cmd); } });
    }

    private String send(String cmd) throws GtpError
    {
        return m_gtp.send(cmd);
    }

    private void send(String cmd, StringBuilder response) throws GtpError
    {
        response.append(m_gtp.send(cmd));
    }

    private void sendGtpFile(String filename)
    {
        java.io.Reader reader;
        try
        {
            reader = new FileReader(new File(filename));
        }
        catch (FileNotFoundException e)
        {
            System.err.println("File not found: " + filename);
            return;
        }
        java.io.BufferedReader in;
        in = new BufferedReader(reader);
        try
        {
            while (true)
            {
                try
                {
                    String line = in.readLine();
                    if (line == null)
                    {
                        in.close();
                        break;
                    }
                    line = line.trim();
                    if (line.equals("") || line.startsWith("#"))
                        continue;
                    try
                    {
                        GtpCommand cmd = new GtpCommand(line);
                        if (GtpUtil.isStateChangingCommand(cmd.getCommand()))
                        {
                            System.err.println("Command " + cmd.getCommand()
                                               + " not allowed in GTP file");
                            break;
                        }
                        send(line);
                    }
                    catch (GtpError e)
                    {
                        System.err.println("Sending commands aborted:"
                                           + e.getMessage());
                        break;
                    }
                }
                catch (IOException e)
                {
                    System.err.println("Sending commands aborted:"
                                       + e.getMessage());
                    break;
                }
            }
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    private void synchronize() throws GtpError
    {
        m_synchronizer.synchronize(m_board, m_komi, m_timeSettings);
    }
}
