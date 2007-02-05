//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.util.TreeMap;

/** Pool of OptionalMessage instances used during a session. */
public class OptionalMessageSession
{
    public OptionalMessageSession(Component parent)
    {
        m_parent = parent;
    }

    public void showMessage(String id, String message)
    {
        get(id).showMessage(message);
    }

    public boolean showQuestion(String id, String message)
    {
        return get(id).showQuestion(message);
    }

    public void showWarning(String id, String message)
    {
        get(id).showWarning(message);
    }

    public boolean showWarningQuestion(String id, String message)
    {
        return get(id).showWarningQuestion(message);
    }

    public int showYesNoCancelQuestion(String id, String message)
    {
        return get(id).showYesNoCancelQuestion(message);
    }

    private TreeMap m_messages = new TreeMap();

    private Component m_parent;

    private OptionalMessage get(String id)
    {
        if (! m_messages.containsKey(id))
            m_messages.put(id, new OptionalMessage(m_parent));
        return (OptionalMessage)m_messages.get(id);
    }
}
