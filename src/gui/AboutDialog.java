//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import version.*;
import utils.GuiUtils;

//----------------------------------------------------------------------------

public class AboutDialog
{
    public static void show(Component parent, String name, String version,
                            String protocolVersion)
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel goguiPanel =
            createPanel("<center>" +
                        "<b>GoGui " + Version.get() + "</b>" +
                        "<p>" +
                        "Graphical interface to Go programs.<br>" +
                        "&copy; 2003-2004, Markus Enzenberger" +
                        "</p>" +
                        "<p>" +
                        "<tt>http://gogui.sourceforge.net</tt>" +
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
        JOptionPane.showMessageDialog(parent, tabbedPane, "About",
                                      JOptionPane.PLAIN_MESSAGE);
    }

    private static JPanel createPanel(String text)
    {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(GuiUtils.createEmptyBorder());        
        panel.add(editorPane);
        JLabel dummyLabel = new JLabel();
        editorPane.setBackground(dummyLabel.getBackground());
        EditorKit editorKit =
            JEditorPane.createEditorKitForContentType("text/html");
        editorPane.setEditorKit(editorKit);
        editorPane.setText(text);
        return panel;
    }
}

//----------------------------------------------------------------------------
