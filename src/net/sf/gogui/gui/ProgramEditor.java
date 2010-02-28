// ProgramEditor.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import static net.sf.gogui.gui.GuiUtil.insertLineBreaks;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.util.StringUtil;

/** Dialog for displaying and editing a program. */
public class ProgramEditor
    implements ObjectListEditor.ItemEditor<Program>
{
    public Program editItem(Component parent, Program object,
                           MessageDialogs messageDialogs)
    {
        return editItem(parent, i18n("TIT_PROGRAMEDIT"),
                        (Program)object, false, false, messageDialogs);
    }

    /** Edit an instance of Program.
        @param parent Parent component for message dialog
        @param title Title for this dialog
        @param program Program instance to edit
        @param editOnlyCommand Show and edit only command and working directory
        (as a first step, such that name, version and suggested label can be
        set after querying the program)
        @param editOnlyLabel Edit only the label (show the other information
        non-editable)
        @param messageDialogs Message dialog manager */
    public Program editItem(Component parent, String title, Program program,
                            boolean editOnlyCommand, boolean editOnlyLabel,
                            MessageDialogs messageDialogs)
    {
        m_editOnlyCommand = editOnlyCommand;
        m_editOnlyLabel = editOnlyLabel;
        JPanel panel = new JPanel(new BorderLayout(GuiUtil.SMALL_PAD, 0));
        Box box = null;
        if (editOnlyCommand || editOnlyLabel)
        {
            box = Box.createVerticalBox();
            panel.add(box, BorderLayout.NORTH);
            String mainMessage;
            String optionalMessage;
            if (editOnlyCommand)
            {
                mainMessage = i18n("MSG_PROGRAMEDIT_EDIT_COMMAND");
                optionalMessage = i18n("MSG_PROGRAMEDIT_EDIT_COMMAND_2");
            }
            else
            {
                mainMessage = i18n("MSG_PROGRAMEDIT_EDIT_LABEL");
                optionalMessage = i18n("MSG_PROGRAMEDIT_EDIT_LABEL_2");
            }
            String css = GuiUtil.getMessageCss();
            JLabel label =
                new JLabel("<html>" + css + "<b>"
                           + insertLineBreaks(mainMessage) + "</b><p>"
                           + insertLineBreaks(optionalMessage) + "</p>");
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(label);

            addFiller(box);
            addFiller(box);
            addFiller(box);
        }
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        panel.add(m_panelRight, BorderLayout.CENTER);
        if (! editOnlyCommand)
            m_label = createEntry("LB_PROGRAMEDIT_LABEL", 20, program.m_label);
        m_command = createFileEntry("LB_PROGRAMEDIT_COMMAND",
                                    program.m_command,
                                    "TT_PROGRAMEDIT_COMMAND",
                                    "TIT_PROGRAMEDIT_COMMAND",
                                    ! m_editOnlyLabel);
        m_workingDirectory = createEntry("LB_PROGRAMEDIT_DIR",
                                         30, program.m_workingDirectory,
                                         ! m_editOnlyLabel);
        if (! editOnlyCommand)
        {
            m_name = createEntry("LB_PROGRAMEDIT_NAME", 20, program.m_name,
                                 false);
            m_version = createEntry("LB_PROGRAMEDIT_VERSION", 20,
                                    program.m_version, false);
        }
        JOptionPane optionPane = new JOptionPane(panel,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION);
        m_dialog = optionPane.createDialog(parent, title);
        m_dialog.addWindowListener(new WindowAdapter() {
                public void windowActivated(WindowEvent e) {
                    if (m_label == null)
                        m_command.requestFocusInWindow();
                    else
                        m_label.requestFocusInWindow();
                }
            });
        if (box != null)
        {
            // Workaround for Sun Bug ID 4545951 (still in Linux JDK
            // 1.5.0_04-b05 or Mac 1.4.2_12)
            box.invalidate();
            m_dialog.pack();
        }
        boolean done = false;
        while (! done)
        {
            m_dialog.setVisible(true);
            Object value = optionPane.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return null;
            done = validate(parent, messageDialogs);
        }
        String newLabel = "";
        String newName = "";
        String newVersion = "";
        if (! editOnlyCommand)
        {
            newLabel = m_label.getText().trim();
            newName = m_name.getText().trim();
            newVersion = m_version.getText().trim();
        }
        String newCommand = m_command.getText().trim();
        String newWorkingDirectory = m_workingDirectory.getText().trim();
        Program newProgram = new Program(newLabel, newName, newVersion,
                                         newCommand, newWorkingDirectory);
        m_dialog.dispose();
        return newProgram;
    }

    public String getItemLabel(Program object)
    {
        return object.m_label;
    }

    public Program cloneItem(Program object)
    {
        return new Program(object);
    }

    private JPanel m_panelLeft;

    private JPanel m_panelRight;

    private JTextField m_label;

    private JTextField m_name;

    private JTextField m_version;

    private JTextField m_command;

    private JTextField m_workingDirectory;

    private JDialog m_dialog;

    private boolean m_editOnlyCommand;

    private boolean m_editOnlyLabel;

    private static void addFiller(JComponent component)
    {
        Box.Filler filler = GuiUtil.createFiller();
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.add(filler);
    }

    private JTextField createEntry(String labelText, int cols, String text)
    {
        return createEntry(labelText, cols, text, true);
    }

    private JTextField createEntry(String labelText, int cols, String text,
                                   boolean editable)
    {
        JComponent label = createEntryLabel(labelText);
        m_panelLeft.add(label);
        Box box = Box.createVerticalBox();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        box.add(Box.createVerticalGlue());
        box.add(panel);
        box.add(Box.createVerticalGlue());
        JTextField field = new JTextField(cols);
        field.setText(text);
        if (! editable)
            GuiUtil.setEditableFalse(field);
        panel.add(field);
        m_panelRight.add(box);
        return field;
    }

    private JComponent createEntryLabel(String text)
    {
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        JLabel label = new JLabel(i18n(text));
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        box.add(label);
        return box;
    }

    private JTextField createFileEntry(String label, String text,
                                       String browseToolTip,
                                       final String title,
                                       boolean editable)
    {
        m_panelLeft.add(createEntryLabel(label));
        Box box = Box.createVerticalBox();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        box.add(Box.createVerticalGlue());
        box.add(panel);
        box.add(Box.createVerticalGlue());
        final JTextField field = new JTextField(30);
        field.setText(text);
        panel.add(field);
        if (editable)
        {
            panel.add(GuiUtil.createSmallFiller());
            JButton button = new JButton();
            panel.add(button);
            button.setIcon(GuiUtil.getIcon("document-open-16x16",
                                           i18n("LB_BROWSE")));
            GuiUtil.setMacBevelButton(button);
            button.setToolTipText(i18n(browseToolTip));
            button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        File file =
                            FileDialogs.showOpen(m_dialog, i18n(title));
                        if (file == null)
                            return;
                        String text = file.toString();
                        if (text.indexOf(' ') >= 0)
                            text = "\"" + text + "\"";
                        field.setText(text);
                        field.setCaretPosition(text.length());
                        field.requestFocusInWindow();
                    }
                });
        }
        else
            GuiUtil.setEditableFalse(field);
        m_panelRight.add(box);
        return field;
    }

    private boolean validate(Component parent, MessageDialogs messageDialogs)
    {
        if (! m_editOnlyCommand)
        {
            if (StringUtil.isEmpty(m_label.getText()))
            {
                String mainMessage = i18n("MSG_PROGRAMEDIT_EMPTY_LABEL");
                String optionalMessage = i18n("MSG_PROGRAMEDIT_EMPTY_LABEL_2");
                messageDialogs.showError(parent, mainMessage, optionalMessage,
                                         false);
                return false;
            }
        }
        if (m_command.getText().trim().equals(""))
        {
            String mainMessage = i18n("MSG_PROGRAMEDIT_EMPTY_COMMAND");
            String optionalMessage = i18n("MSG_PROGRAMEDIT_EMPTY_COMMAND_2");
            messageDialogs.showError(parent, mainMessage, optionalMessage,
                                     false);
            return false;
        }
        String workingDirectory = m_workingDirectory.getText().trim();
        if (! workingDirectory.equals("")
            && ! new File(workingDirectory).isDirectory())
        {
            String mainMessage = i18n("MSG_PROGRAMEDIT_INVALID_DIR");
            String optionalMessage = i18n("MSG_PROGRAMEDIT_INVALID_DIR_2");
            messageDialogs.showError(parent, mainMessage, optionalMessage,
                                     false);
            return false;
        }
        return true;
    }
}
