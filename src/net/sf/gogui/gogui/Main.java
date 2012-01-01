// Main.java

package net.sf.gogui.gogui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.MalformedURLException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.GuiUtil;
import static net.sf.gogui.gui.GuiUtil.insertLineBreaks;
import net.sf.gogui.gui.MessageDialogs;
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
            System.exit(1);
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        catch (Throwable t)
        {
            showError("Unexpected failure", StringUtil.printException(t));
            System.exit(1);
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
            System.exit(1);
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        catch (Throwable t)
        {
            showError("Unexpected failure", StringUtil.printException(t));
            System.exit(1);
        }
    }

    /** Flag if crash dialog is currently shown.
        Avoids that more than one crash dialog pops up, because events in the
        event dispatch thread can still be handled and cause more exceptions
        while the first crash dialog is shown. */
    private static boolean s_duringShowCrashDialog;

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void addFiller(JComponent component)
    {
        Box.Filler filler = GuiUtil.createFiller();
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.add(filler);
    }

    private static void startGoGui(final GoGuiSettings settings)
        throws GtpError, ErrorMessage
    {
        assert ! settings.m_noStartup;
        // Create thread group to catch errors from Swing event thread
        ThreadGroup group = new ThreadGroup("catch-runtime-exceptions") {
                public void uncaughtException(Thread t, Throwable e) {
                    if (s_duringShowCrashDialog)
                        return;
                    StringUtil.printException(e);
                    if (e instanceof RuntimeException
                        || e instanceof AssertionError)
                        showCrashDialog(e);
                    System.exit(1);
                }
            };
        Runnable runnable = new Runnable() {
                public void run() {
                    // Fix wrong taskbar title in Gnome 3. See
      // http://elliotth.blogspot.com/2007/02/fixing-wmclass-for-your-java.html
              // and http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6528430
                    try
                    {
                        Toolkit toolkit = Toolkit.getDefaultToolkit();
                        java.lang.reflect.Field field =
                            toolkit.getClass()
                            .getDeclaredField("awtAppClassName");
                        field.setAccessible(true);
                        field.set(toolkit, "GoGui");
                    }
                    catch (Exception e)
                    {
                    }

                    GuiUtil.initLookAndFeel(settings.m_lookAndFeel);
                    try
                    {
                        new GoGui(settings.m_program, settings.m_file,
                                  settings.m_move, settings.m_time,
                                  settings.m_verbose,
                                  settings.m_initComputerColor,
                                  settings.m_computerBlack,
                                  settings.m_computerWhite, settings.m_auto,
                                  settings.m_register, settings.m_gtpFile,
                                  settings.m_gtpCommand,
                                  settings.m_analyzeCommands);
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

    private static void showCrashDialog(Throwable e)
    {
        s_duringShowCrashDialog = true;
        JPanel panel = new JPanel(new BorderLayout());
        Box box = Box.createVerticalBox();
        panel.add(box, BorderLayout.NORTH);

        String css = GuiUtil.getMessageCss();

        JLabel mainMessageLabel =
            new JLabel("<html>" + css +
                       "<b>The application GoGui has quit unexpectedly</b>");
        mainMessageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(mainMessageLabel);
        addFiller(box);

        String optionalMessage;
        if (Version.get().indexOf("GIT") >= 0)
            optionalMessage =
                "You are running an unreleased version of GoGui. Please don't report this bug to\n" +
                "the GoGui bug tracker, but email the author of GoGui directly instead.\n";
        else
            optionalMessage =
                "Please take a moment to submit a bug report at the GoGui bug tracker.\n";
        optionalMessage = optionalMessage +
                "Include a short summary of the problem together with the following information:";
        JLabel optionalMessageLabel =
            new JLabel("<html>" + css + "<p>"
                       + insertLineBreaks(optionalMessage) + "</p>");
        optionalMessageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(optionalMessageLabel);

        box.add(GuiUtil.createSmallFiller());
        JTextArea textArea = new JTextArea();
        textArea.setForeground(Color.black);
        textArea.setBackground(Color.white);
        textArea.setBorder(GuiUtil.createEmptyBorder());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        JButton copyButton = new JButton("Copy Information");
        final String goguiVersion = "GoGui version: " + Version.get();
        final String javaVersion = "Java version: " +
            Platform.getJavaRuntimeName() + " " +
            System.getProperty("java.version");
        final String osVersion = "Operating system: " +
            System.getProperty("os.name");
        final StringWriter stackTrace = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stackTrace);
        e.printStackTrace(printWriter);
        copyButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    GuiUtil.copyToClipboard(goguiVersion + "\n" +
                                            javaVersion  + "\n" +
                                            osVersion + "\n\n" +
                                            stackTrace);
                }
            });

        JButton urlButton = new JButton("Go to Bug Tracker");
        urlButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try
                    {
                        URL url =
                            new URL("http://sf.net/tracker/?group_id=59117&atid=489964");
                        if (! Platform.openInExternalBrowser(url))
                            showError("Could not open URL in external browser",
                                      "");
                    }
                    catch (MalformedURLException e2)
                    {
                    }
                }
            });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(urlButton);
        buttonPanel.add(copyButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        scrollPane.setPreferredSize(new Dimension(512, 256));
        String text = goguiVersion + "\n" + javaVersion + "\n" + osVersion
            + "\n\n" + stackTrace;
        textArea.setText(text);
        Object[] options = { "Close" };
        JOptionPane optionPane =
            new JOptionPane(panel, JOptionPane.ERROR_MESSAGE,
                            JOptionPane.DEFAULT_OPTION, null, options,
                            options[0]);
        JDialog dialog = optionPane.createDialog(null, "Error");
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        panel.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
    }

    private static void showError(String mainMessage, String optionalMessage)
    {
        MessageDialogs messageDialogs = new MessageDialogs();
        messageDialogs.showError(null, mainMessage, optionalMessage);
    }
}
