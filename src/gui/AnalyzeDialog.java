//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import gtp.*;
import utils.*;

//-----------------------------------------------------------------------------

class AnalyzeCommand
{
    public static final int BWBOARD = 0;

    public static final int CBOARD = 1;

    public static final int DBOARD = 2;

    public static final int HSTRING = 3;

    public static final int HPSTRING = 4;

    public static final int NONE = 5;

    public static final int PLIST = 6;

    public static final int PSTRING = 7;

    public static final int PSPAIRS = 8;

    public static final int STRING = 9;

    public static final int SBOARD = 10;

    public static final int VAR = 11;

    public static final int VARB = 12;

    public static final int VARP = 13;

    public static final int VARPO = 14;

    public static final int VARW = 15;

    public AnalyzeCommand(String line)
    {
        m_scale = 1.0;
        m_title = null;
        m_type = AnalyzeCommand.NONE;
        String array[] = line.split("/");
        String typeStr = array[0];        
        if (typeStr.equals("bwboard"))
            m_type = AnalyzeCommand.BWBOARD;
        else if (typeStr.equals("cboard"))
            m_type = AnalyzeCommand.CBOARD;
        else if (typeStr.equals("dboard"))
            m_type = AnalyzeCommand.DBOARD;
        else if (typeStr.equals("hstring"))
            m_type = AnalyzeCommand.HSTRING;
        else if (typeStr.equals("hpstring"))
            m_type = AnalyzeCommand.HPSTRING;
        else if (typeStr.equals("plist"))
            m_type = AnalyzeCommand.PLIST;
        else if (typeStr.equals("pspairs"))
            m_type = AnalyzeCommand.PSPAIRS;
        else if (typeStr.equals("pstring"))
            m_type = AnalyzeCommand.PSTRING;
        else if (typeStr.equals("string"))
            m_type = AnalyzeCommand.STRING;
        else if (typeStr.equals("sboard"))
            m_type = AnalyzeCommand.SBOARD;
        else if (typeStr.equals("var"))
            m_type = AnalyzeCommand.VAR;
        else if (typeStr.equals("varb"))
            m_type = AnalyzeCommand.VARB;
        else if (typeStr.equals("varp"))
            m_type = AnalyzeCommand.VARP;
        else if (typeStr.equals("varpo"))
            m_type = AnalyzeCommand.VARPO;
        else if (typeStr.equals("varw"))
            m_type = AnalyzeCommand.VARW;
        m_label = array[1];
        m_command = array[2];
        if (array.length > 3)
            m_title = array[3];
        if (array.length > 4)
            m_scale = Double.parseDouble(array[4]);
    }

    public AnalyzeCommand(int type, String label, String command, String title,
                          double scale)
    {
        m_type = type;
        m_label = label;
        m_command = command;
        m_title = title;
        m_scale = scale;
    }

    public static AnalyzeCommand get(Frame owner, String label)
    {
        Vector commands = new Vector(128, 128);
        Vector labels = new Vector(128, 128);
        try
        {
            read(commands, labels, null);
        }
        catch (Exception e)
        {            
            SimpleDialogs.showError(owner, e.getMessage());
        }
        int index = labels.indexOf(label);
        if (index < 0)
            return null;
        return new AnalyzeCommand((String)commands.get(index));
    }

    public String getLabel()
    {
        return m_label;
    }

    public go.Point getPointArg()
    {
        return m_pointArg;
    }

    public Vector getPointListArg()
    {
        return m_pointListArg;
    }

    public double getScale()
    {
        return m_scale;
    }

    public String getTitle()
    {
        return m_title;
    }

    public int getType()
    {
        return m_type;
    }

