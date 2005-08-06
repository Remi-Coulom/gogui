//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** GTP command. */
public class GtpCommand
{
    public GtpCommand()
    {
    }

    public GtpCommand(String line)
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
        m_hasId = false;
        m_command = command;
        try
        {
            m_hasId = true;
            m_id = Integer.parseInt(array[0]);
            m_command = buffer.substring(array[0].length()).trim();
        }
        catch (NumberFormatException e)
        {
            m_hasId = false;
        }
    }

    public boolean hasId()
    {
        return m_hasId; 
    }

    /** Full command without ID. */
    public String getCommand()
    {
        return m_command;
    }

    public int id()
    {
        assert(hasId());
        return m_id;
    }

    public boolean isQuit()
    {
        return m_command.trim().toLowerCase().equals("quit");
    }

    private boolean m_hasId;
    
    private int m_id;
    
    private String m_command;

}

//----------------------------------------------------------------------------
