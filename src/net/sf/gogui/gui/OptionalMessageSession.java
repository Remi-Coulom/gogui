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

    public void showMessage(String id, String mainMessage,
                            String optionalMessage)
    {
        get(id).showMessage(mainMessage, optionalMessage);
    }

    public boolean showQuestion(String id, String mainMessage,
                                String optionalMessage)
    {
        return get(id).showQuestion(mainMessage, optionalMessage);
    }

    public void showWarning(String id, String mainMessage,
                            String optionalMessage)
    {
        get(id).showWarning(mainMessage, optionalMessage);
    }

    public boolean showWarningQuestion(String id, String mainMessage,
                                       String optionalMessage)
    {
        return get(id).showWarningQuestion(mainMessage, optionalMessage);
    }

    public int showYesNoCancelQuestion(String id, String mainMessage,
                                       String optionalMessage)
    {
        return get(id).showYesNoCancelQuestion(mainMessage, optionalMessage);
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
