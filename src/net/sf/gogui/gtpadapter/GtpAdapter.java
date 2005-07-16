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

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (cmd.equals("black"))
            status = cmdPlay(GoColor.BLACK, cmdArray, response);
        else if (cmd.equals("boardsize"))
            status = cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board"))
            status = cmdClearBoard(response);
        else if (cmd.equals("final_score") && m_noScore)
            status = cmdUnknown(response);
        else if (cmd.equals("final_status_list") && m_noScore)
            status = cmdUnknown(response);
        else if (cmd.equals("genmove"))
            status = cmdGenmove(cmdArray, response);
        else if (cmd.equals("genmove_black"))
            status = cmdGenmove(GoColor.BLACK, response);
        else if (cmd.equals("genmove_white"))
            status = cmdGenmove(GoColor.WHITE, response);
        else if (cmd.equals("gg-undo"))
            status = cmdGGUndo(cmdArray, response);
        else if (cmd.equals("gtpadapter_showboard"))
            status = cmdGtpAdapterShowBoard(response);
        else if (cmd.equals("help"))
            status = cmdListCommands(response);
        else if (cmd.equals("list_commands"))
            status = cmdListCommands(response);
        else if (cmd.equals("loadsgf"))
            status = cmdLoadsgf(cmdArray, response);
        else if (cmd.equals("name") && m_name != null)
            status = cmdName(response);
        else if (cmd.equals("place_free_handicap") && m_emuHandicap)
            status = cmdPlaceFreeHandicap(cmdArray, response);
        else if (cmd.equals("play"))
            status = cmdPlay(cmdArray, response);
        else if (cmd.equals("protocol_version"))
            response.append(m_version1 ? "1" : "2");
        else if (cmd.equals("quit"))
            status = cmdQuit(response);
        else if (cmd.equals("set_free_handicap") && m_emuHandicap)
            status = cmdSetFreeHandicap(cmdArray, response);
        else if (cmd.equals("version") && m_name != null)
            status = cmdVersion(response);
        else if (cmd.equals("undo"))
            status = cmdUndo(cmdArray, response);
        else if (cmd.equals("white"))
            status = cmdPlay(GoColor.WHITE, cmdArray, response);
        else
            status = send(cmdLine, response);
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
        if (! send("estimate_score", programResponse))
            return false;
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

    private boolean cmdBoardsize(String cmdArray[], StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (argument.m_integer < 1)
        {
            response.append("Invalid board size");
            return false;
        }
        if (m_size > 0 && argument.m_integer != m_size)
        {
            response.append("Boardsize must be " + m_size);
            return false;
        }
        String command = m_gtp.getCommandBoardsize(argument.m_integer);
        if (command != null && ! send(command, response))
            return false;
        m_boardSize = argument.m_integer;
        m_board = new Board(m_boardSize);
        m_passInserted.clear();
        command = m_gtp.getCommandClearBoard(m_boardSize);
        if (! send(command, response))
            return false;
        return true;
    }

    private boolean cmdClearBoard(StringBuffer response)
    {
        String command = m_gtp.getCommandClearBoard(m_boardSize);
        if (! send(command, response))
            return false;
        m_board = new Board(m_boardSize);
        m_passInserted.clear();
        return true;
    }

    private boolean cmdGenmove(String cmdArray[], StringBuffer response)
    {
        ColorArgument argument = parseColorArgument(cmdArray, response);
        if (argument == null)
            return false;
        return cmdGenmove(argument.m_color, response);
    }

    private boolean cmdGenmove(GoColor color, StringBuffer response)
    {
        if (checkResign(color, response))
            return true;
        String command = m_gtp.getCommandGenmove(color);
        if (! fillPass(color, response))
            return false;
        if (! send(command, response))
        {
            StringBuffer undoResponse = new StringBuffer();
            undoFillPass(undoResponse);
            return false;
        }
        if (response.toString().trim().equals("resign"))
            return true;
        try
        {
            GoPoint point =
                GtpUtils.parsePoint(response.toString(), m_boardSize);
            m_board.play(Move.create(point, color));
            return true;
        }
        catch (GtpError e)
        {
            response.append(" (program played illegal move)");
            m_board.play(Move.create(null, color));
            return false;
        }
    }

    private boolean cmdGGUndo(String cmdArray[], StringBuffer response)
    {
        if (cmdArray.length > 2)
        {
            
            response.append("Too many arguments");
            return false;
        }
        int n = 1;
        if (cmdArray.length == 2)
        {
            try
            {
                n = Integer.parseInt(cmdArray[1]);
                if (n <= 0)
                {                    
                    response.append("Argument must be positive");
                    return false;
                }
                if (n > m_board.getMoveNumber())
                {                    
                    response.append("Not enough moves");
                    return false;
                }
            }
            catch (NumberFormatException e)
            {
                response.append("Invalid argument");
                return false;
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
        if (! send("gg-undo " + total, response))
        {
            while (! stack.empty())
                m_passInserted.push(stack.pop());
            return false;
        }
        m_board.undo(total);
        return true;
    }

    private boolean cmdGtpAdapterShowBoard(StringBuffer response)
    {
        OutputStream outputStream = new ByteArrayOutputStream(2048);
        PrintStream printStream = new PrintStream(outputStream);
        BoardUtils.print(m_board, printStream);
        response.append("\n");
        response.append(outputStream.toString());
        return true;
    }

    private boolean cmdListCommands(StringBuffer response)
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
        return true;
    }

    private boolean cmdLoadsgf(String[] cmdArray, StringBuffer response)
    {
        if (cmdArray.length < 2)
        {
            response.append("Need filename argument");
            return false;
        }
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
                response.append("Invalid move number argument");
                return false;
            }
        }
        if (! m_emuLoadsgf && ! send(command, response))
                return false;
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
                    {
                        if (! play(move.getColor(), move.getPoint(),
                                   response))
                            return false;
                    }
                    else
                        m_board.play(move);
                }
                toMove = node.getToMove();
                node = node.getChild();
            }
            if (toMove != GoColor.EMPTY && toMove != m_board.getToMove())
            {
                if (m_emuLoadsgf)
                {
                    if (! play(m_board.getToMove(), null, response))
                        return false;
                }
                else
                    m_board.setToMove(toMove);
            }
        }
        catch (FileNotFoundException e)
        {
            response.append("File not found");
            return false;
        }
        catch (SgfReader.SgfError e)
        {
            response.append("Could not read file");
            return false;
        }
        return true;
    }

    private boolean cmdName(StringBuffer response)
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index < 0)
            response.append(m_name);
        else
            response.append(m_name.substring(0, index));
        return true;
    }

    private boolean cmdPlaceFreeHandicap(String cmdArray[],
                                         StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        Vector stones =
            Board.getHandicapStones(m_boardSize, argument.m_integer);
        if  (stones == null)
        {
            response.append("Invalid number of handicap stones");
            return false;
        }
        StringBuffer pointList = new StringBuffer(128);
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint point = (GoPoint)stones.get(i);
            if (! play(GoColor.BLACK, point, response))
                break;
            if (pointList.length() > 0)
                pointList.append(" ");
            pointList.append(point);
        }
        response.append(pointList);
        return true;
    }

    private boolean cmdPlay(String cmdArray[], StringBuffer response)
    {
        ColorPointArgument argument =
            parseColorPointArgument(cmdArray, response, m_boardSize);
        if (argument == null)
            return false;
        return play(argument.m_color, argument.m_point, response);
    }

    private boolean cmdPlay(GoColor color, String cmdArray[],
                            StringBuffer response)
    {
        PointArgument argument =
            parsePointArgument(cmdArray, response, m_boardSize);
        if (argument == null)
            return false;
        return play(color, argument.m_point, response);
    }

    private boolean cmdQuit(StringBuffer response)
    {
        return send("quit", response);
    }

    private boolean cmdSetFreeHandicap(String cmdArray[],
                                       StringBuffer response)
    {
        for (int i = 1; i < cmdArray.length; ++i)
        {
            GoPoint point;
            try
            {
                point = GtpUtils.parsePoint(cmdArray[i], m_boardSize);
            }
            catch (GtpError e)
            {
                response.append("Invalid vertex");
                return false;
            }
            if (! play(GoColor.BLACK, point, response))
                return false;
        }
        return true;
    }

    private boolean cmdUndo(String cmdArray[], StringBuffer response)
    {
        if (cmdArray.length != 1)
        {
            response.append("No arguments allowed");
            return false;
        }
        return undo(response);
    }

    private boolean cmdUnknown(StringBuffer response)
    {
        response.append("Unknown command");
        return false;
    }

    private boolean cmdVersion(StringBuffer response)
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index >= 0)
            response.append(m_name.substring(index + 1));
        return true;
    }

    private boolean fillPass(GoColor color, StringBuffer response)
    {
        if (! m_fillPasses)
            return true;
        GoColor toMove = m_board.getToMove();
        if (color == toMove)
        {
            m_passInserted.push(Boolean.FALSE);
            return true;
        }
        String command = m_gtp.getCommandPlay(Move.create(null, toMove));
        if (send(command, response))
        {
            m_passInserted.push(Boolean.TRUE);
            return true;
        }
        m_passInserted.push(Boolean.FALSE);
        return false;
    }

    private boolean play(GoColor color, GoPoint point, StringBuffer response)
    {
        if (! fillPass(color, response))
            return false;
        String command = m_gtp.getCommandPlay(Move.create(point, color));
        if (send(command, response))
        {
            m_board.play(Move.create(point, color));
            return true;
        }
        return false;
    }

    private boolean send(String cmd, StringBuffer response)
    {
        try
        {
            response.append(m_gtp.sendCommand(cmd));
            return true;
        }
        catch (GtpError error)
        {
            response.append(error.getMessage());
            return false;
        }
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
                    StringBuffer response = new StringBuffer();
                    if (! send(line, response))
                    {
                        System.err.println("Sending commands aborted:"
                                           + response);
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

    private boolean undo(StringBuffer response)
    {
        if (send("undo", response))
        {
            m_board.undo();
            return undoFillPass(response);
        }
        return false;
    }

    private boolean undoFillPass(StringBuffer response)
    {
        if (! m_fillPasses)
            return true;
        Boolean passInserted = (Boolean)m_passInserted.pop();
        if (passInserted.booleanValue())
            return send("undo", response);
        return true;
    }
}

//----------------------------------------------------------------------------
