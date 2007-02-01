//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
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
        panel.setBorder(BorderFactory.createLineBorder(Color.gray));
        outerPanel.add(panel, BorderLayout.CENTER);
        m_iconBox = Box.createHorizontalBox();
        panel.add(m_iconBox, BorderLayout.WEST);
        m_showToPlay = showToPlay;
        m_toPlayLabel = new JLabel();
        m_toPlayLabel.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                                   Short.MAX_VALUE));
        setToPlay(GoColor.BLACK);
        m_iconBox.add(m_toPlayLabel);
        m_labelScore
            = new JLabel(GuiUtil.getIcon("gogui-score", "Score"));
        m_labelScore.setToolTipText("Score mode");
        m_labelScore.setVisible(false);
        m_iconBox.add(m_labelScore);

        m_labelSetup
            = new JLabel(GuiUtil.getIcon("gogui-setup-16x16", "Setup"));
        m_labelSetup.setVisible(false);
        m_labelSetup.setToolTipText("Setup mode");
        m_iconBox.add(m_labelSetup);
        m_distanceSetup = GuiUtil.createFiller();
        m_distanceSetup.setVisible(false);
        m_iconBox.add(m_distanceSetup);

        m_progressBar = new JProgressBar();
        m_progressBar.setVisible(false);
        m_progressBar.setPreferredSize(new Dimension(72, 16));
        m_iconBox.add(m_progressBar);
        m_progressBarDistance = GuiUtil.createFiller();
        m_progressBarDistance.setVisible(false);
        m_iconBox.add(m_progressBarDistance);
        m_iconBox.add(GuiUtil.createSmallFiller());

        m_text = new TextFieldWithToolTip();
        m_text.setEditable(false);
        m_text.setFocusable(false);
        m_text.setBorder(null);
        panel.add(m_text, BorderLayout.CENTER);
        Box moveTextBox = Box.createHorizontalBox();
        panel.add(moveTextBox, BorderLayout.EAST);
        m_moveText = new JTextField(12);
        m_moveText.setEditable(false);
        m_moveText.setFocusable(false);
        m_moveText.setBorder(null);
        m_moveText.setHorizontalAlignment(SwingConstants.LEFT);
        m_moveTextSeparator = new JSeparator(SwingConstants.VERTICAL);
        m_moveTextSeparator.setVisible(false);
        moveTextBox.add(m_moveTextSeparator);
        moveTextBox.add(GuiUtil.createSmallFiller());
        moveTextBox.add(m_moveText);
    }

    public void clear()
    {
        setText("");
    }

    public void clearProgress()
    {
        m_progressBar.setVisible(false);
        m_progressBarDistance.setVisible(false);
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
        return m_progressBar.isVisible();
    }

    /** Show progress bar.
        @param percent Percentage between 0 and 100, -1 if unknown.
    */
    public void setProgress(int percent)
    {
        m_progressBar.setVisible(true);
        m_progressBarDistance.setVisible(true);
        // Don't use text on progress bar, because otherwise it will
        // be green in indetemrminate and blue in dterminate mode on
        // Windows XP JDK 1.6
        if (percent < 0)
        {
            // First set to minimum to reset indeterminate animation
            m_progressBar.setIndeterminate(false);
            m_progressBar.setValue(m_progressBar.getMinimum());
            m_progressBar.setIndeterminate(true);
        }
        else
        {
            m_progressBar.setIndeterminate(false);
            m_progressBar.setValue(percent);
            m_progressBar.setStringPainted(false);
        }
    }

    /** Set text with move information.
        This text is displayed right and contains e.g. information about
        the last move, current move number etc.
    */
    public void setMoveText(String text, String toolTip)
    {
        if (text == null || text.trim().equals(""))
        {
            m_moveText.setText("");
            m_moveTextSeparator.setVisible(false);
            return;
        }
        m_moveTextSeparator.setVisible(true);
        if (text.length() > 18)
            text = text.substring(0, 18) + " ...";
        m_moveText.setText(text);
        m_moveText.setToolTipText(toolTip);
    }

    public void setSetupMode(boolean enabled)
    {
        m_labelSetup.setVisible(enabled);
        m_distanceSetup.setVisible(enabled);
    }

    public void setScoreMode(boolean enabled)
    {
        m_labelScore.setVisible(enabled);
        m_toPlayLabel.setVisible(! enabled);
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

    private boolean m_showToPlay;

    private static final Icon m_iconBlack =
        GuiUtil.getIcon("gogui-black-16x16", "Black");

    private static final Icon m_iconWhite =
        GuiUtil.getIcon("gogui-white-16x16", "White");

    private Box m_iconBox;

    private final JLabel m_toPlayLabel;

    private final JLabel m_labelSetup;

    private final JLabel m_labelScore;

    private final JProgressBar m_progressBar;

    private final JTextField m_moveText;

    private final JTextField m_text;

    private final JSeparator m_moveTextSeparator;

    Box.Filler m_progressBarDistance;

    Box.Filler m_distanceSetup;
}

/** Text fiel with tool tip if text is truncated. */
class TextFieldWithToolTip
    extends JTextField
{
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        setToolTipText(null);
        FontMetrics metrics = g.getFontMetrics();
        String text = getText();
        if (text == null || metrics.stringWidth(text) < getWidth())
            return;
        setToolTipText(text);
    }
}
