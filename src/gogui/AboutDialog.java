//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import gui.SimpleDialogs;
import utils.GuiUtils;
import utils.Platform;
import version.Version;

//----------------------------------------------------------------------------

/** About dialog for GoGui. */
public class AboutDialog
    extends JOptionPane
{
    public static void show(Component parent, String name, String version,
                            String protocolVersion, String command)
    {
        AboutDialog aboutDialog =
            new AboutDialog(name, version, protocolVersion, command);
        JDialog dialog = aboutDialog.createDialog(parent, "About");
        dialog.setVisible(true);
        dialog.dispose();
    }

    private AboutDialog(String name, String version, String protocolVersion,
                        String command)
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        ClassLoader classLoader = getClass().getClassLoader();
        URL imageUrl = classLoader.getResource("images/project-support.png");
        String projectUrl = "http://gogui.sourceforge.net";
        String supportUrl =
            "http://sourceforge.net/donate/index.php?group_id=59117";
        JPanel goguiPanel =
            createPanel("<p align=\"center\"><b>GoGui</b></p>" +
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
                        + "<img src=\"" + imageUrl + "\" border=\"0\"></a>" +
                        "</p>");
        tabbedPane.add("GoGui", goguiPanel);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_G);
        boolean isProgramAvailable = (name != null && ! name.equals(""));
        JPanel programPanel;
        if (isProgramAvailable)
        {
            String versionInfo = "";
            if (version != null && ! version.equals(""))
                versionInfo = "<p align=\"center\">Version " + version
                    + "</p>";
            int width = GuiUtils.getDefaultMonoFontSize() * 25;
            programPanel =
                createPanel("<p align=\"center\"><b>" + name + "</b></p>" +
                            versionInfo +
                            "<p align=\"center\" width=\"" + width + "\">" +
                            "GTP protocol version " + protocolVersion
                            + "<br>" +
                            "Command: " +
                            "<tt>" + command + "</tt></p>");
            tabbedPane.add(name, programPanel);
            tabbedPane.setMnemonicAt(1, KeyEvent.VK_P);
            tabbedPane.setSelectedIndex(1);
        }
        setMessage(tabbedPane);
        setOptionType(DEFAULT_OPTION);
    }

    private static JPanel createPanel(String text)
    {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(GuiUtils.createEmptyBorder());        
        editorPane.setEditable(false);
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
}

//----------------------------------------------------------------------------