    public String getResultTitle()
    {
        StringBuffer buffer = new StringBuffer(m_label);
        if (needsPointArg() && m_pointArg != null)
        {
            buffer.append(' ');
            buffer.append(m_pointArg.toString());
        }
        else if (needsPointListArg())
        {
            for (int i = 0; i < m_pointListArg.size(); ++i)
            {
                buffer.append(' ');
                buffer.append(((go.Point)(m_pointListArg.get(i))).toString());
            }
        }
        if (needsStringArg() && m_stringArg != null)
        {
            buffer.append(' ');
            buffer.append(m_stringArg);
        }
        return buffer.toString();
    }

    public boolean isPointArgMissing()
    {
        if (needsPointArg())
            return (m_pointArg == null);
        if (needsPointListArg())
            return m_pointListArg.isEmpty();
        return false;
    }

    public boolean needsFileArg()
    {
        return (m_command.indexOf("%f") >= 0);
    }

    public boolean needsPointArg()
    {
        return (m_command.indexOf("%p") >= 0);
    }

    public boolean needsPointListArg()
    {
        return (m_command.indexOf("%P") >= 0);
    }

    public boolean needsStringArg()
    {
        return (m_command.indexOf("%s") >= 0);
    }

    public static void read(Vector commands, Vector labels,
                            Vector supportedCommands)
        throws Exception
    {
        commands.clear();
        labels.clear();
        Vector files = getFiles();
        if (files.isEmpty())
        {
            File f = new File(getDir(), "analyze-commands");
            copyDefaults(f);
            files = getFiles();
        }
        for (int i = 0; i < files.size(); ++i)
            readFile((File)files.get(i), commands, labels, supportedCommands);
    }

    public String replaceWildCards(go.Color toMove)
    {
        String result = m_command.replaceAll("%m", toMove.toString());
        if (needsPointArg())
            result = result.replaceAll("%p", m_pointArg.toString());
        if (needsPointListArg())
        {
            StringBuffer listBuffer = new StringBuffer(128);
            for (int i = 0; i < m_pointListArg.size(); ++i)
            {
                if (listBuffer.length() > 0)
                    listBuffer.append(' ');
                go.Point point = (go.Point)m_pointListArg.get(i);
                listBuffer.append(point.toString());
            }
            result = result.replaceAll("%P", listBuffer.toString());
        }
        if (needsFileArg())
        {
            assert(m_fileArg != null);
            result = result.replaceAll("%f", m_fileArg.toString());
        }
        if (needsStringArg())
        {
            assert(m_stringArg != null);
            result = result.replaceAll("%s", m_stringArg);
        }
        return result;
    }

    public void setFileArg(File file)
    {
        assert(needsFileArg());
        m_fileArg = file;
    }

    public void setPointArg(go.Point point)
    {
        m_pointArg = point;
    }

    public void setStringArg(String value)
    {
        assert(needsStringArg());
        m_stringArg = value;
    }

    private int m_type;

    private double m_scale;

    private File m_fileArg;

    private String m_label;

    private String m_command;

    private String m_title;

    private String m_stringArg;

    private go.Point m_pointArg;

    private Vector m_pointListArg = new Vector();

    private static void copyDefaults(File file)
    {
        String resource = "config/analyze-commands";
        URL url = ClassLoader.getSystemClassLoader().getResource(resource);
        if (url == null)
            return;
        try
        {
            InputStream in = url.openStream();
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) >= 0)
                out.write(buffer, 0, n);
            in.close();
            out.close();
        }
        catch (Exception e)
        {
        }
    }

    private static File getDir()
    {
        String home = System.getProperty("user.home");
        return new File(home, ".gogui");
    }

    private static Vector getFiles()
    {
        Vector result = new Vector();
        File[] files = getDir().listFiles();
        if (files == null)
            return result;
        String s = new File(getDir(), "analyze-commands").toString();
        for (int i = 0; i < files.length; ++i)
        {
            File f = files[i];
            if (f.toString().startsWith(s))
                result.add(f);
        }
        return result;
    }

    public static void readFile(File file, Vector commands, Vector labels,
                                Vector supportedCommands)
        throws Exception
    {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        int lineNumber = 0;
        while ((line = in.readLine()) != null)
        {
            ++lineNumber;
            line = line.trim();
            if (line.length() > 0 && line.charAt(0) != '#')
            {
                String array[] = line.split("/");
                if (array.length < 3 || array.length > 5)
                    throw new Exception("Error in " + file + " line "
                                        + lineNumber);
                if (supportedCommands != null)
                {
                    String[] cmdArray
                        = StringUtils.tokenize(array[2].trim());
                    if (cmdArray.length == 0
                        || ! supportedCommands.contains(cmdArray[0]))
                        continue;
                }
                String label = array[1];
                if (labels.contains(label))
                    continue;
                labels.add(label);
                commands.add(line);
            }                
        }
        in.close();
    }
}

