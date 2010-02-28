// LiveGfx.java

package net.sf.gogui.gui;

import javax.swing.SwingUtilities;

/** Parse standard error of a GTP engine for GoGui live graphics commands.
    See chapter "Live Graphics" in the GoGui documentation. */
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

    /** Parse line.
        This function can be called from a different thread than the Swing
        event dispatch thread.
        @param s The line received from standard error (may or may not be
        a live gfx line).
        @return true, if the line was a live gfx line */
    public boolean handleLine(String s)
    {
        s = s.trim();
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
            return true;
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
            return true;
        }
        return false;
    }

    private boolean m_duringMultiLineResponse;

    private Listener m_listener;

    private final StringBuilder m_response = new StringBuilder(1024);

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
