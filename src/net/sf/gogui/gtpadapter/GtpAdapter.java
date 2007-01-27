//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtil;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpCallback;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.sgf.SgfUtil;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.StringUtil;

/** GTP adapter for logging or protocol translations.
    @param fillPasses Fill moves of non-alternating colors with pass moves.
*/
public class GtpAdapter
    extends GtpEngine
{
    public GtpAdapter(String program, PrintStream log, String gtpFile,
                      boolean verbose, boolean noScore, boolean version1,
                      boolean fillPasses, boolean lowerCase, int size)
        throws Exception
    {
        super(log);
        if (program.equals(""))
            throw new Exception("No program set");
        m_gtp = new GtpClient(program, verbose, null);
        if (lowerCase)
            m_gtp.setLowerCase();
        m_synchronizer = new GtpSynchronizer(m_gtp, null, fillPasses);
        if (gtpFile != null)
            sendGtpFile(gtpFile);
        init(noScore, version1, size);
    }

    /** Construct with existing GtpClientBase.
        For testing this class.
    */
    public GtpAdapter(GtpClientBase gtp, PrintStream log, boolean noScore,
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
        play(GoColor.BLACK, getPointArg(cmd, 0));
    }

    public void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAXSIZE);
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
        cmdGenmove(cmd.getColorArg(), cmd);
    }

    public void cmdGenmoveBlack(GtpCommand cmd) throws GtpError
    {
        cmdGenmove(GoColor.BLACK, cmd);
    }

    public void cmdGenmoveWhite(GtpCommand cmd) throws GtpError
    {
        cmdGenmove(GoColor.WHITE, cmd);
    }

    public void cmdGenmove(GoColor color, GtpCommand cmd) throws GtpError
    {
        if (checkResign(color, cmd.getResponse()))
            return;
        String command = m_gtp.getCommandGenmove(color);
        String response = send(command);
        if (response.toLowerCase(Locale.ENGLISH).trim().equals("resign"))
            return;
        GoPoint point = GtpUtil.parsePoint(response, m_board.getSize());
        m_board.play(point, color);
        m_synchronizer.updateAfterGenmove(m_board);
        cmd.setResponse(response);
    }

    public void cmdGGUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(1);
        int n = 1;
        if (cmd.getNuArg() == 1)
            n = cmd.getIntArg(0, 1, m_board.getNumberPlacements());
        m_board.undo(n);
        synchronize();
    }

    public void cmdGoGuiAnalyzeCommands(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        String response =
            "string/GtpAdapter ShowBoard/gtpadapter-showboard\n"  ;
        String command = null;
        if (m_gtp.isCommandSupported("gogui-analyze_commands"))
            command = "gogui-analyze_commands";
        else if (m_gtp.isCommandSupported("gogui_analyze_commands"))
            command = "gogui_analyze_commands"; // deprecated
        if (command != null)
            response += send(command);
        cmd.setResponse(response);
    }

    public void cmdGtpAdapterShowBoard(GtpCommand cmd) throws GtpError
    {
        cmd.getResponse().append("\n");
        cmd.getResponse().append(BoardUtil.toString(m_board, true));
    }

    public void cmdLoadsgf(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(2);
        File file = new File(cmd.getArg(0));
        int maxMove = -1;
        if (cmd.getNuArg() == 2)
            maxMove = cmd.getIntArg(1);
        try
        {
            BoardUtil.copy(m_board, SgfUtil.loadSgf(file, maxMove));
        }
        catch (ErrorMessage e)
        {
            throw new GtpError(e.getMessage());
        }
        synchronize();
    }

    public void cmdPlaceFreeHandicap(GtpCommand cmd) throws GtpError
    {
        ArrayList stones;
        if (m_gtp.isCommandSupported("place_free_handicap"))
        {
            String response = send(cmd.getLine());
            stones = GtpUtil.parsePointList(response, m_board.getSize());
        }
        else
        {
            int n = cmd.getIntArg();
            stones = Board.getHandicapStones(m_board.getSize(), n);
            if  (stones == null)
                throw new GtpError("Invalid number of handicap stones");
        }
        StringBuffer pointList = new StringBuffer(128);
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint point = (GoPoint)stones.get(i);            
            m_board.setup(point, GoColor.BLACK);
            if (pointList.length() > 0)
                pointList.append(' ');
            pointList.append(point);
        }
        cmd.setResponse(pointList.toString());
        synchronize();
    }

    public void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = getPointArg(cmd, 1);
        if (point != null && m_board.getColor(point) != GoColor.EMPTY)
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
        for (int i = 0; i < cmd.getNuArg(); ++i)
            m_board.setup(getPointArg(cmd, i), GoColor.BLACK);
        synchronize();
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
        play(GoColor.WHITE, getPointArg(cmd, 0));
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

    /** Check estimate_score and resign, if score too bad.
        Should be set before handling any commands.
    */
    public void setResign(int resignScore)
    {
        m_resign = true;
        m_resignScore = Math.abs(resignScore);
    }

    private boolean m_resign;

    /** Only accept this board size.
        A value of -1 means accept any size.
    */
    private int m_resignScore;

    private Board m_board;

    private GtpCallback m_callbackForward = new GtpCallback() {
            public void run(GtpCommand cmd) throws GtpError {
                cmdForward(cmd); } };

    private final GtpClientBase m_gtp;

    private final GtpSynchronizer m_synchronizer;

    private boolean checkResign(GoColor color, StringBuffer response)
    {
        if (! m_resign)
            return false;
        StringBuffer programResponse = new StringBuffer();
        try
        {
            send("estimate_score", programResponse);
        }
        catch (GtpError e)
        {
            return false;
        }
        boolean isValid = false;
        double score = 0;
        String[] args
            = StringUtil.splitArguments(programResponse.toString());
        if (args.length > 0)
        {
            String s = args[0];
            try
            {
                if (! s.equals("?"))
                {
                    if (s.indexOf("B+") >= 0)
                        score = Double.parseDouble(s.substring(2));
                    else if (s.indexOf("W+") >= 0)
                        score = - Double.parseDouble(s.substring(2));
                    isValid = true;
                }
            }
            catch (NumberFormatException e)
            {
            }
        }
        if (isValid)
        {
            boolean isBlack = (color == GoColor.BLACK);
            if ((isBlack && score < - m_resignScore)
                || (! isBlack && score > m_resignScore))
            {
                response.append("resign");
                return true;
            }
        }
        return false;
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
        m_resign = false;
        registerCommands(noScore, version1);
        synchronize();
    }

    private void play(GoColor color, GoPoint point) throws GtpError
    {
        Move move = Move.get(point, color);
        m_board.play(move);
        synchronize();
    }

    private void registerCommands(boolean noScore, boolean version1)
    {
        ArrayList commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
        {
            String command = (String)commands.get(i);
            if (! GtpUtil.isStateChangingCommand(command))
                register(command, m_callbackForward);
        }
        register("boardsize", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdBoardsize(cmd); } });
        register("gogui-analyze_commands", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGoGuiAnalyzeCommands(cmd); } });
        register("gtpadapter-showboard", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGtpAdapterShowBoard(cmd); } });
        if (version1)
            register("protocol_version", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdProtocolVersion1(cmd); } });
        register("loadsgf", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdLoadsgf(cmd); } });
        setName(null);
        register("place_free_handicap", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdPlaceFreeHandicap(cmd); } });
        register("set_free_handicap", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdSetFreeHandicap(cmd); } });
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

    private void send(String cmd, StringBuffer response) throws GtpError
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
        m_synchronizer.synchronize(m_board);
    }
}