//-----------------------------------------------------------------------------

interface AnalyzeCallback
{
    public void cbGtpShell();

    public void clearAnalyzeCommand();

    public void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                  boolean clearBoard);

    public void toTop();
}

//-----------------------------------------------------------------------------

class AnalyzeDialog
    extends JDialog
    implements ActionListener, ListSelectionListener, MouseListener
{
    public AnalyzeDialog(Frame owner, AnalyzeCallback callback,
                         Preferences prefs, Vector supportedCommands)
    {
        super(owner, "GoGui: Analyze");
        m_prefs = prefs;
        setPrefsDefaults(prefs);
        m_onlySupportedCommands =
            prefs.getBool("analyze-only-supported-commands");
        m_sort = prefs.getBool("analyze-sort");
        m_supportedCommands = supportedCommands;
        m_callback = callback;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowAdapter windowAdapter = new WindowAdapter()
            {
                public void windowClosing(WindowEvent event)
                {
                    close();
                }
            };
        addWindowListener(windowAdapter);
        Container contentPane = getContentPane();
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        contentPane.add(createCommandPanel(owner), BorderLayout.CENTER);
        createMenu();
        comboBoxChanged();
        pack();
        m_list.requestFocusInWindow();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("clear"))
            clearCommand();
        else if (command.equals("close"))
            close();
        else if (command.equals("comboBoxChanged"))
            comboBoxChanged();
        else if (command.equals("gogui"))
            m_callback.toTop();
        else if (command.equals("gtp-shell"))
            m_callback.cbGtpShell();
        else if (command.equals("only-supported"))
            onlySupported();
        else if (command.equals("reload"))
            reload();
        else if (command.equals("run"))
            setCommand();
        else if (command.equals("sort"))
            sort();
    }

    public void mouseClicked(MouseEvent event)
    {
        int modifiers = event.getModifiers();
        int mask = ActionEvent.ALT_MASK;
        if (event.getClickCount() == 2
            || ((modifiers & mask) != 0))
        {
            int index = m_list.locationToIndex(event.getPoint());
            selectCommand(index);
            setCommand();
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void setTitlePrefix(String title)
    {
        setTitle(title + ": Analyze");
    }

    public void toTop()
    {
        setVisible(true);
        toFront();
    }

    public void valueChanged(ListSelectionEvent e)
    {
        int index = m_list.getSelectedIndex();
        if (index >= 0)
        {
            m_runButton.setEnabled(true);
            m_list.ensureIndexIsVisible(index);
        }
        else
        {
            if (m_runButton.hasFocus())
                m_list.requestFocusInWindow();
            m_runButton.setEnabled(false);
        }
    }

    private boolean m_onlySupportedCommands;

    private boolean m_sort;

    private boolean m_recentModified;

    private JButton m_clearButton;

    private JButton m_runButton;

    private JCheckBox m_autoRun;

    private JCheckBox m_clearBoard;

    private JComboBox m_comboBox;

    private JList m_list;

    private JMenuItem m_itemOnlySupported;

    private JMenuItem m_itemSort;

    private Vector m_commands = new Vector(128, 64);

    private Vector m_supportedCommands;

    private Vector m_labels = new Vector(128, 64);

    private AnalyzeCallback m_callback;

    private Preferences m_prefs;

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
                                  String command, String toolTip)
    {
        JMenuItem item = addMenuItem(menu, label, mnemonic, command);
        item.setToolTipText(toolTip);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  int accel, int modifier, String command)
    {
        JMenuItem item = new JMenuItem(label);
        KeyStroke k = KeyStroke.getKeyStroke(accel, modifier); 
        item.setAccelerator(k);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private void clearCommand()
    {
        m_callback.clearAnalyzeCommand();
        m_autoRun.setSelected(false);
        m_clearButton.setEnabled(false);
        if (m_clearButton.hasFocus())
            m_list.requestFocusInWindow();
    }

    private void close()
    {
        if (! m_autoRun.isSelected())
            clearCommand();
        saveRecent();
        setVisible(false);
    }

    private void comboBoxChanged()
    {
        String label = (String)m_comboBox.getSelectedItem();        
        if (! m_labels.contains(label))
        {
            m_list.clearSelection();
            return;
        }
        String selectedValue = (String)m_list.getSelectedValue();
        if (selectedValue == null || ! selectedValue.equals(label))
            m_list.setSelectedValue(label, true);
    }

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtils.PAD, 0));
        innerPanel.setBorder(GuiUtils.createEmptyBorder());
        m_runButton = new JButton("Run");
        m_runButton.setToolTipText("Run command");
        m_runButton.setActionCommand("run");
        m_runButton.addActionListener(this);
        m_runButton.setMnemonic(KeyEvent.VK_R);
        getRootPane().setDefaultButton(m_runButton);
        innerPanel.add(m_runButton);
        m_clearButton = new JButton("Clear");
        m_clearButton.setToolTipText("Clear board and cancel auto run");
        m_clearButton.setActionCommand("clear");
        m_clearButton.addActionListener(this);
        m_clearButton.setMnemonic(KeyEvent.VK_C);
        m_clearButton.setEnabled(false);
        innerPanel.add(m_clearButton);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private JPanel createCommandPanel(Frame owner)
    {
        JPanel panel = new JPanel(new BorderLayout());
        m_list = new JList();
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setVisibleRowCount(25);
        m_list.addMouseListener(this);
        m_list.addListSelectionListener(this);
        JScrollPane scrollPane = new JScrollPane(m_list);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createLowerPanel(), BorderLayout.SOUTH);
        reload();
        return panel;
    }

    private JPanel createLowerPanel()
    {
        JPanel panel = new JPanel(new GridLayout(0, 1, GuiUtils.PAD, 0));
        m_comboBox = new JComboBox();
        m_comboBox.addActionListener(this);
        panel.add(m_comboBox);
        m_autoRun = new JCheckBox("Auto run");
        m_autoRun.setToolTipText("Auto run after changes on board");
        panel.add(m_autoRun);
        m_clearBoard = new JCheckBox("Clear board");
        m_clearBoard.setToolTipText("Clear board before displaying result");
        panel.add(m_clearBoard);
        m_clearBoard.setSelected(true);
        loadRecent();
        return panel;
    }

    private void createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createMenuFile());
        menuBar.add(createMenuSettings());
        menuBar.add(createMenuWindows());
        setJMenuBar(menuBar);
    }

    private JMenu createMenuFile()
    {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        JMenuItem item =
            addMenuItem(menu, "Reload", KeyEvent.VK_R, "reload",
                        "Reload commands from configuration files");
        menu.addSeparator();
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    ActionEvent.CTRL_MASK, "close");
        return menu;
    }

    private JMenu createMenuSettings()
    {
        JMenu menu = new JMenu("Settings");
        menu.setMnemonic(KeyEvent.VK_S);
        m_itemOnlySupported =
            new JCheckBoxMenuItem("Only supported commands");
        m_itemOnlySupported.setSelected(m_onlySupportedCommands);
        addMenuItem(menu, m_itemOnlySupported, KeyEvent.VK_O,
                    "only-supported");
        m_itemSort = new JCheckBoxMenuItem("Sort alphabetically");
        m_itemSort.setSelected(m_sort);
        addMenuItem(menu, m_itemSort, KeyEvent.VK_S, "sort");
        return menu;
    }

    private JMenu createMenuWindows()
    {
        JMenu menu = new JMenu("Windows");
        menu.setMnemonic(KeyEvent.VK_W);
        addMenuItem(menu, "Board", KeyEvent.VK_B, KeyEvent.VK_F6, 0,
                    "gogui");
        addMenuItem(menu, "GTP shell", KeyEvent.VK_G, KeyEvent.VK_F9, 0,
                    "gtp-shell");
        return menu;
    }

    private File getRecentFile()
    {
        String home = System.getProperty("user.home");
        return new File(new File(home, ".gogui"), "recent-analyze");
    }

    private void onlySupported()
    {
        m_onlySupportedCommands = m_itemOnlySupported.isSelected();
        m_prefs.setBool("analyze-only-supported-commands",
                        m_onlySupportedCommands);
        reload();
    }

    private void loadRecent()
    {
        m_comboBox.removeAllItems();
        File file = getRecentFile();
        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        String line;
        try
        {
            while((line = reader.readLine()) != null)
            {
                m_comboBox.addItem(line);
            }
            reader.close();
        }
        catch (IOException e)
        {
            System.err.println("IOException in AnalyzeDialog.loadRecent");
        }
    }

    private void reload()
    {
        try
        {
            Vector supportedCommands = null;
            if (m_onlySupportedCommands)
                supportedCommands = m_supportedCommands;
            AnalyzeCommand.read(m_commands, m_labels, supportedCommands);
            if (m_sort)
                sortLists();
            m_list.setListData(m_labels);
            if (m_labels.size() > 0)
                // Avoid focus problem with Sun JDK 1.4.2 if focus was at an
                // index greater than the new list length
                m_list.setSelectedIndex(0);
            comboBoxChanged();
        }
        catch (Exception e)
        {            
            SimpleDialogs.showError(this, e.getMessage());
        }
    }

    public void saveRecent()
    {
        if (! m_recentModified)
            return;
        File file = getRecentFile();
        PrintStream out;
        try
        {
            out = new PrintStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e)
        {
            System.err.println("FileNotFoundException in"
                               + " AnalyzeDialog.saveRecent");
            return;
        }
        final int max = 20;
        for (int i = 0; i < m_comboBox.getItemCount() && i < max; ++i)
            out.println(m_comboBox.getItemAt(i));
        out.close();
    }

    private void selectCommand(int index)
    {
        String label = (String)m_labels.get(index);
        m_comboBox.insertItemAt(label, 0);
        for (int i = 1; i < m_comboBox.getItemCount(); ++i)
            if (((String)m_comboBox.getItemAt(i)).equals(label))
            {
                m_comboBox.removeItemAt(i);
                break;
            }
        m_comboBox.setSelectedIndex(0);
        m_recentModified = true;
    }

    private void setCommand()
    {
        int index = m_list.getSelectedIndex();        
        if (index < 0)
            return;
        selectCommand(index);
        String analyzeCommand = (String)m_commands.get(index);
        AnalyzeCommand command = new AnalyzeCommand(analyzeCommand);
        String label = command.getLabel();
        if (command.needsStringArg())
        {
            String stringArg =
                JOptionPane.showInputDialog(this, label);
            if (stringArg == null)
                return;
            command.setStringArg(stringArg);
        }
        if (command.needsFileArg())
        {
            
            File fileArg =
                SimpleDialogs.showSelectFile(this, label);
            if (fileArg == null)
                return;
            command.setFileArg(fileArg);
        }
        boolean autoRun = m_autoRun.isSelected();
        boolean clearBoard = m_clearBoard.isSelected();
        if (clearBoard)
            m_callback.clearAnalyzeCommand();
        m_clearButton.setEnabled(true);
        m_callback.setAnalyzeCommand(command, autoRun, false);
    }

    private static void setPrefsDefaults(Preferences prefs)
    {
        prefs.setBoolDefault("analyze-only-supported-commands", true);
        prefs.setBoolDefault("analyze-sort", true);
    }

    private void sort()
    {
        m_sort = m_itemSort.isSelected();
        m_prefs.setBool("analyze-sort", m_sort);
        reload();
    }

    private void sortLists()
    {
        for (int i = 0; i < m_labels.size() - 1; ++i)
            for (int j = i + 1; j < m_labels.size(); ++j)
            {
                String labelI = (String)m_labels.get(i);
                String labelJ = (String)m_labels.get(j);
                if (labelI.compareTo(labelJ) > 0)
                {
                    m_labels.set(i, labelJ);
                    m_labels.set(j, labelI);
                    String cmdI = (String)m_commands.get(i);
                    String cmdJ = (String)m_commands.get(j);
                    m_commands.set(i, cmdJ);
                    m_commands.set(j, cmdI);
                }
            }
    }
}

