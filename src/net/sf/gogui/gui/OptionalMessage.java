//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.UIManager;

/** Message which can be disabled.
    Also provides multi-line word-wrapped text.
*/
public class OptionalMessage
{
    public OptionalMessage(Component parent)
    {
        m_parent = parent;
    }
    
    public void showMessage(String message)
    {
        if (m_disabled)
            return;
        show(message, JOptionPane.INFORMATION_MESSAGE, false);
        m_disabled = m_disabledCheckBox.isSelected();
    }

    public boolean showQuestion(String message)
    {
        if (m_disabled)
            return true;
        show(message, JOptionPane.QUESTION_MESSAGE, false);
        boolean result = (m_optionPane.getValue() == m_options[0]);
        if (result)
            m_disabled = m_disabledCheckBox.isSelected();
        return result;
    }

    public boolean showWarning(String message)
    {
        if (m_disabled)
            return true;
        show(message, JOptionPane.WARNING_MESSAGE, false);
        boolean result = (m_optionPane.getValue() == m_options[0]);
        if (result)
            m_disabled = m_disabledCheckBox.isSelected();
        return result;
    }

    public int showYesNoCancelQuestion(String message)
    {
        if (m_disabled)
            return 1;
        show(message, JOptionPane.QUESTION_MESSAGE, true);
        Object value = m_optionPane.getValue();
        int result;
        if (value == m_options[0])
            result = 0;
        else if (value == m_options[1])
            result = 1;
        else
        {
            assert(value == m_options[2] || value == null);
            result = 2;
        }
        if (result != 2)
            m_disabled = m_disabledCheckBox.isSelected();
        return result;
    }

    private final Component m_parent;

    private boolean m_disabled;

    private Object[] m_options;

    private JOptionPane m_optionPane;

    private JCheckBox m_disabledCheckBox;

    /** Show message dialog if it was not disabled.
        @param message The message text
        @param type The message type (JOptionPane.QUESTION_MESSAGE,
        JOptionPane.WARNING_MESSAGE or JOptionPane.INFORMATION_MESSAGE)
        @param isYesNoCancel true, if buttons should be "yes", "no", "cancel";
        false if buttons should be "ok", "cancel"
        @return true, if message was not shown or confirmed
    */
    private void show(String message, int type, boolean isYesNoCancel)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        int columns = Math.min(30, message.length());
        JTextArea textArea = new JTextArea(message, 0, columns);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setForeground(UIManager.getColor("Label.foreground"));
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        panel.add(GuiUtil.createFiller());
        panel.add(GuiUtil.createFiller());
        panel.add(textArea);
        panel.add(GuiUtil.createFiller());
        panel.add(GuiUtil.createFiller());
        JPanel checkBoxPanel = new JPanel(new BorderLayout());
        String title;
        Object defaultOption;
        int optionType;
        if (type == JOptionPane.QUESTION_MESSAGE)
        {
            m_disabledCheckBox = new JCheckBox("Do not ask again");
            title = "Question";
            if (isYesNoCancel)
            {
                m_options = new Object[3];
                m_options[0] = "Yes";
                m_options[1] = "No";
                m_options[2] = "Cancel";
                defaultOption = m_options[2];
                optionType = JOptionPane.YES_NO_CANCEL_OPTION;
            }
            else
            {
                m_options = new Object[2];
                m_options[0] = "Ok";
                m_options[1] = "Cancel";
                defaultOption = m_options[1];
                optionType = JOptionPane.OK_CANCEL_OPTION;
            }
        }
        else if (type == JOptionPane.WARNING_MESSAGE)
        {
            m_disabledCheckBox =
                new JCheckBox("Do not show this warning again");
            title = "Warning";
            m_options = new Object[2];
            m_options[0] = "Ok";
            m_options[1] = "Cancel";
            defaultOption = m_options[1];
            optionType = JOptionPane.OK_CANCEL_OPTION;
        }
        else
        {
            m_disabledCheckBox =
                new JCheckBox("Do not show this message again");
            title = "Information";
            m_options = new Object[1];
            m_options[0] = "Ok";
            defaultOption = m_options[0];
            optionType = JOptionPane.OK_OPTION;
        }
        m_disabledCheckBox.setSelected(m_disabled);
        checkBoxPanel.add(m_disabledCheckBox, BorderLayout.WEST);
        panel.add(checkBoxPanel);
        m_optionPane = new JOptionPane(panel, type, optionType, null,
                                       m_options, defaultOption);
        JDialog dialog = m_optionPane.createDialog(m_parent, title);
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        panel.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
    }
}

