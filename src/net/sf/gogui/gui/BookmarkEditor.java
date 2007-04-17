//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Dialog for displaying and editing a bookmark. */
public class BookmarkEditor
    implements ObjectListEditor.ItemEditor
{
    public Object editItem(Component parent, Object object,
                           MessageDialogs messageDialogs)
    {
        return editItem(parent, "Edit Bookmark", (Bookmark)object, false,
                        messageDialogs);
    }

    public Bookmark editItem(Component parent, String title,
                             Bookmark bookmark, boolean selectName,
                             MessageDialogs messageDialogs)
    {
        JPanel panel = new JPanel(new BorderLayout(GuiUtil.SMALL_PAD, 0));
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight =
            new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        panel.add(m_panelRight, BorderLayout.CENTER);
        m_name = createEntry("Name", 25, bookmark.m_name);
        String file = "";
        if (bookmark.m_file != null)
            file = bookmark.m_file.toString();
        m_file = createEntry("File", 25, file);
        String move = "";
        if (bookmark.m_move > 0)
            move = Integer.toString(bookmark.m_move);
        m_move = createEntry("Move", 10, move);
        m_variation = createEntry("Variation", 10, bookmark.m_variation);
        JOptionPane optionPane = new JOptionPane(panel,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(parent, title);
        boolean done = false;
        while (! done)
        {
            if (selectName)
                m_name.selectAll();
            dialog.addWindowListener(new WindowAdapter() {
                    public void windowActivated(WindowEvent e) {
                        m_name.requestFocusInWindow();
                    }
                });
            dialog.setVisible(true);
            Object value = optionPane.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return null;
            done = validate(parent, messageDialogs);
        }
        String newName = m_name.getText().trim();
        File newFile = new File(m_file.getText());
        int newMove = getMove();
        String newVariation = m_variation.getText().trim();
        Bookmark newBookmark =
            new Bookmark(newName, newFile, newMove, newVariation);
        dialog.dispose();
        return newBookmark;
    }

    public String getItemLabel(Object object)
    {
        return ((Bookmark)object).m_name;
    }

    public Object cloneItem(Object object)
    {
        return new Bookmark((Bookmark)object);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JPanel m_panelLeft;

    private JPanel m_panelRight;

    private JTextField m_name;

    private JTextField m_file;

    private JTextField m_move;

    private JTextField m_variation;

    private JTextField createEntry(String labelText, int cols, String text)
    {
        Box boxLabel = Box.createHorizontalBox();
        boxLabel.add(Box.createHorizontalGlue());
        JLabel label = new JLabel(labelText + ":");
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        boxLabel.add(label);
        m_panelLeft.add(boxLabel);
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JTextField field = new JTextField(cols);
        field.setText(text);
        fieldPanel.add(field);
        m_panelRight.add(fieldPanel);
        return field;
    }

    private int getMove()
    {
        String text = m_move.getText().trim();
        if (text.equals(""))
            return 0;
        try
        {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private boolean validate(Component parent, MessageDialogs messageDialogs)
    {
        if (m_name.getText().trim().equals(""))
        {
            messageDialogs.showError(parent, "Name cannot be empty",
                                     "You need to enter a name for the "
                                     + " item in the Bookmarks menu. ",
                                     false);
            return false;
        }
        if (getMove() < 0)
        {
            messageDialogs.showError(parent, "Invalid move number",
                                     "Only positive move numbers are valid.",
                                     false);
            return false;
        }
        File file = new File(m_file.getText().trim());
        if (! file.exists())
        {
            messageDialogs.showError(parent, "File does not exist",
                                     "You need to enter the name of an "
                                     + "existing file.",
                                     false);
            return false;
        }
        return true;
    }
}

