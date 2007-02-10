//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** GoGui main function. */
public final class Main
{
    /** GoGui main function. */
    public static void main(String[] args)
    {
        GoGuiSettings settings;
        try
        {
            settings =
                new GoGuiSettings(args,
                                  Class.forName("net.sf.gogui.gogui.GoGui"));
            if (settings.m_noStartup)
                return;
            startGoGui(settings);
        }
        catch (RuntimeException e)
        {
            showCrashDialog(e);
            System.exit(-1);
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtil.printException(t));
            System.exit(-1);
        }
    }

    public static void main(GoGuiSettings settings)
    {
        try
        {
            startGoGui(settings);
        }
        catch (RuntimeException e)
        {
            showCrashDialog(e);
            System.exit(-1);
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtil.printException(t));
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void startGoGui(final GoGuiSettings settings)
        throws GtpError, ErrorMessage
    {
        assert(! settings.m_noStartup);
        // Create thread group to catch errors from Swing event thread
        ThreadGroup group = new ThreadGroup("catch-runtime-exceptions") {
                public void uncaughtException(Thread t, Throwable e) {
                    StringUtil.printException(e);
                    if (e instanceof RuntimeException
                        || e instanceof AssertionError)
                        showCrashDialog((RuntimeException)e);
                    System.exit(-1);
                }
            };
        Runnable runnable = new Runnable() {
                public void run() {
                    GuiUtil.initLookAndFeel(settings.m_lookAndFeel);
                    try
                    {
                        new GoGui(settings.m_program, settings.m_file,
                                  settings.m_move, settings.m_time,
                                  settings.m_verbose, settings.m_computerBlack,
                                  settings.m_computerWhite, settings.m_auto,
                                  settings.m_gtpFile, settings.m_gtpCommand,
                                  settings.m_initAnalyze);
                    }
                    catch (ErrorMessage e)
                    {
                        System.err.println(e.getMessage());
                        return;
                    }
                }
            };
        Thread thread = new Thread(group, runnable);
        thread.start();
    }

    private static void showCrashDialog(RuntimeException e)
    {
        if ("GNU libgcj".equals(System.getProperty("java.vm.name")))
        {
            System.err.print("--------------------------------------------\n" +
                             "GNU libgcj is not supported !\n" +
                             "--------------------------------------------\n");
            SimpleDialogs.showError(null, "GNU libgcj is not supported");
            return;
        }
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(GuiUtil.createEmptyBorder());        
        editorPane.setEditable(false);
        if (Platform.isMac())
        {
            Color color = UIManager.getColor("Label.background");
            if (color != null)
                editorPane.setBackground(color);
        }
        JScrollPane scrollPane = new JScrollPane(editorPane);
        panel.add(scrollPane);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        EditorKit editorKit =
            JEditorPane.createEditorKitForContentType("text/html");
        editorPane.setEditorKit(editorKit);
        StringWriter stackTrace = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stackTrace);
        e.printStackTrace(printWriter);
        String text =
            "<p><b>The application GoGui has quit unexpectedly</b></p>" +
            "<p>Please take a moment to submit a bug report at the " +
            "<a href=\"http://sf.net/tracker/?group_id=59117&atid=489964\">" +
            "GoGui bug tracker</a> and include the following " +
            "information:</p>" +
            "<p>GoGui version: " + Version.get() + "<br>" +
            "Java version: " + 
            System.getProperty("java.vm.name") + " " +
            System.getProperty("java.vm.version")+ "<br>" +
            "Operating system: " + System.getProperty("os.name")
            + "</p>" +
            "<pre>" + stackTrace + "</pre>";
        editorPane.setText(text);
        editorPane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    HyperlinkEvent.EventType type = event.getEventType();
                    if (type == HyperlinkEvent.EventType.ACTIVATED)
                    {
                        URL url = event.getURL();
                        if (! Platform.openInExternalBrowser(url))
                            SimpleDialogs.showError(null,
                                                    "Could not open URL"
                                                    + " in external browser");
                    }
                }
            });
        JOptionPane optionPane =
            new JOptionPane(panel, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = optionPane.createDialog(null, "Error");
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        panel.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
    }
}

