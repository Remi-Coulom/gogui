//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtpterminal;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import go.*;
import gtp.*;
import utils.*;
import version.*;

//-----------------------------------------------------------------------------

public class GtpTerminal
    implements Gtp.IOCallback
{
    public GtpTerminal(String program, int size)
        throws Exception
    {
        if (program.equals(""))
            throw new Exception("No program set.");
        m_gtp = new Gtp(program, false, this);
        m_gtp.queryProtocolVersion();
        m_gtp.querySupportedCommands();
        m_board = new Board(size);
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
                "size:"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpTerminal " + Version.get());
                System.exit(0);
            }
            int size = opt.getInteger("size", 19, 1);
            Vector arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            GtpTerminal gtpTerminal = new GtpTerminal(program, size);
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
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            System.exit(-1);
        }
    }

    public void mainLoop() throws IOException
    {
        newGame();
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
    
    public void receivedResponse(boolean error, String s)
    {
    }
    
    public void receivedStdErr(String s)
    {
        System.err.print(s);
    }

    public void sentCommand(String s)
    {
    }

    private Board m_board;

    private Gtp m_gtp;

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
            m_board.play(new Move(point, toMove));
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
        else if (cmd.equals("list_commands"))
            listCommands();
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
                play(point);
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
                         "  list_commands\n" +
                         "  undo\n" +
                         "  quit\n" +
                         "The following commands are not allowed:\n" +
                         "  black, boardsize, clear_board, genmove_black\n" +
                         "  genmove_white, white\n" +
                         "Other commands are forwarded to the program.\n");
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

    private void newGame()
    {
        String command;
        StringBuffer response = new StringBuffer();
        command = m_gtp.getCommandBoardsize(m_board.getSize());
        if (command != null && ! send(command, response))
        {
            System.out.println(response);
            return;
        }
        command = m_gtp.getCommandClearBoard(m_board.getSize());
        if (! send(command, response))
        {
            System.out.println(response);
            return;
        }
    }

    private void play(Point point)
    {
        Color toMove = m_board.getToMove();
        String command = m_gtp.getCommandPlay(toMove);
        command = command + " " + Point.toString(point);
        StringBuffer response = new StringBuffer();
        if (! send(command, response))
        {
            System.out.println(response);
            return;
        }
        m_board.play(new Move(point, toMove));
        printBoard();
        genmove();
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
            "-size n       board size (default 19)\n" +
            "-version      print version and exit\n";
        out.print(helpText);
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
        printBoard();
    }
}

//-----------------------------------------------------------------------------
