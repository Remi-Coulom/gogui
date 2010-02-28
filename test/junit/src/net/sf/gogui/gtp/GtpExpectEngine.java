// GtpExpectEngine.java

package net.sf.gogui.gtp;

import java.io.PrintStream;
import java.util.ArrayList;

/** GTP engine that expects a certain sequence of commands.
    Intended for testing GTP controllers. */
public final class GtpExpectEngine
    extends GtpEngine
{
    public GtpExpectEngine(PrintStream log)
    {
        super(log);
    }

    public void expect(String command)
    {
        String response = "";
        expect(command, response);
    }

    public void expect(String command, String response)
    {
        m_commands.add(command);
        m_responses.add(response);
    }

    public String getNextExpectedCommand()
    {
        if (m_commands.size() == 0)
            return null;
        return m_commands.get(0);
    }

    public void interruptCommand()
    {
    }

    public boolean isExpectQueueEmpty()
    {
        assert m_commands.size() == m_responses.size();
        return m_commands.size() == 0;
    }

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        assert m_commands.size() == m_responses.size();
        String line = cmd.getLine();
        if (m_commands.size() == 0)
            throw new GtpError("unexpected command: " + line);
        if (line.equals(m_commands.get(0)))
        {
            cmd.setResponse(m_responses.remove(0));
            m_commands.remove(0);
        }
        else
            throw new GtpError("expected '" + m_commands.get(0) + "' got :"
                               + line);
    }

    private ArrayList<String> m_commands = new ArrayList<String>();

    private ArrayList<String> m_responses = new ArrayList<String>();
}
