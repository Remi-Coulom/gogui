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

/** Warning which can be disabled.
    Also provides multi-line word-wrapped text.
*/
public class OptionalWarning
{
    public OptionalWarning(Component parent)
    {
        m_parent = parent;
    }
    
    public boolean show(String message)
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
        JCheckBox disabled = new JCheckBox("Disable this warning");
        disabled.setSelected(m_disabled);
        checkBoxPanel.add(disabled, BorderLayout.WEST);
        panel.add(checkBoxPanel);
        Object options[] = { "Ok", "Cancel" };
        JOptionPane optionPane
            = new JOptionPane(panel, JOptionPane.WARNING_MESSAGE,
                              JOptionPane.OK_CANCEL_OPTION, null, options,
                              options[1]);
        JDialog dialog = optionPane.createDialog(m_parent, "Warning");
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
