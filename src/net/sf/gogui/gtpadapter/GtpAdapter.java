//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.NodeUtil;
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
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.util.StringUtil;

/** GTP adapter for logging or protocol translations. */
public class GtpAdapter
    extends GtpEngine
{
    public GtpAdapter(String program, PrintStream log, String gtpFile,
                      boolean verbose, boolean emuHandicap, boolean noScore,
                      boolean version1)
        throws Exception
    {
        super(log);
        if (program.equals(""))
            throw new Exception("No program set");
        m_gtp = new GtpClient(program, verbose, null);
        if (gtpFile != null)
            sendGtpFile(gtpFile);
        init(emuHandicap, noScore, version1);
    }

    /** Construct with existing GtpClientBase.
        For testing this class.
    */
    public GtpAdapter(GtpClientBase gtp, PrintStream log, boolean emuHandicap,
                      boolean noScore, boolean version1)
        throws GtpError
    {
        super(log);
        m_gtp = gtp;
        init(emuHandicap, noScore, version1);
    }

    public void close()
    {
        m_gtp.close();
        m_gtp.waitForExit();
    }

    public void cmdBlack(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        play(GoColor.BLACK, cmd.getPointArg(0, m_boardSize));
    }

    public void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        int size = cmd.getIntArg();
        if (size < 1)
            throw new GtpError("Invalid board size");
        if (m_size > 0 && size != m_size)
            throw new GtpError("Boardsize must be " + m_size);
        String command = m_gtp.getCommandBoardsize(size);
        if (command != null)
            send(command);
        m_boardSize = size;
        m_board = new Board(m_boardSize);
        m_passInserted.clear();
        command = m_gtp.getCommandClearBoard(m_boardSize);
        send(command);
    }

    public void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        send(m_gtp.getCommandClearBoard(m_boardSize));
        m_board = new Board(m_boardSize);
        m_passInserted.clear();
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
        fillPass(color);
        String response;
        try
        {
            response = send(command);
        }
        catch (GtpError e)
        {
            undoFillPass();
            throw e;
        }
        if (response.toLowerCase(Locale.ENGLISH).trim().equals("resign"))
            return;
        GoPoint point = GtpUtil.parsePoint(response, m_boardSize);
        m_board.play(point, color);
        cmd.setResponse(response);
    }

    public void cmdGGUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(1);
        int n = 1;
        if (cmd.getNuArg() == 1)
            n = cmd.getIntArg(0, 1, m_board.getNumberPlacements());
        int total = 0;
        Stack stack = new Stack();
        for (int i = 0; i < n; ++i)
        {
            ++total;
            if (m_fillPasses)
            {
                Boolean passInserted = (Boolean)m_passInserted.pop();
                stack.push(passInserted);
                if (passInserted.booleanValue())
                    ++total;
            }
        }
        try
        {
            send("gg-undo " + total);
        }
        catch (GtpError e)
        {
            while (! stack.empty())
                m_passInserted.push(stack.pop());
            return;
        }
        m_board.undo(total);
    }

    public void cmdGoGuiAnalyzeCommands(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        String response =
            "string/GtpAdapter ShowBoard/gtpadapter-showboard\n";
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
        String filename = cmd.getArg(0);
        int maxMove = -1;
        if (cmd.getNuArg() == 2)
            maxMove = cmd.getIntArg(1);
        GoColor toMove = GoColor.EMPTY;
        try
        {
            FileInputStream fileStream =
                new FileInputStream(new File(filename));
            SgfReader reader = new SgfReader(fileStream, filename, null, 0);
            GameTree gameTree = reader.getGameTree();
            m_boardSize = gameTree.getGameInformation().getBoardSize();
            m_board = new Board(m_boardSize);
            m_passInserted.clear();
            m_gtp.sendBoardsize(m_boardSize);
            m_gtp.sendClearBoard(m_boardSize);
            ConstNode node = gameTree.getRoot();
            int moveNumber = 0;
            while (node != null)
            {
                if (node.getMove() != null)
                {
                    ++moveNumber;
                    if (maxMove >= 0 && moveNumber >= maxMove)
                        break;
                }
                ArrayList moves = new ArrayList();
                NodeUtil.getAllAsMoves(node, moves);
                for (int i = 0; i < moves.size(); ++i)
                {
                    Move move = (Move)moves.get(i);
                    play(move.getColor(), move.getPoint());
                }
                toMove = node.getToMove();
                node = node.getChildConst();
            }
            if (toMove != GoColor.EMPTY && toMove != m_board.getToMove())
            {
                play(m_board.getToMove(), null);
            }
        }
        catch (FileNotFoundException e)
        {
            throw new GtpError("File not found");
        }
        catch (SgfReader.SgfError e)
        {
            throw new GtpError("Could not read file");
        }
    }

    public void cmdPlaceFreeHandicap(GtpCommand cmd) throws GtpError
    {
        int n = cmd.getIntArg();
        ArrayList stones = Board.getHandicapStones(m_boardSize, n);
        if  (stones == null)
            throw new GtpError("Invalid number of handicap stones");
        StringBuffer pointList = new StringBuffer(128);
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint point = (GoPoint)stones.get(i);
            play(GoColor.BLACK, point);
            if (pointList.length() > 0)
                pointList.append(' ');
            pointList.append(point);
        }
        cmd.setResponse(pointList.toString());
    }

    public void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_boardSize);
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

    public void interruptProgram(GtpClientBase gtp)
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
        interruptProgram(m_gtp);
    }

    /** Fill moves of non-alternating colors with pass moves.
        Should be set before handling any commands.
    */
    public void setFillPasses()
    {
        m_fillPasses = true;
    }

    /** Accept only a fixed board size.
        Should be set before handling any commands.
    */
    public void setFixedSize(int size)
    {
        assert(size > 0);
        assert(size <= GoPoint.MAXSIZE);
        m_size = size;
        m_boardSize = size;
        m_board = new Board(m_boardSize);
    }

    public void cmdSetFreeHandicap(GtpCommand cmd) throws GtpError
    {
        for (int i = 0; i < cmd.getNuArg(); ++i)
            play(GoColor.BLACK, cmd.getPointArg(i, m_boardSize));
    }

    public void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        undo();
    }

    public void cmdWhite(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        play(GoColor.WHITE, cmd.getPointArg(0, m_boardSize));
    }

    /** Translate move commands to lower case.
        Should be set before handling any commands.
    */
    public void setLowerCase()
    {
        m_lowerCase = true;
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

    private boolean m_fillPasses;

    private boolean m_lowerCase;

    private boolean m_resign;

    /** Only accept this board size.
        A value of -1 means accept any size.
    */
    private int m_size;

    private int m_boardSize;

    private int m_resignScore;

    private Board m_board;

    private GtpCallback m_callbackForward = new GtpCallback() {
            public void run(GtpCommand cmd) throws GtpError {
                cmdForward(cmd); } };

    private final GtpClientBase m_gtp;

    private final Stack m_passInserted = new Stack();

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

    private void fillPass(GoColor color) throws GtpError
    {
        if (! m_fillPasses)
            return;
        GoColor toMove = m_board.getToMove();
        if (color.equals(toMove))
        {
            m_passInserted.push(Boolean.FALSE);
            return;
        }
        String command = m_gtp.getCommandPlay(Move.getPass(toMove));
        try
        {
            send(command);
            m_passInserted.push(Boolean.TRUE);
        }
        catch (GtpError e)
        {
            m_passInserted.push(Boolean.FALSE);
            throw e;
        }
    }

    private void init(boolean emuHandicap, boolean noScore,
                      boolean version1) throws GtpError
    {
        m_gtp.queryProtocolVersion();
        m_gtp.querySupportedCommands();
        m_boardSize = GoPoint.DEFAULT_SIZE;
        m_board = new Board(m_boardSize);
        m_size = -1;
        m_resign = false;
        m_fillPasses = false;
        registerCommands(emuHandicap, noScore, version1);
    }

    private void play(GoColor color, GoPoint point) throws GtpError
    {
        fillPass(color);
        Move move = Move.get(point, color);
        String command = m_gtp.getCommandPlay(move);
        if (m_lowerCase)
            command = command.toLowerCase(Locale.ENGLISH);
        send(command);
        m_board.play(move);
    }

    private void registerCommands(boolean emuHandicap, boolean noScore,
                                  boolean version1)
    {
        GtpCallback callbackUnknown = new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdUnknown(cmd); } };
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
        if (emuHandicap)
        {
            register("place_free_handicap", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdPlaceFreeHandicap(cmd); } });
            register("set_free_handicap", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdSetFreeHandicap(cmd); } });
        }
        if (noScore)
        {
            register("final_score", callbackUnknown);
            register("final_status_list", callbackUnknown);
        }
        if (version1)
        {
            register("black", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdBlack(cmd); } });
            register("clear_board", callbackUnknown);
            register("genmove", callbackUnknown);
            register("genmove_black", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmoveBlack(cmd); } });
            register("genmove_white", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmoveWhite(cmd); } });
            register("help", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdListCommands(cmd); } });
            register("known_command", callbackUnknown);
            register("list_commands", callbackUnknown);
            register("play", callbackUnknown);
            register("white", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdWhite(cmd); } });
        }
        else
        {
            register("black", callbackUnknown);
            register("clear_board", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdClearBoard(cmd); } });
            register("genmove", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdGenmove(cmd); } });
            register("genmove_black", callbackUnknown);
            register("genmove_white", callbackUnknown);
            register("help", callbackUnknown);
            register("known_command", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdKnownCommand(cmd); } });
            register("list_commands", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdListCommands(cmd); } });
            register("play", new GtpCallback() {
                    public void run(GtpCommand cmd) throws GtpError {
                        cmdPlay(cmd); } });
            register("white", callbackUnknown);
        }
        ArrayList commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
        {
            String command = (String)commands.get(i);
            if (! isRegistered(command))
                register(command, m_callbackForward);
        }
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

    private void undo() throws GtpError
    {
        send("undo");
        m_board.undo();
        undoFillPass();
    }

    private void undoFillPass() throws GtpError
    {
        if (! m_fillPasses)
            return;
        Boolean passInserted = (Boolean)m_passInserted.pop();
        if (passInserted.booleanValue())
            send("undo");
    }
}

