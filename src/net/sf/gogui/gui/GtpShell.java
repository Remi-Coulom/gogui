// GtpShell.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import net.sf.gogui.gtp.GtpUtil;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.PrefUtil;

/** Dialog for displaying the GTP stream and for entering commands. */
public class GtpShell
    extends JDialog
    implements ActionListener
{
    /** Callback for events generated by GtpShell. */
    public interface Listener
    {
        void actionSendCommand(String command, boolean isCritical,
                               boolean showError);

        /** Callback if some text is selected. */
        void textSelected(String text);
    }

    public GtpShell(Frame owner, Listener listener,
                    MessageDialogs messageDialogs)
    {
        super(owner, i18n("TIT_SHELL"));
        m_messageDialogs = messageDialogs;
        m_listener = listener;
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        m_historyMin = prefs.getInt("history-min", 2000);
        m_historyMax = prefs.getInt("history-max", 3000);
        JPanel panel = new JPanel(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        m_gtpShellText = new GtpShellText(m_historyMin, m_historyMax, false);
        CaretListener caretListener = new CaretListener()
            {
                public void caretUpdate(CaretEvent event)
                {
                    if (m_listener == null)
                        return;
                    // Call the callback only if the selected text has changed.
                    // This avoids that the callback is called multiple times
                    // if the caret position changes, but the text selection
                    // was null before and after the change (see also bug
                    // #2964755)
                    String selectedText = m_gtpShellText.getSelectedText();
                    if (! ObjectUtil.equals(selectedText, m_selectedText))
                    {
                        m_listener.textSelected(selectedText);
                        m_selectedText = selectedText;
                    }
                }
            };
        m_gtpShellText.addCaretListener(caretListener);
        m_scrollPane =
            new JScrollPane(m_gtpShellText,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (Platform.isMac())
            // Default Apple L&F uses no border, but Quaqua 3.7.4 does
            m_scrollPane.setBorder(null);
        panel.add(m_scrollPane, BorderLayout.CENTER);
        panel.add(createCommandInput(), BorderLayout.SOUTH);
        setMinimumSize(new Dimension(160, 112));
        pack();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("run"))
            commandEntered();
        else if (command.equals("close"))
            setVisible(false);
    }

    /** @see net.sf.gogui.gui.GtpShellText#isLastTextNonGTP */
    public boolean isLastTextNonGTP()
    {
        return m_gtpShellText.isLastTextNonGTP();
    }

    public void receivedInvalidResponse(final String response,
                                        boolean invokeLater)
    {
        if (SwingUtilities.isEventDispatchThread())
            appendInvalidResponse(response);
        else
        {
            Runnable r = new Runnable() {
                    public void run() {
                        appendInvalidResponse(response);
                    } };
            if (invokeLater)
                SwingUtilities.invokeLater(r);
            else
                GuiUtil.invokeAndWait(r);
        }
    }

    public void receivedResponse(final boolean error, final String response,
                                 boolean invokeLater)
    {
        if (SwingUtilities.isEventDispatchThread())
            appendResponse(error, response);
        else
        {
            Runnable r = new Runnable() {
                    public void run() {
                        appendResponse(error, response);
                    } };
            if (invokeLater)
                SwingUtilities.invokeLater(r);
            else
                GuiUtil.invokeAndWait(r);
        }
    }

    public void receivedStdErr(final String s, boolean invokeLater,
                               final boolean isLiveGfx,
                               final boolean isWarning)
    {
        if (SwingUtilities.isEventDispatchThread())
            appendLog(s, isLiveGfx, isWarning);
        else
        {
            Runnable r = new Runnable() {
                    public void run() {
                        appendLog(s, isLiveGfx, isWarning);
                    } };
            if (invokeLater)
                SwingUtilities.invokeLater(r);
            else
                GuiUtil.invokeAndWait(r);
        }
    }

    public void saveLog(JFrame parent)
    {
        save(parent, m_gtpShellText.getLog(),
             m_gtpShellText.getLinesTruncated());
    }

    public void saveCommands(JFrame parent)
    {
        save(parent, m_commands.toString(), m_linesTruncated);
    }

    public void saveHistory()
    {
        int maxHistory = 100;
        int max = m_history.size();
        if (max > maxHistory)
            max = maxHistory;
        ArrayList<String> list = new ArrayList<String>(max);
        for (int i = m_history.size() - max; i < m_history.size(); ++i)
            list.add(m_history.get(i));
        PrefUtil.putList("net/sf/gogui/gui/gtpshell/recentcommands", list);
    }

    public void setCommandInProgess(boolean commandInProgess)
    {
        m_commandInProgress = commandInProgess;
    }

    public void setCommandCompletion(boolean commandCompletion)
    {
        m_disableCompletions = ! commandCompletion;
    }

    public void setTimeStamp(boolean enable)
    {
        m_gtpShellText.setTimeStamp(enable);
    }

    public void sentCommand(final String command)
    {
        if (SwingUtilities.isEventDispatchThread())
            appendSentCommand(command);
        else
            GuiUtil.invokeAndWait(new Runnable() {
                    public void run() {
                        appendSentCommand(command);
                    } });
    }

    public void setInitialCompletions(ArrayList<String> completions)
    {
        for (int i = completions.size() - 1; i >= 0; --i)
        {
            String command = completions.get(i);
            if (! GtpUtil.isStateChangingCommand(command))
                appendToHistory(command);
        }
        ArrayList<String> list =
            PrefUtil.getList("net/sf/gogui/gui/gtpshell/recentcommands");
        for (int i = 0; i < list.size(); ++i)
            appendToHistory(list.get(i));
        addAllCompletions(m_history);
    }

    public void setProgramCommand(String command)
    {
        m_programCommand = command;
    }

    public void setProgramName(String name)
    {
        m_programName = name;
    }

    public void setProgramVersion(String version)
    {
        m_programVersion = version;
    }

    private boolean m_disableCompletions;

    private boolean m_commandInProgress;

    private final int m_historyMax;

    private final int m_historyMin;

    private int m_linesTruncated;

    private int m_numberCommands;

    private final Listener m_listener;

    private ComboBoxEditor m_editor;

    private JButton m_runButton;

    private JTextField m_textField;

    /** @note JComboBox is a generic type since Java 7. We use a raw type
        and suppress unchecked warnings where needed to be compatible with
        earlier Java versions. */
    private JComboBox m_comboBox;

    private final JScrollPane m_scrollPane;

    private final GtpShellText m_gtpShellText;

    private final StringBuilder m_commands = new StringBuilder(4096);

    private final ArrayList<String> m_history = new ArrayList<String>(128);

    private String m_selectedText;

    private String m_programCommand = "unknown";

    private String m_programName = "unknown";

    private String m_programVersion = "unknown";

    private final MessageDialogs m_messageDialogs;

    // See comment at m_comboBox
    @SuppressWarnings("unchecked")
    private void addAllCompletions(ArrayList<String> completions)
    {
        // On Windows JDK 1.4 changing the popup automatically
        // selects all text in the text field, so we remember and
        // restore the state.
        String oldText = m_textField.getText();
        int oldCaretPosition = m_textField.getCaretPosition();
        if (completions.size() > m_comboBox.getItemCount())
            m_comboBox.hidePopup();
        m_comboBox.removeAllItems();
        for (int i = completions.size() - 1; i >= 0; --i)
            m_comboBox.addItem(GuiUtil.createComboBoxItem(completions.get(i)));
        m_comboBox.setSelectedIndex(-1);
        m_textField.setText(oldText);
        m_textField.setCaretPosition(oldCaretPosition);
    }

    private void appendInvalidResponse(String response)
    {
        assert SwingUtilities.isEventDispatchThread();
        m_gtpShellText.appendInvalidResponse(response);
    }

    private void appendLog(String line, boolean isLiveGfx, boolean isWarning)
    {
        assert SwingUtilities.isEventDispatchThread();
        m_gtpShellText.appendLog(line, isLiveGfx, isWarning);
    }

    private void appendResponse(boolean error, String response)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (error)
            m_gtpShellText.appendError(response);
        else
            m_gtpShellText.appendInput(response);
    }

    private void appendSentCommand(String command)
    {
        assert SwingUtilities.isEventDispatchThread();
        m_commands.append(command);
        m_commands.append('\n');
        ++m_numberCommands;
        if (m_numberCommands > m_historyMax)
        {
            int truncateLines = m_numberCommands - m_historyMin;
            String s = m_commands.toString();
            int index = GtpShellText.findTruncateIndex(s, truncateLines);
            assert index != -1;
            m_commands.delete(0, index);
            m_linesTruncated += truncateLines;
            m_numberCommands = 0;
        }
        m_gtpShellText.appendOutput(command + "\n");
    }

    private void appendToHistory(String command)
    {
        command = command.trim();
        int i = m_history.indexOf(command);
        if (i >= 0)
            m_history.remove(i);
        m_history.add(command);
    }

    private void commandEntered()
    {
        assert SwingUtilities.isEventDispatchThread();
        String command = m_textField.getText().trim();
        if (command.trim().equals(""))
            return;
        if (command.startsWith("#"))
        {
            m_gtpShellText.appendComment(command + "\n");
        }
        else
        {
            if (GtpUtil.isStateChangingCommand(command))
            {
                showError(i18n("MSG_SHELL_BOARDCHANGING"),
                          i18n("MSG_SHELL_BOARDCHANGING_2"), false);
                return;
            }
            if (m_commandInProgress)
            {
                showError(i18n("MSG_SHELL_CMD_IN_PROGRESS"),
                          i18n("MSG_SHELL_CMD_IN_PROGRESS_2"), false);
                return;
            }
            m_listener.actionSendCommand(command, false, false);
        }
        appendToHistory(command);
        m_gtpShellText.setPositionToEnd();
        m_comboBox.hidePopup();
        addAllCompletions(m_history);
        m_editor.setItem(null);
    }

    private JComponent createCommandInput()
    {
        Box box = Box.createVerticalBox();
        //JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel = new JPanel(new BorderLayout());
        box.add(GuiUtil.createSmallFiller());
        box.add(Box.createVerticalGlue());
        box.add(panel);
        box.add(Box.createVerticalGlue());
        m_comboBox = new JComboBox();
        m_editor = m_comboBox.getEditor();
        m_textField = (JTextField)m_editor.getEditorComponent();
        m_textField.setFocusTraversalKeysEnabled(false);
        KeyAdapter keyAdapter = new KeyAdapter()
            {
                public void keyReleased(KeyEvent e)
                {
                    int c = e.getKeyCode();
                    int mod = e.getModifiersEx();
                    if (c == KeyEvent.VK_ESCAPE)
                        return;
                    else if (c == KeyEvent.VK_TAB)
                    {
                        findBestCompletion();
                        popupCompletions();
                    }
                    else if (c == KeyEvent.VK_PAGE_UP
                             && mod == KeyEvent.SHIFT_DOWN_MASK)
                        scrollPage(true);
                    else if (c == KeyEvent.VK_PAGE_DOWN
                             && mod == KeyEvent.SHIFT_DOWN_MASK)
                        scrollPage(false);
                    else if (c == KeyEvent.VK_ENTER
                             && ! m_comboBox.isPopupVisible())
                        commandEntered();
                    else if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
                        popupCompletions();
                }
            };
        m_textField.addKeyListener(keyAdapter);
        m_comboBox.setEditable(true);
        m_comboBox.setFont(m_gtpShellText.getFont());
        m_comboBox.addActionListener(this);
        addWindowListener(new WindowAdapter() {
                public void windowActivated(WindowEvent e) {
                    m_comboBox.requestFocusInWindow();
                    m_textField.requestFocusInWindow();
                }
            });
        panel.add(m_comboBox, BorderLayout.CENTER);
        m_runButton = new JButton();
        m_runButton.setIcon(GuiUtil.getIcon("gogui-key_enter", i18n("LB_RUN")));
        m_runButton.setActionCommand("run");
        m_runButton.setFocusable(false);
        m_runButton.setToolTipText(i18n("TT_SHELL_RUN"));
        m_runButton.addActionListener(this);
        GuiUtil.setMacBevelButton(m_runButton);
        JPanel buttonPanel =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(buttonPanel, BorderLayout.EAST);
        buttonPanel.add(GuiUtil.createSmallFiller());
        buttonPanel.add(m_runButton);
        // add some empty space so that status bar does not overlap the
        // window resize widget on Mac OS X
        if (Platform.isMac())
        {
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            buttonPanel.add(filler);
        }
        return box;
    }

    private void findBestCompletion()
    {
        String text = m_textField.getText().trim();
        if (text.isEmpty())
            return;
        String bestCompletion = null;
        for (int i = 0; i < m_history.size(); ++i)
        {
            String completion = m_history.get(i);
            if (completion.startsWith(text))
            {
                if (bestCompletion == null)
                {
                    bestCompletion = completion;
                    continue;
                }
                int j = text.length();
                while (true)
                {
                    if (j >= bestCompletion.length())
                    {
                        break;
                    }
                    if (j >= completion.length())
                        break;
                    if (bestCompletion.charAt(j) != completion.charAt(j))
                        break;
                    ++j;
                }
                bestCompletion = completion.substring(0, j);
            }
        }
        if (bestCompletion != null)
            m_textField.setText(bestCompletion);
    }

    private void popupCompletions()
    {
        String text = m_textField.getText();
        text = text.replaceAll("^ *", "");
        ArrayList<String> completions = new ArrayList<>(128);
        for (String c : m_history) {
            if (c.startsWith(text))
                completions.add(c);
        }
        addAllCompletions(completions);
        if (m_disableCompletions)
            return;
        int size = completions.size();
        if (!text.isEmpty()
            && (size > 1 || (size == 1 && ! text.equals(completions.get(0)))))
            m_comboBox.showPopup();
        else
            m_comboBox.hidePopup();
    }

    private void save(JFrame parent, String s, int linesTruncated)
    {
        File file = FileDialogs.showSave(parent, null, m_messageDialogs);
        if (file == null)
            return;
        try
        {
            PrintStream out = new PrintStream(file);
            out.println("# Name: " + m_programName);
            out.println("# Version: " + m_programVersion);
            out.println("# Command: " + m_programCommand);
            out.println("# Lines truncated: " + linesTruncated);
            out.print(s);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            m_messageDialogs.showError(parent, i18n("MSG_SHELL_SAVE_FAILURE"),
                                       "");
        }
    }

    private void showError(String mainMessage, String optionalMessage,
                           boolean isCritical)
    {
        m_messageDialogs.showError(this, mainMessage, optionalMessage,
                                   isCritical);
    }

    private void scrollPage(boolean up)
    {
        JViewport viewport = m_scrollPane.getViewport();
        Point position = viewport.getViewPosition();
        int delta = m_scrollPane.getSize().height
            - m_gtpShellText.getFont().getSize();
        if (up)
        {
            position.y -= delta;
            if (position.y < 0)
                position.y = 0;
        }
        else
        {
            position.y += delta;
            int max = viewport.getViewSize().height
                - m_scrollPane.getSize().height;
            if (position.y > max)
                position.y = max;
        }
        viewport.setViewPosition(position);
    }
}
