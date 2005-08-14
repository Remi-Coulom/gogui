//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Stack;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** GTP adapter for logging or protocol translations. */
public class GtpAdapter
    extends GtpEngine
{
    public GtpAdapter(InputStream in, OutputStream out, String program,
                      PrintStream log, boolean version1, int size,
                      String name, boolean noScore, boolean emuHandicap,
                      boolean emuLoadsgf, boolean resign, int resignScore,
                      String gtpFile, boolean verbose, boolean fillPasses)
        throws Exception
    {
        super(in, out, log);
        if (program.equals(""))
            throw new Exception("No program set.");
        m_gtp = new Gtp(program, verbose, null);
        if (gtpFile != null)
            sendGtpFile(gtpFile);
        m_gtp.queryProtocolVersion();
        m_gtp.querySupportedCommands();
        if (size > 0)
            m_boardSize = size;
        else
            m_boardSize = 19;
        m_board = new Board(m_boardSize);
        m_version1 = version1;
        m_noScore = noScore;
        m_emuHandicap = emuHandicap;
        m_emuLoadsgf = emuLoadsgf;
        m_size = size;
        m_name = name;
        m_resign = resign;
        m_resignScore = Math.abs(resignScore);
        m_fillPasses = fillPasses;
    }

    public void close()
    {
        m_gtp.close();
        m_gtp.waitForExit();
    }

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        if (cmd.getCommand().equals("black"))
            cmdPlay(GoColor.BLACK, cmd);
        else if (cmd.getCommand().equals("boardsize"))
            cmdBoardsize(cmd);
        else if (cmd.getCommand().equals("clear_board"))
            cmdClearBoard();
        else if (cmd.getCommand().equals("final_score") && m_noScore)
            cmdUnknown();
        else if (cmd.getCommand().equals("final_status_list") && m_noScore)
            cmdUnknown();
        else if (cmd.getCommand().equals("genmove"))
            cmdGenmove(cmd);
        else if (cmd.getCommand().equals("genmove_black"))
            cmdGenmove(GoColor.BLACK, cmd);
        else if (cmd.getCommand().equals("genmove_white"))
            cmdGenmove(GoColor.WHITE, cmd);
        else if (cmd.getCommand().equals("gg-undo"))
            cmdGGUndo(cmd);
        else if (cmd.getCommand().equals("gtpadapter_showboard"))
            cmdGtpAdapterShowBoard(cmd);
        else if (cmd.getCommand().equals("help"))
            cmdListCommands(cmd);
        else if (cmd.getCommand().equals("list_commands"))
            cmdListCommands(cmd);
        else if (cmd.getCommand().equals("loadsgf"))
            cmdLoadsgf(cmd);
        else if (cmd.getCommand().equals("name") && m_name != null)
            cmdName(cmd);
        else if (cmd.getCommand().equals("place_free_handicap")
                 && m_emuHandicap)
            cmdPlaceFreeHandicap(cmd);
        else if (cmd.getCommand().equals("play"))
            cmdPlay(cmd);
        else if (cmd.getCommand().equals("protocol_version"))
            cmd.getResponse().append(m_version1 ? "1" : "2");
        else if (cmd.getCommand().equals("quit"))
            cmdQuit();
        else if (cmd.getCommand().equals("set_free_handicap")
                 && m_emuHandicap)
            cmdSetFreeHandicap(cmd);
        else if (cmd.getCommand().equals("version") && m_name != null)
            cmdVersion(cmd);
        else if (cmd.getCommand().equals("undo"))
            cmdUndo(cmd);
        else if (cmd.getCommand().equals("white"))
            cmdPlay(GoColor.WHITE, cmd);
        else
            send(cmd.getLine(), cmd.getResponse());
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
        interruptProgram(m_gtp);
    }

    private final boolean m_emuHandicap;

    private final boolean m_emuLoadsgf;

    private final boolean m_fillPasses;

    private final boolean m_noScore;

    private final boolean m_resign;

    private final boolean m_version1;

    /** Only accept this board size.
        A value of -1 means accept any size.
    */
    private final int m_size;

    private int m_boardSize;

    private final int m_resignScore;

    private Board m_board;

    private final Gtp m_gtp;

    private final Stack m_passInserted = new Stack();

    private final String m_name;

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
        String[] tokens = StringUtils.tokenize(programResponse.toString());
        if (tokens.length > 0)
        {
            String s = tokens[0];
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

    private void cmdBoardsize(GtpCommand cmd) throws GtpError
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

    private void cmdClearBoard() throws GtpError
    {
        send(m_gtp.getCommandClearBoard(m_boardSize));
        m_board = new Board(m_boardSize);
        m_passInserted.clear();
    }

    private void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        cmdGenmove(cmd.getColorArg(), cmd);
    }

    private void cmdGenmove(GoColor color, GtpCommand cmd) throws GtpError
    {
        if (checkResign(color, cmd.getResponse()))
            return;
        String command = m_gtp.getCommandGenmove(color);
        fillPass(color);
        try
        {
            send(command);
        }
        catch (GtpError e)
        {
            undoFillPass();
            throw e;
        }
        if (cmd.getResponse().toString().trim().equals("resign"))
            return;
        GoPoint point
            = GtpUtils.parsePoint(cmd.getResponse().toString(), m_boardSize);
        m_board.play(Move.create(point, color));
    }

    private void cmdGGUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(1);
        int n = 1;
        if (cmd.getNuArg() == 1)
            n = cmd.getIntArg(0, 1, m_board.getMoveNumber());
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

    private void cmdGtpAdapterShowBoard(GtpCommand cmd) throws GtpError
    {
        OutputStream outputStream = new ByteArrayOutputStream(2048);
        PrintStream printStream = new PrintStream(outputStream);
        BoardUtils.print(m_board, printStream);
        cmd.getResponse().append("\n");
        cmd.getResponse().append(outputStream.toString());
    }

    private void cmdListCommands(GtpCommand cmd) throws GtpError
    {
        ArrayList commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
        {
            String c = (String)commands.get(i);
            if (c.equals("boardsize")
                || c.equals("black")
                || c.equals("clear_board")
                || c.equals("genmove")
                || c.equals("genmove_black")
                || c.equals("genmove_white")
                || c.equals("help")
                || c.equals("list_commands")
                || c.equals("play")
                || c.equals("protocol_version")
                || c.equals("quit")
                || c.equals("white"))
                continue;
            if ((c.equals("set_free_handicap")
                 || c.equals("get_free_handicap"))
                && m_emuHandicap)
                continue;
            if (c.equals("loadsgf") && m_emuLoadsgf)
                continue;
            if (m_noScore
                && (c.equals("final_score")
                    || (c.equals("final_status_list"))))
                    continue;
            cmd.getResponse().append(cmd);
            cmd.getResponse().append("\n");
        }
        cmd.getResponse().append("boardsize\n");
        cmd.getResponse().append("protocol_version\n");
        cmd.getResponse().append("quit\n");
        if (m_version1)
        {
            cmd.getResponse().append("black\n");
            cmd.getResponse().append("help\n");
            cmd.getResponse().append("genmove_white\n");
            cmd.getResponse().append("genmove_black\n");
            cmd.getResponse().append("white\n");
        }
        else
        {
            cmd.getResponse().append("clear_board\n");        
            cmd.getResponse().append("genmove\n");
            cmd.getResponse().append("list_commands\n");
            cmd.getResponse().append("play\n");
        }
        if (m_emuHandicap)
        {
            cmd.getResponse().append("set_free_handicap\n");
            cmd.getResponse().append("get_free_handicap\n");
        }
        if (m_emuLoadsgf)
            cmd.getResponse().append("loadsgf\n");
        cmd.getResponse().append("gtpadapter_showboard\n");
    }

    private void cmdLoadsgf(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(2);
        String filename = cmd.getArg(0);
        String command = "loadsgf " + filename;
        int maxMove = -1;
        if (cmd.getNuArg() == 2)
            maxMove = cmd.getIntArg(1);
        if (! m_emuLoadsgf)
            send(command);
        GoColor toMove = GoColor.EMPTY;
        try
        {
            FileInputStream fileStream =
                new FileInputStream(new File(filename));
            SgfReader reader = new SgfReader(fileStream, filename, null, 0);
            GameTree gameTree = reader.getGameTree();
            m_boardSize = gameTree.getGameInformation().m_boardSize;
            m_board = new Board(m_boardSize);
            m_passInserted.clear();
            Node node = gameTree.getRoot();
            int moveNumber = 0;
            while (node != null)
            {
                if (node.getMove() != null)
                {
                    ++moveNumber;
                    if (maxMove >= 0 && moveNumber >= maxMove)
                        break;
                }
                ArrayList moves = node.getAllAsMoves();
                for (int i = 0; i < moves.size(); ++i)
                {
                    Move move = (Move)moves.get(i);
                    if (m_emuLoadsgf)
                        play(move.getColor(), move.getPoint());
                    else
                        m_board.play(move);
                }
                toMove = node.getToMove();
                node = node.getChild();
            }
            if (toMove != GoColor.EMPTY && toMove != m_board.getToMove())
            {
                if (m_emuLoadsgf)
                    play(m_board.getToMove(), null);
                else
                    m_board.setToMove(toMove);
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

    private void cmdName(GtpCommand cmd) throws GtpError
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index < 0)
            cmd.setResponse(m_name);
        else
            cmd.setResponse(m_name.substring(0, index));
    }

    private void cmdPlaceFreeHandicap(GtpCommand cmd) throws GtpError
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
                pointList.append(" ");
            pointList.append(point);
        }
        cmd.setResponse(pointList.toString());
    }

    private void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_boardSize);
        play(color, point);
    }

    private void cmdPlay(GoColor color, GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        play(color, cmd.getPointArg(0, m_boardSize));
    }

    private void cmdQuit() throws GtpError
    {
        send("quit");
    }

    private void cmdSetFreeHandicap(GtpCommand cmd) throws GtpError
    {
        for (int i = 0; i < cmd.getNuArg(); ++i)
            play(GoColor.BLACK, cmd.getPointArg(i, m_boardSize));
    }

    private void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        undo();
    }

    private void cmdUnknown() throws GtpError
    {
        throw new GtpError("Unknown command");
    }

    private void cmdVersion(GtpCommand cmd) throws GtpError
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index >= 0)
            cmd.setResponse(m_name.substring(index + 1));
    }

    private void fillPass(GoColor color) throws GtpError
    {
        if (! m_fillPasses)
            return;
        GoColor toMove = m_board.getToMove();
        if (color == toMove)
        {
            m_passInserted.push(Boolean.FALSE);
            return;
        }
        String command = m_gtp.getCommandPlay(Move.create(null, toMove));
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

    private void play(GoColor color, GoPoint point) throws GtpError
    {
        fillPass(color);
        Move move = Move.create(point, color);
        String command = m_gtp.getCommandPlay(move);
        send(command);
        m_board.play(move);
    }

    private void send(String cmd) throws GtpError
    {
        m_gtp.sendCommand(cmd);
    }

    private void send(String cmd, StringBuffer response) throws GtpError
    {
        response.append(m_gtp.sendCommand(cmd));
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

//----------------------------------------------------------------------------
