//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import utils.*;

//-----------------------------------------------------------------------------

class AnalyzeCommand
{
    public static final int NONE = 0;

    public static final int STRING = 1;    

    public static final int DOUBLEBOARD = 2;

    public static final int POINTLIST = 3;

    public static final int POINTSTRINGLIST = 4;

    public static final int POINTSTRING = 5;

    public static final int STRINGBOARD = 6;

    public static final int COLORBOARD = 7;

    public AnalyzeCommand(String line)
    {
        m_scale = 1.0;
        m_title = null;
        m_type = AnalyzeCommand.NONE;
        String array[] = StringUtils.split(line, '/');
        String typeStr = array[0];        
        if (typeStr.equals("cboard"))
            m_type = AnalyzeCommand.COLORBOARD;
        else if (typeStr.equals("dboard"))
            m_type = AnalyzeCommand.DOUBLEBOARD;
        else if (typeStr.equals("sboard"))
            m_type = AnalyzeCommand.STRINGBOARD;
        else if (typeStr.equals("plist"))
            m_type = AnalyzeCommand.POINTLIST;
        else if (typeStr.equals("pslist"))
            m_type = AnalyzeCommand.POINTSTRINGLIST;
        else if (typeStr.equals("pstring"))
            m_type = AnalyzeCommand.POINTSTRING;
        else if (typeStr.equals("string"))
            m_type = AnalyzeCommand.STRING;
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

    public String getResultTitle(go.Point pointArg, Vector pointListArg)
    {
        StringBuffer buffer = new StringBuffer(m_label);
        if (needsPointArg() && pointArg != null)
        {
            buffer.append(' ');
            buffer.append(pointArg.toString());
        }
        else if (needsPointListArg() && pointListArg != null)
        {
            for (int i = 0; i < pointListArg.size(); ++i)
            {
                buffer.append(' ');
                buffer.append(((go.Point)(pointListArg.get(i))).toString());
            }
        }
        if (needsStringArg() && m_stringArg != null)
        {
            buffer.append(' ');
            buffer.append(m_stringArg);
        }
        return buffer.toString();
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
        File file = getFile();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        int lineNumber = 0;
        while ((line = in.readLine()) != null)
        {
            ++lineNumber;
            line = line.trim();
            if (line.length() > 0 && line.charAt(0) != '#')
            {
                String array[] = StringUtils.split(line, '/');
                if (array.length < 3 || array.length > 5)
                    throw new Exception("Error in " + file + " line "
                                        + lineNumber);
                if (supportedCommands != null)
                {
                    String[] cmdArray
                        = StringUtils.getCmdArray(array[2].trim());
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
    }

    public String replaceWildCards(go.Color toMove, go.Point pointArg,
                                   Vector pointListArg)
    {
        StringBuffer buffer = new StringBuffer(m_command);
        StringUtils.replace(buffer, "%m", toMove.toString());
        if (needsPointArg())
        {
            assert(pointArg != null);
            StringUtils.replace(buffer, "%p", pointArg.toString());
        }
        if (needsPointListArg())
        {
            assert(pointListArg != null);
            StringBuffer listBuffer = new StringBuffer(128);
            for (int i = 0; i < pointListArg.size(); ++i)
            {
                if (listBuffer.length() > 0)
                    listBuffer.append(' ');
                listBuffer.append(((go.Point)pointListArg.get(i)).toString());
            }
            StringUtils.replace(buffer, "%P", listBuffer.toString());
        }
        if (needsFileArg())
        {
            assert(m_fileArg != null);
            StringUtils.replace(buffer, "%f", m_fileArg.toString());
        }
        if (needsStringArg())
        {
            assert(m_stringArg != null);
            StringUtils.replace(buffer, "%s", m_stringArg);
        }
        return buffer.toString();
    }

    public void setFileArg(File file)
    {
        assert(needsFileArg());
        m_fileArg = file;
    }

    public void setStringArg(String value)
    {
        assert(needsStringArg());
        m_stringArg = value;
    }

    private int m_type;

    private File m_fileArg;

    private String m_label;

    private String m_command;

    private String m_title;

    private double m_scale;

    private String m_stringArg;

    private static File getDir()
    {
        String home = System.getProperty("user.home");
        return new File(home, ".gogui");
    }

    private static File getFile()
    {
        return new File(getDir(), "analyze-commands");
    }
}

//-----------------------------------------------------------------------------

interface AnalyzeCallback
{
    public void clearAnalyzeCommand();

    public void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                  boolean clearBoard);
}

//-----------------------------------------------------------------------------

class AnalyzeDialog
    extends JDialog
    implements ActionListener, ListSelectionListener, MouseListener,
               WindowListener
{
    public AnalyzeDialog(Frame owner, AnalyzeCallback callback,
                         Preferences prefs, Vector supportedCommands)
    {
        super(owner, "GoGui: Analyze command");
        m_prefs = prefs;
        setPrefsDefaults(prefs);
        m_onlySupportedCommands =
            prefs.getBool("analyze-only-supported-commands");
        m_supportedCommands = supportedCommands;
        m_callback = callback;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        Container contentPane = getContentPane();
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        contentPane.add(createCommandPanel(owner), BorderLayout.CENTER);
        createMenu();
        comboBoxChanged();
        pack();
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
        else if (command.equals("only-supported"))
            onlySupported();
        else if (command.equals("reload"))
            reload();
        else if (command.equals("run"))
            setCommand();
    }

    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2)
        {
            int index = m_list.locationToIndex(e.getPoint());
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

    public void toTop()
    {
        m_list.requestFocus();
        setVisible(true);
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
                m_list.requestFocus();
            m_runButton.setEnabled(false);
        }
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        close();
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }

    private boolean m_onlySupportedCommands;

    private boolean m_recentModified;

    private JButton m_clearButton;

    private JButton m_runButton;

    private JCheckBox m_autoRun;

    private JCheckBox m_clearBoard;

    private JComboBox m_comboBox;

    private JList m_list;

    private JMenuItem m_itemOnlySupported;

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
            m_list.requestFocus();
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
            return;
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
        m_list.setVisibleRowCount(20);
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
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, GuiUtils.PAD, 0));
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
        setJMenuBar(menuBar);
    }

    private JMenu createMenuFile()
    {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(menu, "Reload", KeyEvent.VK_R, "reload");
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
        return menu;
    }

    private void onlySupported()
    {
        m_onlySupportedCommands = m_itemOnlySupported.isSelected();
        m_prefs.setBool("analyze-only-supported-commands",
                        m_onlySupportedCommands);
        reload();
    }

    private void reload()
    {
        try
        {
            Vector supportedCommands = null;
            if (m_onlySupportedCommands)
                supportedCommands = m_supportedCommands;
            AnalyzeCommand.read(m_commands, m_labels, supportedCommands);
            m_list.setListData(m_labels);
        }
        catch (Exception e)
        {            
            SimpleDialogs.showError(this, e.getMessage());
        }
    }

    private File getRecentFile()
    {
        String home = System.getProperty("user.home");
        return new File(new File(home, ".gogui"), "recent-analyze");
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
        }
        catch (IOException e)
        {
        }
        try
        {
            reader.close();
        }
        catch (IOException e)
        {
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
            return;
        }
        final int max = 10;
        for (int i = 0; i < m_comboBox.getItemCount() && i < max; ++i)
            out.println(m_comboBox.getItemAt(i));
        out.close();
    }

    private void selectCommand(int index)
    {
        String label = (String)m_labels.get(index);
        for (int i = 0; i < m_comboBox.getItemCount(); ++i)
            if (((String)m_comboBox.getItemAt(i)).equals(label))
            {
                m_comboBox.removeItemAt(i);
                break;
            }
        m_comboBox.insertItemAt(label, 0);
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
        if (command.needsStringArg())
        {
            String stringArg =
                JOptionPane.showInputDialog(this, "Argument for "
                                            + command.getLabel());
            if (stringArg == null)
                return;
            command.setStringArg(stringArg);
        }
        if (command.needsFileArg())
        {
            File fileArg = SimpleDialogs.showSelectFile(this);
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
        prefs.setBoolDefault("analyze-only-supported-commands", false);
    }
}

//-----------------------------------------------------------------------------

class AnalyzeTextOutput
    extends JDialog
    implements KeyListener
{
    public AnalyzeTextOutput(Frame owner, String title, String response)
    {
        super(owner, "GoGui: " + title);
        JLabel label = new JLabel(title);
        Container contentPane = getContentPane();
        contentPane.add(label, BorderLayout.NORTH);
        JTextArea textArea = new JTextArea(response);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN,
                                  getFont().getSize()));
        JScrollPane scrollPane = new JScrollPane(textArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        textArea.addKeyListener(this);
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
}

//-----------------------------------------------------------------------------
