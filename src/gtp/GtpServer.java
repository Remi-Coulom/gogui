//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import utils.*;

//-----------------------------------------------------------------------------

public abstract class GtpServer
{
    public GtpServer(InputStream in, OutputStream out)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
        m_out = new PrintStream(out);
    }

    public abstract boolean handleCommand(String command,
                                          StringBuffer response);

    public void mainLoop() throws IOException
    {
        m_quit = false;
        while (true && ! m_quit)
        {
            String line = m_in.readLine();
            if (line == null)
                return;
            line = line.trim();
            if (line.equals("") || line.charAt(0) == '#')
                continue;
            parseLine(line);
        }
    }

    protected void setQuit()
    {
        m_quit = true;
    }

    private boolean m_commandHadId;

    private boolean m_quit;

    private int m_commandId;

    private BufferedReader m_in;

    private PrintStream m_out;

    private void parseLine(String line)
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
