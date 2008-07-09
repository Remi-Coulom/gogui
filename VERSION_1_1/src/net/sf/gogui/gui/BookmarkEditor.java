// BookmarkEditor.java

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
import static net.sf.gogui.gui.I18n.i18n;

/** Dialog for displaying and editing a bookmark. */
public class BookmarkEditor
    implements ObjectListEditor.ItemEditor<Bookmark>
{
    public Bookmark editItem(Component parent, Bookmark object,
                             MessageDialogs messageDialogs)
    {
        return editItem(parent, i18n("TIT_BOOKMARKEDITOR"),
                        (Bookmark)object, false, messageDialogs);
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
        m_name = createEntry("LB_BOOKMARKEDITOR_NAME", 25, bookmark.m_name);
        String file = "";
        if (bookmark.m_file != null)
            file = bookmark.m_file.toString();
        m_file = createEntry("LB_BOOKMARKEDITOR_FILE", 25, file);
        String move = "";
        if (bookmark.m_move > 0)
            move = Integer.toString(bookmark.m_move);
        m_move = createEntry("LB_BOOKMARKEDITOR_MOVE", 10, move);
        m_variation = createEntry("LB_BOOKMARKEDITOR_VARIATION", 10,
                                  bookmark.m_variation);
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

    public String getItemLabel(Bookmark object)
    {
        return object.m_name;
    }

    public Bookmark cloneItem(Bookmark object)
    {
        return new Bookmark(object);
    }

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
        JLabel label = new JLabel(i18n(labelText));
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
            messageDialogs.showError(parent,
                                     i18n("MSG_BOOKMARKEDITOR_EMPTYNAME"),
                                     i18n("MSG_BOOKMARKEDITOR_EMPTYNAME_2"),
                                     false);
            return false;
        }
        if (getMove() < 0)
        {
            messageDialogs.showError(parent,
                                     i18n("MSG_BOOKMARKEDITOR_INVALIDMOVE"),
                                     i18n("MSG_BOOKMARKEDITOR_INVALIDMOVE_2"),
                                     false);
            return false;
        }
        File file = new File(m_file.getText().trim());
        if (! file.exists())
        {
            messageDialogs.showError(parent,
                                     i18n("MSG_BOOKMARKEDITOR_FILENOTEXIST"),
                                     i18n("MSG_BOOKMARKEDITOR_FILENOTEXIST_2"),
                                     false);
            return false;
        }
        return true;
    }
}
