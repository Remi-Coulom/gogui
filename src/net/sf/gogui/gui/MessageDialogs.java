//----------------------------------------------------------------------------
// $Id: SimpleDialogs.java 4309 2007-02-14 22:49:26Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.awt.Font;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;

/** Simple message dialogs. */
public final class MessageDialogs
{
    public void showError(Component frame, String mainMessage,
                          String optionalMessage)
    {
        showError(frame, mainMessage, optionalMessage, true);
    }

    public void showError(Component frame, String mainMessage,
                          String optionalMessage, boolean isCritical)
    {
        int type;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        else
            type = JOptionPane.ERROR_MESSAGE;
        Object[] options = { "Close" };
        Object defaultOption = options[0];
        show(null, frame, "Error", mainMessage, optionalMessage, type,
             options, defaultOption);
    }

    public void showError(Component frame, String message, Exception e)
    {
        showError(frame, message, e, true);
    }

    public void showError(Component frame, String message, Exception e,
                          boolean isCritical)
    {
        showError(frame, message, StringUtil.getErrorMessage(e), isCritical);
    }

    public void showInfo(Component frame, String mainMessage,
                         String optionalMessage, boolean isCritical)
    {        
        showInfo(null, frame, mainMessage, optionalMessage, isCritical);
    }

    public void showInfo(String disableKey, Component frame,
                         String mainMessage, String optionalMessage,
                         boolean isCritical)
    {        
        if (disableKey != null && m_disabled.contains(disableKey))
            return;
        int type;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        else
            type = JOptionPane.INFORMATION_MESSAGE;
        Object[] options = { "Close" };
        Object defaultOption = options[0];
        show(disableKey, frame, "Information", mainMessage, optionalMessage,
             type, options, defaultOption);
    }

    public int showYesNoCancelQuestion(Component parent, String mainMessage,
                                       String optionalMessage,
                                       String destructiveOption,
                                       String nonDestructiveOption)
    {
        return showYesNoCancelQuestion(null, parent, mainMessage,
                                       optionalMessage, destructiveOption,
                                       nonDestructiveOption);
    }

    /** Show a question with two options and cancel.
        @return 0 for the destructive option; 1 for the non-destructive
        option; 2 for cancel
    */
    public int showYesNoCancelQuestion(String disableKey, Component parent,
                                       String mainMessage,
                                       String optionalMessage,
                                       String destructiveOption,
                                       String nonDestructiveOption)
    {
        if (disableKey != null && m_disabled.contains(disableKey))
            return 0;
        Object[] options =
            { nonDestructiveOption, destructiveOption, "Cancel" };
        Object defaultOption = options[0];
        int type = JOptionPane.QUESTION_MESSAGE;
        Object value = show(disableKey, parent, "Question", mainMessage,
                            optionalMessage, type, options, defaultOption);
        int result;
        if (value == options[1])
            result = 0;
        else if (value == options[0])
            result = 1;
        else
        {
            assert(value == options[2] || value == null
                   || value.equals(new Integer(-1)));
            result = 2;
        }
        return result;
    }

    public void showWarning(Component parent, String mainMessage,
                            String optionalMessage, boolean isCritical)
    {
        showWarning(null, parent, mainMessage, optionalMessage, isCritical);
    }

    public void showWarning(String disableKey, Component parent,
                            String mainMessage, String optionalMessage,
                            boolean isCritical)
    {
        if (disableKey != null && m_disabled.contains(disableKey))
            return;
        int type;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        else
            type = JOptionPane.WARNING_MESSAGE;
        Object[] options = { "Close" };
        Object defaultOption = options[0];
        show(disableKey, parent, "Error", mainMessage, optionalMessage, type,
             options, defaultOption);
    }

    public boolean showQuestion(Component parent, String mainMessage,
                                String optionalMessage,
                                String destructiveOption, boolean isCritical)
    {
        return showQuestion(null, parent, mainMessage, optionalMessage,
                            destructiveOption, isCritical);
    }

