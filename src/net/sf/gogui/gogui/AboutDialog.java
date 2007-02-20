//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.gui.MessageDialogs;
import net.sf.gogui.util.Platform;
import net.sf.gogui.version.Version;

/** About dialog for GoGui. */
public final class AboutDialog
    extends JOptionPane
{
    public static void show(Component parent, String name, String version,
                            String command, MessageDialogs messageDialogs)
    {
        AboutDialog aboutDialog = new AboutDialog(name, version, command,
                                                  messageDialogs);
        JDialog dialog = aboutDialog.createDialog(parent, "About");
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        aboutDialog.m_tabbedPane.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JTabbedPane m_tabbedPane;

    private MessageDialogs m_messageDialogs;

    private AboutDialog(String name, String version, String command,
                        MessageDialogs messageDialogs)
    {
        m_messageDialogs = messageDialogs;
        m_tabbedPane = new JTabbedPane();
        if (! Platform.isMac())
            m_tabbedPane.putClientProperty("jgoodies.noContentBorder",
                                           Boolean.TRUE);
        boolean isProgramAvailable = (name != null && ! name.equals(""));
        int tabIndex = 0;
        m_tabbedPane.add("GoGui", createPanelGoGui());
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_G);
        m_tabbedPane.setSelectedIndex(tabIndex);
        ++tabIndex;
        JPanel programPanel;
        if (isProgramAvailable)
        {
            String versionInfo = "";
            if (version != null && ! version.equals(""))
                versionInfo = "<p align=\"center\">Version " + version
                    + "</p>";
            int width = GuiUtil.getDefaultMonoFontSize() * 25;
            programPanel =
                createPanel("<p align=\"center\"><img src=\""
                            + getImage("gogui-program.png") + "\"></p>" +
                            "<p align=\"center\"><b>" + name + "</b></p>" +
                            versionInfo +
                            "<p align=\"center\" width=\"" + width + "\">" +
                            "Program command:<br>" +
                            command + "</p>");
        }
        else
            programPanel = new JPanel();
        m_tabbedPane.add("Program", programPanel);
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_P);
        m_tabbedPane.setEnabledAt(tabIndex, isProgramAvailable);
        ++tabIndex;
        m_tabbedPane.add("Java", createPanelJava());
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_J);
        ++tabIndex;
        setMessage(m_tabbedPane);
        Object[] options = { "Close" };
        setOptions(options);
    }

    private JPanel createPanel(String text)
    {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(GuiUtil.createEmptyBorder());        
        editorPane.setEditable(false);
        if (Platform.isMac())
        {
            editorPane.setForeground(UIManager.getColor("Label.foreground"));
            editorPane.setBackground(UIManager.getColor("Label.background"));
        }
        else
        {
            editorPane.setForeground(Color.black);
            editorPane.setBackground(Color.white);
        }
        panel.add(editorPane);
        EditorKit editorKit =
            JEditorPane.createEditorKitForContentType("text/html");
        editorPane.setEditorKit(editorKit);
        editorPane.setText(text);
        editorPane.addHyperlinkListener(new HyperlinkListener()
            {
                public void hyperlinkUpdate(HyperlinkEvent event)
                {
                    HyperlinkEvent.EventType type = event.getEventType();
                    if (type == HyperlinkEvent.EventType.ACTIVATED)
                    {
                        URL url = event.getURL();
                        if (! Platform.openInExternalBrowser(url))
                            m_messageDialogs.showError(null,
                                                       "Could not open URL in"
                                                       + " external browser",
                                                       "", false);
                    }
                }
            });
        return panel;
    }

    private JPanel createPanelGoGui()
    {
        URL imageUrl = getImage("project-support.png");
        String projectUrl = "http://gogui.sf.net";
        String supportUrl =
            "http://sourceforge.net/donate/index.php?group_id=59117";
        return createPanel("<p align=\"center\"><img src=\""
                           + getImage("gogui-48x48.png") + "\"></p>" +
                           "<p align=\"center\"><b>GoGui</b></p>" +
                           "<p align=\"center\">" +
                           "Version " + Version.get() + "</p>" +
                           "<p align=\"center\">" +
                           "Graphical user interface to Go programs<br>" +
                           "&copy; 2001-2007 Markus Enzenberger" +
                           "<br>" +
                           "<tt><a href=\"" + projectUrl + "\">"
                           + projectUrl + "</a></tt>" +
                           "</p>" +
                           "<p align=\"center\">" +
                           "<a href=\"" + supportUrl + "\">"
                           + "<img src=\"" + imageUrl
                           + "\" border=\"0\"></a>" + "</p>");
    }

    private JPanel createPanelJava()
    {
        StringBuffer buffer = new StringBuffer(256);
        String name = System.getProperty("java.vm.name");
        buffer.append("<p align=\"center\"><img src=\""
                      + getImage("java.png") + "\"></p>");
        if (name == null)
            buffer.append("<p align=\"center\">Unknown Java VM</p>");
        else
        {
            buffer.append("<p align=\"center\"><b>");
            buffer.append(name);
            buffer.append("</b></p>");
            String version = System.getProperty("java.vm.version");
            if (version != null)
            {
                buffer.append("<p align=\"center\">Version ");
                buffer.append(version);
                buffer.append("</p>");
            }
            buffer.append("<p align=\"center\">");
            String vendor = System.getProperty("java.vm.vendor");
            if (vendor != null)
                buffer.append(vendor);
            buffer.append("<br>");
        }
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        String maxString =
            (max == Long.MAX_VALUE ? "none" : getMemorySizeString(max));
        buffer.append("Memory limit: ");
        buffer.append(maxString);
        buffer.append("<br>(");
        buffer.append(getMemorySizeString(runtime.totalMemory()));
        buffer.append(" current, ");
        buffer.append(getMemorySizeString(runtime.freeMemory()));
        buffer.append(" free)");
        String lafName = "unknown";
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null)
            lafName = laf.getName();
        buffer.append("<br>Look and Feel: ");
        buffer.append(lafName);
        return createPanel(buffer.toString());
    }

    private URL getImage(String name)
    {
        ClassLoader loader = getClass().getClassLoader();
        return loader.getResource("net/sf/gogui/images/" + name);
    }

    private String getMemorySizeString(long size)
    {
        if (size < 1000)
            return size + " Bytes";
        if (size < 1000000)
            return (size / 1000) + " kB";
        return (size / 1000000) + " MB";
    }
}

