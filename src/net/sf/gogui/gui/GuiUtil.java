//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.ProgressShow;

/** GUI utility classes and static functions. */
public class GuiUtil
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

    public static void copyToClipboard(String text)
    {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        ClipboardOwner owner = new ClipboardOwner() {
                public void lostOwnership(Clipboard clipboard,
                                          Transferable contents)
                {
                }
            };
        clipboard.setContents(selection, owner);
    }

    /** Create empty border with normal padding.
        @see #PAD
    */
    public static Border createEmptyBorder()
    {
        return EMPTY_BORDER;
    }

    /** Create empty box with size of normal padding.
        @see #PAD
    */
    public static Box.Filler createFiller()
    {
        return new Box.Filler(FILLER_DIMENSION, FILLER_DIMENSION,
                              FILLER_DIMENSION);
    }

    /** Create empty border with small padding.
        @see #SMALL_PAD
    */
    public static Border createSmallEmptyBorder()
    {
        return SMALL_EMPTY_BORDER;
    }

    /** Create empty box with size of small padding.
        @see #SMALL_PAD
    */
    public static Box.Filler createSmallFiller()
    {
        return new Box.Filler(SMALL_FILLER_DIMENSION, SMALL_FILLER_DIMENSION,
                              SMALL_FILLER_DIMENSION);
    }

    public static String getClipboardText()
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable content = clipboard.getContents(null);
        if (content == null
            || ! content.isDataFlavorSupported(DataFlavor.stringFlavor))
            return null;
        try
        {
            return (String)content.getTransferData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException e)
        {
            return null;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /** Get size of default monspaced font.
        Can be used for setting the initial size of some GUI elements.
        @bug Does not return the size in pixels but in points.
    */
    public static int getDefaultMonoFontSize()
    {
        return MONOSPACED_FONT.getSize();
    }

    public static ImageIcon getIcon(String icon, String name)
    {
        String resource = "net/sf/gogui/images/" + icon + ".png";
        URL url = GuiUtil.class.getClassLoader().getResource(resource);
        return new ImageIcon(url, name);
    }

    /** Init look and feel.
        If parameter is empty string, no initialization will be done.
        If parameter is null, try to use plasticxp (jwindows on Windows), but
        don't show an error message, if it fails.
        Otherwise use parameter as class name or shortcut as decoumented
        in the GoGui reference, and show message on failure.
    */
    public static void initLookAndFeel(String lookAndFeel)
    {
        // Disables renaming mode in Sun Java 1.5, which makes using the
        // JFileChooser a pain and is also enabled for open dialogs
        // for no good reason. Unfortunately that sacrifices the new directory
        // button, which is useful for save dialogs
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        if ("".equals(lookAndFeel))
            return;
        boolean showError = true;
        if (lookAndFeel == null)
        {
            showError = false;
            if (Platform.isWindows())
                lookAndFeel = "system";
            else if (Platform.isUnix())
                lookAndFeel = "plasticxp";
            else
                return;
        }
        UIManager.put("jgoodies.useNarrowButtons", Boolean.FALSE);
        UIManager.put("ScrollBar.maxBumpsWidth", new Integer(22));
        if ("cross".equals(lookAndFeel))
            lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
        else if ("gtk".equals(lookAndFeel))
            lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        else if ("motif".equals(lookAndFeel))
            lookAndFeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        else if ("jwindows".equals(lookAndFeel))
            lookAndFeel = "com.jgoodies.looks.windows.WindowsLookAndFeel";
        else if ("plasticxp".equals(lookAndFeel))
            lookAndFeel = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";
        else if ("system".equals(lookAndFeel))
            lookAndFeel = UIManager.getSystemLookAndFeelClassName();
        else if ("windows".equals(lookAndFeel))
            lookAndFeel =
                "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        try
        {
            UIManager.setLookAndFeel(lookAndFeel);
        }
        catch (Exception e)
        {
            if (showError)
                SimpleDialogs.showWarning(null,
                                          "Look and feel not found:\n" +
                                          lookAndFeel);
        }
    }

    public static boolean isActiveWindow(Window window)
    {
        KeyboardFocusManager manager =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        return (manager.getActiveWindow() == window);
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

    public static void removeKeyBinding(JComponent component, String key)
    {
        int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        InputMap inputMap = component.getInputMap(condition);
        // According to the docs, null should remove the action, but it does
        // not seem to work with Sun Java 1.4.2, new Object() works
        inputMap.put(KeyStroke.getKeyStroke(key), new Object());
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

    /** Set antialias rendering hint if graphics is instance of Graphics2D. */
    public static void setAntiAlias(Graphics graphics)
    {
        if (graphics instanceof Graphics2D)
        {
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    /** Set Go icon on frame. */
    public static void setGoIcon(Frame frame)
    {
        URL url = s_iconURL;
        if (url != null)
            frame.setIconImage(new ImageIcon(url).getImage());
    }

    public static void setMonospacedFont(JComponent component)
    {
        if (MONOSPACED_FONT != null)
            component.setFont(MONOSPACED_FONT);
    }

    static
    {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        // There are problems on some platforms with transparency (e.g. Linux
        // Sun Java 1.5.0). Best solution for now is to take an icon without
        // transparency
        s_iconURL =
            loader.getResource("net/sf/gogui/images/gogui-48x48-notrans.png");
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

    private static final Font MONOSPACED_FONT = Font.decode("Monospaced");

    private static final Border EMPTY_BORDER =
        BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD);

    private static final Border SMALL_EMPTY_BORDER =
        BorderFactory.createEmptyBorder(SMALL_PAD, SMALL_PAD,
                                        SMALL_PAD, SMALL_PAD);

    private static final Dimension FILLER_DIMENSION =
        new Dimension(PAD, PAD);

    private static final Dimension SMALL_FILLER_DIMENSION =
        new Dimension(SMALL_PAD, SMALL_PAD);

    private static URL s_iconURL;
}

