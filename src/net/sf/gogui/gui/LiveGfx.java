//----------------------------------------------------------------------------
// LiveGfx.java
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import javax.swing.SwingUtilities;
import net.sf.gogui.go.ConstBoard;

/** Parse standard error of a GTP engine for GoGui live graphics commands.
    See chapter "Live Graphics" in the GoGui documentation.
*/
public class LiveGfx
{
    public interface Listener
    {
        void showLiveGfx(String text);
    }

    public LiveGfx(Listener listener)
    {
        m_listener = listener;
        m_duringMultiLineResponse = false;
    }

    /** Parse the next sequence of characters.
        This function can be called from a different thread than the Swing
        event disapatch thread.
    */
    public void receivedStdErr(String s)
    {
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n')
            {
                handleLine(m_buffer.toString());
                m_buffer.setLength(0);
            }
            else
                m_buffer.append(c);
        }
    }

    private boolean m_duringMultiLineResponse;

    Listener m_listener;

    private final StringBuilder m_buffer = new StringBuilder(1024);

    private final StringBuilder m_response = new StringBuilder(1024);

    private void handleLine(String s)
    {
        if (m_duringMultiLineResponse)
        {
            if (s.equals(""))
            {
                showGfx(m_response.toString());
                m_duringMultiLineResponse = false;
            }
            else
            {
                m_response.append(s);
                m_response.append('\n');
            }
            return;
        }
        s = s.trim();
        if (s.startsWith("gogui-gfx:"))
        {
            int pos = s.indexOf(':');
            String response = s.substring(pos + 1);
            if (response.trim().equals(""))
            {
                m_response.setLength(0);
                m_duringMultiLineResponse = true;
            }
            else
                showGfx(response);
        }
    }

    private void showGfx(final String text)
    {
        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    m_listener.showLiveGfx(text);
                }
            });
    }
}
