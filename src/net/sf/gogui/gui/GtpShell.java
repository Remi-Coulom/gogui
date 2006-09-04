//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.PrefUtils;

//----------------------------------------------------------------------------

class GtpShellText
    extends GuiTextPane
{
    public GtpShellText(int historyMin, int historyMax, boolean timeStamp,
                        boolean fast)
    {
        super(fast);
        GuiUtils.setMonospacedFont(get());
        m_startTime = System.currentTimeMillis();
        m_timeStamp = timeStamp;
        m_historyMin = historyMin;
        m_historyMax = historyMax;
        setNoLineSpacing();
        addStyle("error", Color.red);
        addStyle("output", null, null, true);
        addStyle("log", new Color(0.5f, 0.5f, 0.5f));
        addStyle("time", new Color(0, 0, 0.5f));
        addStyle("invalid", Color.white, Color.red, false);
        get().setEditable(false);
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
        appendTimeStamp();
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
        appendTimeStamp();
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

    public int getLinesTruncated()
    {
        return m_truncated;
    }

    public String getLog()
    {
        Document doc = getDocument();
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

    public void setPositionToEnd()
    {
        int length = getDocument().getLength();
        get().setCaretPosition(length);
    }

    public void setTimeStamp(boolean enable)
    {
        m_timeStamp = enable;
    }

    private boolean m_timeStamp;

    private final int m_historyMin;

    private final int m_historyMax;

    private int m_lines;

    private int m_truncated;

    private final long m_startTime;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

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
        Document doc = getDocument();
        Style s = null;
        if (style != null)
            s = getStyle(style);
        try
        {
            int length = doc.getLength();
            doc.insertString(length, text, s);
            setPositionToEnd();
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
        if (m_lines > m_historyMax)
        {
            truncateHistory();
            setPositionToEnd();
        }
    }

    private void appendTimeStamp()
    {
        if (! m_timeStamp)
            return;
        long timeMillis = System.currentTimeMillis();
        double diff = (float)(timeMillis - m_startTime) / 1000;
        appendText(Clock.getTimeString(diff, -1) + " ", "time");
    }

    private void truncateHistory()
    {
        int truncateLines = m_lines - m_historyMin;
        Document doc = getDocument();
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

/** Dialog for displaying the GTP stream and for entering commands. */
public class GtpShell
    extends JDialog
    implements ActionListener, GtpClient.IOCallback
{
    /** Callback for events generated by GtpShell. */
    public interface Callback
    {
        void cbAnalyze();

        /** @see GtpShell#send */
        boolean sendGtpCommand(String command, boolean sync) throws GtpError;
    }

    public GtpShell(Frame owner, Callback callback, boolean fast)
    {
        super(owner, "Shell");
        m_callback = callback;
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        m_historyMin = prefs.getInt("history-min", 2000);
        m_historyMax = prefs.getInt("history-max", 3000);
        JPanel panel = new JPanel(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        m_gtpShellText
            = new GtpShellText(m_historyMin, m_historyMax, m_timeStamp, fast);
        m_scrollPane =
            new JScrollPane(m_gtpShellText.get(),
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        int fontSize = m_gtpShellText.get().getFont().getSize();
        m_finalSize = new Dimension(fontSize * 40, fontSize * 30);
        panel.add(m_scrollPane, BorderLayout.CENTER);
        panel.add(createCommandInput(), BorderLayout.SOUTH);
        pack();
    }
    
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("analyze"))
            m_callback.cbAnalyze();
        else if (command.equals("comboBoxEdited"))
            comboBoxEdited();
        else if (command.equals("run"))
            comboBoxEdited();
        else if (command.equals("close"))
            setVisible(false);
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
        ArrayList list = new ArrayList(max);
        for (int i = m_history.size() - max; i < m_history.size(); ++i)
            list.add(m_history.get(i));
        PrefUtils.putList("net/sf/gogui/gui/gtpshell/recentcommands", list);
    }

    public void setCommandInProgess(boolean commandInProgess)
    {
        m_comboBox.setEnabled(! commandInProgess);
        m_runButton.setEnabled(! commandInProgess);
        if (! commandInProgess)
        {
            m_comboBox.requestFocusInWindow();
            m_textField.requestFocusInWindow();
        }
    }

    public void setCommandCompletion(boolean commandCompletion)
    {
        m_disableCompletions = ! commandCompletion;
    }

    public void setTimeStamp(boolean enable)
    {
        m_gtpShellText.setTimeStamp(enable);
    }

    /** Send Gtp command to callback.
        If owner != null, send synchronously and display error dialog on
        owner, otherwise send asynchronously.
    */
    public boolean send(String command, Component owner, boolean askContinue)
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
            if (c.startsWith("boardsize ")
                || c.startsWith("black ")
                || c.equals("clear_board")
                || c.startsWith("genmove ")
                || c.startsWith("genmove_black ")
                || c.startsWith("genmove_cleanup ")
                || c.startsWith("genmove_white ")
                || c.startsWith("kgs-genmove_cleanup ")
                || c.startsWith("loadsgf ")
                || c.startsWith("play ")
                || c.startsWith("play_sequence ")
                || c.startsWith("white ")
                || c.startsWith("quit"))
            {
                if (m_modifyWarning == null)
                    m_modifyWarning = new OptionalMessage(this);
                String message = 
                    "The command '" + command + "' " +
                    "will modify the board state " +
                    "and cause the graphical board to be out of sync. " +
                    "You should start a new game before using " +
                    "the graphical board again.";
                if (! m_modifyWarning.showWarning(message))
                    return true;
            }
            try
            {
                m_callback.sendGtpCommand(command, owner != null);
            }
            catch (GtpError e)
            {
                Utils.showError(owner, m_programName, e);
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
    
    public void setFinalSize(int x, int y, int width, int height)
    {
        if (m_isFinalSizeSet)
            setBounds(x, y, width, height);
        else
        {
            m_finalSize = new Dimension(width, height);
            m_finalLocation = new Point(x, y);
        }
    }

    public void setInitialCompletions(ArrayList completions)
    {
        for (int i = completions.size() - 1; i >= 0; --i)
            appendToHistory(completions.get(i).toString());
        ArrayList list =
            PrefUtils.getList("net/sf/gogui/gui/gtpshell/recentcommands");
        for (int i = 0; i < list.size(); ++i)
            appendToHistory((String)list.get(i));
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

        private final String m_text;

        private final GtpShell m_gtpShell;
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

        private final String m_text;

        private final GtpShell m_gtpShell;
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

        private final boolean m_error;

        private final String m_text;

        private final GtpShell m_gtpShell;
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

        private final String m_text;

        private final GtpShell m_gtpShell;
    }

    /** Wrapper object for JComboBox items.
        JComboBox can have focus and keyboard navigation problems if
        duplicate String objects are added.
        See JDK 1.4 doc for JComboBox.addItem.
    */
    private static class WrapperObject
    {
        WrapperObject(String item)
        {
            m_item = item;
        }

        public String toString()
        {
            return m_item;
        }

        private final String m_item;
    }

    private boolean m_timeStamp;

    private boolean m_disableCompletions;

    private boolean m_isFinalSizeSet;

    private final int m_historyMax;

    private final int m_historyMin;

    private int m_linesTruncated;

    private int m_numberCommands;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final Callback m_callback;

    private ComboBoxEditor m_editor;

    private Dimension m_finalSize;

    private JButton m_runButton;

    private JTextField m_textField;

    private JComboBox m_comboBox;

    private final JScrollPane m_scrollPane;

    private final GtpShellText m_gtpShellText;

    private OptionalMessage m_modifyWarning;

    private Point m_finalLocation;

    private final StringBuffer m_commands = new StringBuffer(4096);

    private final ArrayList m_history = new ArrayList(128);

    private String m_programCommand = "unknown";

    private String m_programName = "unknown";

    private String m_programVersion = "unknown";

    private void addAllCompletions(ArrayList completions)
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
            Object object = new WrapperObject((String)completions.get(i));
            m_comboBox.addItem(object);
        }
        m_comboBox.setSelectedIndex(-1);
        m_textField.setText(oldText);
        m_textField.setCaretPosition(oldCaretPosition);
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
        m_commands.append('\n');
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

    private void comboBoxEdited()
    {
        Object selectedItem = m_comboBox.getSelectedItem();
        if (selectedItem == null)
            return;
        String command = selectedItem.toString();        
        if (command.trim().equals(""))
            return;
        send(command, null, false);
        appendToHistory(command);
        m_gtpShellText.setPositionToEnd();
        m_comboBox.hidePopup();
        addAllCompletions(m_history);
        m_editor.setItem(null);
    }

    private JPanel createCommandInput()
    {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.EAST);
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
        m_comboBox.setFont(m_gtpShellText.get().getFont());
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
        m_runButton = new JButton();
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url =
            classLoader.getResource("net/sf/gogui/images/exec.png");
        if (url == null)
            m_runButton.setText("Run");
        else
        {
            ImageIcon imageIcon = new ImageIcon(url, "Run");
            m_runButton.setIcon(imageIcon);
        }
        m_runButton.setActionCommand("run");
        m_runButton.addActionListener(this);
        buttonPanel.add(GuiUtils.createSmallFiller(), BorderLayout.WEST);
        buttonPanel.add(m_runButton, BorderLayout.CENTER);
        // Workaround for Java 1.4.1 on Mac OS X add some empty space
        // so that combobox does not overlap the window resize widget
        if (Platform.isMac())
        {
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            buttonPanel.add(filler, BorderLayout.EAST);
        }
        return panel;
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
        ArrayList completions = new ArrayList(128);
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

    private void save(JFrame parent, String s, int linesTruncated)
    {
        File file = SimpleDialogs.showSave(parent, null);
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
            JOptionPane.showMessageDialog(parent, "Could not save to file.",
                                          "GoGui: Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scrollPage(boolean up)
    {
        JViewport viewport = m_scrollPane.getViewport();
        Point position = viewport.getViewPosition();
        int delta = m_scrollPane.getSize().height
            - m_gtpShellText.get().getFont().getSize();
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
}

//----------------------------------------------------------------------------
