//----------------------------------------------------------------------------
// $Id$
// $Source$
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

//----------------------------------------------------------------------------

/** Message which can be disabled.
    Also provides multi-line word-wrapped text.
*/
public class OptionalMessage
{
    public OptionalMessage(Component parent)
    {
        m_parent = parent;
    }
    
    public boolean show(String message)
    {
        return show(message, JOptionPane.WARNING_MESSAGE);
    }


    /** Show message dialog if it was not disabled.
        @param message The message text
        @param type The message type (JOptionPane.QUESTION_MESSAGE,
        JOptionPane.WARNING_MESSAGE or JOptionPane.INFORMATION_MESSAGE)
        @return true, if message was not shown or confirmed
    */
    public boolean show(String message, int type)
    {
        if (m_disabled)
            return true;
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        int columns = Math.min(30, message.length());
        JTextArea textArea = new JTextArea(message, 0, columns);
        textArea.setEditable(false);
        textArea.setForeground(UIManager.getColor("Label.foreground"));
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        panel.add(textArea);
        panel.add(GuiUtils.createFiller());
        JPanel checkBoxPanel = new JPanel(new BorderLayout());
        JCheckBox disabled;
        String title;
        Object[] options;
        Object defaultOption;
        int optionType;
        if (type == JOptionPane.QUESTION_MESSAGE)
        {
            disabled = new JCheckBox("Do not ask again");
            title = "Question";
            options = new Object[2];
            options[1] = "Ok";
            options[2] = "Cancel";
            defaultOption = options[1];
            optionType = JOptionPane.OK_CANCEL_OPTION;
        }
        else if (type == JOptionPane.WARNING_MESSAGE)
        {
            disabled = new JCheckBox("Do not show this warning again");
            title = "Warning";
            options = new Object[2];
            options[1] = "Ok";
            options[2] = "Cancel";
            defaultOption = options[1];
            optionType = JOptionPane.OK_CANCEL_OPTION;
        }
        else
        {
            disabled = new JCheckBox("Do not show this message again");
            title = "Information";
            options = new Object[1];
            options[0] = "Ok";
            defaultOption = options[0];
            optionType = JOptionPane.OK_OPTION;
        }
        disabled.setSelected(m_disabled);
        checkBoxPanel.add(disabled, BorderLayout.WEST);
        panel.add(checkBoxPanel);
        JOptionPane optionPane
            = new JOptionPane(panel, type, optionType, null, options,
                              defaultOption);
        JDialog dialog = optionPane.createDialog(m_parent, title);
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        panel.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        boolean result = (optionPane.getValue() == options[0]);
        dialog.dispose();
        if (result)
            m_disabled = disabled.isSelected();
        return result;
    }

    private final Component m_parent;

    private boolean m_disabled;
}

//----------------------------------------------------------------------------
