//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import utils.StringUtils;

//-----------------------------------------------------------------------------

class Command
{
    public boolean m_hasId;
    
    public int m_id;
    
    public String m_command;

    public boolean isQuit()
    {
        return m_command.trim().toLowerCase().equals("quit");
    }
}

class ReadThread extends Thread
{
    public ReadThread(GtpServer gtpServer, InputStream in, PrintStream out,
                      boolean log)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
        m_out = out;
        m_gtpServer = gtpServer;
        m_log = log;
    }

    public boolean endOfFile()
    {
        return m_endOfFile;
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
            assert(m_endOfFile || ! m_waitCommand);
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
                {
                    m_endOfFile = true;
                    return;
                }
                if (m_log)
                    m_gtpServer.log(line);
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
                    m_command = parseLine(line);
                    notifyAll();
                    m_waitCommand = false;
                    if (m_command.isQuit())
                        return;
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

    private boolean m_endOfFile = false;

    private boolean m_log;

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
        String[] array = StringUtils.tokenize(buffer.toString());
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
    public GtpServer(InputStream in, OutputStream out, PrintStream log)
    {
        m_out = new PrintStream(out);
        m_in = in;
        m_log = log;
    }

    /** Callback for interrupting commands.
        This callback will be invoked if the special comment line
        "# interrupt" is received. It will be invoked from a different thread.
    */
    public abstract void interruptCommand();

    public abstract boolean handleCommand(String command,
                                          StringBuffer response);

    public synchronized void log(String line)
    {
        assert(m_log != null);
        m_log.println(line);
    }

    public void mainLoop() throws IOException
    {
        ReadThread readThread = new ReadThread(this, m_in, m_out,
                                               m_log != null);
        readThread.start();
        while (true)
        {
            Command command = readThread.getCommand();
            if (readThread.endOfFile())
                return;
            sendResponse(command);
            if (command.isQuit())
                return;
        }
    }

    public void respond(boolean status, boolean hasId, int id, String response)
    {
        StringBuffer fullResponse = new StringBuffer(256);
        if (status)
            fullResponse.append('=');
        else
            fullResponse.append('?');
        if (hasId)
            fullResponse.append(id);
        fullResponse.append(' ');
        fullResponse.append(response);
        if (response.length() == 0
            || response.charAt(response.length() - 1) != '\n')
            fullResponse.append('\n');
        m_out.println(fullResponse);
        if (m_log != null)
            m_log.println(fullResponse);
    }

    private boolean m_commandHadId;

    private int m_commandId;

    private InputStream m_in;

    private PrintStream m_log;

    private PrintStream m_out;

    private void sendResponse(Command cmd)
    {
        StringBuffer response = new StringBuffer();
        boolean status = handleCommand(cmd.m_command.trim(), response);
        respond(status, cmd.m_hasId, cmd.m_id, response.toString());
    }
}

//-----------------------------------------------------------------------------
