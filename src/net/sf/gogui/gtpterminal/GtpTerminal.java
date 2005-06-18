//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpterminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** Simple text based interface to Go programs supporting GTP. */
public class GtpTerminal
    implements Gtp.IOCallback
{
    public GtpTerminal(String program, int size, boolean verbose)
        throws Exception
    {
        if (program.equals(""))
            throw new Exception("No program set.");
        m_verbose = verbose;
        m_gtp = new Gtp(program, verbose, this);
        m_gtp.queryProtocolVersion();
        m_gtp.querySupportedCommands();
        initGame(size);
    }

    public void close()
    {
        m_gtp.close();
        m_gtp.waitForExit();
    }

    public void mainLoop() throws IOException
    {
        newGame(m_board.getSize());
        printBoard();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));
        try
        {
            while (true)
            {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.equals(""))
                    continue;
                if (handleCommand(line))
                    break;
            }
        }
        finally
        {
            System.out.println();
        }
    }
    
    public void receivedInvalidResponse(String s)
    {
        System.out.print(s);
    }
    
    public void receivedResponse(boolean error, String s)
    {
    }
    
    public void receivedStdErr(String s)
    {
        // If m_verbose, logging is already done by Gtp
        if (! m_verbose)
            System.err.print(s);
    }

    public void sentCommand(String s)
    {
    }

    private final boolean m_verbose;

    private Board m_board;

    private GameTree m_gameTree;

    private final Gtp m_gtp;

    private Node m_currentNode;

    private void cmdPlay(GoPoint point)
    {
        if (! cmdPlay(m_board.getToMove(), point))
            return;
        printBoard();
        genmove();
    }

    private boolean cmdPlay(GoColor color, GoPoint point)
    {
        String command = m_gtp.getCommandPlay(color);
        command = command + " " + GoPoint.toString(point);
        StringBuffer response = new StringBuffer();
        if (! send(command, response))
        {
            System.out.println(response);
            return false;
        }
        play(Move.create(point, color));
        return true;
    }

    private void genmove()
    {
        GoColor toMove = m_board.getToMove();
        String command = m_gtp.getCommandGenmove(toMove);
        StringBuffer response = new StringBuffer();
        if (! send(command, response))
        {
            System.out.println(response);
            return;
        }
        try
        {
            GoPoint point =
                GtpUtils.parsePoint(response.toString(), m_board.getSize());
            System.out.println("Computer move: " + GoPoint.toString(point));
            play(Move.create(point, toMove));
            printBoard();
        }
        catch (GtpError error)
        {
            System.out.println(response);
        }
    }

    /** Handle command line from user.
        @return true if quit command received.
    */
    private boolean handleCommand(String cmdLine)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        if (cmd.equals("quit"))
        {
            send("quit");
            return true;
        }
        if (cmd.equals("genmove"))
            genmove();
        else if (cmd.equals("help"))
            help();
        else if (cmd.equals("list"))
            listCommands();
        else if (cmd.equals("load"))
            load(cmdArray);
        else if (cmd.equals("newgame"))
            newGame(cmdArray);
        else if (cmd.equals("save"))
            save(cmdArray);
        else if (cmd.equals("undo"))
            undo();
        else if (cmd.equals("black")
                 || cmd.equals("boardsize")
                 || cmd.equals("clear_board")
                 || cmd.equals("genmove_white")
                 || cmd.equals("genmove_black")
                 || cmd.equals("white"))
            System.out.println("Command not allowed");
        else
        {
            try
            {
                GoPoint point
                    = GtpUtils.parsePoint(cmdLine, m_board.getSize());
                cmdPlay(point);
            }
            catch (GtpError error)
            {
                StringBuffer response = new StringBuffer();
                send(cmdLine, response);
                System.out.println(response);
            }
        }
        return false;
    }

    private void help()
    {
        System.out.print("Enter a move or one of the following commands:\n" +
                         "  genmove\n" +
                         "  help\n" +
                         "  load\n" +
                         "  list\n" +
                         "  newgame\n" +
                         "  save\n" +
                         "  undo\n" +
                         "  quit\n" +
                         "The following commands are not allowed:\n" +
                         "  black, boardsize, clear_board, genmove_black\n" +
                         "  genmove_white, white\n" +
                         "Other commands are forwarded to the program.\n");
    }

    private void initGame(int size)
    {
        m_board = new Board(size);
        m_gameTree = new GameTree(size, 0, null, null, null);
        m_currentNode = m_gameTree.getRoot();
    }

    private void listCommands()
    {
        try
        {
            m_gtp.querySupportedCommands();
        }
        catch (GtpError error)
        {
            System.out.println(error.getMessage());
            return;
        }
        Vector commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
            System.out.println((String)commands.get(i));
    }

    private void load(String[] cmdArray)
    {
        if (cmdArray.length < 2)
        {
            System.out.println("Need filename argument.");
            return;
        }
        File file = new File(cmdArray[1]);
        try
        {
            FileInputStream fileStream = new FileInputStream(file);
            SgfReader reader =
                new SgfReader(fileStream, file.toString(), null, 0);
            String warnings = reader.getWarnings();
            if (warnings != null)
                System.out.print(warnings);
            GameTree gameTree = reader.getGameTree();
            GameInformation gameInformation = gameTree.getGameInformation();
            if (gameInformation.m_handicap > 0)
            {
                System.out.println("Handicap games not supported.");
                return;
            }
            if (! newGame(gameInformation.m_boardSize))
                return;
            send("komi " + gameInformation.m_komi);
            Node node = gameTree.getRoot();
            while (node != null)
            {
                for (int i = 0; i < node.getNumberAddBlack(); ++i)
                    if (! cmdPlay(GoColor.BLACK, node.getAddBlack(i)))
                        return;
                for (int i = 0; i < node.getNumberAddWhite(); ++i)
                    if (! cmdPlay(GoColor.WHITE, node.getAddWhite(i)))
                        return;
                Move move = node.getMove();
                if (move != null
                    && ! cmdPlay(move.getColor(), move.getPoint()))
                        return;
                node = node.getChild();
            }
            printBoard();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found: " + file);
        }
        catch (SgfReader.SgfError e)
        {
            System.out.println("Could not read file " + file
                               + ": " + e.getMessage());
        }
    }

    private boolean newGame(int size)
    {
        String command;
        StringBuffer response = new StringBuffer();
        command = m_gtp.getCommandBoardsize(size);
        if (command != null && ! send(command, response))
        {
            System.out.println(response);
            return false;
        }
        command = m_gtp.getCommandClearBoard(size);
        if (! send(command, response))
        {
            System.out.println(response);
            return false;
        }
        initGame(size);
        return true;
    }

    private void newGame(String[] cmdArray)
    {
        int size = m_board.getSize();
        if (cmdArray.length > 1)
        {
            try
            {
                size = Integer.parseInt(cmdArray[1]);
            }
            catch (NumberFormatException exception)
            {
            }
        }
        newGame(size);
        printBoard();
    }

    private void play(Move move)
    {
        m_board.play(move);
        Node node = new Node(move);
        m_currentNode.append(node);
        m_currentNode = node;
    }

    private void printBoard()
    {
        BoardUtils.print(m_board, System.out);
    }

    private void save(String[] cmdArray)
    {
        if (cmdArray.length < 2)
        {
            System.out.println("Need filename argument.");
            return;
        }
        File file = new File(cmdArray[1]);
        try
        {
            OutputStream out = new FileOutputStream(file);
            new SgfWriter(out, m_gameTree, file, "GtpTerminal",
                           Version.get());
        }
        catch (FileNotFoundException e) 
        {
            System.out.println("Write error.");
            return;
        }
    }

    private String send(String cmd)
    {
        StringBuffer response = new StringBuffer();
        send(cmd, response);
        return response.toString();
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

    private void undo()
    {
        StringBuffer response = new StringBuffer();
        if (! send("undo", response))
        {
            System.out.println(response);
            return;
        }
        m_board.undo();
        m_currentNode = m_currentNode.getFather();
        printBoard();
    }
}

//----------------------------------------------------------------------------
