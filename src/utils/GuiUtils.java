//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

//----------------------------------------------------------------------------

public class GuiUtils
{
    public interface ProgressRunnable
    {
        void run(ProgressShow progressShow) throws Throwable;
    }

    public static final int PAD = 5;

    public static final int SMALL_PAD = 2;

    public static Border createEmptyBorder()
    {
        return m_emptyBorder;
    }

    public static Box.Filler createFiller()
    {
        return new Box.Filler(m_fillerDimension, m_fillerDimension,
                              m_fillerDimension);
    }

    public static Border createSmallEmptyBorder()
    {
        return m_smallEmptyBorder;
    }

    public static Box.Filler createSmallFiller()
    {
        return new Box.Filler(m_smallFillerDimension, m_smallFillerDimension,
                              m_smallFillerDimension);
    }

    public static int getDefaultMonoFontSize()
    {
        return m_defaultMonoFontSize;
    }

    public static boolean isNormalSizeMode(JFrame window)
    {
        int state = window.getExtendedState();
        int mask = Frame.MAXIMIZED_BOTH | Frame.MAXIMIZED_VERT
            | Frame.MAXIMIZED_HORIZ | Frame.ICONIFIED;
        return ((state & mask) == 0);
    }

    /** Run in separate thread protected by a modal progress dialog. */
    public static void runProgess(Frame owner, String message,
                                  ProgressRunnable runnable)
        throws Throwable
    {
        assert(SwingUtilities.isEventDispatchThread());
        JDialog dialog = new JDialog(owner, message, true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);
        JProgressBar progressBar = new JProgressBar(0, 100);
        //progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        JPanel panel = new JPanel();
        panel.setBorder(createSmallEmptyBorder());
        panel.add(progressBar);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        ProgressThread thread =
            new ProgressThread(dialog, runnable, progressBar);
        thread.start();
        dialog.setVisible(true);
        if (thread.getThrowable() != null)
            throw thread.getThrowable();
    }

    public static void setGoIcon(Frame frame)
    {
        URL url = m_iconURL;
        if (url != null)
            frame.setIconImage(new ImageIcon(url).getImage());
    }

    static
    {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        // There are problems on most platforms with larger icons auto-scaled
        // down and transparency issues (Windows, Linux Sun Java 1.5.0)
        // Best solution for now is to take a 16x16 icon with no transparency
        m_iconURL = loader.getResource("images/gogui-16x16-notrans.png");
    }

    private static class ProgressThread
        extends Thread
        implements ProgressShow
    {
        public ProgressThread(Dialog dialog, ProgressRunnable runnable,
                              JProgressBar progressBar)
        {
            m_dialog = dialog;
            m_runnable = runnable;
            m_progressBar = progressBar;
        }

        public Throwable getThrowable()
        {
            return m_throwable;
        }

        public void run()
        {
            long startTime = System.currentTimeMillis();
            try
            {
                m_runnable.run(this);
            }
            catch (Throwable t)
            {
                m_throwable = t;
            }
            // Show progress dialog for at least 1 sec
            long timeDiff = System.currentTimeMillis() - startTime;
            if (timeDiff < 1000)
            {
                try
                {
                    Thread.sleep(1000 - timeDiff);
                }
                catch (InterruptedException e)
                {
                    assert(false);
                }
            }
            SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        m_dialog.dispose();
                    }
                });
        }

        public void showProgress(int percent)
        {
            m_percent = percent;
            SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        m_progressBar.setValue(m_percent);
                    }
                });
        }

        private int m_percent;

        private Dialog m_dialog;

        private JProgressBar m_progressBar;

        private ProgressRunnable m_runnable;

        private Throwable m_throwable;
    }

    static
    {
        // That is not correct, since Font.getSize does not return pixels
        // Should query some default Graphics device
        m_defaultMonoFontSize =
            getTextAreaFont() == null ? 10 : getTextAreaFont().getSize();
    }

    private static final int m_defaultMonoFontSize;

    private static final Border m_emptyBorder =
        BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD);

    private static final Border m_smallEmptyBorder =
        BorderFactory.createEmptyBorder(SMALL_PAD, SMALL_PAD,
                                        SMALL_PAD, SMALL_PAD);

    private static final Dimension m_fillerDimension =
        new Dimension(PAD, PAD);

    private static final Dimension m_smallFillerDimension =
        new Dimension(SMALL_PAD, SMALL_PAD);

    private static URL m_iconURL;

    private static Font getTextAreaFont()
    {
        return UIManager.getFont("TextArea.font");
    }
}

//----------------------------------------------------------------------------