    /** Show warning message to confirm destructive actions.
        @return true, if destructive was chosen; false if cancel was chosen.
    */
    public boolean showQuestion(String disableKey, Component parent,
                                String mainMessage,
                                String optionalMessage,
                                String destructiveOption, boolean isCritical)
    {
        if (disableKey != null && m_disabled.contains(disableKey))
            return true;
        Object[] options = { destructiveOption, "Cancel" };
        Object defaultOption = options[1];
        int type;
        if (isCritical)
            // No reason to show a warning icon for confirmation dialogs
            // of frequent actions
            type = JOptionPane.QUESTION_MESSAGE;
        else
            type = JOptionPane.PLAIN_MESSAGE;
        Object result = show(disableKey, parent, "Question", mainMessage,
                             optionalMessage, type, options, defaultOption);
        return (result == options[0]);
    }

    public boolean showWarningQuestion(Component parent, String mainMessage,
                                       String optionalMessage,
                                       String destructiveOption,
                                       boolean isCritical)
    {
        return showWarningQuestion(null, parent, mainMessage, optionalMessage,
                                   destructiveOption, isCritical);
    }

    /** Show warning message to confirm destructive actions.
        @return true, if destructive was chosen; false if cancel was chosen.
    */
    public boolean showWarningQuestion(String disableKey, Component parent,
                                       String mainMessage,
                                       String optionalMessage,
                                       String destructiveOption,
                                       boolean isCritical)
    {
        if (disableKey != null && m_disabled.contains(disableKey))
            return true;
        Object[] options = { destructiveOption, "Cancel" };
        Object defaultOption = options[1];
        int type;
        if (isCritical)
            type = JOptionPane.WARNING_MESSAGE;
        else
            type = JOptionPane.PLAIN_MESSAGE;
        Object result = show(disableKey, parent, "Question", mainMessage,
                             optionalMessage, type, options, defaultOption);
        return (result == options[0]);
    }

    private TreeSet m_disabled = new TreeSet();

    private static void addFiller(JComponent component)
    {
        Box.Filler filler = GuiUtil.createFiller();
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.add(filler);
    }

    private Object show(String disableKey, Component parent, String title,
                        String mainMessage, String optionalMessage,
                        int messageType, Object[] options,
                        Object defaultOption)
    {
        if (optionalMessage == null)
            optionalMessage = "";
        Box box = Box.createVerticalBox();
        JLabel label = new JLabel(mainMessage);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        Font labelFont = UIManager.getFont("Label.font");
        Font labelFontBold = labelFont.deriveFont(Font.BOLD);
        label.setFont(labelFontBold);
        addFiller(box);
        box.add(label);
        int columns = Math.min(30, optionalMessage.length());
        JTextArea textArea = new JTextArea(optionalMessage, 0, columns);
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setForeground(UIManager.getColor("Label.foreground"));
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(labelFont);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        addFiller(box);
        box.add(textArea);
        addFiller(box);
        addFiller(box);
        JCheckBox disableCheckBox = null;
        if (disableKey != null)
        {
            if (messageType == JOptionPane.QUESTION_MESSAGE)
                disableCheckBox = new JCheckBox("Do not ask again");
            else if (messageType == JOptionPane.WARNING_MESSAGE)
                disableCheckBox =
                    new JCheckBox("Do not show this warning again");
            else
                disableCheckBox =
                    new JCheckBox("Do not show this message again");
            String toolTipText =
                "Disable this kind of messages for the current session";
            disableCheckBox.setToolTipText(toolTipText);
            disableCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(disableCheckBox);
        }
        if (Platform.isMac())
            // Don't show icons on Mac, problem with icon generation in
            // Quaqua 3.7.2
            messageType = JOptionPane.PLAIN_MESSAGE;
        JOptionPane optionPane =
            new JOptionPane(box, messageType, JOptionPane.DEFAULT_OPTION,
                            null, options, defaultOption);
        JDialog dialog = optionPane.createDialog(parent, title);
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        box.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
        if (disableKey != null && disableCheckBox.isSelected())
            m_disabled.add(disableKey);
        return optionPane.getValue();
    }
}
