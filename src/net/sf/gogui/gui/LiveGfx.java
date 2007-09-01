//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import javax.swing.SwingUtilities;
import net.sf.gogui.go.ConstBoard;

/** Parse standard error of a GTP engine for GoGui live graphics commands.
    See chapter "Live Graphics" in the GoGui documentation.
*/
public class LiveGfx
{
    public LiveGfx(ConstBoard board, GuiBoard guiBoard, StatusBar statusBar)
    {
        m_board = board;
        m_guiBoard = guiBoard;
        m_statusBar = statusBar;
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

    private final StringBuilder m_buffer = new StringBuilder(1024);

    private final StringBuilder m_response = new StringBuilder(1024);

    private final ConstBoard m_board;

    private final GuiBoard m_guiBoard;

    private final StatusBar m_statusBar;

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
        Runnable runnable = new Runnable() {
                public void run()
                {
                    m_guiBoard.clearAll();
                    GuiBoardUtil.updateFromGoBoard(m_guiBoard, m_board,
                                                   false);
                    AnalyzeShow.showGfx(text, m_guiBoard, m_statusBar);
                }
            };
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
            return;
        }
        try
        {
            // Use invokeAndWait to ensure that each gogui-gfx command is
            // really shown (and no commands are merged by the repaint
            // manager)
            SwingUtilities.invokeAndWait(runnable);
            // Throttle thread a bit to avoid long delays of other repaint
            // events in the event queue
            Thread.sleep(50);
        }
        catch (InterruptedException e)
        {
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
        }
    }
}
