//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.Preferences;

//----------------------------------------------------------------------------

class GtpShellText
    extends JTextPane
    implements Scrollable
{
    public GtpShellText(int historyMin, int historyMax, boolean timeStamp)
    {
        m_startTime = System.currentTimeMillis();
        m_timeStamp = timeStamp;
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
        initCaret();
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
        int length = getStyledDocument().getLength();
        setCaretPosition(length);
        try
        {
            scrollRectToVisible(modelToView(length));
        }
        catch (BadLocationException e)
        {
        }
    }

    public void setTimeStamp(boolean enable)
    {
        m_timeStamp = enable;
    }

    private boolean m_highlight;

    private boolean m_timeStamp;

    private final int m_historyMin;

    private final int m_historyMax;

    private int m_lines;

    private int m_truncated;

    private long m_startTime;

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
        StyledDocument doc = getStyledDocument();
        Style s = null;
        if (style != null && m_highlight)
            s = getStyle(style);
        try
        {
            int length = doc.getLength();
            boolean visible = isVisible(length);
            doc.insertString(length, text, s);
            if (visible)
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
        double diff = (timeMillis - m_startTime) / 1000;
        appendText(Clock.getTimeString(diff, -1) + " ", "log");
    }

    /** Try to set the default caret with update policy NEVER_UPDATE.
        Uses reflection API, because setUpdatePolicy(NEVER_UPDATE)
        is not available on Java 1.4
    */
    private void initCaret()
    {
        DefaultCaret caret = new DefaultCaret();
        Class classCaret;
        try
        {
            classCaret = Class.forName("javax.swing.text.DefaultCaret");
        }
        catch (ClassNotFoundException e)
        {
            assert(false);
            return;
        }        
        Field field;
        try
        {
            field = classCaret.getField("NEVER_UPDATE");
        }
        catch (NoSuchFieldException e)
        {
            return;
        }        
        assert(Modifier.isStatic(field.getModifiers()));
        int neverUpdate;
        try
        {
            neverUpdate = field.getInt(caret);
        }
        catch (IllegalAccessException e)
        {
            assert(false);
            return;
        }
        Class [] args = new Class[1];
        args[0] = int.class;
        Method method;
        try
        {
            method = classCaret.getMethod("setUpdatePolicy", args);
        }
        catch (NoSuchMethodException e)
        {
            assert(false);
            return;
        }
        assert(method.getReturnType() == void.class);
        Object[] objArgs = new Object[1];
        objArgs[0] = new Integer(neverUpdate);
        try
        {
            method.invoke(caret, objArgs);
        }
        catch (InvocationTargetException e)
        {
            assert(false);
            return;
        }
        catch (IllegalAccessException e)
        {
            assert(false);
            return;
        }
        setCaret(caret);
    }

    private boolean isVisible(int pos)
    {
        try
        {
            Rectangle rect = modelToView(pos);
            Rectangle visibleRect = getVisibleRect();
            boolean result = visibleRect.contains(rect.x, rect.y);
            return result;
        }
        catch (BadLocationException e)
        {
            return true;
        }
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

/** Dialog for displaying the GTP stream and for entering commands. */
public class GtpShell
    extends JDialog
    implements ActionListener, Gtp.IOCallback
{
    /** Callback for events generated by GtpShell. */
    public interface Callback
    {
        void cbAnalyze();

        boolean sendGtpCommand(String command, boolean sync) throws GtpError;
    }

    public GtpShell(Frame owner, Callback callback, Preferences prefs)
    {
        super(owner, "GTP Shell");
        m_callback = callback;
        m_prefs = prefs;
        setPrefsDefaults(prefs);
        m_historyMin = prefs.getInt("gtpshell-history-min");
        m_historyMax = prefs.getInt("gtpshell-history-max");
        Container contentPane = getContentPane();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        getContentPane().add(panel, BorderLayout.CENTER);
        m_gtpShellText
            = new GtpShellText(m_historyMin, m_historyMax, m_timeStamp);
        m_scrollPane =
            new JScrollPane(m_gtpShellText,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        m_fontSize = m_gtpShellText.getFont().getSize();
        m_finalSize = new Dimension(m_fontSize * 40, m_fontSize * 30);
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
        else if (command.equals("close"))
            setVisible(false);
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

    public void setHighlight(boolean highlight)
    {
        m_gtpShellText.setHighlight(highlight);
    }

    public void setTimeStamp(boolean enable)
    {
        m_gtpShellText.setTimeStamp(enable);
    }

    public void toTop()
    {
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
                    "The command '" + command + "'\n" +
                    "will modify the board state and\n" +
                    "cause the graphical board to be out of sync.\n" +
                    "You should start a new game before using\n" +
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
                Utils.showError(owner, e);
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

    private boolean m_timeStamp;

    private boolean m_disableCompletions;

    private boolean m_isFinalSizeSet;

    private boolean m_showModifyWarning = true;

    private int m_fontSize;

    private int m_historyMax;

    private int m_historyMin;

    private int m_linesTruncated;

    private int m_numberCommands;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private Callback m_callback;

    private ComboBoxEditor m_editor;

    private Dimension m_finalSize;

    private JTextField m_textField;

    private JComboBox m_comboBox;

    private JScrollPane m_scrollPane;

    private GtpShellText m_gtpShellText;

    private Point m_finalLocation;

    private final StringBuffer m_commands = new StringBuffer(4096);

    private final Vector m_history = new Vector(128, 128);

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

    private void comboBoxEdited()
    {
        String command = m_comboBox.getSelectedItem().toString();        
        if (command.trim().equals(""))
            return;
        sendCommand(command, null, false);
        appendToHistory(command);
        m_gtpShellText.setPositionToEnd();
        m_comboBox.hidePopup();
        addAllCompletions(m_history);
        m_editor.setItem(null);
        m_comboBox.requestFocusInWindow();
        m_textField.requestFocusInWindow();
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
        prefs.setBoolDefault("gtpshell-timestamp", false);
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
