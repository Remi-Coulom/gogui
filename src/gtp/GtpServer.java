//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import utils.*;

//-----------------------------------------------------------------------------

class ReadThread
    extends Thread
{
    public ReadThread(InputStream in)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
    }

    public String getLine()
    {        
        synchronized (this)
        {
            notifyAll();
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted");
            }
            assert(m_receivedLine);
            m_receivedLine = false;
            return m_line;
        }
    }

    public void run()
    {
        try
        {
            synchronized (this)
            {
                while (! m_quit)
                {
                    assert(! m_receivedLine);
                    m_line = m_in.readLine();
                    if (m_line == null)
                        return;
                    m_line = m_line.trim();
                    if (m_line.equals("") || m_line.charAt(0) == '#')
                        continue;
                    m_receivedLine = true;
                    notifyAll();
                    wait();
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

    public void quit()
    {
        synchronized (this)
        {
            m_quit = true;
            notifyAll();
        }
    }

    private boolean m_quit = false;

    private boolean m_receivedLine = false;

    private BufferedReader m_in;

    private String m_line;
}

public abstract class GtpServer
{
    public GtpServer(InputStream in, OutputStream out)
    {
        m_out = new PrintStream(out);
        m_in = in;
    }

    public abstract boolean handleCommand(String command,
                                          StringBuffer response);

    public void mainLoop() throws IOException
    {
        m_quit = false;
        ReadThread readThread = new ReadThread(m_in);
        readThread.start();
        while (! m_quit)
        {
            String line = readThread.getLine();
            parseLine(line);
        }
        readThread.quit();
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

    public synchronized void parseLine(String line)
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
        try
        {
            m_commandId = Integer.parseInt(array[0]);
            m_commandHadId = true;
            command = buffer.substring(array[0].length());
        }
        catch (NumberFormatException e)
        {
            m_commandHadId = false;
        }
        StringBuffer response = new StringBuffer();
        boolean status = handleCommand(command.trim(), response);
        if (status)
            m_out.print('=');
        else
            m_out.print('?');
        if (m_commandHadId)
            m_out.print(m_commandId);
        m_out.print(' ');
        if (response.length() == 0
            || response.charAt(response.length() - 1) != '\n')
            response.append('\n');
        response.append('\n');
        m_out.print(response.toString());
    }
}

//-----------------------------------------------------------------------------
