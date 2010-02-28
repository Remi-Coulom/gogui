// GtpError.java

package net.sf.gogui.gtp;

import net.sf.gogui.util.ErrorMessage;

/** Exception indicating the failure of a GTP command. */
public class GtpError
    extends ErrorMessage
{
    public GtpError(String s)
    {
        super(s);
    }

    /** The command that caused the error.
        Can return null, if the command is not known. */
    public String getCommand()
    {
        return m_command;
    }

    public void setCommand(String command)
    {
        m_command = command;
    }

    private String m_command;
}
