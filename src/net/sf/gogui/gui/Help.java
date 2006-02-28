//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import net.sf.gogui.utils.Platform;

//----------------------------------------------------------------------------

class AntialiasingEditorPane
    extends JEditorPane
{
    public void paintComponent(Graphics graphics)
    {
        if (graphics instanceof Graphics2D)
        {
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(graphics);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID
}

//----------------------------------------------------------------------------

/** Dialog for displaying help in HTML format. */
public class Help
    extends JDialog
    implements ActionListener, HyperlinkListener
{
    public Help(Frame owner, URL contents)
    {
        super(owner, "Documentation - GoGui");
        m_contents = contents;
        Container contentPane = getContentPane();
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

    public void toTop()
    {
        setVisible(true);
        toFront();
    }

    private int m_historyIndex = -1;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JButton m_buttonBack;

    private JButton m_buttonForward;

    private final JEditorPane m_editorPane;

    private java.util.List m_history = new ArrayList();

    private final URL m_contents;

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

    private JComponent createButtons()
    {
        JToolBar toolBar = new JToolBar();
        toolBar.add(createToolBarButton("gohome.png", "contents",
                                        "Table of Contents"));
        m_buttonBack = createToolBarButton("back.png", "back", "Back");
        toolBar.add(m_buttonBack);
        m_buttonForward = createToolBarButton("forward.png", "forward",
                                              "Forward");
        toolBar.add(m_buttonForward);
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        // For com.jgoodies.looks.plastic.PlasticLookAndFeel
        toolBar.putClientProperty("Plastic.is3D", Boolean.TRUE);
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
        URL url = classLoader.getResource("net/sf/gogui/images/" + icon);
        if (url == null)
            button.setText(command);
        else
        {
            ImageIcon imageIcon = new ImageIcon(url, command);
            button.setIcon(imageIcon);
            button.setDisabledIcon(imageIcon);
        }
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
