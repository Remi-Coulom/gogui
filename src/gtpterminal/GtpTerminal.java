//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtpterminal;

import java.io.*;
import java.util.*;
import game.*;
import go.*;
import gtp.*;
import utils.*;
import version.*;

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

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "help",
                "size:",
                "verbose",
                "version"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpTerminal " + Version.get());
                return;
            }
            int size = opt.getInteger("size", 19, 1);
            boolean verbose = opt.isSet("verbose");
            Vector arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            GtpTerminal gtpTerminal = new GtpTerminal(program, size, verbose);
            gtpTerminal.mainLoop();
            gtpTerminal.close();
        }
        catch (Error e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            System.err.println(StringUtils.formatException(t));
            System.exit(-1);
        }
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

    private boolean m_verbose;

    private Board m_board;

    private GameTree m_gameTree;

    private Gtp m_gtp;

    private Node m_currentNode;

    private void cmdPlay(Point point)
    {
        if (! cmdPlay(m_board.getToMove(), point))
            return;
        printBoard();
        genmove();
    }

    private boolean cmdPlay(Color color, Point point)
    {
        String command = m_gtp.getCommandPlay(color);
        command = command + " " + Point.toString(point);
        StringBuffer response = new StringBuffer();
        if (! send(command, response))
        {
            System.out.println(response);
            return false;
        }
        play(new Move(point, color));
        return true;
    }

    private void genmove()
    {
        Color toMove = m_board.getToMove();
        String command = m_gtp.getCommandGenmove(toMove);
        StringBuffer response = new StringBuffer();
        if (! send(command, response))
        {
            System.out.println(response);
            return;
        }
        try
        {
            Point point =
                Gtp.parsePoint(response.toString(), m_board.getSize());
            System.out.println("Computer move: " + Point.toString(point));
            play(new Move(point, toMove));
            printBoard();
        }
        catch (Gtp.Error error)
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
                Point point = Gtp.parsePoint(cmdLine, m_board.getSize());
                cmdPlay(point);
            }
            catch (Gtp.Error error)
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
        m_gameTree = new GameTree(size, 0, null, null);
        m_currentNode = m_gameTree.getRoot();
    }

    private void listCommands()
    {
        try
        {
            m_gtp.querySupportedCommands();
        }
        catch (Gtp.Error error)
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
            java.io.Reader fileReader = new FileReader(file);
            sgf.Reader reader = new sgf.Reader(fileReader, file.toString());
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
                    if (! cmdPlay(Color.BLACK, node.getAddBlack(i)))
                        return;
                for (int i = 0; i < node.getNumberAddWhite(); ++i)
                    if (! cmdPlay(Color.WHITE, node.getAddWhite(i)))
                        return;
                Move move = node.getMove();
                if (move != null)
                    if (! cmdPlay(move.getColor(), move.getPoint()))
                        return;
                node = node.getChild();
            }
            printBoard();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found: " + file);
        }
        catch (sgf.Reader.Error e)
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

    private static void printUsage(PrintStream out)
    {
        String helpText =
            "Usage: java -jar gtpterminal.jar program\n" +
            "\n" +
            "-config       config file\n" +
            "-help         print help and exit\n" +
            "-size n       board size (default 19)\n" +
            "-verbose      print debug information\n" +
            "-version      print version and exit\n";
        out.print(helpText);
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
            new sgf.Writer(out, m_gameTree, file, "GtpTerminal",
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
        catch (Gtp.Error error)
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
