//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import utils.GuiUtils;
import utils.Platform;
import utils.StreamCopy;

//----------------------------------------------------------------------------

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

//----------------------------------------------------------------------------

public class Help
    extends JFrame
    implements ActionListener, HyperlinkListener
{
    public Help(URL contents)
    {
        super("Help - GoGui");
        GuiUtils.setGoIcon(this);
        m_contents = contents;
        Container contentPane = getContentPane();
        createMenuBar();
        JPanel panel = new JPanel(new BorderLayout());
        contentPane.add(panel);
        panel.add(createButtons(), BorderLayout.NORTH);
        m_editorPane = new AntialiasingEditorPane();
        m_editorPane.setEditable(false);
        m_editorPane.addHyperlinkListener(this);
        int width = GuiUtils.getDefaultMonoFontSize() * 50;
        int height = GuiUtils.getDefaultMonoFontSize() * 60;
        JScrollPane scrollPane =
            new JScrollPane(m_editorPane,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(width, height));
        panel.add(scrollPane, BorderLayout.CENTER);
        pack();
        loadURL(m_contents);
        appendHistory(m_contents);
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("back"))
            back();
        else if (command.equals("close"))
            setVisible(false);
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

    public void toTop()
    {
        setState(Frame.NORMAL);
        setVisible(true);
        toFront();
    }

    private int m_historyIndex = -1;

    private static final int m_shortcutKeyMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

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

    private JMenu createMenu(String name, int mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private void createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = createMenu("File", KeyEvent.VK_F);
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    m_shortcutKeyMask, "close");
        menuBar.add(menu);
        menu = createMenu("Go", KeyEvent.VK_G);
        m_itemBack = addMenuItem(menu, "Back", KeyEvent.VK_B, KeyEvent.VK_B,
                                 m_shortcutKeyMask, "back");
        m_itemForward = addMenuItem(menu, "Forward", KeyEvent.VK_F,
                                    KeyEvent.VK_F, m_shortcutKeyMask,
                                    "forward");
        m_itemContents = addMenuItem(menu, "Contents", KeyEvent.VK_C,
                                     KeyEvent.VK_E, m_shortcutKeyMask,
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
        JButton button = new JButton();
        button.setActionCommand(command);
        button.setToolTipText(toolTip);
        button.addActionListener(this);
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource("images/" + icon);
        if (url != null)
            button.setIcon(new ImageIcon(url, command));
        else
            button.setText(command);
        button.setFocusable(false);
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
            SimpleDialogs.showError(this,
                                    "Could not load page\n" +
                                    url.toString() + ":\n" +
                                    e.getMessage());
        }
    }

    /** Open URL in external browser if possible.
        If it doesn't work, the URL is opened inside this dialog.
    */
    private void openExternal(URL url)
    {
        if (! Platform.openInExternalBrowser(url))
            loadURL(url);
        appendHistory(url);        
    }
}

//----------------------------------------------------------------------------
