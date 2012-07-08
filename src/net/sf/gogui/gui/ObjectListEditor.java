// ObjectListEditor.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
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
import static net.sf.gogui.gui.I18n.i18n;

/** Dialog for displaying and editing a list of objects. */
public class ObjectListEditor<OBJECT>
{
    /** Edit properties of object. */
    public interface ItemEditor<OBJECT>
    {
        OBJECT editItem(Component parent, OBJECT object,
                        MessageDialogs messageDialogs);

        String getItemLabel(OBJECT object);

        OBJECT cloneItem(OBJECT object);
    }

    public boolean edit(Component parent, String title,
                        ArrayList<OBJECT> objects,
                        ItemEditor<OBJECT> editor,
                        MessageDialogs messageDialogs)
    {
        m_messageDialogs = messageDialogs;
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
                        assert false;
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
        int rows = Math.min(Math.max(objects.size(), 8), 15);
        m_list.setVisibleRowCount(rows);
        JScrollPane scrollPane = new JScrollPane(m_list);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.EAST);
        JOptionPane optionPane = new JOptionPane(panel,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION);
        m_objects = new ArrayList<OBJECT>();
        copyObjects(objects, m_objects);
        updateList(m_objects.isEmpty() ? -1 : 0);
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

    private ActionListener m_actionListener;

    private JButton m_edit;

    private JButton m_moveDown;

    private JButton m_moveUp;

    private JButton m_remove;

    /** @note JList is a generic type since Java 7. We use a raw type
        and suppress unchecked warnings where needed to be compatible with
        earlier Java versions. */
    private JList m_list;

    private JDialog m_dialog;

    private ArrayList<OBJECT> m_objects;

    private ItemEditor<OBJECT> m_editor;

    private MessageDialogs m_messageDialogs;

    private void cbMoveDown()
    {
        int index = m_list.getSelectedIndex();
        if (index < 0 || index >= m_objects.size() - 1)
            return;
        OBJECT temp = m_objects.get(index);
        m_objects.set(index, m_objects.get(index + 1));
        m_objects.set(index + 1, temp);
        updateList(index + 1);
    }

    private void cbEdit()
    {
        int index = m_list.getSelectedIndex();
        if (index == -1)
            return;
        OBJECT object = m_editor.editItem(m_dialog, getObject(index),
                                          m_messageDialogs);
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
        OBJECT temp = m_objects.get(index);
        m_objects.set(index, m_objects.get(index - 1));
        m_objects.set(index - 1, temp);
        updateList(index - 1);
    }

    private void cbRemove()
    {
        int index = m_list.getSelectedIndex();
        if (index == -1)
            return;
        OBJECT object = getObject(index);
        String name = m_editor.getItemLabel(object);
        String disableKey = "net.sf.gogui.gui.ObjectListEditor.remove";
        String mainMessage =
            MessageFormat.format(i18n("MSG_LISTEDITOR_REALLY_REMOVE"),
                                 name);
        String optionalMessage = i18n("MSG_LISTEDITOR_REALLY_REMOVE_2");
        if (! m_messageDialogs.showQuestion(disableKey, m_dialog, mainMessage,
                                            optionalMessage,
                                            i18n("LB_REMOVE"), false))
            return;
        m_objects.remove(object);
        if (index >= m_objects.size())
            index = -1;
        updateList(index);
    }

    private void copyObjects(ArrayList<OBJECT> from, ArrayList<OBJECT> to)
    {
        to.clear();
        for (int i = 0; i < from.size(); ++i)
            to.add(m_editor.cloneItem(from.get(i)));
    }

    private JButton createButton(String label, String command)
    {
        JButton button = new JButton(i18n(label));
        button.setEnabled(false);
        button.setActionCommand(command);
        button.addActionListener(m_actionListener);
        return button;
    }

    private JPanel createButtonPanel()
    {
        JPanel buttonPanel
            = new JPanel(new GridLayout(0, 1, GuiUtil.PAD, GuiUtil.PAD));
        m_moveUp = createButton("LB_LISTEDITOR_MOVE_UP", "move-up");
        GuiUtil.setMacBevelButton(m_moveUp);
        buttonPanel.add(m_moveUp);
        m_moveDown = createButton("LB_LISTEDITOR_MOVE_DOWN", "move-down");
        GuiUtil.setMacBevelButton(m_moveDown);
        buttonPanel.add(m_moveDown);
        m_edit = createButton("LB_EDIT", "edit");
        GuiUtil.setMacBevelButton(m_edit);
        buttonPanel.add(m_edit);
        m_remove = createButton("LB_REMOVE", "remove");
        GuiUtil.setMacBevelButton(m_remove);
        buttonPanel.add(m_remove);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.NORTH);
        return panel;
    }

    private OBJECT getObject(int i)
    {
        return m_objects.get(i);
    }

    private void selectionChanged()
    {
        int index = m_list.getSelectedIndex();
        m_edit.setEnabled(index >= 0);
        m_remove.setEnabled(index >= 0);
        m_moveUp.setEnabled(index >= 1);
        m_moveDown.setEnabled(index < m_objects.size() - 1);
    }

    // See comment at m_list
    @SuppressWarnings("unchecked")
    private void updateList(int selectedIndex)
    {
        ArrayList<String> data = new ArrayList<String>();
        for (int i = 0; i < m_objects.size(); ++i)
        {
            String name = m_editor.getItemLabel(getObject(i));
            data.add(name);
        }
        m_list.setListData(data.toArray());
        m_list.setSelectedIndex(selectedIndex);
        m_list.ensureIndexIsVisible(selectedIndex);
    }
}
