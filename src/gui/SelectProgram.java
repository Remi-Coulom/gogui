//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import utils.GuiUtils;
import utils.StringUtils;

//-----------------------------------------------------------------------------

class SelectProgram
    extends JDialog
    implements ActionListener
{
    public SelectProgram(Frame owner)
    {
        super(owner, "Select Program", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.add(createCommandPanel(), BorderLayout.CENTER);
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        pack();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("cancel"))
            dispose();
        else if (command.equals("ok"))
        {
            m_command = m_comboBox.getSelectedItem().toString();
            m_comboBox.insertItemAt(m_command, 0);
            saveHistory(getHistory());
            dispose();
        }
        else if (command.equals("open"))
            open();
    }

    public static void addHistory(String program)
    {
        program = program.trim();
        if (program.equals(""))
            return;
        String[] tokens = StringUtils.tokenize(program);
        if (tokens.length > 0)
        {
            try
            {
                File file = new File(tokens[0]);
                file = file.getCanonicalFile();
                if (file.exists())
                    program = file.toString()
                        + program.substring(tokens[0].length());
            }
            catch (IOException e)
            {
            }
        }
        Vector history = loadHistory();
        history.add(0, program);
        saveHistory(history);
    }

    public void keyPressed(KeyEvent e)
    {
        int c = e.getKeyCode();        
        if (c == KeyEvent.VK_ESCAPE)
        {
            if (! m_comboBox.isPopupVisible())
                dispose();
        }
    }

    public static String select(Frame owner)
    {
        SelectProgram dialog = new SelectProgram(owner);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return dialog.m_command;
    }

    private JComboBox m_comboBox;

    private JTextField m_textField;

    private String m_command;

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtils.PAD, 0));
        innerPanel.setBorder(GuiUtils.createEmptyBorder());
        JButton okButton = new JButton("Ok");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        okButton.setMnemonic(KeyEvent.VK_O);
        getRootPane().setDefaultButton(okButton);
        innerPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic(KeyEvent.VK_C);
        innerPanel.add(cancelButton);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private JPanel createCommandPanel()
    {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(GuiUtils.createEmptyBorder());
        JLabel label = new JLabel("Go Program Command");
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        panel.add(createInputPanel());
        return panel;
    }

    private JPanel createInputPanel()
    {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel(new BorderLayout());
        m_comboBox = new JComboBox(loadHistory());
        StringBuffer prototype = new StringBuffer(70);
        for (int i = 0; i < 70; ++i)
            prototype.append('-');
        m_comboBox.setPrototypeDisplayValue(prototype.toString());
        m_comboBox.setEditable(true);
        ComboBoxEditor editor = m_comboBox.getEditor();
        m_textField = (JTextField)editor.getEditorComponent();
        m_textField.setColumns(40);
        m_textField.selectAll();
        KeyListener keyListener = new KeyAdapter()
            {
                public void keyPressed(KeyEvent e)
                {
                    int c = e.getKeyCode();        
                    if (c == KeyEvent.VK_ESCAPE)
                    {
                        if (! m_comboBox.isPopupVisible())
                            dispose();
                    }
                }
            };
        m_textField.addKeyListener(keyListener);
        int fontSize = GuiUtils.getDefaultMonoFontSize();
        m_comboBox.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        innerPanel.add(m_comboBox, BorderLayout.CENTER);
        JButton button =
            new ImageButton("images/fileopen.png", "Browse",
                            "Browse for Go program");
        button.setActionCommand("open");
        button.addActionListener(this);
        innerPanel.add(button, BorderLayout.EAST);
        outerPanel.add(innerPanel, BorderLayout.NORTH);
        return outerPanel;
    }

    private Vector getHistory()
    {
        Vector result = new Vector(32, 32);
        int maxHistory = 20;
        int itemCount = m_comboBox.getItemCount();
        int n = itemCount;
        if (n > maxHistory)
            n = maxHistory;
        for (int i = 0; i < n; ++i)
            result.add(m_comboBox.getItemAt(i).toString().trim());
        return result;
    }

    private static File getHistoryFile()
    {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".gogui");
        if (! dir.exists())
            dir.mkdir();
        return new File(dir, "program-history");
    }

    private static Vector loadHistory()
    {
        Vector result = new Vector(32, 32);
        File file = getHistoryFile();
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(file));
            try
            {
                String line = in.readLine();
                while (line != null)
                {
                    line = line.trim();
                    if (! result.contains(line))
                        result.add(line);
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
        if (! result.contains("gnugo --mode gtp"))
            result.add("gnugo --mode gtp");
        return result;
    }

    private void open()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();
        m_comboBox.insertItemAt(file.toString(), 0);
        m_comboBox.setSelectedIndex(0);
        m_textField.setCaretPosition(m_textField.getText().length());
        m_textField.requestFocusInWindow();
    }

    private static void saveHistory(Vector history)
    {
        File file = getHistoryFile();
        try
        {
            PrintWriter out = new PrintWriter(new FileOutputStream(file));
            int size = history.size();
            for (int i = 0; i < size; ++i)
            {
                String s = (String)history.get(i);
                if (! s.equals(""))
                    out.println(s);
            }
            out.close();
        }
        catch (FileNotFoundException e)
        {
        }

    }
}

//-----------------------------------------------------------------------------
