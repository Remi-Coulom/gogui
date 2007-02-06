//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.util.PrefUtil;

/** Dialog for selecting a Go engine. */
public class SelectProgram
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
            putHistory();
            dispose();
        }
        else if (command.equals("open"))
            open();
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
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return dialog.m_command;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JComboBox m_comboBox;

    private JTextField m_textField;

    private String m_command;

    private JPanel createButtons()
    {
        JPanel innerPanel = new JPanel(new GridLayout(1, 0, GuiUtil.PAD, 0));
        innerPanel.setBorder(GuiUtil.createEmptyBorder());
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
        panel.setBorder(GuiUtil.createEmptyBorder());
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
        m_comboBox = new JComboBox(getHistory().toArray());
        StringBuffer prototype = new StringBuffer(65);
        for (int i = 0; i < 65; ++i)
            prototype.append('-');
        m_comboBox.setPrototypeDisplayValue(prototype.toString());
        m_comboBox.setEditable(true);
        ComboBoxEditor editor = m_comboBox.getEditor();
        m_textField = (JTextField)editor.getEditorComponent();
        //m_textField.setColumns(40);
        m_textField.selectAll();
        KeyListener keyListener = new KeyAdapter()
            {
                public void keyPressed(KeyEvent e)
                {
                    int c = e.getKeyCode();
                    if (c == KeyEvent.VK_ESCAPE
                        && ! m_comboBox.isPopupVisible())
                        dispose();
                }
            };
        m_textField.addKeyListener(keyListener);
        innerPanel.add(m_comboBox, BorderLayout.CENTER);
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(GuiUtil.createSmallFiller());
        JButton button = new JButton();
        buttonBox.add(button);
        button.setIcon(GuiUtil.getIcon("document-open", "Browse"));
        button.setToolTipText("Browse for Go program");
        button.setActionCommand("open");
        button.addActionListener(this);
        innerPanel.add(buttonBox, BorderLayout.EAST);
        outerPanel.add(innerPanel, BorderLayout.NORTH);
        return outerPanel;
    }

    private static ArrayList getHistory()
    {
        ArrayList result =
            PrefUtil.getList("net/sf/gogui/gui/selectprogram");
        if (! result.contains("gnugo --mode gtp"))
            result.add("gnugo --mode gtp");
        return result;
    }

    private void open()
    {
        File file = SimpleDialogs.showOpen(this, "Select Go Program");
        if (file == null)
            return;
        String text = file.toString();
        if (text.indexOf(' ') >= 0)
            text = "\"" + text + "\"";        
        if (file.getName().toLowerCase(Locale.ENGLISH).startsWith("gnugo"))
        {
            String message = "Append option '--mode gtp' for GNU Go?";
            if (SimpleDialogs.showQuestion(this, message))
                text = text + " --mode gtp";
        }
        m_comboBox.insertItemAt(text, 0);
        m_comboBox.setSelectedIndex(0);
        m_textField.setCaretPosition(m_textField.getText().length());
        m_textField.requestFocusInWindow();
    }

    private void putHistory()
    {
        ArrayList history = new ArrayList(32);
        int maxHistory = 20;
        int itemCount = m_comboBox.getItemCount();
        int n = itemCount;
        if (n > maxHistory)
            n = maxHistory;
        for (int i = 0; i < n; ++i)
        {
            String element = m_comboBox.getItemAt(i).toString().trim();
            if (! history.contains(element))
                history.add(element);
        }
        PrefUtil.putList("net/sf/gogui/gui/selectprogram", history);
    }
}

