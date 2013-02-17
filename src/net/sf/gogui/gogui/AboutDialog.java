// AboutDialog.java

package net.sf.gogui.gogui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.net.URL;
import static java.text.MessageFormat.format;
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
import static net.sf.gogui.gogui.I18n.i18n;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.gui.MessageDialogs;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.XmlUtil;
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
        JDialog dialog = aboutDialog.createDialog(parent, i18n("TIT_ABOUT"));
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        aboutDialog.m_tabbedPane.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
    }

    private final JTabbedPane m_tabbedPane;

    private final MessageDialogs m_messageDialogs;

    private AboutDialog(String name, String version, String command,
                        MessageDialogs messageDialogs)
    {
        m_messageDialogs = messageDialogs;
        m_tabbedPane = new JTabbedPane();
        boolean isProgramAvailable = (name != null && ! name.equals(""));
        int tabIndex = 0;
        m_tabbedPane.add(i18n("LB_GOGUI"), createPanelGoGui());
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_G);
        m_tabbedPane.setSelectedIndex(tabIndex);
        ++tabIndex;
        JPanel programPanel;
        if (isProgramAvailable)
        {
            int width = GuiUtil.getDefaultMonoFontSize() * 25;
            String versionInfo = "";
            if (version != null && ! version.equals(""))
            {
                if (version.length() > 40)
                    version = version.substring(0, 40) + "...";
                versionInfo = "<p align=\"center\" width=\"" + width + "\">"
                    + XmlUtil.escapeText(format(i18n("MSG_ABOUT_VERSION"), version))
                    + "</p>";
            }
            programPanel =
                createPanel("<p align=\"center\"><img src=\""
                            + getImage("gogui-program.png") + "\"></p>" +
                            "<p align=\"center\" width=\"" + width + "\"><b>"
                            + XmlUtil.escapeText(name) + "</b></p>" +
                            versionInfo +
                            "<p align=\"center\" width=\"" + width + "\">"
                            + XmlUtil.escapeText(i18n("MSG_ABOUT_COMMAND"))
                            + "<br>" + command + "</p>");
        }
        else
            programPanel = new JPanel();
        m_tabbedPane.add(i18n("LB_ABOUT_PROGRAM"), programPanel);
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_P);
        m_tabbedPane.setEnabledAt(tabIndex, isProgramAvailable);
        ++tabIndex;
        m_tabbedPane.add(i18n("LB_ABOUT_JAVA"), createPanelJava());
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_J);
        ++tabIndex;
        setMessage(m_tabbedPane);
        Object[] options = { i18n("LB_CLOSE") };
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
                                                       i18n("MSG_ABOUT_OPEN_URL_FAIL"),
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
            "https://www.paypal.com/cgi-bin/webscr?item_name=Donation+to+GoGui&cmd=_donations&business=enz%40users.sourceforge.net";
        return createPanel("<p align=\"center\"><img src=\""
                           + getImage("gogui-48x48.png") + "\"></p>" +
                           "<p align=\"center\"><b>" +
                           XmlUtil.escapeText(i18n("LB_GOGUI"))
                           + "</b></p>" +
                           "<p align=\"center\">" +
                           XmlUtil.escapeText(format(i18n("MSG_ABOUT_VERSION"),
                                                     Version.get()))

                           + "</p>" +
                           "<p align=\"center\">" +
                           XmlUtil.escapeText(i18n("MSG_ABOUT_DESC")) + "<br>" +
                           XmlUtil.escapeText(i18n("MSG_ABOUT_COPY")) + "<br>" +
                           "<a href=\"" + projectUrl + "\">" + projectUrl +
                           "</a><br>" +
                           "</p>" +
                           "<p align=\"center\">" +
                           "<a href=\"" + supportUrl + "\">"
                           + "<img src=\"" + imageUrl
                           + "\" border=\"0\"></a>" + "</p>");
    }

    private JPanel createPanelJava()
    {
        StringBuilder buffer = new StringBuilder(256);
        String name = Platform.getJavaRuntimeName();
        buffer.append("<p align=\"center\"><img src=\""
                      + getImage("java.png") + "\"></p>");
        if (name == null)
        {
            buffer.append("<p align=\"center\">");
            XmlUtil.escapeText(i18n("MSG_ABOUT_UNKNOWN_JAVA"));
            buffer.append("</p>");
        }
        else
        {
            buffer.append("<p align=\"center\"><b>");
            buffer.append(name);
            buffer.append("</b></p>");
            String version = System.getProperty("java.version");
            if (version != null)
            {
                buffer.append("<p align=\"center\">");
                buffer.append(XmlUtil.escapeText(format(i18n("MSG_ABOUT_VERSION"), version)));
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
            (max == Long.MAX_VALUE ? i18n("LB_ABOUT_MEMORYLIMIT_NONE")
             : getMemorySizeString(max));
        buffer.append(XmlUtil.escapeText(i18n("LB_ABOUT_JAVA_MEMLIMIT")));
        buffer.append(' ');
        buffer.append(XmlUtil.escapeText(maxString));
        buffer.append("<br>");
        buffer.append(XmlUtil.escapeText(format(i18n("LB_ABOUT_JAVA_MEMLIMIT_1"),
                                                getMemorySizeString(runtime.totalMemory()),
                                                getMemorySizeString(runtime.freeMemory()))));
        String lafName = i18n("LB_ABOUT_LAF_UNKNOWN");
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null)
            lafName = laf.getName();
        buffer.append("<br>");
        buffer.append(XmlUtil.escapeText(i18n("LB_ABOUT_LAF")));
        buffer.append(' ');
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
