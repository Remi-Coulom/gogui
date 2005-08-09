//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

//----------------------------------------------------------------------------

public class BookmarkDialog
    extends JOptionPane
{
    public static boolean show(Component parent, String title,
                               Bookmark bookmark, boolean selectName)
    {
        BookmarkDialog bookmarkDialog = new BookmarkDialog(bookmark);
        JDialog dialog = bookmarkDialog.createDialog(parent, title);
        boolean done = false;
        while (! done)
        {
            if (selectName)
                bookmarkDialog.m_name.selectAll();
            dialog.setVisible(true);
            Object value = bookmarkDialog.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return false;
            done = bookmarkDialog.validate(parent);
        }
        bookmark.m_name = bookmarkDialog.m_name.getText().trim();
        bookmark.m_file = new File(bookmarkDialog.m_file.getText());
        bookmark.m_move = bookmarkDialog.getMove();
        bookmark.m_variation = bookmarkDialog.m_variation.getText().trim();
        dialog.dispose();
        return true;
    }

    public BookmarkDialog(Bookmark bookmark)
    {
        JPanel panel = new JPanel(new BorderLayout(GuiUtils.PAD, 0));
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, 0));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight = new JPanel(new GridLayout(0, 1, 0, 0));
        panel.add(m_panelRight, BorderLayout.CENTER);
        m_name = createEntry("Name:", bookmark.m_name);
        String file = "";
        if (bookmark.m_file != null)
            file = bookmark.m_file.toString();
        m_file = createEntry("File:", file);
        String move = "";
        if (bookmark.m_move > 0)
            move = Integer.toString(bookmark.m_move);
        m_move = createEntry("Move:", move);
        m_variation = createEntry("Variation:", bookmark.m_variation);
        setMessage(panel);
        setOptionType(OK_CANCEL_OPTION);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final JPanel m_panelLeft;

    private final JPanel m_panelRight;

    private JTextField m_name;

    private JTextField m_file;

    private JTextField m_move;

    private JTextField m_variation;

    private JTextField createEntry(String labelText, String text)
    {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        m_panelLeft.add(label);
        JTextField textField = new JTextField(text);
        textField.setColumns(25);
        m_panelRight.add(textField);
        return textField;
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

    private boolean validate(Component parent)
    {
        if (m_name.getText().trim().equals(""))
        {
            SimpleDialogs.showError(parent, "Name cannot be empty");
            return false;
        }
        if (getMove() < 0)
        {
            SimpleDialogs.showError(parent, "Invalid move number");
            return false;
        }
        File file = new File(m_file.getText().trim());
        if (! file.exists())
        {
            SimpleDialogs.showError(parent, "File does not exist");
            return false;
        }
        return true;
    }
}

//----------------------------------------------------------------------------
