//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import utils.*;

//-----------------------------------------------------------------------------

class Command
{
    boolean m_hasId;
    
    int m_id;
    
    String m_command;
}

class ReadThread extends Thread
{
    public ReadThread(GtpServer gtpServer, InputStream in, PrintStream out)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
        m_out = out;
        m_gtpServer = gtpServer;
    }

    public Command getCommand()
    {
        synchronized (this)
        {
            assert(! m_waitCommand);
            m_waitCommand = true;
            notifyAll();
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted");
            }
            assert(! m_waitCommand);
            Command result = m_command;
            m_command = null;
            return result;
        }
    }

    public void run()
    {
        try
        {            
            while (true)
            {
                String line;
                line = m_in.readLine();
                if (line == null)
                    return;
                line = line.trim();
                if (line.equals("# interrupt"))
                {
                    m_gtpServer.interruptCommand();
                }
                if (line.equals("") || line.charAt(0) == '#')
                    continue;
                synchronized (this)
                {
                    while (! m_waitCommand)
                    {
                        wait();
                    }
                    if (m_quit)
                        return;
                    m_command = parseLine(line);
                    notifyAll();
                    m_waitCommand = false;
                }
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            System.err.println(msg);
        }
    }

    public synchronized void quit()
    {
        m_quit = true;
        notifyAll();
    }

    private boolean m_quit = false;

    private boolean m_waitCommand = false;

    private BufferedReader m_in;

    private Command m_command;

    private PrintStream m_out;

    private GtpServer m_gtpServer;

    private Command parseLine(String line)
    {
        assert(! line.trim().equals(""));
        int len = line.length();
        StringBuffer buffer = new StringBuffer(len);
        boolean wasLastSpace = false;
        for (int i = 0; i < len; ++i)
        {
            char c = line.charAt(i);
            if (Character.isISOControl(c))
                continue;
            if (Character.isWhitespace(c))
            {
                if (! wasLastSpace)
                {
                    buffer.append(' ');
                    wasLastSpace = true;
                }
            }
            else
            {
                buffer.append(c);
                wasLastSpace = false;
            }
        }
        String[] array = StringUtils.split(buffer.toString(), ' ');
        assert(array.length > 0);
        String command = buffer.toString();
        Command result = new Command();
        result.m_hasId = false;
        result.m_command = command;
        try
        {
            result.m_hasId = true;
            result.m_id = Integer.parseInt(array[0]);
            result.m_command = buffer.substring(array[0].length());
        }
        catch (NumberFormatException e)
        {
            result.m_hasId = false;
        }
        return result;
    }
}

public abstract class GtpServer
{
    public GtpServer(InputStream in, OutputStream out)
    {
        m_out = new PrintStream(out);
        m_in = in;
    }

    /** Callback for interrupting commands.
        This callback will be invoked if the special comment line
        "# interrupt" is received. It will be invoked from a different thread.
    */
    public abstract void interruptCommand();

    public abstract boolean handleCommand(String command,
                                          StringBuffer response);

    public void mainLoop() throws IOException
    {
        m_quit = false;
        ReadThread readThread = new ReadThread(this, m_in, m_out);
        readThread.start();
        while (! m_quit)
        {
            Command command = readThread.getCommand();
            sendResponse(command);
        }
        readThread.quit();
    }

    public static void respond(PrintStream out, boolean status, boolean hasId,
                               int id, String response)
    {
        if (status)
            out.print('=');
        else
            out.print('?');
        if (hasId)
            out.print(id);
        out.print(' ');
        out.print(response);
        if (response.length() == 0
            || response.charAt(response.length() - 1) != '\n')
            out.print('\n');
        out.print('\n');
    }

    protected void setQuit()
    {
        m_quit = true;
    }

    private boolean m_commandHadId;

    private boolean m_quit;

    private int m_commandId;

    private InputStream m_in;

    private PrintStream m_out;

    private void sendResponse(Command cmd)
    {
        StringBuffer response = new StringBuffer();
        boolean status = handleCommand(cmd.m_command.trim(), response);
        respond(m_out, status, cmd.m_hasId, cmd.m_id, response.toString());
    }
}

//-----------------------------------------------------------------------------
