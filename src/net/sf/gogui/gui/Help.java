//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import net.sf.gogui.util.Platform;

class AntialiasingEditorPane
    extends JEditorPane
{
    public void paintComponent(Graphics graphics)
    {
        GuiUtil.setAntiAlias(graphics);
        super.paintComponent(graphics);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}

/** Window for displaying help in HTML format.
    The window is a JFrame on all platforms but the Mac, where it is a
    parent-less JDialog (to avoid the brushed-metal look, which shouldn't be
    used for the help window)
 */
public class Help
    implements ActionListener, HyperlinkListener
{
    public Help(URL contents, MessageDialogs messageDialogs)
    {
        m_messageDialogs = messageDialogs;
        m_contents = contents;
        String title = "Documentation - GoGui";
        Container contentPane;
        if (Platform.isMac())
        {
            m_window = new JDialog((Frame)null, title);
            contentPane = ((JDialog)m_window).getContentPane();
        }
        else
        {
            m_window = new JFrame(title);
            GuiUtil.setGoIcon((Frame)m_window);
            contentPane = ((JFrame)m_window).getContentPane();
        }
        JPanel panel = new JPanel(new BorderLayout());
        contentPane.add(panel);
        panel.add(createButtons(), BorderLayout.NORTH);
        m_editorPane = new AntialiasingEditorPane();
        m_editorPane.setEditable(false);
        m_editorPane.addHyperlinkListener(this);
        int width = GuiUtil.getDefaultMonoFontSize() * 50;
        int height = GuiUtil.getDefaultMonoFontSize() * 60;
        JScrollPane scrollPane =
            new JScrollPane(m_editorPane,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (Platform.isMac())
            // Default Apple L&F uses no border, but Quaqua 3.7.4 does
            scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(width, height));
        panel.add(scrollPane, BorderLayout.CENTER);
        m_window.pack();
        loadURL(m_contents);
        appendHistory(m_contents);
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("back"))
            back();
        else if (command.equals("close"))
            m_window.setVisible(false);
        else if (command.equals("contents"))
        {
            loadURL(m_contents);
            appendHistory(m_contents);
        }
        else if (command.equals("forward"))
            forward();
    }

    public Window getWindow()
    {
        return m_window;
    }

    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            URL url = e.getURL();
            String protocol = url.getProtocol();
            if (protocol.equals("jar") || protocol.equals("file"))
            {
                loadURL(url);
                appendHistory(url);
            }
            else
                openExternal(url);
        }
    }

    private int m_historyIndex = -1;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JButton m_buttonBack;

    private JButton m_buttonForward;

    private final JEditorPane m_editorPane;

    private java.util.List m_history = new ArrayList();

    private final URL m_contents;

    private final MessageDialogs m_messageDialogs;

    private final Window m_window;

    private void appendHistory(URL url)
    {
        if (m_historyIndex >= 0 && historyEquals(m_historyIndex, url))
            return;
        if (m_historyIndex + 1 < m_history.size())
        {
            if (! historyEquals(m_historyIndex + 1, url))
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

    private JComponent createButtons()
    {
        JToolBar toolBar = new JToolBar();
        toolBar.add(createToolBarButton("go-home", "contents",
                                        "Table of Contents"));
        m_buttonBack = createToolBarButton("go-previous", "back", "Back");
        toolBar.add(m_buttonBack);
        m_buttonForward = createToolBarButton("go-next", "forward", "Forward");
        toolBar.add(m_buttonForward);
        if (! Platform.isMac())
            toolBar.setRollover(true);
        toolBar.setFloatable(false);
        // For com.jgoodies.looks
        toolBar.putClientProperty("jgoodies.headerStyle", "Single");
        return toolBar;
    }

    private JButton createToolBarButton(String icon, String command,
                                        String toolTip)
    {
        JButton button = new JButton();
        button.setActionCommand(command);
        button.setToolTipText(toolTip);
        button.addActionListener(this);
        button.setIcon(GuiUtil.getIcon(icon, command));
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
        boolean backPossible = (m_historyIndex > 0);
        boolean forwardPossible = (m_historyIndex < m_history.size() - 1);
        m_buttonBack.setEnabled(backPossible);
        m_buttonForward.setEnabled(forwardPossible);
    }

    private boolean historyEquals(int index, URL url)
    {
        // Compare as strings to avoid Findbugs warning about potentially
        // blocking URL.equals()
        return getHistory(index).toString().equals(url.toString());
    }

    private void loadURL(URL url)
    {
        try
        {
            m_editorPane.setPage(url);
        }
        catch (IOException e)
        {
            m_messageDialogs.showError(m_window,
                                       "Could not load page\n" +
                                       url.toString(), e.getMessage(), false);
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

