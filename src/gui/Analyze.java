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

    public static final int STRINGBOARD = 4;

    public static final int COLORBOARD = 5;

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

    public static AnalyzeCommand get(String label)
    {
        Vector commands = new Vector(128, 128);
        Vector labels = new Vector(128, 128);
        read(commands, labels);
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

    public String getResultTitle(go.Point pointArg)
    {
        StringBuffer buffer = new StringBuffer(m_label);
        if (pointArg != null)
        {
            buffer.append(" ");
            buffer.append(pointArg.toString());
        }
        return buffer.toString();
    }

    public boolean needsPointArg()
    {
        return (m_command.indexOf("%p") >= 0);
    }

    public static void read(Vector commands, Vector labels)
    {
        commands.clear();
        labels.clear();
        try
        {
            File file = getFile();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null)
            {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#')
                {
                    String array[] = StringUtils.split(line, '/');
                    if (array.length < 3)
                        break;
                    String label = array[1];
                    if (labels.contains(label))
                        continue;
                    labels.add(label);
                    commands.add(line);
                }                
            }
        }
        catch (IOException e)
        {
        }
    }

    public String replaceWildCards(go.Color toMove, go.Point pointArg)
    {
        StringBuffer buffer = new StringBuffer(m_command);
        StringUtils.replace(buffer, "%m", toMove.toString());
        if (needsPointArg())
        {
            assert(pointArg != null);
            StringUtils.replace(buffer, "%p", pointArg.toString());
        }
        return buffer.toString();
    }

    private int m_type;

    private String m_label;

    private String m_command;

    private String m_title;

    private double m_scale;

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

    public void initAnalyzeCommand(AnalyzeCommand command);

    public void setAnalyzeCommand(AnalyzeCommand command);


}

//-----------------------------------------------------------------------------

class AnalyzeDialog
    extends JDialog
    implements ActionListener, KeyListener, MouseListener, WindowListener
{
    public AnalyzeDialog(Frame owner, AnalyzeCallback callback,
                         Preferences prefs)
    {
        super(owner, "GoGui: Analyze command");
        m_prefs = prefs;
        m_callback = callback;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addKeyListener(this);
        addWindowListener(this);
        Container contentPane = getContentPane();
        contentPane.add(createCommandPanel(), BorderLayout.CENTER);
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        createMenu();
        pack();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("close"))
            close();
        else if (command.equals("comboBoxChanged"))
            comboBoxChanged();
        else if (command.equals("run"))
            setCommand();
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e) 
    {
        int code = e.getKeyCode();        
        Object source = e.getSource();
        if (source == m_list && code == KeyEvent.VK_ENTER)
        {
            int index = m_list.getSelectedIndex();
            selectCommand(index);
        }
    }

    public void keyTyped(KeyEvent e)
    {
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
        if (m_comboBox.getItemCount() > 0)
            m_comboBox.requestFocus();
        else
            m_list.requestFocus();
        setVisible(true);
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

    private JComboBox m_comboBox;

    private JList m_list;

    private Vector m_commands = new Vector(128, 64);

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

    private void close()
    {
        m_callback.clearAnalyzeCommand();
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
        JButton runButton = new JButton("Run");
        runButton.setActionCommand("run");
        runButton.addActionListener(this);
        runButton.setMnemonic(KeyEvent.VK_R);
        getRootPane().setDefaultButton(runButton);
        innerPanel.add(runButton);
        JButton closeButton = new JButton("Close");
        closeButton.setActionCommand("close");
        closeButton.addActionListener(this);
        closeButton.setMnemonic(KeyEvent.VK_C);
        innerPanel.add(closeButton);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private JPanel createCommandPanel()
    {
        AnalyzeCommand.read(m_commands, m_labels);
        JPanel panel = new JPanel(new BorderLayout());
        m_list = new JList(m_labels);
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setVisibleRowCount(20);
        m_list.addMouseListener(this);
        JScrollPane scrollPane = new JScrollPane(m_list);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(createLowerPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLowerPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, GuiUtils.PAD, 0));
        m_comboBox = new JComboBox();
        m_comboBox.addActionListener(this);
        panel.add(m_comboBox);
        loadRecent();
        return panel;
    }

    private void createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createMenuFile());
        setJMenuBar(menuBar);
    }

    private JMenu createMenuFile()
    {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    ActionEvent.CTRL_MASK, "close");
        return menu;
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
    }

    private void setCommand()
    {
        String label = (String)m_comboBox.getSelectedItem();        
        int index = m_labels.indexOf(label);
        if (index < 0)
            return;
        String analyzeCommand = (String)m_commands.get(index);
        AnalyzeCommand command = new AnalyzeCommand(analyzeCommand);
        m_callback.setAnalyzeCommand(command);
    }
}

//-----------------------------------------------------------------------------
