//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import version.*;
import utils.GuiUtils;

//-----------------------------------------------------------------------------

public class AboutDialog
{
    public static void show(Component parent, String name, String version)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        String program = null;
        if (name != null && ! name.equals(""))
        {
            program = name;
            if (version != null && ! version.equals(""))
                program = program + " " + version;
        }
        if (program != null)
        {
            panel.add(createPanel("<center>" +
                                  "<b>" + program + "</b>" +
                                  "</center>"));
            panel.add(GuiUtils.createSmallFiller());
        }
        panel.add(createPanel("<center>" +
                              "<b>GoGui " + Version.get() + "</b>" +
                              "<p>" +
                              "Graphical interface to Go programs.<br>" +
                              "&copy; 2003-2004, Markus Enzenberger" +
                              "</p>" +
                              "<p>" +
                              "<tt>http://gogui.sourceforge.net</tt>" +
                              "</p>" +
                              "</center>"));
        JOptionPane.showMessageDialog(parent, panel, "About GoGui",
                                      JOptionPane.PLAIN_MESSAGE);
    }

    private static JPanel createPanel(String text)
    {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setBorder(BorderFactory.createEtchedBorder());        
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
