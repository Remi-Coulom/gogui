//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import version.*;
import utils.GuiUtils;
import utils.Platform;

//----------------------------------------------------------------------------

public class AboutDialog
    extends JOptionPane
{
    public static void show(Component parent, String name, String version,
                            String protocolVersion)
    {
        AboutDialog aboutDialog =
            new AboutDialog(name, version, protocolVersion);
        JDialog dialog = aboutDialog.createDialog(parent, "About");
        dialog.setVisible(true);
        dialog.dispose();
    }

    private AboutDialog(String name, String version, String protocolVersion)
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        ClassLoader classLoader = getClass().getClassLoader();
        URL imageUrl = classLoader.getResource("images/project-support.png");
        String projectUrl = "http://gogui.sourceforge.net";
        String supportUrl =
            "http://sourceforge.net/donate/index.php?group_id=59117";
        JPanel goguiPanel =
            createPanel("<center>" +
                        "<b>GoGui " + Version.get() + "</b>" +
                        "<p>" +
                        "Graphical interface to Go programs<br>" +
                        "&copy; 2003-2004, Markus Enzenberger" +
                        "</p>" +
                        "<p>" +
                        "<tt><a href=\"" + projectUrl + "\">"
                        + projectUrl + "</a></tt>" +
                        "</p>" +
                        "<p>" +
                        "<a href=\"" + supportUrl + "\">"
                        + "<img src=\"" + imageUrl + "\" border=\"0\"></a>" +
                        "</p>" +
                        "</center>");
        tabbedPane.add("GoGui", goguiPanel);
        boolean isProgramAvailable = (name != null && ! name.equals(""));
        JPanel programPanel;
        if (isProgramAvailable)
        {
            String fullName = name;
            if (version != null || ! version.equals(""))
                fullName = fullName + " " + version;
            programPanel =
                createPanel("<center>" +
                            "<b>" + fullName + "</b>" +
                            "<p>" +
                            "GTP Protocol Version " + protocolVersion
                            + "<br>" +
                            "</p>" +
                            "</center>");
        }
        else
            programPanel = new JPanel();
        tabbedPane.add("Go Program", programPanel);
        if (! isProgramAvailable)
            tabbedPane.setEnabledAt(1, false);
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
        JLabel dummyLabel = new JLabel();
        editorPane.setBackground(dummyLabel.getBackground());
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
