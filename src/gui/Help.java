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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

//-----------------------------------------------------------------------------

class AntialiasingEditorPane
    extends JEditorPane
{
    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(graphics);
    }
}

//-----------------------------------------------------------------------------

class Help
    extends JDialog
    implements ActionListener, HyperlinkListener
{
    public Help(Frame owner, URL contents)
    {
        super(owner, "GoGui: Help");
        m_contents = contents;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        createMenu();
        JPanel panel = new JPanel(new BorderLayout());
        contentPane.add(panel);
        panel.add(createButtons(), BorderLayout.NORTH);
        m_editorPane = new AntialiasingEditorPane();
        m_editorPane.setEditable(false);
        m_editorPane.addHyperlinkListener(this);
        JScrollPane scrollPane =
            new JScrollPane(m_editorPane,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(600, 600));
        panel.add(scrollPane, BorderLayout.CENTER);
        pack();
        setVisible(true);
        loadURL(m_contents);
        appendHistory(m_contents);
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("back"))
            back();
        else if (command.equals("close"))
            dispose();
        else if (command.equals("contents"))
        {
            loadURL(m_contents);
            appendHistory(m_contents);
        }
        else if (command.equals("forward"))
            forward();
    }

    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            if (e instanceof HTMLFrameHyperlinkEvent)
            {
                HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
                HTMLDocument doc = (HTMLDocument)m_editorPane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            }
            else
            {
                URL url = e.getURL();
                if (url.getProtocol().equals("jar"))
                {
                    loadURL(url);
                    appendHistory(url);
                }
                else
                    openExternal(url);
            }
        }
    }

    private int m_historyIndex = -1;

    private JButton m_buttonBack;

    private JButton m_buttonContents;

    private JButton m_buttonForward;

    private JEditorPane m_editorPane;

    private java.util.List m_history = new ArrayList();

    private JMenuItem m_itemBack;

    private JMenuItem m_itemContents;

    private JMenuItem m_itemForward;

    private URL m_contents;

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

    private void appendHistory(URL url)
    {
        if (m_historyIndex >= 0 && getHistory(m_historyIndex).equals(url))
            return;
        if (m_historyIndex + 1 < m_history.size())
        {
            if (! getHistory(m_historyIndex + 1).equals(url))
            {
                m_history = m_history.subList(0, m_historyIndex + 1);
                m_history.add(url);
            }
        }
        else
            m_history.add(url);
        ++m_historyIndex;
        historyChanged();
    }

    private void back()
    {
        assert(m_historyIndex > 0);
        assert(m_historyIndex < m_history.size());
        --m_historyIndex;
        loadURL(getHistory(m_historyIndex));
        historyChanged();
    }

    private void createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    ActionEvent.CTRL_MASK, "close");
        menuBar.add(menu);
        menu = new JMenu("Go");
        menu.setMnemonic(KeyEvent.VK_G);
        m_itemBack = addMenuItem(menu, "Back", KeyEvent.VK_B, KeyEvent.VK_B,
                                 ActionEvent.CTRL_MASK, "back");
        m_itemForward = addMenuItem(menu, "Forward", KeyEvent.VK_F,
                                    KeyEvent.VK_F, ActionEvent.CTRL_MASK,
                                    "forward");
        m_itemContents = addMenuItem(menu, "Contents", KeyEvent.VK_C,
                                     KeyEvent.VK_E, ActionEvent.CTRL_MASK,
                                     "contents");
        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    private JComponent createButtons()
    {
        JToolBar toolBar = new JToolBar();
        m_buttonBack = createToolBarButton("back.png", "back", "Back");
        toolBar.add(m_buttonBack);
        m_buttonForward = createToolBarButton("forward.png", "forward",
                                              "Forward");
        toolBar.add(m_buttonForward);
        m_buttonContents = createToolBarButton("gohome.png", "contents",
                                               "Contents");
        toolBar.add(m_buttonContents);
        return toolBar;
    }

    private JButton createToolBarButton(String icon, String command,
                                        String toolTip)
    {
        JButton button =
            new ToolBarButton("images/" + icon, "[" + command + "]", toolTip);
        button.setActionCommand(command);
        button.addActionListener(this);
        return button;
    }

    private void forward()
    {
        assert(m_historyIndex + 1 < m_history.size());
        ++m_historyIndex;
        loadURL(getHistory(m_historyIndex));
        historyChanged();
    }

    private URL getHistory(int index)
    {
        return (URL)m_history.get(index);
    }

    private void historyChanged()
    {        
        URL currentUrl = getHistory(m_historyIndex);
        boolean backPossible = (m_historyIndex > 0);
        boolean forwardPossible = (m_historyIndex < m_history.size() - 1);
        boolean contentsPossible = (! currentUrl.equals(m_contents));
        m_buttonBack.setEnabled(backPossible);
        m_buttonForward.setEnabled(forwardPossible);
        m_buttonContents.setEnabled(contentsPossible);
        m_itemBack.setEnabled(backPossible);
        m_itemForward.setEnabled(forwardPossible);
        m_itemContents.setEnabled(contentsPossible);
    }

    private void loadURL(URL url)
    {
        try
        {
            m_editorPane.setPage(url);
        }
        catch (IOException e)
        {
            String message =
                "Could not load page\n" +
                url.toString() + ":\n" +
                e.getMessage();
            JOptionPane.showMessageDialog(this, message, "GoGui: Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Open URL in external browser if possible.
        Supported on KDE and Windows.
        If it doesn't work, the URL is opened inside this dialog.
    */
    private void openExternal(URL url)
    {
        Runtime runtime = Runtime.getRuntime();
        try
        {
            runtime.exec("kfmclient exec " + url);
            return;
        }
        catch (Exception e)
        {
        }
        try
        {
            runtime.exec("rundll32 url.dll,FileProtocolHandler" + url);
        }
        catch (Exception e)
        {
        }
        loadURL(url);
        appendHistory(url);        
    }
}

//-----------------------------------------------------------------------------
