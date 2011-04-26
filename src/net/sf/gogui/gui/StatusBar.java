// StatusBar.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.gui.I18n.i18n;
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
            // add some empty space so that status bar does not overlap the
            // window resize widget on Mac OS X
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            outerPanel.add(filler, BorderLayout.EAST);
        }
        JPanel panel = new JPanel(new BorderLayout());
        //panel.setBorder(BorderFactory.createLineBorder(Color.gray));
        outerPanel.add(panel, BorderLayout.CENTER);
        m_iconBox = Box.createHorizontalBox();
        panel.add(m_iconBox, BorderLayout.WEST);
        m_toPlayLabel = new JLabel();
        m_toPlayLabel.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                                   Short.MAX_VALUE));
        setToPlay(BLACK);
        m_iconBox.add(m_toPlayLabel);

        m_labelSetup
            = new JLabel(GuiUtil.getIcon("gogui-setup-16x16",
                                         i18n("LB_STATUS_SETUP")));
        m_labelSetup.setVisible(false);
        m_labelSetup.setToolTipText(i18n("TT_STATUS_SETUP"));
        m_iconBox.add(m_labelSetup);

        m_iconBox.add(GuiUtil.createSmallFiller());

        m_text = new JLabel() {
                /** Use tool tip if text is truncated. */
                protected void paintComponent(Graphics g)
                {
                    super.paintComponent(g);
                    String text = super.getText();
                    if (text == null
                        || g.getFontMetrics().stringWidth(text) < getWidth())
                        setToolTipText(null);
                    else
                        setToolTipText(text);
                }
            };
        setPreferredLabelSize(m_text, 10);
        panel.add(m_text, BorderLayout.CENTER);
        Box moveTextBox = Box.createHorizontalBox();
        panel.add(moveTextBox, BorderLayout.EAST);
        m_moveText = new JLabel();
        setPreferredLabelSize(m_moveText, 12);
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
        the last move, current move number etc. */
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

    public void setText(String text)
    {
        m_text.setText(text);
    }

    public final void setToPlay(GoColor color)
    {
        if (color == BLACK)
        {
            m_toPlayLabel.setIcon(ICON_BLACK);
            m_toPlayLabel.setToolTipText(i18n("LB_STATUS_TO_PLAY_BLACK"));
        }
        else
        {
            assert color == WHITE;
            m_toPlayLabel.setIcon(ICON_WHITE);
            m_toPlayLabel.setToolTipText(i18n("LB_STATUS_TO_PLAY_WHITE"));
        }
    }

    /** Hide or show the text field for move information.
        @see #setMoveText */
    public void showMoveText(boolean show)
    {
        m_moveText.setVisible(show);
        m_moveTextSeparator.setVisible(show);
    }

    private static final Icon ICON_BLACK =
        GuiUtil.getIcon("gogui-black-16x16", i18n("LB_BLACK"));

    private static final Icon ICON_WHITE =
        GuiUtil.getIcon("gogui-white-16x16", i18n("LB_WHITE"));

    private final Box m_iconBox;

    private final JLabel m_toPlayLabel;

    private final JLabel m_labelSetup;

    private final JLabel m_moveText;

    private final JLabel m_text;

    private final JSeparator m_moveTextSeparator;

    /** Set a preferred size, such that the layout does not change,
        if a text label on the status bar is empty.
        The preferred size is derived from the font. */
    private static void setPreferredLabelSize(JLabel label, int columns)
    {
        Font font = label.getFont();
        Insets insets = label.getInsets();
        int height = font.getSize() + insets.top + insets.bottom;
        int width =
            columns * font.getSize() + insets.left + insets.right;
        label.setPreferredSize(new Dimension(width, height));
    }
}
