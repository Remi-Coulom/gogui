// Help.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
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
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.util.Platform;

/** Window for displaying help in HTML format.
    The window is a JFrame on all platforms but the Mac, where it is a
    parent-less JDialog (to avoid the brushed-metal look, which shouldn't be
    used for the help window) */
public class Help
    implements ActionListener, HyperlinkListener
{
    public Help(URL contents, MessageDialogs messageDialogs,
                String title)
    {
        m_messageDialogs = messageDialogs;
        m_contents = contents;
        Container contentPane;
        if (Platform.isMac())
        {
            JDialog dialog = new JDialog((Frame)null, title);
            contentPane = dialog.getContentPane();
            m_window = dialog;
        }
        else
        {
            JFrame frame = new JFrame(title);
            GuiUtil.setGoIcon(frame);
            contentPane = frame.getContentPane();
            m_window = frame;
        }
        JPanel panel = new JPanel(new BorderLayout());
        contentPane.add(panel);
        panel.add(createButtons(), BorderLayout.NORTH);
        m_editorPane = new JEditorPane();
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

    private JButton m_buttonBack;

    private JButton m_buttonForward;

    private final JEditorPane m_editorPane;

    private java.util.List<URL> m_history = new ArrayList<URL>();

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
        assert m_historyIndex > 0;
        assert m_historyIndex < m_history.size();
        --m_historyIndex;
        loadURL(getHistory(m_historyIndex));
        historyChanged();
    }

    private JComponent createButtons()
    {
        JToolBar toolBar = new JToolBar();
        toolBar.add(createToolBarButton("go-home", "contents", "TT_HELP_TOC"));
        m_buttonBack = createToolBarButton("go-previous", "back",
                                           "TT_HELP_BACK");
        toolBar.add(m_buttonBack);
        m_buttonForward = createToolBarButton("go-next", "forward",
                                              "TT_HELP_FORWARD");
        toolBar.add(m_buttonForward);
        if (! Platform.isMac())
            toolBar.setRollover(true);
        toolBar.setFloatable(false);
        return toolBar;
    }

    private JButton createToolBarButton(String icon, String command,
                                        String toolTip)
    {
        JButton button = new JButton();
        button.setActionCommand(command);
        button.setToolTipText(i18n(toolTip));
        button.addActionListener(this);
        button.setIcon(GuiUtil.getIcon(icon, command));
        button.setFocusable(false);
        return button;
    }

    private void forward()
    {
        assert m_historyIndex + 1 < m_history.size();
        ++m_historyIndex;
        loadURL(getHistory(m_historyIndex));
        historyChanged();
    }

    private URL getHistory(int index)
    {
        return m_history.get(index);
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
            String mainMessage =
                MessageFormat.format("MSG_HELP_LOAD_FAILURE", url.toString());
            m_messageDialogs.showError(m_window, mainMessage, e.getMessage(),
                                       false);
        }
    }

    /** Open URL in external browser if possible.
        If it doesn't work, the URL is opened inside this dialog. */
    private void openExternal(URL url)
    {
        if (! Platform.openInExternalBrowser(url))
            loadURL(url);
        appendHistory(url);
    }
}
