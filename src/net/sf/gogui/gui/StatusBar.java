//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.util.Platform;

/** Status bar. */
public class StatusBar
    extends JPanel
{
    public StatusBar()
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
        m_toPlayLabel = new JLabel();
        m_toPlayLabel.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                                   Short.MAX_VALUE));
        setToPlay(BLACK);
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

        m_iconBox.add(GuiUtil.createSmallFiller());

        m_text = new TextFieldWithToolTip();
        m_text.setBorder(null);
        panel.add(m_text, BorderLayout.CENTER);
        Box moveTextBox = Box.createHorizontalBox();
        panel.add(moveTextBox, BorderLayout.EAST);
        m_moveText = new JTextField(12);
        GuiUtil.setEditableFalse(m_moveText);
        if (Platform.isMac())
        {
            m_moveText.setForeground(UIManager.getColor("Label.foreground"));
            m_moveText.setBackground(UIManager.getColor("Label.background"));
        }
        m_moveText.setBorder(null);
        m_moveText.setHorizontalAlignment(SwingConstants.LEFT);
        m_moveTextSeparator = new JSeparator(SwingConstants.VERTICAL);
        moveTextBox.add(m_moveTextSeparator);
        moveTextBox.add(GuiUtil.createSmallFiller());
        moveTextBox.add(m_moveText);
    }

    public void clear()
    {
        setText("");
    }

    public String getText()
    {
        return m_text.getText();
    }

    public void immediatelyPaintMoveText(String text)
    {
        assert SwingUtilities.isEventDispatchThread();
        setMoveText(text, null);
        GuiUtil.paintImmediately(m_moveText);
        GuiUtil.paintImmediately(m_moveTextSeparator);
    }

    public void immediatelyPaintText(String text)
    {
        assert SwingUtilities.isEventDispatchThread();
        setText(text);
        GuiUtil.paintImmediately(m_text);
    }

    /** Set text with move information.
        This text is displayed right and contains e.g. information about
        the last move, current move number etc.
    */
    public void setMoveText(String text, String toolTip)
    {
        if (text.length() > 18)
            text = text.substring(0, 18) + "...";
        m_moveText.setText(text);
        m_moveText.setToolTipText(toolTip);
    }

    public void setSetupMode(boolean enabled)
    {
        m_labelSetup.setVisible(enabled);
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

    public final void setToPlay(GoColor color)
    {
        if (color == BLACK)
        {
            m_toPlayLabel.setIcon(ICON_BLACK);
            m_toPlayLabel.setToolTipText("Black to play");
        }
        else
        {
            assert color == WHITE;
            m_toPlayLabel.setIcon(ICON_WHITE);
            m_toPlayLabel.setToolTipText("White to play");
        }
    }

    /** Hide or show the text field for move information.
        @see #setMoveText
    */
    public void showMoveText(boolean show)
    {
        m_moveText.setVisible(show);
        m_moveTextSeparator.setVisible(show);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private static final Icon ICON_BLACK =
        GuiUtil.getIcon("gogui-black-16x16", "Black");

    private static final Icon ICON_WHITE =
        GuiUtil.getIcon("gogui-white-16x16", "White");

    private final Box m_iconBox;

    private final JLabel m_toPlayLabel;

    private final JLabel m_labelSetup;

    private final JLabel m_labelScore;

    private final JTextField m_moveText;

    private final JTextField m_text;

    private final JSeparator m_moveTextSeparator;
}

/** Non-editable text field with tool tip if text is truncated. */
class TextFieldWithToolTip
    extends JTextField
{
    public TextFieldWithToolTip()
    {
        GuiUtil.setEditableFalse(this);
        if (Platform.isMac())
        {
            setForeground(UIManager.getColor("Label.foreground"));
            setBackground(UIManager.getColor("Label.background"));
        }
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        String text = getText();
        if (text == null || g.getFontMetrics().stringWidth(text) < getWidth())
            setToolTipText(null);
        else
            setToolTipText(text);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
