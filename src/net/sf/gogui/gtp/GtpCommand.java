//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

//----------------------------------------------------------------------------

/** GTP command. */
class GtpCommand
{
    public boolean m_hasId;
    
    public int m_id;
    
    public String m_command;

    public boolean isQuit()
    {
        return m_command.trim().toLowerCase().equals("quit");
    }
}

//----------------------------------------------------------------------------
