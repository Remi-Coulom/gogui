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
        JLabel dummyLabel = new JLabel();
        editorPane.setBackground(dummyLabel.getBackground());
        EditorKit editorKit =
            JEditorPane.createEditorKitForContentType("text/html");
        editorPane.setEditorKit(editorKit);
        editorPane.setText(message);
        JOptionPane.showMessageDialog(parent, editorPane, "About - GoGui",
                                      JOptionPane.PLAIN_MESSAGE);
    }
}
