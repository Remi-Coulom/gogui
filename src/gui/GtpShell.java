//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import gtp.Gtp;
import gtp.GtpError;
import utils.GuiUtils;
import utils.Platform;
import utils.Preferences;

//----------------------------------------------------------------------------

class GtpShellText
    extends JTextPane
    implements Scrollable
{
    public GtpShellText(int historyMin, int historyMax)
    {
        m_historyMin = historyMin;
        m_historyMax = historyMax;
        m_highlight = true;
        int fontSize = GuiUtils.getDefaultMonoFontSize();
        Font font = new Font("Monospaced", Font.PLAIN, fontSize);
        setFont(font);
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setLineSpacing(def, 0f);
        Style error = addStyle("error", def);
        StyleConstants.setForeground(error, Color.red);
        Style output = addStyle("output", def);
        StyleConstants.setBold(output, true);
        Style log = addStyle("log", def);
        StyleConstants.setForeground(log, new Color(0.5f, 0.5f, 0.5f));
        Style invalid = addStyle("invalid", def);
        StyleConstants.setForeground(invalid, Color.white);
        StyleConstants.setBackground(invalid, Color.red);
        setEditable(false);
    }

    public void appendComment(String text)
    {
        appendText(text, "log");
    }

    public void appendError(String text)
    {
        appendText(text, "error");
    }

    public void appendInput(String text)
    {
        appendText(text, null);
    }

    public void appendInvalidResponse(String text)
    {
        appendText(text, "invalid");
    }

    public void appendLog(String text)
    {
        appendText(text, "log");
    }

    public void appendOutput(String text)
    {
        appendText(text, "output");
    }

    public static int findTruncateIndex(String text, int truncateLines)
    {
        int indexNewLine = 0;
        int lines = 0;
        while ((indexNewLine = text.indexOf('\n', indexNewLine)) != -1)
        {
            ++indexNewLine;
            ++lines;
            if (lines == truncateLines)
                return indexNewLine;
        }
        return -1;
    }

    public boolean getHighlight()
    {
        return m_highlight;
    }

    public int getLinesTruncated()
    {
        return m_truncated;
    }

    public String getLog()
    {
        StyledDocument doc = getStyledDocument();
        try
        {
            return doc.getText(0, doc.getLength());
        }
        catch (BadLocationException e)
        {
            assert(false);
            return "";
        }
    }

    public void setHighlight(boolean highlight)
    {
        m_highlight = highlight;
    }

    public void setPositionToEnd()
    {
        setCaretPosition(getStyledDocument().getLength());
    }

    private boolean m_highlight;

    private int m_historyMin;

    private int m_historyMax;

    private int m_lines;

    private int m_truncated;

    private void appendText(String text, String style)
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (text.equals(""))
            return;
        int indexNewLine = 0;
        while ((indexNewLine = text.indexOf('\n', indexNewLine)) != -1)
        {
            ++m_lines;
            ++indexNewLine;
        }
        StyledDocument doc = getStyledDocument();
        Style s = null;
        if (style != null && m_highlight)
            s = getStyle(style);
        try
        {
            doc.insertString(doc.getLength(), text, s);
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
        if (m_lines > m_historyMax)
            truncateHistory();
    }

    private void truncateHistory()
    {
        int truncateLines = m_lines - m_historyMin;
        StyledDocument doc = getStyledDocument();
        try
        {
            String text = doc.getText(0, doc.getLength());
            int truncateIndex = findTruncateIndex(text, truncateLines);
            assert(truncateIndex != -1);
            doc.remove(0, truncateIndex);
            m_lines -= truncateLines;
            m_truncated += truncateLines;
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}

//----------------------------------------------------------------------------

public class GtpShell
    extends JFrame
    implements ActionListener, Gtp.IOCallback
{
    public interface Callback
    {
        public void cbAnalyze();

        public void cbShowGameTree();

        public boolean sendGtpCommand(String command, boolean sync)
            throws GtpError;

        public void toTop();
    }

    public GtpShell(String appName, Callback callback, Preferences prefs)
    {
        super("GTP - " + appName);
        GuiUtils.setGoIcon(this);
        m_callback = callback;
        m_prefs = prefs;
        setPrefsDefaults(prefs);
        m_historyMin = prefs.getInt("gtpshell-history-min");
        m_historyMax = prefs.getInt("gtpshell-history-max");
        m_disableCompletions = prefs.getBool("gtpshell-disable-completions");
        m_autoNumber = prefs.getBool("gtpshell-autonumber");
        boolean highlight = prefs.getBool("gtpshell-highlight");
        createMenuBar(highlight);
        Container contentPane = getContentPane();
        m_gtpShellText = new GtpShellText(m_historyMin, m_historyMax);
        m_gtpShellText.setHighlight(highlight);
        m_scrollPane =
            new JScrollPane(m_gtpShellText,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        m_fontSize = m_gtpShellText.getFont().getSize();
        m_finalSize = new Dimension(m_fontSize * 40, m_fontSize * 30);
        contentPane.add(m_scrollPane, BorderLayout.CENTER);
        contentPane.add(createCommandInput(), BorderLayout.SOUTH);
        pack();
    }
    
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("analyze"))
            m_callback.cbAnalyze();
        else if (command.equals("auto-number"))
            autoNumber();
        else if (command.equals("comboBoxEdited"))
            comboBoxEdited();
        else if (command.equals("command-completion"))
            commandCompletion();
        else if (command.equals("show-gametree"))
            m_callback.cbShowGameTree();
        else if (command.equals("gogui"))
            m_callback.toTop();
        else if (command.equals("highlight"))
            highlight();
        else if (command.equals("save-log"))
            saveLog();
        else if (command.equals("save-commands"))
            saveCommands();
        else if (command.equals("send-file"))
            sendFile();
        else if (command.equals("close"))
            setVisible(false);
    }
    
    public boolean getAutoNumber()
    {
        return m_autoNumber;
    }

    public void loadHistory()
    {
        File file = getHistoryFile();
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(file));
            try
            {
                String line = in.readLine();
                while (line != null)
                {
                    appendToHistory(line);
                    line = in.readLine();
                }
            }
            finally
            {
                in.close();
            }
        }
        catch (IOException e)
        {
        }
    }

    public void receivedInvalidResponse(String response)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            appendInvalidResponse(response);
            return;
        }
        Runnable r = new UpdateInvalidResponse(this, response);
        invokeAndWait(r);
    }
    
    public void receivedResponse(boolean error, String response)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            appendResponse(error, response);
            return;
        }
        Runnable r = new UpdateResponse(this, error, response);
        invokeAndWait(r);
    }
    
    public void receivedStdErr(String s)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            appendLog(s);
            return;
        }
        Runnable r = new UpdateStdErr(this, s);
        invokeAndWait(r);
    }

    public void saveHistory()
    {
        File file = getHistoryFile();
        try
        {
            PrintWriter out = new PrintWriter(new FileOutputStream(file));
            int maxHistory = 100;
            int n = m_history.size();
            if (n > maxHistory)
                n = maxHistory;
            for (int i = m_history.size() - n; i < m_history.size(); ++i)
                out.println(m_history.get(i));
            out.close();
        }
        catch (FileNotFoundException e)
        {
        }

    }

    public void setCommandInProgess(boolean commandInProgess)
    {
        m_comboBox.setEnabled(! commandInProgess);
        m_sendGtpFile.setEnabled(! commandInProgess);
        if (! commandInProgess)
        {
            m_comboBox.requestFocusInWindow();
            m_textField.requestFocusInWindow();
        }
    }

    public void toTop()
    {
        setState(Frame.NORMAL);
        setVisible(true);
        toFront();
    }

    /** Send Gtp command to callback.
        If owner != null, send synchronously and display error dialog on
        owner, otherwise send asynchronously.
    */
    public boolean sendCommand(String command, Component owner,
                               boolean askContinue)
    {
        assert(SwingUtilities.isEventDispatchThread());
        String c = command.trim();
        if (c.equals(""))
            return true;
        if (c.startsWith("#"))
        {
            m_gtpShellText.appendComment(command + "\n");
        }
        else
        {
            if (m_showModifyWarning
                && (c.startsWith("boardsize ")
                    || c.startsWith("black ")
                    || c.startsWith("clear_board ")
                    || c.startsWith("genmove ")
                    || c.startsWith("genmove_black ")
                    || c.startsWith("genmove_white ")
                    || c.startsWith("loadsgf ")
                    || c.startsWith("play ")
                    || c.startsWith("white ")
                    || c.startsWith("quit")))
            {

                Object[] options = { "Ok", "Cancel" };
                String message = 
                    "This command will modify the board state and\n" +
                    "will cause the graphical board to be out of sync.\n" +
                    "You must start a new game before using\n" +
                    "the graphical board again.";
                int n =
                    JOptionPane.showOptionDialog(this, message, "Warning",
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 JOptionPane.WARNING_MESSAGE,
                                                 null, options, options[1]);
                if (n != 0)
                    return true;
                message = 
                    "Would you like to disable the warnings about\n" +
                    "commands modifying the board state?";
                m_showModifyWarning =
                    ! SimpleDialogs.showQuestion(this, message);
            }
            try
            {
                m_callback.sendGtpCommand(command, owner != null);
            }
            catch (GtpError e)
            {
                SimpleDialogs.showError(owner, e.getMessage());
                if (askContinue)
                    return ! SimpleDialogs.showQuestion(owner, "Abort?");
            }
        }
        return true;
    }

    public void sentCommand(String command)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            appendSentCommand(command);
            return;
        }
        Runnable r = new UpdateCommand(this, command);
        invokeAndWait(r);
    }
    
    public void setAppName(String appName)
    {
        setTitle("GTP - " + appName);
    }

    public void sendGtp(Reader reader)
    {
        java.io.BufferedReader in;
        in = new BufferedReader(reader);
        try
        {
            while (true)
            {
                try
                {
                    String line = in.readLine();
                    if (line == null)
                    {
                        in.close();
                        break;
                    }
                    if (! sendCommand(line, this, true))
                        break;
                }
                catch (IOException e)
                {
                    SimpleDialogs.showError(this, "Sending commands aborted",
                                            e);
                    break;
                }
            }
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public void sendGtpFile(File file)
    {
        try
        {
            sendGtp(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            SimpleDialogs.showError(this, "Could not send commands.", e);
        }
    }

    public void setFinalSize(int x, int y, int width, int height)
    {
        if (m_isFinalSizeSet)
            setBounds(x, y, width, height);
        else
        {
            m_finalSize = new Dimension(width, height);
            m_finalLocation = new java.awt.Point(x, y);
        }
    }

    public void setInitialCompletions(Vector completions)
    {
        for (int i = completions.size() - 1; i >= 0; --i)
            appendToHistory(completions.get(i).toString());
        loadHistory();
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

        
    private static class UpdateCommand implements Runnable
    {
        public UpdateCommand(GtpShell gtpShell, String text)
        {
            m_gtpShell = gtpShell;
            m_text = text;
        }

        public void run()
        {
            m_gtpShell.appendSentCommand(m_text);
        }

        private String m_text;

        private GtpShell m_gtpShell;
    }

    private static class UpdateInvalidResponse implements Runnable
    {
        public UpdateInvalidResponse(GtpShell gtpShell, String text)
        {
            m_gtpShell = gtpShell;
            m_text = text;
        }

        public void run()
        {
            m_gtpShell.appendInvalidResponse(m_text);
        }

        private String m_text;

        private GtpShell m_gtpShell;
    }

    private static class UpdateResponse implements Runnable
    {
        public UpdateResponse(GtpShell gtpShell, boolean error, String text)
        {
            m_gtpShell = gtpShell;
            m_error = error;
            m_text = text;
        }

        public void run()
        {
            m_gtpShell.appendResponse(m_error, m_text);
        }

        private boolean m_error;

        private String m_text;

        private GtpShell m_gtpShell;
    }

    private static class UpdateStdErr implements Runnable
    {
        public UpdateStdErr(GtpShell gtpShell, String text)
        {
            m_gtpShell = gtpShell;
            m_text = text;
        }

        public void run()
        {
            assert(SwingUtilities.isEventDispatchThread());
            m_gtpShell.appendLog(m_text);
            m_gtpShell.setFinalSize();
        }

        private String m_text;

        private GtpShell m_gtpShell;
    }

    private boolean m_autoNumber;

    private boolean m_disableCompletions;

    private boolean m_isFinalSizeSet;

    private boolean m_showModifyWarning = true;

    private int m_fontSize;

    private int m_historyMax;

    private int m_historyMin;

    private int m_linesTruncated;

    private int m_numberCommands;

    private static final int m_shortcutKeyMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private Callback m_callback;

    private ComboBoxEditor m_editor;

    private Dimension m_finalSize;

    private JTextField m_textField;

    private JComboBox m_comboBox;

    private JCheckBoxMenuItem m_itemAutoNumber;

    private JCheckBoxMenuItem m_itemCommandCompletion;

    private JCheckBoxMenuItem m_itemHighlight;

    private JMenuItem m_sendGtpFile;

    private JScrollPane m_scrollPane;

    private GtpShellText m_gtpShellText;

    private java.awt.Point m_finalLocation;

    private StringBuffer m_commands = new StringBuffer(4096);

    private Vector m_history = new Vector(128, 128);

    private String m_programCommand = "unknown";

    private String m_programName = "unknown";

    private String m_programVersion = "unknown";

    private Preferences m_prefs;

    private void addAllCompletions(Vector completions)
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
        {
            Object object = wrapperObject((String)completions.get(i));
            m_comboBox.addItem(object);
        }
        m_comboBox.setSelectedIndex(-1);
        m_textField.setText(oldText);
        m_textField.setCaretPosition(oldCaretPosition);
    }

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, int mnemonic,
                                  String command)
    {
        item.addActionListener(this);
        item.setActionCommand(command);
        item.setMnemonic(mnemonic);
        menu.add(item);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addMenuItem(menu, item, mnemonic, command);        
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  int accel, int modifier, String command)
    {
        JMenuItem item = new JMenuItem(label);
        KeyStroke k = KeyStroke.getKeyStroke(accel, modifier); 
        item.setAccelerator(k);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private void appendInvalidResponse(String response)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_gtpShellText.appendInvalidResponse(response);
    }
    
    private void appendLog(String line)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_gtpShellText.appendLog(line);
    }
    
    private void appendResponse(boolean error, String response)
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (error)
            m_gtpShellText.appendError(response);
        else
            m_gtpShellText.appendInput(response);
        setFinalSize();
    }
    
    private void appendSentCommand(String command)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_commands.append(command);
        m_commands.append("\n");
        ++m_numberCommands;
        if (m_numberCommands > m_historyMax)
        {
            int truncateLines = m_numberCommands - m_historyMin;
            String s = m_commands.toString();
            int index = GtpShellText.findTruncateIndex(s, truncateLines);
            assert(index != -1);
            m_commands.delete(0, index);
            m_linesTruncated += truncateLines;
            m_numberCommands = 0;
        }
        m_gtpShellText.appendOutput(command + "\n");
        setFinalSize();
    }
    
    private void appendToHistory(String command)
    {
        command = command.trim();
        int i = m_history.indexOf(command);
        if (i >= 0)
            m_history.remove(i);
        m_history.add(command);
    }

    private void autoNumber()
    {
        m_autoNumber = m_itemAutoNumber.isSelected();        
        m_prefs.setBool("gtpshell-autonumber", m_autoNumber);
        SimpleDialogs.showInfo(this,
                               "Setting will take effect on next start");
    }

    private void comboBoxEdited()
    {
        m_gtpShellText.setPositionToEnd();
        String command = m_comboBox.getSelectedItem().toString();        
        if (command.trim().equals(""))
            return;
        sendCommand(command, null, false);
        appendToHistory(command);
        m_comboBox.hidePopup();
        addAllCompletions(m_history);
        m_editor.setItem(null);
        m_comboBox.requestFocusInWindow();
        m_textField.requestFocusInWindow();
    }

    private void commandCompletion()
    {
        m_disableCompletions = ! m_itemCommandCompletion.isSelected();
        m_prefs.setBool("gtpshell-disable-completions", m_disableCompletions);
    }

    private JPanel createCommandInput()
    {
        JPanel panel = new JPanel(new BorderLayout());
        m_comboBox = new JComboBox();
        m_editor = m_comboBox.getEditor();
        m_textField = (JTextField)m_editor.getEditorComponent();
        m_textField.setFocusTraversalKeysEnabled(false);
        KeyAdapter keyAdapter = new KeyAdapter()
            {
                public void keyReleased(KeyEvent e) 
                {
                    int c = e.getKeyCode();        
                    int mod = e.getModifiers();
                    if (c == KeyEvent.VK_ESCAPE)
                        return;
                    else if (c == KeyEvent.VK_TAB)
                    {
                        findBestCompletion();
                        popupCompletions();
                    }
                    else if (c == KeyEvent.VK_PAGE_UP
                             && mod == ActionEvent.SHIFT_MASK)
                        scrollPage(true);
                    else if (c == KeyEvent.VK_PAGE_DOWN
                             && mod == ActionEvent.SHIFT_MASK)
                        scrollPage(false);
                    else if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
                        popupCompletions();
                }
            };
        m_textField.addKeyListener(keyAdapter);
        m_comboBox.setEditable(true);
        m_comboBox.setFont(m_gtpShellText.getFont());
        m_comboBox.addActionListener(this);
        // Necessary for Mac Java 1.4.2, otherwise combobox will not have
        // focus after window is re-activated
        addWindowListener(new WindowAdapter()
            {
                public void windowActivated(WindowEvent e)
                {
                    m_comboBox.requestFocusInWindow();
                    m_textField.requestFocusInWindow();
                }
            });
        panel.add(m_comboBox);
        // Workaround for Java 1.4.1 on Mac OS X add some empty space
        // so that combobox does not overlap the window resize widget
        if (Platform.isMac())
        {
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            panel.add(filler, BorderLayout.EAST);
        }
        return panel;
    }

    private void createMenuBar(boolean highlight)
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createMenuWindows());
        menuBar.add(createMenuFile());
        menuBar.add(createMenuSettings(highlight));
        setJMenuBar(menuBar);
    }

    private JMenu createMenu(String name, int mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private JMenu createMenuFile()
    {
        JMenu menu = createMenu("File", KeyEvent.VK_F);
        addMenuItem(menu, "Save...", KeyEvent.VK_S, KeyEvent.VK_S,
                    m_shortcutKeyMask, "save-log");
        addMenuItem(menu, "Save Commands...", KeyEvent.VK_M, "save-commands");
        menu.addSeparator();
        m_sendGtpFile =
            addMenuItem(menu, "Send GTP File...", KeyEvent.VK_G, "send-file");
        return menu;
    }

    private JMenu createMenuSettings(boolean highlight)
    {
        JMenu menu = createMenu("Settings", KeyEvent.VK_S);
        m_itemHighlight = new JCheckBoxMenuItem("Highlight");
        m_itemHighlight.setSelected(highlight);
        addMenuItem(menu, m_itemHighlight, KeyEvent.VK_H, "highlight");
        m_itemCommandCompletion = new JCheckBoxMenuItem("Popup Completions");
        m_itemCommandCompletion.setSelected(! m_disableCompletions);
        addMenuItem(menu, m_itemCommandCompletion, KeyEvent.VK_C,
                    "command-completion");
        m_itemAutoNumber = new JCheckBoxMenuItem("Auto Number");
        m_itemAutoNumber.setSelected(m_autoNumber);
        addMenuItem(menu, m_itemAutoNumber, KeyEvent.VK_A,
                    "auto-number");
        return menu;
    }

    private JMenu createMenuWindows()
    {
        int shortcutKeyMask = 0;
        if (Platform.isMac())
            shortcutKeyMask = m_shortcutKeyMask;
        JMenu menu = createMenu("Window", KeyEvent.VK_W);
        addMenuItem(menu, "Board", KeyEvent.VK_B, KeyEvent.VK_F6,
                    shortcutKeyMask, "gogui");
        addMenuItem(menu, "Game Tree", KeyEvent.VK_T, KeyEvent.VK_F7,
                    shortcutKeyMask, "show-gametree");
        addMenuItem(menu, "Analyze", KeyEvent.VK_A, KeyEvent.VK_F8,
                    shortcutKeyMask, "analyze");
        menu.addSeparator();
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    m_shortcutKeyMask, "close");
        return menu;
    }

    private void findBestCompletion()
    {
        String text = m_textField.getText().trim();
        if (text.equals(""))
            return;
        String bestCompletion = null;
        for (int i = 0; i < m_history.size(); ++i)
        {
            String completion = (String)m_history.get(i);
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

    private File getHistoryFile()
    {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".gogui");
        if (! dir.exists())
            dir.mkdir();
        return new File(dir, "gtpshell-history");
    }

    private void highlight()
    {
        boolean highlight = m_itemHighlight.isSelected();
        m_gtpShellText.setHighlight(highlight);
        m_prefs.setBool("gtpshell-highlight", highlight);
    }

    private void invokeAndWait(Runnable runnable)
    {
        try
        {
            SwingUtilities.invokeAndWait(runnable);
        }
        catch (InterruptedException e)
        {
            System.err.println("Thread interrupted");
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            System.err.println("InvocationTargetException");
        }
    }

    private void popupCompletions()
    {
        String text = m_textField.getText();
        text = text.replaceAll("^ *", "");
        Vector completions = new Vector(128, 128);
        for (int i = 0; i < m_history.size(); ++i)
        {
            String c = (String)m_history.get(i);
            if (c.startsWith(text))
                completions.add(c);
        }
        addAllCompletions(completions);
        if (m_disableCompletions)
            return;
        int size = completions.size();
        if (text.length() > 0)
            if (size > 1 || (size == 1 && ! text.equals(completions.get(0))))
                m_comboBox.showPopup();
    }

    private File queryFile()
    {
        String dir = System.getProperties().getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setMultiSelectionEnabled(false);
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile();
        return null;
    }

    private void save(String s, int linesTruncated)
    {
        File file = queryFile();
        if (file == null)
            return;
        try
        {
            PrintStream out = new PrintStream(new FileOutputStream(file));
            out.println("# Name: " + m_programName);
            out.println("# Version: " + m_programVersion);
            out.println("# Command: " + m_programCommand);
            out.println("# Lines truncated: " + linesTruncated);
            out.print(s);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save to file.",
                                          "GoGui: Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveLog()
    {
        save(m_gtpShellText.getLog(), m_gtpShellText.getLinesTruncated());
    }

    private void saveCommands()
    {
        save(m_commands.toString(), m_linesTruncated);
    }

    private void sendFile()
    {
        File file = SimpleDialogs.showOpen(this, "Choose GTP file.");
        if (file != null)
            sendGtpFile(file);
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

    /** Modify dialog size after first write.
        This is a workaround for problems with a JTextPane in a JScrollable
        in Sun JDK 1.4 and Mac JDK 1.4.
        Sometimes garbage text is left after inserting text
        if the JTextPane was created empty and the size was set by
        JScrollpane.setPreferredSize or letting the scrollable track the
        viewport width.
        The garbage text disappears after resizing the dialog,
        so we use JDialog.setSize after the first text was inserted.
    */
    private void setFinalSize()
    {
        if (m_isFinalSizeSet)
            return;
        setSize(m_finalSize);
        if (m_finalLocation != null)
            setLocation(m_finalLocation);
        m_isFinalSizeSet = true;
    }

    private static void setPrefsDefaults(Preferences prefs)
    {
        prefs.setBoolDefault("gtpshell-autonumber", false);
        // JComboBox has problems on the Mac, see section Bugs in
        // documentation
        prefs.setBoolDefault("gtpshell-disable-completions",
                             Platform.isMac());
        prefs.setBoolDefault("gtpshell-highlight", true);
        prefs.setIntDefault("gtpshell-history-max", 3000);
        prefs.setIntDefault("gtpshell-history-min", 2000);
    }

    /** Create wrapper object for addItem.
        See JDK 1.4 doc for JComboBox.addItem.
    */
    private Object wrapperObject(final String item)
    {
        return new Object()
            {
                public String toString()
                {
                    return item;
                }
            };
    }

}

//----------------------------------------------------------------------------
