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
        if (white.equals(""))
            throw new Exception("No white program set.");
        m_black = new Gtp(black, verbose, null);
        m_black.setLogPrefix("B");
        m_white = new Gtp(white, verbose, null);
        m_white.setLogPrefix("W");
        m_inconsistentState = false;
        m_blackToMove.add(Boolean.TRUE);
        String blackName = getName(m_black);
        String whiteName = getName(m_white);
        m_name = "TwoGtp (" + blackName + " - " + whiteName + ")";
    }

    public boolean handleCommand(String command, StringBuffer response)
    {
        boolean status = true;
        if (command.startsWith("twogtp_black"))
        {
            status = twogtpColor(m_black, command, response);
        }
        else if (command.startsWith("twogtp_white"))
        {
            status = twogtpColor(m_white, command, response);
        }
        else if (command.equals("quit"))
        {
            status = sendBoth(command, response, false, false);
        }
        else if (command.startsWith("black"))
        {
            status = sendBoth(command, response, true, true);
            m_blackToMove.push(Boolean.FALSE);
        }
        else if (command.startsWith("white"))
        {
            status = sendBoth(command, response, true, true);
            m_blackToMove.push(Boolean.TRUE);
        }
        else if (command.startsWith("undo"))
        {
            status = sendBoth(command, response, true, false);
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
            status = sendBoth(command, response, true, false);
            if (status)
                m_inconsistentState = false;
            m_blackToMove.clear();
            m_blackToMove.push(Boolean.TRUE);
        }
        else if (command.startsWith("komi")
                 || command.startsWith("scoring_system"))
            status = sendBoth(command, response, false, false);
        else if (command.equals("name"))
            response.append(m_name);
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
                            "twogtp_black\n" +
                            "twogtp_white\n" +
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
        m_black.sendInterrupt();
        m_white.sendInterrupt();
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
                    "Usage: java -jar twogtp.jar [options]\n" +
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

    private boolean m_inconsistentState;

    private Stack m_blackToMove = new Stack();

    private String m_name;

    private Gtp m_black;

    private Gtp m_white;

    private boolean checkInconsistentState(StringBuffer response)
    {
        if (m_inconsistentState)
            response.append("Inconsistent state");
        return m_inconsistentState;
    }

    private static String getName(Gtp gtp)
    {
        try
        {
            String name = gtp.sendCommand("name");
            if (! name.trim().equals(""))
                return name;
        }
        catch (Gtp.Error e)
        {
        }
        return "Unknown";
    }

    private void mergeResponse(StringBuffer response,
                               String response1, String response2,
                               String prefix1, String prefix2)
    {
        boolean empty1 = (response1 == null || response1.equals(""));
        boolean empty2 = (response2 == null || response2.equals(""));
        if (empty1 && empty2)
            return;
        if (! empty1)
        {
            response.append(prefix1);
            response.append(": ");
            response.append(response1);
            if (! empty2)
                response.append("  ");
        }
        if (! empty2)
        {
            response.append(prefix2);
            response.append(": ");
            response.append(response2);
        }
    }

    private boolean send(Gtp gtp1, Gtp gtp2, String command1, String command2,
                         StringBuffer response, boolean changesState,
                         boolean tryUndo)
    {
        assert((gtp1 == m_black && gtp2 == m_white)
               || (gtp1 == m_white && gtp2 == m_black));
        if (changesState && checkInconsistentState(response))
            return false;
        String prefix1 = (gtp1 == m_black ? "B" : "W");
        String prefix2 = (gtp2 == m_black ? "B" : "W");
        String response1 = null;
        String response2 = null;
        boolean status = true;
        try
        {
            response1 = gtp1.sendCommand(command1);
        }
        catch (Gtp.Error e)
        {
            response1 = e.getMessage();
            status = false;
            if (changesState)
            {
                mergeResponse(response, response1, response2, prefix1,
                              prefix2);
                return status;
            }
        }
        try
        {
            response2 = gtp2.sendCommand(command2);
        }
        catch (Gtp.Error e)
        {
            response2 = e.getMessage();
            if (changesState && status)
            {
                if (tryUndo)
                {
                    try
                    {
                        gtp1.sendCommand("undo");
                    }
                    catch (Gtp.Error errorUndo)
                    {
                        m_inconsistentState = true;
                    }
                }
                else
                    m_inconsistentState = true;
            }
            status = false;
        }
        mergeResponse(response, response1, response2, prefix1, prefix2);
        return status;
    }

    private boolean sendBoth(String command, StringBuffer response,
                             boolean changesState, boolean tryUndo)
    {
        return send(m_black, m_white, command, command, response, changesState,
                    tryUndo);
    }

    private boolean sendGenmove(String command, StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        Gtp gtp1;
        Gtp gtp2;
        String prefix1;
        String prefix2;
        if (command.startsWith("genmove_black"))
        {
            gtp1 = m_black;
            gtp2 = m_white;
            prefix1 = "B";
            prefix2 = "W";
        }
        else
        {
            gtp1 = m_white;
            gtp2 = m_black;
            prefix1 = "W";
            prefix2 = "B";
        }
        String response1 = null;
        String response2 = null;
        try
        {
            response1 = gtp1.sendCommand(command);
        }
        catch (Gtp.Error e)
        {
            response1 = e.getMessage();
            mergeResponse(response, response1, response2, prefix1,
                          prefix2);
            return false;
        }
        StringBuffer command2 = new StringBuffer(command);
        StringUtils.replace(command2, "genmove_", "");
        command2.append(' ');
        command2.append(response1);
        try
        {
            response2 = gtp2.sendCommand(command2.toString());
        }
        catch (Gtp.Error e)
        {
            response2 = e.getMessage();
            try
            {
                gtp1.sendCommand("undo");
            }
            catch (Gtp.Error errorUndo)
            {
                m_inconsistentState = true;
            }
            mergeResponse(response, response1, response2, prefix1, prefix2);
            return false;
        }
        response.append(response1);
        return true;
    }

    private boolean twogtpColor(Gtp gtp, String command, StringBuffer response)
    {
        int index = command.indexOf(' ');
        if (index < 0)
        {
            response.append("Missing argument");
            return false;
        }
        command = command.substring(index).trim();
        try
        {
            response.append(gtp.sendCommand(command));
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }        
        return true;
    }
}

//-----------------------------------------------------------------------------
