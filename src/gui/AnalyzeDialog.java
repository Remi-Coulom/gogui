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

class AnalyzeDialog
    extends JDialog
    implements ActionListener, ListSelectionListener
{
    public interface Callback
    {
        public void cbGtpShell();
        
        public void clearAnalyzeCommand();
        
        public void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                      boolean clearBoard);
        
        public void toTop();
    }

    public AnalyzeDialog(Frame owner, Callback callback, Preferences prefs,
                         Vector supportedCommands)
    {
        super(owner, "Analyze - GoGui");
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

    public void setAppName(String name)
    {
        setTitle("Analyze" + " - " + name);
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

    private static final int m_shortcutKeyMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

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

    private Callback m_callback;

    private Preferences m_prefs;

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, int mnemonic,
                                  String command)
    {
        item.addActionListener(this);
        item.setActionCommand(command);
        if (! Platform.isPlatformMacOSX())
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
        if (! Platform.isPlatformMacOSX())
            m_runButton.setMnemonic(KeyEvent.VK_R);
        getRootPane().setDefaultButton(m_runButton);
        innerPanel.add(m_runButton);
        m_clearButton = new JButton("Clear");
        m_clearButton.setToolTipText("Clear board and cancel auto run");
        m_clearButton.setActionCommand("clear");
        m_clearButton.addActionListener(this);
        if (! Platform.isPlatformMacOSX())
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
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
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
            };
        m_list.addMouseListener(mouseAdapter);
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
        m_autoRun = new JCheckBox("Auto Run");
        m_autoRun.setToolTipText("Auto run after changes on board");
        panel.add(m_autoRun);
        m_clearBoard = new JCheckBox("Clear Board");
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
        if (! Platform.isPlatformMacOSX())
            menu.setMnemonic(KeyEvent.VK_F);
        JMenuItem item =
            addMenuItem(menu, "Reload", KeyEvent.VK_R, "reload",
                        "Reload commands from configuration files");
        menu.addSeparator();
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    m_shortcutKeyMask, "close");
        return menu;
    }

    private JMenu createMenuSettings()
    {
        JMenu menu = new JMenu("Settings");
        if (! Platform.isPlatformMacOSX())
            menu.setMnemonic(KeyEvent.VK_S);
        m_itemOnlySupported =
            new JCheckBoxMenuItem("Only Supported Commands");
        m_itemOnlySupported.setSelected(m_onlySupportedCommands);
        addMenuItem(menu, m_itemOnlySupported, KeyEvent.VK_O,
                    "only-supported");
        m_itemSort = new JCheckBoxMenuItem("Sort Alphabetically");
        m_itemSort.setSelected(m_sort);
        addMenuItem(menu, m_itemSort, KeyEvent.VK_S, "sort");
        return menu;
    }

    private JMenu createMenuWindows()
    {
        JMenu menu = new JMenu("Windows");
        if (! Platform.isPlatformMacOSX())
            menu.setMnemonic(KeyEvent.VK_W);
        addMenuItem(menu, "Board", KeyEvent.VK_B, KeyEvent.VK_F6, 0,
                    "gogui");
        addMenuItem(menu, "GTP Shell", KeyEvent.VK_G, KeyEvent.VK_F9, 0,
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
