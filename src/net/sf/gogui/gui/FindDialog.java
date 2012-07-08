// FindDialog.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.util.PrefUtil;
import net.sf.gogui.util.StringUtil;

/** Dialog for entering a search pattern. */
public class FindDialog
    extends JDialog
    implements ActionListener
{
    public FindDialog(Frame owner, String initialValue)
    {
        super(owner, i18n("TIT_FIND"), true);
        m_initialValue = initialValue;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.add(createPanel(), BorderLayout.CENTER);
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        pack();
    }

    // See comment at m_comboBox
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("cancel"))
            dispose();
        else if (command.equals("comboBoxEdited") || command.equals("find"))
        {
            m_pattern = m_comboBox.getSelectedItem().toString();
            m_comboBox.insertItemAt(m_pattern, 0);
            putHistory();
            dispose();
        }
    }

    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE
            && ! m_comboBox.isPopupVisible())
            dispose();
    }

    public static Pattern run(Frame owner, String initialValue,
                              MessageDialogs messageDialogs)
    {
        while (true)
        {
            FindDialog dialog = new FindDialog(owner, initialValue);
            dialog.setLocationByPlatform(true);
            dialog.setVisible(true);
            String regex = dialog.m_pattern;
            if (StringUtil.isEmpty(regex))
                return null;
            int flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
            try
            {
                return Pattern.compile(regex, flags);
            }
            catch (PatternSyntaxException e)
            {
                String mainMessage = i18n("MSG_FINDDIALOG_INVALID_PATTERN");
                String optionalMessage =
                    format(i18n("MSG_FINDDIALOG_INVALID_PATTERN_2"),
                           e.getDescription());
                messageDialogs.showError(owner, mainMessage, optionalMessage,
                                         false);
                initialValue = regex;
            }
        }
    }

    /** @note JComboBox is a generic type since Java 7. We use a raw type
        and suppress unchecked warnings where needed to be compatible with
        earlier Java versions. */
    private JComboBox m_comboBox;

    private JTextField m_textField;

    private final String m_initialValue;

    private String m_pattern;

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtil.PAD, 0));
        innerPanel.setBorder(GuiUtil.createEmptyBorder());
        JButton findButton = new JButton(i18n("LB_FIND"));
        findButton.setActionCommand("find");
        findButton.addActionListener(this);
        findButton.setMnemonic(KeyEvent.VK_F);
        getRootPane().setDefaultButton(findButton);
        innerPanel.add(findButton);
        JButton cancelButton = new JButton(i18n("LB_CANCEL"));
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic(KeyEvent.VK_C);
        innerPanel.add(cancelButton);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private JPanel createPanel()
    {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(GuiUtil.createEmptyBorder());
        JLabel label = new JLabel(i18n("LB_FIND_SEARCH_PATTERN"));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        panel.add(createInputPanel());
        return panel;
    }

    // See comment at m_comboBox
    @SuppressWarnings("unchecked")
    private JPanel createInputPanel()
    {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel(new BorderLayout());
        m_comboBox = new JComboBox(getHistory().toArray());
        StringBuilder prototype = new StringBuilder(70);
        for (int i = 0; i < 40; ++i)
            prototype.append('-');
        m_comboBox.setPrototypeDisplayValue(prototype.toString());
        m_comboBox.setEditable(true);
        ComboBoxEditor editor = m_comboBox.getEditor();
        m_comboBox.addActionListener(this);
        m_textField = (JTextField)editor.getEditorComponent();
        m_textField.selectAll();
        KeyListener keyListener = new KeyAdapter()
            {
                public void keyPressed(KeyEvent e)
                {
                    int c = e.getKeyCode();
                    if (c == KeyEvent.VK_ESCAPE
                        && ! m_comboBox.isPopupVisible())
                        dispose();
                }
            };
        m_textField.addKeyListener(keyListener);
        GuiUtil.setMonospacedFont(m_comboBox);
        innerPanel.add(m_comboBox, BorderLayout.CENTER);
        outerPanel.add(innerPanel, BorderLayout.NORTH);
        return outerPanel;
    }

    private void putHistory()
    {
        ArrayList<String> history = new ArrayList<String>(32);
        int maxHistory = 20;
        int itemCount = m_comboBox.getItemCount();
        int n = itemCount;
        if (n > maxHistory)
            n = maxHistory;
        for (int i = 0; i < n; ++i)
        {
            String element = m_comboBox.getItemAt(i).toString().trim();
            if (! history.contains(element))
                history.add(element);
        }
        PrefUtil.putList("net/sf/gogui/gui/finddialog", history);
    }

    private ArrayList<String> getHistory()
    {
        ArrayList<String> result
            = PrefUtil.getList("net/sf/gogui/gui/finddialog");
        if (m_initialValue != null)
            result.add(0, m_initialValue);
        return result;
    }
}
