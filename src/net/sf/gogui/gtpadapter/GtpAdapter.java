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
import java.util.Stack;
import java.util.Vector;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.Gtp;
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

    public void handleCommand(String cmdLine, StringBuffer response)
        throws GtpError
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        if (cmd.equals("black"))
            cmdPlay(GoColor.BLACK, cmdArray);
        else if (cmd.equals("boardsize"))
            cmdBoardsize(cmdArray);
        else if (cmd.equals("clear_board"))
            cmdClearBoard();
        else if (cmd.equals("final_score") && m_noScore)
            cmdUnknown();
        else if (cmd.equals("final_status_list") && m_noScore)
            cmdUnknown();
        else if (cmd.equals("genmove"))
            cmdGenmove(cmdArray, response);
        else if (cmd.equals("genmove_black"))
            cmdGenmove(GoColor.BLACK, response);
        else if (cmd.equals("genmove_white"))
            cmdGenmove(GoColor.WHITE, response);
        else if (cmd.equals("gg-undo"))
            cmdGGUndo(cmdArray);
        else if (cmd.equals("gtpadapter_showboard"))
            cmdGtpAdapterShowBoard(response);
        else if (cmd.equals("help"))
            cmdListCommands(response);
        else if (cmd.equals("list_commands"))
            cmdListCommands(response);
        else if (cmd.equals("loadsgf"))
            cmdLoadsgf(cmdArray, response);
        else if (cmd.equals("name") && m_name != null)
            cmdName(response);
        else if (cmd.equals("place_free_handicap") && m_emuHandicap)
            cmdPlaceFreeHandicap(cmdArray, response);
        else if (cmd.equals("play"))
            cmdPlay(cmdArray);
        else if (cmd.equals("protocol_version"))
            response.append(m_version1 ? "1" : "2");
        else if (cmd.equals("quit"))
            cmdQuit();
        else if (cmd.equals("set_free_handicap") && m_emuHandicap)
            cmdSetFreeHandicap(cmdArray);
        else if (cmd.equals("version") && m_name != null)
            cmdVersion(response);
        else if (cmd.equals("undo"))
            cmdUndo(cmdArray);
        else if (cmd.equals("white"))
            cmdPlay(GoColor.WHITE, cmdArray);
        else
            send(cmdLine, response);
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

    private void cmdBoardsize(String cmdArray[]) throws GtpError
    {
        int size = parseIntegerArgument(cmdArray);
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

    private void cmdGenmove(String cmdArray[], StringBuffer response)
        throws GtpError
    {
        cmdGenmove(parseColorArgument(cmdArray), response);
    }

    private void cmdGenmove(GoColor color, StringBuffer response)
        throws GtpError
    {
        if (checkResign(color, response))
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
        if (response.toString().trim().equals("resign"))
            return;
        GoPoint point = GtpUtils.parsePoint(response.toString(), m_boardSize);
        m_board.play(Move.create(point, color));
    }

    private void cmdGGUndo(String cmdArray[])
        throws GtpError
    {
        if (cmdArray.length > 2)
            throw new GtpError("Too many arguments");
        int n = 1;
        if (cmdArray.length == 2)
        {
            try
            {
                n = Integer.parseInt(cmdArray[1]);
                if (n <= 0)
                    throw new GtpError("Argument must be positive");
                if (n > m_board.getMoveNumber())
                    throw new GtpError("Not enough moves");
            }
            catch (NumberFormatException e)
            {
                throw new GtpError("Invalid argument");
            }
        }        
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

    private void cmdGtpAdapterShowBoard(StringBuffer response)
        throws GtpError
    {
        OutputStream outputStream = new ByteArrayOutputStream(2048);
        PrintStream printStream = new PrintStream(outputStream);
        BoardUtils.print(m_board, printStream);
        response.append("\n");
        response.append(outputStream.toString());
    }

    private void cmdListCommands(StringBuffer response)
        throws GtpError
    {
        Vector commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
        {
            String cmd = (String)commands.get(i);
            if (cmd.equals("boardsize")
                || cmd.equals("black")
                || cmd.equals("clear_board")
                || cmd.equals("genmove")
                || cmd.equals("genmove_black")
                || cmd.equals("genmove_white")
                || cmd.equals("help")
                || cmd.equals("list_commands")
                || cmd.equals("play")
                || cmd.equals("protocol_version")
                || cmd.equals("quit")
                || cmd.equals("white"))
                continue;
            if ((cmd.equals("set_free_handicap")
                 || cmd.equals("get_free_handicap"))
                && m_emuHandicap)
                continue;
            if (cmd.equals("loadsgf") && m_emuLoadsgf)
                continue;
            if (m_noScore
                && (cmd.equals("final_score")
                    || (cmd.equals("final_status_list"))))
                    continue;
            response.append(cmd);
            response.append("\n");
        }
        response.append("boardsize\n");
        response.append("protocol_version\n");
        response.append("quit\n");
        if (m_version1)
        {
            response.append("black\n");
            response.append("help\n");
            response.append("genmove_white\n");
            response.append("genmove_black\n");
            response.append("white\n");
        }
        else
        {
            response.append("clear_board\n");        
            response.append("genmove\n");
            response.append("list_commands\n");
            response.append("play\n");
        }
        if (m_emuHandicap)
        {
            response.append("set_free_handicap\n");
            response.append("get_free_handicap\n");
        }
        if (m_emuLoadsgf)
            response.append("loadsgf\n");
        response.append("gtpadapter_showboard\n");
    }

    private void cmdLoadsgf(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        if (cmdArray.length < 2)
            throw new GtpError("Need filename argument");
        String filename = cmdArray[1];
        String command = "loadsgf " + filename;
        int maxMove = -1;
        if (cmdArray.length > 2)
        {
            try
            {
                maxMove = Integer.parseInt(cmdArray[2]);
                command = command + " " + maxMove;
            }
            catch (NumberFormatException e)
            {
                throw new GtpError("Invalid move number argument");
            }
        }
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
                Vector moves = node.getAllAsMoves();
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

    private void cmdName(StringBuffer response) throws GtpError
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index < 0)
            response.append(m_name);
        else
            response.append(m_name.substring(0, index));
    }

    private void cmdPlaceFreeHandicap(String cmdArray[],
                                      StringBuffer response) throws GtpError
    {
        int n = parseIntegerArgument(cmdArray);
        Vector stones = Board.getHandicapStones(m_boardSize, n);
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
        response.append(pointList);
    }

    private void cmdPlay(String cmdArray[]) throws GtpError
    {
        ColorPointArgument argument
            = parseColorPointArgument(cmdArray, m_boardSize);
        play(argument.m_color, argument.m_point);
    }

    private void cmdPlay(GoColor color, String cmdArray[]) throws GtpError
    {
        GoPoint point = parsePointArgument(cmdArray, m_boardSize);
        play(color, point);
    }

    private void cmdQuit() throws GtpError
    {
        send("quit");
    }

    private void cmdSetFreeHandicap(String cmdArray[]) throws GtpError
    {
        for (int i = 1; i < cmdArray.length; ++i)
        {
            GoPoint point = GtpUtils.parsePoint(cmdArray[i], m_boardSize);
            play(GoColor.BLACK, point);
        }
    }

    private void cmdUndo(String cmdArray[]) throws GtpError
    {
        if (cmdArray.length != 1)
            throw new GtpError("No arguments allowed");
        undo();
    }

    private void cmdUnknown() throws GtpError
    {
        throw new GtpError("Unknown command");
    }

    private void cmdVersion(StringBuffer response) throws GtpError
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index >= 0)
            response.append(m_name.substring(index + 1));
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
