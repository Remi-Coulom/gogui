// Terminal.java

package net.sf.gogui.tools.terminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtil;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK_WHITE;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** Simple text based interface to Go programs supporting GTP. */
public class Terminal
    implements GtpClient.IOCallback
{
    public Terminal(String program, int size, boolean verbose)
        throws Exception
    {
        if (program.equals(""))
            throw new Exception("No program set");
        m_verbose = verbose;
        m_gtp = new GtpClient(program, null, verbose, this);
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

    /** Colorize go board text output.
        @see Argument color in
        net.sf.gogui.go.Board.toString(ConstBoard,boolean,boolean) */
    public void setColor(boolean enable)
    {
        m_color = enable;
    }

    private final boolean m_verbose;

    private boolean m_color;

    private Board m_board;

    private GameTree m_tree;

    private final GtpClient m_gtp;

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
        String command = m_gtp.getCommandPlay(Move.get(color, point));
        StringBuilder response = new StringBuilder();
        if (! send(command, response))
        {
            System.out.println(response);
            return false;
        }
        play(Move.get(color, point));
        return true;
    }

    private void genmove()
    {
        GoColor toMove = m_board.getToMove();
        String command = m_gtp.getCommandGenmove(toMove);
        StringBuilder response = new StringBuilder();
        if (! send(command, response))
        {
            System.out.println(response);
            return;
        }
        try
        {
            GoPoint point =
                GtpUtil.parsePoint(response.toString(), m_board.getSize());
            System.out.println("Computer move: " + GoPoint.toString(point));
            play(Move.get(toMove, point));
            printBoard();
        }
        catch (GtpResponseFormatError e)
        {
            System.out.println(response);
        }
    }

    /** Handle command line from user.
        @return true if quit command received. */
    private boolean handleCommand(String cmdLine)
    {
        String[] cmdArray = StringUtil.splitArguments(cmdLine);
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
            undo(cmdArray);
        else if (GtpUtil.isStateChangingCommand(cmd))
            System.out.println("Command not allowed");
        else
        {
            try
            {
                GoPoint point
                    = GtpUtil.parsePoint(cmdLine, m_board.getSize());
                cmdPlay(point);
            }
            catch (GtpResponseFormatError e)
            {
                StringBuilder response = new StringBuilder();
                send(cmdLine, response);
                System.out.println(response);
            }
        }
        return false;
    }

    private void help()
    {
        String text =
            "Enter a move or one of the following commands:\n" +
            "genmove, help, load, list, newgame, save, undo, quit.\n" +
            "GTP commands that change the board state are not allowed.\n" +
            "Other GTP commands are forwarded to the program.\n";
        System.out.print(text);
    }

    private void initGame(int size)
    {
        m_board = new Board(size);
        m_tree = new GameTree(size, null, null, null, null);
        setCurrentNode(m_tree.getRoot());
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
        ArrayList<String> commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
            System.out.println(commands.get(i));
    }

    private void load(String[] cmdArray)
    {
        if (cmdArray.length < 2)
        {
            System.out.println("Need filename argument");
            return;
        }
        File file = new File(cmdArray[1]);
        try
        {
            FileInputStream fileStream = new FileInputStream(file);
            SgfReader reader = new SgfReader(fileStream, file, null, 0);
            String warnings = reader.getWarnings();
            if (warnings != null)
                System.out.print(warnings);
            GameTree tree = reader.getTree();
            GameInfo info = tree.getGameInfo(tree.getRoot());
            if (info.getHandicap() > 0)
            {
                System.out.println("Handicap games not supported");
                return;
            }
            if (! newGame(tree.getBoardSize()))
                return;
            send("komi " + info.getKomi());
            ConstNode node = tree.getRoot();
            while (node != null)
            {
                for (GoColor c : BLACK_WHITE)
                {
                    for (GoPoint stone : node.getSetup(c))
                        if (! cmdPlay(c, stone))
                            return;
                }
                Move move = node.getMove();
                if (move != null
                    && ! cmdPlay(move.getColor(), move.getPoint()))
                        return;
                node = node.getChildConst();
            }
            printBoard();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found: " + file);
        }
        catch (SgfError e)
        {
            System.out.println("Could not read file " + file
                               + ": " + e.getMessage());
        }
    }

    private boolean newGame(int size)
    {
        String command;
        StringBuilder response = new StringBuilder();
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
        System.out.print(BoardUtil.toString(m_board, true, m_color));
    }

    private void save(String[] cmdArray)
    {
        if (cmdArray.length < 2)
        {
            System.out.println("Need filename argument");
            return;
        }
        File file = new File(cmdArray[1]);
        try
        {
            OutputStream out = new FileOutputStream(file);
            new SgfWriter(out, m_tree, "gogui-terminal", Version.get());
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Write error");
            return;
        }
    }

    private String send(String cmd)
    {
        StringBuilder response = new StringBuilder();
        send(cmd, response);
        return response.toString();
    }

    private boolean send(String cmd, StringBuilder response)
    {
        try
        {
            response.append(m_gtp.send(cmd));
            return true;
        }
        catch (GtpError error)
        {
            response.append(error.getMessage());
            return false;
        }
    }

    private void setCurrentNode(ConstNode node)
    {
        m_currentNode = m_tree.getNode(node);
    }

    private void undo(String[] cmdArray)
    {
        if (cmdArray.length > 1)
        {
            System.out.println("undo command takes no arguments");
            return;
        }
        StringBuilder response = new StringBuilder();
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