//-----------------------------------------------------------------------------

class AnalyzeTextOutput
    extends JDialog
    implements KeyListener
{
    public AnalyzeTextOutput(Frame owner, String title, String response,
                             boolean highlight)
    {
        super(owner, "GoGui: " + title);
        setLocationRelativeTo(owner);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(GuiUtils.createSmallEmptyBorder());
        Container contentPane = getContentPane();
        contentPane.add(panel, BorderLayout.CENTER);
        JLabel label = new JLabel(title);
        panel.add(label, BorderLayout.NORTH);
        m_textPane = new JTextPane();
        StyledDocument doc = m_textPane.getStyledDocument();
        try
        {
            doc.insertString(0, response, null);
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
        int fontSize = GuiUtils.getDefaultMonoFontSize();
        m_textPane.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        JScrollPane scrollPane = new JScrollPane(m_textPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        m_textPane.addKeyListener(this);
        if (highlight)
            doSyntaxHighlight();
        m_textPane.setEditable(false);
        pack();
        setVisible(true);
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e) 
    {
        int c = e.getKeyCode();        
        if (c == KeyEvent.VK_ESCAPE)
            dispose();
    }

    public void keyTyped(KeyEvent e)
    {
    }

    private JTextPane m_textPane;

    private void doSyntaxHighlight()
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style styleTitle = doc.addStyle("title", def);
        StyleConstants.setBold(styleTitle, true);
        Style stylePoint = doc.addStyle("point", def);
        StyleConstants.setForeground(stylePoint, new Color(0.25f, 0.5f, 0.7f));
        Style styleNumber = doc.addStyle("number", def);
        StyleConstants.setForeground(styleNumber, new Color(0f, 0.54f, 0f));
        Style styleConst = doc.addStyle("const", def);
        StyleConstants.setForeground(styleConst, new Color(0.8f, 0f, 0f));
        Style styleColor = doc.addStyle("color", def);
        StyleConstants.setForeground(styleColor, new Color(0.54f, 0f, 0.54f));
        m_textPane.setEditable(true);
        highlight("number", "\\b-?[0-9]+\\.?+[0-9]*\\b");
        highlight("const", "\\b[A-Z_][A-Z_]+[A-Z]\\b");
        highlight("color",
                  "\\b([Bb][Ll][Aa][Cc][Kk]|[Ww][Hh][Ii][Tt][Ee])\\b");
        highlight("point", "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1[0-9]|[1-9]))\\b");
        highlight("title", "^\\S+:\\s*$");
        m_textPane.setEditable(false);
    }

    private void highlight(String style, String regex)
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        try
        {
            Matcher matcher = pattern.matcher(doc.getText(0, doc.getLength()));
            while (matcher.find())
            {
                int start = matcher.start();
                int end = matcher.end();
                doc.setCharacterAttributes(start, end - start,
                                           doc.getStyle(style), true);
            }
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}

//-----------------------------------------------------------------------------

class AnalyzeShow
{
    public static void show(AnalyzeCommand command, gui.Board guiBoard,
                            go.Board board, String response) throws Gtp.Error
    {
        go.Point pointArg = command.getPointArg();
        Vector pointListArg = command.getPointListArg();
        guiBoard.clearAllSelect();
        for (int i = 0; i < pointListArg.size(); ++i)
            guiBoard.setSelect((go.Point)pointListArg.get(i), true);
        if (pointArg != null)
            guiBoard.setSelect(pointArg, true);
        int type = command.getType();
        String title = command.getTitle();
        int size = board.getSize();
        switch (type)
        {
        case AnalyzeCommand.BWBOARD:
            {
                String b[][] = Gtp.parseStringBoard(response, title, size);
                guiBoard.showBWBoard(b);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.CBOARD:
            {
                String b[][] = Gtp.parseStringBoard(response, title, size);
                guiBoard.showColorBoard(b);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.DBOARD:
            {
                double b[][] = Gtp.parseDoubleBoard(response, title, size);
                guiBoard.showDoubleBoard(b, command.getScale());
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.PLIST:
            {
                go.Point list[] = Gtp.parsePointList(response, size);
                guiBoard.showPointList(list);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.HPSTRING:
        case AnalyzeCommand.PSTRING:
            {
                go.Point list[] = Gtp.parsePointString(response, size);
                guiBoard.showPointList(list);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.PSPAIRS:
            {
                Vector pointList = new Vector(32, 32);
                Vector stringList = new Vector(32, 32);
                Gtp.parsePointStringList(response, pointList, stringList,
                                         size);
                guiBoard.showPointStringList(pointList, stringList);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.SBOARD:
            {
                String b[][] = Gtp.parseStringBoard(response, title, size);
                guiBoard.showStringBoard(b);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.VAR:
            {                    
                go.Point list[] = Gtp.parsePointString(response, size);
                guiBoard.showVariation(list, board.getToMove());
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.VARB:
            {
                go.Point list[] = Gtp.parsePointString(response, size);
                guiBoard.showVariation(list, go.Color.BLACK);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.VARW:
            {
                go.Point list[] = Gtp.parsePointString(response, size);
                guiBoard.showVariation(list, go.Color.WHITE);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.VARP:
            {
                go.Color c = getColor(board, pointArg, pointListArg);
                if (c != go.Color.EMPTY)
                {
                    go.Point list[] = Gtp.parsePointString(response, size);
                    guiBoard.showVariation(list, c);
                    guiBoard.repaint();
                }
            }
            break;
        case AnalyzeCommand.VARPO:
            {
                go.Color c = getColor(board, pointArg, pointListArg);
                if (c != go.Color.EMPTY)
                {
                    go.Point list[] = Gtp.parsePointString(response, size);
                    guiBoard.showVariation(list, c.otherColor());
                    guiBoard.repaint();
                }
            }
            break;
        }
    }

    private static go.Color getColor(go.Board board, go.Point pointArg,
                                     Vector pointListArg)
    {
        go.Color color = go.Color.EMPTY;
        if (pointArg != null)
            color = board.getColor(pointArg);
        if (color != go.Color.EMPTY)
            return color;
        for (int i = 0; i < pointListArg.size(); ++i)
        {
            go.Point point = (go.Point)pointListArg.get(i);
            color = board.getColor(point);
            if (color != go.Color.EMPTY)
                break;
        }
        return color;
    }
}

//-----------------------------------------------------------------------------
