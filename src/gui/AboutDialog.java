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

//-----------------------------------------------------------------------------

public class AboutDialog
{
    public static void show(Component parent)
    {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());        
        String message =
            "<center>" +
            "<b>GoGui " + Version.get() + "</b>" +
            "<p>" +
            "Graphical interface to Go programs.<br>" +
            "&copy; 2003-2004, Markus Enzenberger" +
            "</p>" +
            "<p>" +
            "<tt>http://gogui.sourceforge.net</tt>" +
            "</p>" +
            "</center>";
        JEditorPane editorPane = new JEditorPane();
        panel.add(editorPane);
        JLabel dummyLabel = new JLabel();
        editorPane.setBackground(dummyLabel.getBackground());
        EditorKit editorKit =
            JEditorPane.createEditorKitForContentType("text/html");
        editorPane.setEditorKit(editorKit);
        editorPane.setText(message);
        JOptionPane.showMessageDialog(parent, panel, "About - GoGui",
                                      JOptionPane.PLAIN_MESSAGE);
    }
}
