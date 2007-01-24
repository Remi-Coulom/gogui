//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.util.Platform;

/** Status bar. */
public class StatusBar
    extends JPanel
{
    public StatusBar(boolean showToPlay)
    {
        super(new BorderLayout());
        JPanel outerPanel = new JPanel(new BorderLayout());
        add(outerPanel, BorderLayout.CENTER);
        if (Platform.isMac())
        {
            // Workaround for Java 1.4.1 on Mac OS X: add some empty space
            // so that status bar does not overlap the window resize widget
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            outerPanel.add(filler, BorderLayout.EAST);
        }
        JPanel panel = new JPanel(new BorderLayout());
        outerPanel.add(panel, BorderLayout.CENTER);
        m_iconBox = Box.createHorizontalBox();
        panel.add(m_iconBox, BorderLayout.WEST);
        m_showProgress = false;
        m_showToPlay = showToPlay;
        m_toPlayLabel = new JLabel();
        m_toPlayLabel.setBorder(UIManager.getBorder("TextField.border"));
        m_toPlayLabel.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                                   Short.MAX_VALUE));
        setToPlay(GoColor.BLACK);
        m_progressBar = new JProgressBar();
        initIconBox();
        m_text = new JTextField();
        m_text.setEditable(false);
        m_text.setFocusable(false);
        panel.add(m_text, BorderLayout.CENTER);
        Box moveTextBox = Box.createHorizontalBox();
        panel.add(moveTextBox, BorderLayout.EAST);
        m_moveText = new JTextField(12);
        m_moveText.setEditable(false);
        m_moveText.setFocusable(false);
        m_moveText.setHorizontalAlignment(SwingConstants.LEFT);
        moveTextBox.add(GuiUtil.createSmallFiller());
        moveTextBox.add(m_moveText);
    }

    public void clear()
    {
        setText("");
    }

    public void clearProgress()
    {
        if (m_showProgress)
        {
            m_showProgress = false;
            initIconBox();
        }
    }

    public String getText()
    {
        return m_text.getText();
    }


    public void immediatelyPaintMoveText(String text)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_moveText.setText(text);
        m_moveText.paintImmediately(m_moveText.getVisibleRect());
    }

    public boolean isProgressShown()
    {
        return m_showProgress;
    }

    /** Show progress bar.
        @param percent Percentage between 0 and 100, -1 if unknown.
    */
    public void setProgress(int percent)
    {
        if (! m_showProgress)
        {
            m_showProgress = true;
            initIconBox();
        }
        if (percent < 0)
        {
            // First set to minimum to reset indeterminate animation
            m_progressBar.setIndeterminate(false);
            m_progressBar.setValue(m_progressBar.getMinimum());

            m_progressBar.setIndeterminate(true);
            m_progressBar.setStringPainted(true);
            m_progressBar.setString("Thinking");
        }
        else
        {
            m_progressBar.setIndeterminate(false);
            m_progressBar.setValue(percent);
            m_progressBar.setStringPainted(true);
            m_progressBar.setString(null);
        }
    }

    /** Set text with move information.
        This text is displayed right and contains e.g. information about
        the last move, current move number etc.
    */
    public void setMoveText(String text, String toolTip)
    {
        if (text.length() > 18)
            text = text.substring(0, 18) + " ...";
        m_moveText.setText(text);
        m_moveText.setToolTipText(toolTip);
    }

    public void setText(String text)
    {
        m_text.setText(text);
    }

    public void setToPlay(GoColor color)
    {
        if (color == GoColor.BLACK)
        {
            m_toPlayLabel.setIcon(m_iconBlack);
            m_toPlayLabel.setToolTipText("Black to play");
        }
        else
        {
            assert(color == GoColor.WHITE);
            m_toPlayLabel.setIcon(m_iconWhite);
            m_toPlayLabel.setToolTipText("White to play");
        }
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private boolean m_showProgress;

    private boolean m_showToPlay;

    private static final Icon m_iconBlack =
        GuiUtil.getIcon("gogui-black", "Black");

    private static final Icon m_iconWhite =
        GuiUtil.getIcon("gogui-white", "White");

    private Box m_iconBox;

    private final JLabel m_toPlayLabel;

    private final JProgressBar m_progressBar;

    private final JTextField m_moveText;

    private final JTextField m_text;

    private void initIconBox()
    {
        m_iconBox.removeAll();
        if (m_showToPlay)
        {
            m_iconBox.add(m_toPlayLabel);
            m_iconBox.add(GuiUtil.createSmallFiller());
        }
        if (m_showProgress)
        {
            m_iconBox.add(m_progressBar);
            m_iconBox.add(GuiUtil.createSmallFiller());
        }
        m_iconBox.revalidate();
    }
}

