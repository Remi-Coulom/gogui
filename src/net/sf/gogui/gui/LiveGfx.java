//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import javax.swing.SwingUtilities;

//----------------------------------------------------------------------------

/** Parse standard error for GoGui live graphics commands. */
public class LiveGfx
{
    public LiveGfx(GuiBoard guiBoard, StatusBar statusBar)
    {
        m_guiBoard = guiBoard;
        m_statusBar = statusBar;
    }

    public void receivedStdErr(String s)
    {
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n')
            {
                if (SwingUtilities.isEventDispatchThread())
                    handleLine(m_buffer.toString());
                else
                    invokeAndWait(new Runnable()
                        {
                            public void run()
                            {
                                handleLine(m_buffer.toString());
                            }
                        });
                m_buffer.setLength(0);
            }
            else
                m_buffer.append(c);
        }
    }

    private StringBuffer m_buffer = new StringBuffer(1024);

    private GuiBoard m_guiBoard;

    private StatusBar m_statusBar;

    private void handleLine(String s)
    {
        s = s.trim();
        if (s.equals("") || ! s.startsWith("gogui-gfx:"))
            return;
        int pos = s.indexOf(':');
        AnalyzeShow.showGfxLine(s.substring(pos + 1), m_guiBoard,
                                m_statusBar);
    }

    private void invokeAndWait(Runnable runnable)
    {
        try
        {
            SwingUtilities.invokeAndWait(runnable);
        }
        catch (InterruptedException e)
        {
            System.err.println("Thread interrupted");
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            System.err.println("InvocationTargetException");
        }
    }
}

//----------------------------------------------------------------------------
