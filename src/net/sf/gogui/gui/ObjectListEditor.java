//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** Dialog for displaying and editing a list of objects. */
public class ObjectListEditor
{
    interface ItemEditor
    {
        Object editItem(Component parent, Object object);

        String getItemLabel(Object object);

        Object cloneItem(Object object);
    }

    public boolean edit(Component parent, String title, ArrayList objects,
                        ItemEditor editor)
    {
        m_editor = editor;
        m_actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
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
        JPanel panel = new JPanel(new BorderLayout(GuiUtil.PAD, 0));
        m_list = new JList();
        m_list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    selectionChanged();
                }
            });
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(m_list);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.EAST);
        JOptionPane optionPane = new JOptionPane(panel,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION);
        m_objects = new ArrayList();
        copyObjects(objects, m_objects);
        updateList(m_objects.size() == 0 ? -1 : 0);
        m_dialog = optionPane.createDialog(parent, title);
        m_dialog.setVisible(true);
        Object value = optionPane.getValue();
        boolean result = true;
        if (! (value instanceof Integer)
            || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
            result = false;
        m_dialog.dispose();
        if (result)
            copyObjects(m_objects, objects);
        return result;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private ActionListener m_actionListener;

    private JButton m_edit;

    private JButton m_moveDown;

    private JButton m_moveUp;

    private JButton m_remove;

    private JList m_list;

    private OptionalMessage m_removeWarning;

    private JDialog m_dialog;

    private ArrayList m_objects;

    private ItemEditor m_editor;

    private void cbMoveDown()
    {
        int index = m_list.getSelectedIndex();
        if (index < 0 || index >= m_objects.size() - 1)
            return;
        Object temp = m_objects.get(index);
        m_objects.set(index, m_objects.get(index + 1));
        m_objects.set(index + 1, temp);
        updateList(index + 1);
    }

    private void cbEdit()
    {
        int index = m_list.getSelectedIndex();
        if (index == -1)
            return;
        Object object = m_editor.editItem(m_dialog, getObject(index));
        if (object == null)
            return;
        m_objects.set(index, object);
        updateList(index);
    }

    private void cbMoveUp()
    {
        int index = m_list.getSelectedIndex();
        if (index < 0 || index == 0)
            return;
        Object temp = m_objects.get(index);
        m_objects.set(index, m_objects.get(index - 1));
        m_objects.set(index - 1, temp);
        updateList(index - 1);
    }

    private void cbRemove()
    {
        int index = m_list.getSelectedIndex();
        if (index == -1)
            return;
        Object object = getObject(index);
        String name = m_editor.getItemLabel(object);
        if (m_removeWarning == null)
            m_removeWarning = new OptionalMessage(m_dialog);
        if (! m_removeWarning.showWarningQuestion("Really remove " + name
                                                  + "?", null, true))
            return;
        m_objects.remove(object);
        if (index >= m_objects.size())
            index = -1;
        updateList(index);
    }

    private void copyObjects(ArrayList from, ArrayList to)
    {
        to.clear();
        for (int i = 0; i < from.size(); ++i)
            to.add(m_editor.cloneItem(from.get(i)));
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
            = new JPanel(new GridLayout(0, 1, GuiUtil.PAD, GuiUtil.PAD));
        m_moveUp = createButton("Move Up", "move-up");
        buttonPanel.add(m_moveUp);
        m_moveDown = createButton("Move Down", "move-down");
        buttonPanel.add(m_moveDown);
        m_edit = createButton("Edit", "edit");
        buttonPanel.add(m_edit);
        m_remove = createButton("Remove", "remove");
        buttonPanel.add(m_remove);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.NORTH);
        return panel;
    }

    private Object getObject(int i)
    {
        return (Object)m_objects.get(i);
    }

    private void selectionChanged()
    {
        int index = m_list.getSelectedIndex();
        m_edit.setEnabled(index >= 0);
        m_remove.setEnabled(index >= 0);
        m_moveUp.setEnabled(index >= 1);
        m_moveDown.setEnabled(index < m_objects.size() - 1);
    }

    private void updateList(int selectedIndex)
    {
        ArrayList data = new ArrayList();
        for (int i = 0; i < m_objects.size(); ++i)
        {
            String name = m_editor.getItemLabel(getObject(i));
            data.add(name);
        }
        m_list.setListData(data.toArray());
        m_list.setSelectedIndex(selectedIndex);
    }
}

