//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//----------------------------------------------------------------------------

public class EditBookmarksDialog
    extends JOptionPane
{
    public static boolean show(Component parent, Vector bookmarks)
    {
        Vector tempBookmarks = new Vector();
        copyBookmarks(bookmarks, tempBookmarks);
        EditBookmarksDialog editDialog
            = new EditBookmarksDialog(tempBookmarks);
        JDialog dialog = editDialog.createDialog(parent, "Edit Bookmarks");
        dialog.setVisible(true);
        Object value = editDialog.getValue();
        boolean result = true;
        if (! (value instanceof Integer)
            || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
            result = false;
        dialog.dispose();
        if (result)
            copyBookmarks(tempBookmarks, bookmarks);
        return result;
    }

    public EditBookmarksDialog(Vector bookmarks)
    {
        m_bookmarks = bookmarks;
        m_actionListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    String command = event.getActionCommand();
                    if (command.equals("edit"))
                        cbEdit();
                    else if (command.equals("move-up"))
                        cbMoveUp();
                    else if (command.equals("move-down"))
                        cbMoveDown();
                    else if (command.equals("remove"))
                        cbRemove();
                    else
                        assert(false);
                }
            };
        JPanel panel = new JPanel(new BorderLayout(GuiUtils.PAD, 0));
        m_list = new JList();
        m_list.addListSelectionListener(new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    selectionChanged();
                }
            });
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(m_list);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.EAST);
        setMessage(panel);
        setOptionType(OK_CANCEL_OPTION);
        updateList(m_bookmarks.size() == 0 ? -1 : 0);
    }

    private ActionListener m_actionListener;

    private JButton m_edit;

    private JButton m_moveDown;

    private JButton m_moveUp;

    private JButton m_remove;

    private JList m_list;

    private Vector m_bookmarks;

    private void cbMoveDown()
    {
        int index = m_list.getSelectedIndex();
        if (index < 0 || index >= m_bookmarks.size() - 1)
            return;
        Object temp = m_bookmarks.get(index);
        m_bookmarks.set(index, m_bookmarks.get(index + 1));
        m_bookmarks.set(index + 1, temp);
        updateList(index + 1);
    }

    private void cbEdit()
    {
        Bookmark bookmark = getSelected();
        if (bookmark == null)
            return;
        int selectedIndex = m_list.getSelectedIndex();
        Bookmark tempBookmark = new Bookmark(bookmark);
        if (! BookmarkDialog.show(this, "Edit Bookmark", tempBookmark, false))
            return;
        bookmark.copyFrom(tempBookmark);
        updateList(selectedIndex);
    }

    private void cbMoveUp()
    {
        int index = m_list.getSelectedIndex();
        if (index < 0 || index == 0)
            return;
        Object temp = m_bookmarks.get(index);
        m_bookmarks.set(index, m_bookmarks.get(index - 1));
        m_bookmarks.set(index - 1, temp);
        updateList(index - 1);
    }

    private void cbRemove()
    {
        Bookmark bookmark = getSelected();
        if (bookmark == null)
            return;
        int selectedIndex = m_list.getSelectedIndex();
        String name = bookmark.m_name;
        if (! SimpleDialogs.showQuestion(this, "Remove '" + name + "'?"))
            return;
        m_bookmarks.remove(bookmark);
        if (selectedIndex >= m_bookmarks.size())
            selectedIndex = -1;
        updateList(selectedIndex);
    }

    private static void copyBookmarks(Vector from, Vector to)
    {
        to.clear();
        for (int i = 0; i < from.size(); ++i)
            to.add(new Bookmark((Bookmark)from.get(i)));
    }

    private JButton createButton(String label, String command)
    {
        JButton button = new JButton(label);
        button.setEnabled(false);
        button.setActionCommand(command);
        button.addActionListener(m_actionListener);
        return button;
    }

    private JPanel createButtonPanel()
    {
        JPanel buttonPanel
            = new JPanel(new GridLayout(0, 1, GuiUtils.PAD, GuiUtils.PAD));
        m_edit = createButton("Edit", "edit");
        buttonPanel.add(m_edit);
        m_remove = createButton("Remove", "remove");
        buttonPanel.add(m_remove);
        m_moveUp = createButton("Move Up", "move-up");
        buttonPanel.add(m_moveUp);
        m_moveDown = createButton("Move Down", "move-down");
        buttonPanel.add(m_moveDown);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.NORTH);
        return panel;
    }

    private Bookmark getBookmark(int i)
    {
        return (Bookmark)m_bookmarks.get(i);
    }

    private Bookmark getSelected()
    {
        int i = m_list.getSelectedIndex();
        if (i == -1)
            return null;
        return getBookmark(i);
    }

    private void selectionChanged()
    {
        int index = m_list.getSelectedIndex();
        m_edit.setEnabled(index >= 0);
        m_remove.setEnabled(index >= 0);
        m_moveUp.setEnabled(index >= 1);
        m_moveDown.setEnabled(index < m_bookmarks.size() - 1);
    }

    private void updateList(int selectedIndex)
    {
        Vector data = new Vector();
        for (int i = 0; i < m_bookmarks.size(); ++i)
        {
            String name = getBookmark(i).m_name;
            data.add(name);
        }
        m_list.setListData(data);
        m_list.setSelectedIndex(selectedIndex);
    }
}

//----------------------------------------------------------------------------
