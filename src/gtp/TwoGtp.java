//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import utils.*;

//-----------------------------------------------------------------------------

public class TwoGtp
    extends GtpServer
{
    public TwoGtp(InputStream in, OutputStream out, String black, String white,
                  boolean verbose)
        throws Exception
    {
        super(in, out);
        if (black.equals(""))
            throw new Exception("No black program set.");
        m_black = new Gtp(black, verbose, null);
        if (white.equals(""))
            throw new Exception("No white program set.");
        m_white = new Gtp(white, verbose, null);
        m_blackToMove.add(Boolean.TRUE);
    }

    public boolean handleCommand(String command, StringBuffer response)
    {
        boolean status = true;
        if (command.equals("quit"))
        {
            status = sendBoth(command, response);
        }
        else if (command.startsWith("black"))
        {
            status = sendBoth(command, response);
            m_blackToMove.push(Boolean.FALSE);
        }
        else if (command.startsWith("white"))
        {
            status = sendBoth(command, response);
            m_blackToMove.push(Boolean.TRUE);
        }
        else if (command.startsWith("undo"))
        {
            status = sendBoth(command, response);
            m_blackToMove.pop();
        }
        else if (command.startsWith("genmove_black"))
        {
            status = sendGenmove(command, response);
            m_blackToMove.push(Boolean.FALSE);
        }
        else if (command.startsWith("genmove_white"))
        {
            status = sendGenmove(command, response);
            m_blackToMove.push(Boolean.TRUE);
        }
        else if (command.startsWith("boardsize"))
        {
            status = sendBoth(command, response);
            m_blackToMove.clear();
            m_blackToMove.push(Boolean.TRUE);
        }
        else if (command.startsWith("komi")
                 || command.startsWith("scoring_system"))
            status = sendBoth(command, response);
        else if (command.equals("name"))
            response.append("TwoGtp");
        else if (command.equals("version"))
            ;
        else if (command.equals("protocol_version"))
            response.append("1");
        else if (command.equals("help"))
            response.append("quit\n" +
                            "black\n" +
                            "white\n" +
                            "undo\n" +
                            "genmove_black\n" +
                            "genmove_white\n" +
                            "boardsize\n" +
                            "komi\n" +
                            "scoring_system\n" +
                            "name\n" +
                            "version\n");
        else
        {
            response.append("unknown command");
            status = false;
        }
        return status;
    }

    public void interruptCommand()
    {
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "black:",
                "help",
                "verbose",
                "white:"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -cp gogui.jar gtp.TwoGtp [options]\n" +
                    "\n" +
                    "-black   command for black program\n" +
                    "-help    display this help and exit\n" +
                    "-verbose log GTP streams to stderr\n" +
                    "-white   command for white program\n";
                System.out.print(helpText);
                System.exit(0);
            }
            boolean verbose = opt.isSet("verbose");
            String black = opt.getString("black", "");
            String white = opt.getString("white", "");
            TwoGtp twoGtp = new TwoGtp(System.in, System.out, black, white,
                                       verbose);
            twoGtp.mainLoop();
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private Stack m_blackToMove = new Stack();

    private Gtp m_black;

    private Gtp m_white;

    private boolean sendBoth(String command, StringBuffer response)
    {
        boolean status = true;
        try
        {
            response.append(m_black.sendCommand(command));
        }
        catch (Gtp.Error e)
        {
            response.append("B: ");
            response.append(e.getMessage());
            status = false;
        }
        try
        {
            response.append(m_white.sendCommand(command));
        }
        catch (Gtp.Error e)
        {
            if (! status)
                response.append("  ");
            response.append("W: ");
            response.append(e.getMessage());
            status = false;
        }
        return status;
    }

    private boolean sendGenmove(String command, StringBuffer response)
    {
        Gtp program;
        Gtp other;
        if (command.startsWith("genmove_black"))
        {
            program = m_black;
            other = m_white;
        }
        else
        {
            program = m_white;
            other = m_black;
        }
        try
        {
            response.append(program.sendCommand(command));
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }
        
        try
        {
            StringBuffer playCommand = new StringBuffer(command);
            StringUtils.replace(playCommand, "genmove_", "");
            playCommand.append(' ');
            playCommand.append(response);
            if (response.length() > 0)
                response.append(' ');
            response.append(other.sendCommand(playCommand.toString()));
        }
        catch (Gtp.Error e)
        {
            response.setLength(0);
            response.append(e.getMessage());
            return false;
        }
        return true;
    }
}

//-----------------------------------------------------------------------------
