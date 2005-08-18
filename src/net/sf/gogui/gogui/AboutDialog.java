//----------------------------------------------------------------------------
// $Id$
// $Source$
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
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** About dialog for GoGui. */
public final class AboutDialog
    extends JOptionPane
{
    public static void show(Component parent, String name, String version,
                            String protocolVersion, String command)
    {
        AboutDialog aboutDialog =
            new AboutDialog(name, version, protocolVersion, command);
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

    private AboutDialog(String name, String version, String protocolVersion,
                        String command)
    {
        m_tabbedPane = new JTabbedPane();
        boolean isProgramAvailable = (name != null && ! name.equals(""));
        int tabIndex = 0;
        JPanel programPanel;
        if (isProgramAvailable)
        {
            String versionInfo = "";
            if (version != null && ! version.equals(""))
                versionInfo = "<p align=\"center\">Version " + version
                    + "</p>";
            int width = GuiUtils.getDefaultMonoFontSize() * 25;
            programPanel =
                createPanel("<p align=\"center\"><img src=\""
                            + getImage("program.png") + "\"></p>" +
                            "<p align=\"center\"><b>" + name + "</b></p>" +
                            versionInfo +
                            "<p align=\"center\" width=\"" + width + "\">" +
                            "GTP protocol version " + protocolVersion
                            + "<br>" +
                            "Command: " +
                            "<tt>" + command + "</tt></p>");
            m_tabbedPane.add("Program", programPanel);
            m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_P);
            m_tabbedPane.setSelectedIndex(tabIndex);
            ++tabIndex;
        }
        m_tabbedPane.add("GoGui", createPanelGoGui());
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_G);
        ++tabIndex;
        m_tabbedPane.add("Java", createPanelJava());
        m_tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_J);
        ++tabIndex;
        setMessage(m_tabbedPane);
        setOptionType(DEFAULT_OPTION);
    }

    private static JPanel createPanel(String text)
    {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(GuiUtils.createEmptyBorder());        
        editorPane.setEditable(false);
        if (Platform.isMac())
        {
            Color color = UIManager.getColor("Label.background");
            if (color != null)
                editorPane.setBackground(color);
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
                            SimpleDialogs.showError(null,
                                                    "Could not open URL"
                                                    + " in external browser");
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
                           + getImage("gogui.png") + "\"></p>" +
                           "<p align=\"center\"><b>GoGui</b></p>" +
                           "<p align=\"center\">" +
                           "Version " + Version.get() + "</p>" +
                           "<p align=\"center\">" +
                           "Graphical interface to Go programs<br>" +
                           "&copy; 2005 Markus Enzenberger" +
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
            buffer.append("<p>Unknown Java VM</p>");
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
            {
                buffer.append(vendor);
            }
            String info = System.getProperty("java.vm.info");
            if (info != null)
            {
                buffer.append("<br>");
                buffer.append(info);
            }
            buffer.append("</p>");
        }
        return createPanel(buffer.toString());
    }

    private URL getImage(String name)
    {
        ClassLoader loader = getClass().getClassLoader();
        return loader.getResource("net/sf/gogui/images/" + name);
    }
}

//----------------------------------------------------------------------------
