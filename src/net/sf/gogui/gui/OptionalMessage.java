//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import net.sf.gogui.util.Platform;

/** Message which can be disabled.
    Also provides multi-line word-wrapped text.
*/
public class OptionalMessage
{
    public OptionalMessage(Component parent)
    {
        m_parent = parent;
    }
    
    public void showMessage(String mainMessage, String optionalMessage)
    {
        if (m_disabled)
            return;
        show(mainMessage, optionalMessage, JOptionPane.INFORMATION_MESSAGE,
             JOptionPane.OK_OPTION);
        m_disabled = m_disabledCheckBox.isSelected();
    }

    public boolean showQuestion(String mainMessage, String optionalMessage)
    {
        if (m_disabled)
            return true;
        show(mainMessage, optionalMessage, JOptionPane.QUESTION_MESSAGE,
             JOptionPane.OK_CANCEL_OPTION);
        boolean result = (m_optionPane.getValue() == m_options[0]);
        if (result)
            m_disabled = m_disabledCheckBox.isSelected();
        return result;
    }

    public void showWarning(String mainMessage, String optionalMessage,
                            boolean isCritical)
    {
        if (m_disabled)
            return;
        int type = JOptionPane.WARNING_MESSAGE;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        show(mainMessage, optionalMessage, type, JOptionPane.OK_OPTION);
        m_disabled = m_disabledCheckBox.isSelected();
    }

    public boolean showWarningQuestion(String mainMessage,
                                       String optionalMessage,
                                       boolean isCritical)
    {
        if (m_disabled)
            return true;
        int type = JOptionPane.WARNING_MESSAGE;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        show(mainMessage, optionalMessage, type, JOptionPane.OK_CANCEL_OPTION);
        boolean result = (m_optionPane.getValue() == m_options[0]);
        if (result)
            m_disabled = m_disabledCheckBox.isSelected();
        return result;
    }

    public int showYesNoCancelQuestion(String mainMessage,
                                       String optionalMessage)
    {
        if (m_disabled)
            return 1;
        show(mainMessage, optionalMessage, JOptionPane.QUESTION_MESSAGE,
             JOptionPane.YES_NO_CANCEL_OPTION);
        Object value = m_optionPane.getValue();
        int result;
        if (value == m_options[0])
            result = 0;
        else if (value == m_options[1])
            result = 1;
        else
        {
            assert(value == m_options[2] || value == null
                   || value.equals(new Integer(-1)));
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
        @param type The message type (JOptionPane.QUESTION_MESSAGE,
        JOptionPane.WARNING_MESSAGE or JOptionPane.INFORMATION_MESSAGE)
        @param isYesNoCancel true, if buttons should be "yes", "no", "cancel";
        false if buttons should be "ok", "cancel"
        @return true, if message was not shown or confirmed
    */
    private void show(String mainMessage, String optionalMessage,
                      int messageType, int optionType)
    {
        if (Platform.isMac())
            messageType = JOptionPane.PLAIN_MESSAGE;
        Box box = Box.createVerticalBox();
        JLabel label =
            new JLabel(GuiUtil.formatMessage(mainMessage, optionalMessage));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        GuiUtil.setUnlimitedSize(label);
        box.add(GuiUtil.createFiller());
        box.add(label);
        box.add(GuiUtil.createFiller());
        box.add(GuiUtil.createFiller());
        JPanel checkBoxPanel = new JPanel(new BorderLayout());
        checkBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        String title;
        Object defaultOption;
        if (messageType == JOptionPane.QUESTION_MESSAGE)
        {
            m_disabledCheckBox = new JCheckBox("Do not ask again");
            title = "Question";
            if (optionType == JOptionPane.YES_NO_CANCEL_OPTION)
            {
                m_options = new Object[3];
                m_options[0] = "Yes";
                m_options[1] = "No";
                m_options[2] = "Cancel";
                defaultOption = m_options[2];
            }
            else
            {
                m_options = new Object[2];
                m_options[0] = "Ok";
                m_options[1] = "Cancel";
                defaultOption = m_options[1];
            }
        }
        else if (messageType == JOptionPane.WARNING_MESSAGE)
        {
            m_disabledCheckBox =
                new JCheckBox("Do not show this warning again");
            title = "Warning";
            if (optionType == JOptionPane.OK_CANCEL_OPTION)
            {
                m_options = new Object[2];
                m_options[0] = "Ok";
                m_options[1] = "Cancel";
                defaultOption = m_options[1];
            }
            else
            {
                m_options = new Object[1];
                m_options[0] = "Ok";
                defaultOption = m_options[0];
            }
        }
        else
        {
            m_disabledCheckBox =
                new JCheckBox("Do not show this message again");
            title = "Information";
            m_options = new Object[1];
            m_options[0] = "Ok";
            defaultOption = m_options[0];
        }
        m_disabledCheckBox.setSelected(m_disabled);
        String toolTipText =
            "Disable this kind of messages for the current session";
        m_disabledCheckBox.setToolTipText(toolTipText);
        checkBoxPanel.add(m_disabledCheckBox, BorderLayout.WEST);
        box.add(checkBoxPanel);
        m_optionPane = new JOptionPane(box, messageType, optionType, null,
                                       m_options, defaultOption);
        JDialog dialog = m_optionPane.createDialog(m_parent, title);
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        box.invalidate();
        dialog.pack();
        box.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
    }
}

