//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import utils.GuiUtils;
import utils.StringUtils;

//----------------------------------------------------------------------------

public class FindDialog
    extends JDialog
    implements ActionListener
{
    public FindDialog(Frame owner, String initialValue)
    {
        super(owner, "Find", true);
        m_initialValue = initialValue;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.add(createPanel(), BorderLayout.CENTER);
        contentPane.add(createButtons(), BorderLayout.SOUTH);
        pack();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("cancel"))
            dispose();
        else if (command.equals("comboBoxEdited") || command.equals("find"))
        {
            m_pattern = m_comboBox.getSelectedItem().toString();
            m_comboBox.insertItemAt(m_pattern, 0);
            saveHistory(getHistory());
            dispose();
        }
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

    public static String run(Frame owner, String initialValue)
    {
        FindDialog dialog = new FindDialog(owner, initialValue);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return dialog.m_pattern;
    }

    private JComboBox m_comboBox;

    private JTextField m_textField;

    private String m_initialValue;

    private String m_pattern;

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtils.PAD, 0));
        innerPanel.setBorder(GuiUtils.createEmptyBorder());
        JButton findButton = new JButton("Find");
        findButton.setActionCommand("find");
        findButton.addActionListener(this);
        findButton.setMnemonic(KeyEvent.VK_F);
        getRootPane().setDefaultButton(findButton);
        innerPanel.add(findButton);
        JButton cancelButton = new JButton("Cancel");
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
        panel.setBorder(GuiUtils.createEmptyBorder());
        JLabel label = new JLabel("Search Pattern");
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
        return new File(dir, "find-history");
    }

    private Vector loadHistory()
    {
        Vector result = new Vector(32, 32);
        if (m_initialValue != null)
            result.add(m_initialValue);
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
        return result;
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

//----------------------------------------------------------------------------
