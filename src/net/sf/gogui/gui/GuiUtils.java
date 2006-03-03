//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
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
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import net.sf.gogui.utils.ProgressShow;

//----------------------------------------------------------------------------

/** GUI utility classes and static functions. */
public class GuiUtils
{
    /** Runnable for running protected by modal progress dialog.
        @see #runProgress
    */
    public interface ProgressRunnable
    {
        /** Function to run.
            The function is expected to call ProgressShow.showProgress
            regularly to indicate progress made.
        */
        void run(ProgressShow progressShow) throws Throwable;
    }

    /** Constant used for padding in dialogs. */
    public static final int PAD = 5;

    /** Constant used for small padding in dialogs. */
    public static final int SMALL_PAD = 2;

    public static void addStyle(JTextPane pane, String name, Color foreground,
                                Color background)
    {
        StyledDocument doc = pane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style defaultStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style style = doc.addStyle(name, defaultStyle);
        StyleConstants.setForeground(style, foreground);
        StyleConstants.setBackground(style, background);
    }

    /** Create empty border with normal padding.
        @see #PAD
    */
    public static Border createEmptyBorder()
    {
        return m_emptyBorder;
    }

    /** Create empty box with size of normal padding.
        @see #PAD
    */
    public static Box.Filler createFiller()
    {
        return new Box.Filler(m_fillerDimension, m_fillerDimension,
                              m_fillerDimension);
    }

    /** Create empty border with small padding.
        @see #SMALL_PAD
    */
    public static Border createSmallEmptyBorder()
    {
        return m_smallEmptyBorder;
    }

    /** Create empty box with size of small padding.
        @see #SMALL_PAD
    */
    public static Box.Filler createSmallFiller()
    {
        return new Box.Filler(m_smallFillerDimension, m_smallFillerDimension,
                              m_smallFillerDimension);
    }

    /** Get size of default monspaced font.
        Can be used for setting the initial size of some GUI elements.
        @bug Does not return the size in pixels but in points.
    */
    public static int getDefaultMonoFontSize()
    {
        return m_defaultMonoFontSize;
    }

    /** Check window for normal state.
        Checks if window is not maximized (in either or both directions) and
        not iconified.
    */
    public static boolean isNormalSizeMode(JFrame window)
    {
        int state = window.getExtendedState();
        int mask = Frame.MAXIMIZED_BOTH | Frame.MAXIMIZED_VERT
            | Frame.MAXIMIZED_HORIZ | Frame.ICONIFIED;
        return ((state & mask) == 0);
    }

    /** Run in separate thread protected by a modal progress dialog.
        Replacement for javax.swing.ProgressMonitor, which does not create
        a modal dialog.
        Ensures that the GUI gets repaint events while the runnable is
        running, but cannot get other events and displays a progress bar
        as a user feedback.
        The progress dialog is displayed for at least one second.
        @param owner Parent for the progress dialog.
        @param message Title for the progress dialog.
        @param runnable Runnable to run.
        @throws Throwable Any exception that ProgressRunnable.run throwed,
        you have to use instanceof to check for specific exception classes.
    */
    public static void runProgress(Frame owner, String message,
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

    /** Set Go icon on frame. */
    public static void setGoIcon(Frame frame)
    {
        URL url = s_iconURL;
        if (url != null)
            frame.setIconImage(new ImageIcon(url).getImage());
    }

    static
    {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        // There are problems on most platforms with larger icons auto-scaled
        // down and transparency issues (Windows, Linux Sun Java 1.5.0)
        // Best solution for now is to take a 16x16 icon with no transparency
        s_iconURL =
            loader.getResource("net/sf/gogui/images/gogui-16x16-notrans.png");
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

        private final Dialog m_dialog;

        private final JProgressBar m_progressBar;

        private final ProgressRunnable m_runnable;

        private Throwable m_throwable;
    }

    static
    {
        Font textAreaFont = UIManager.getFont("TextArea.font");
        // That is not correct, since Font.getSize does not return pixels
        // Should query some default Graphics device
        m_defaultMonoFontSize =
            textAreaFont == null ? 10 : textAreaFont.getSize();
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

    private static URL s_iconURL;
}

//----------------------------------------------------------------------------
