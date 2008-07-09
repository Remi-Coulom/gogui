// GameInfoPanel.java

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.text.MessageFormat;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Clock;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.go.BlackWhiteSet;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.BLACK_WHITE;
import static net.sf.gogui.go.GoColor.WHITE_BLACK;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.util.StringUtil;

/** Panel displaying information about the current position. */
public class GameInfoPanel
    extends JPanel
{
    public GameInfoPanel(Game game)
    {
        setBorder(GuiUtil.createEmptyBorder());
        JPanel panel =
            new JPanel(new GridLayout(0, 2, GuiUtil.PAD, GuiUtil.PAD));
        add(panel, BorderLayout.CENTER);
        m_game = game;
        for (GoColor c : WHITE_BLACK)
        {
            Box box = Box.createVerticalBox();
            panel.add(box);
            ImageIcon icon;
            if (c == BLACK)
                icon = GuiUtil.getIcon("gogui-black-32x32", i18n("LB_BLACK"));
            else
                icon = GuiUtil.getIcon("gogui-white-32x32", i18n("LB_WHITE"));
            m_icon.set(c, new JLabel(icon));
            m_icon.get(c).setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(m_icon.get(c));
            box.add(GuiUtil.createFiller());
            m_clock.set(c, new GuiClock(c));
            m_clock.get(c).setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(m_clock.get(c));
            GoColor otherColor = c.otherColor();
            m_prisoners.set(otherColor, new Prisoners(otherColor));
            box.add(m_prisoners.get(otherColor));
        }
        Clock.Listener listener = new Clock.Listener() {
                public void clockChanged()
                {
                    SwingUtilities.invokeLater(m_updateTime);
                }
            };
        game.setClockListener(listener);
    }

    public void update()
    {
        ConstBoard board = m_game.getBoard();
        ConstNode node = m_game.getCurrentNode();
        ConstGameTree tree = m_game.getTree();
        ConstGameInfo info = tree.getGameInfoConst(node);
        for (GoColor c : BLACK_WHITE)
        {
            String name = info.get(StringInfoColor.NAME, c);
            String rank = info.get(StringInfoColor.RANK, c);
            updatePlayerToolTip(m_icon.get(c), name, rank, c);
            m_prisoners.get(c).setCount(board.getCaptured(c));
            updateTimeFromClock(m_game.getClock(), c);
        }
    }

    private class UpdateTimeRunnable
        implements Runnable
    {
        public void run()
        {
            for (GoColor c : BLACK_WHITE)
                updateTimeFromClock(m_game.getClock(), c);
        }
    }

    private final BlackWhiteSet<GuiClock> m_clock
        = new BlackWhiteSet<GuiClock>();

    private final BlackWhiteSet<JLabel> m_icon
        = new BlackWhiteSet<JLabel>();

    private final BlackWhiteSet<Prisoners> m_prisoners
        = new BlackWhiteSet<Prisoners>();

    private final Game m_game;

    private final UpdateTimeRunnable m_updateTime = new UpdateTimeRunnable();

    private void updatePlayerToolTip(JLabel label, String player, String rank,
                                     GoColor color)
    {
        assert color.isBlackWhite();
        StringBuilder buffer = new StringBuilder(128);
        if (color == BLACK)
            buffer.append(i18n("TT_INFOPANEL_PLAYER_BLACK"));
        else
            buffer.append(i18n("TT_INFOPANEL_PLAYER_WHITE"));
        buffer.append(" (");
        if (StringUtil.isEmpty(player))
            buffer.append(i18n("TT_INFOPANEL_UNKNOWN_NAME"));
        else
        {
            buffer.append(player);
            if (! StringUtil.isEmpty(rank))
            {
                buffer.append(' ');
                buffer.append(rank);
            }
        }
        buffer.append(')');
        label.setToolTipText(buffer.toString());
    }

    private void updateTimeFromClock(ConstClock clock, GoColor c)
    {
        assert c.isBlackWhite();
        String text = clock.getTimeString(c);
        m_clock.get(c).setText(text);
    }
}

class GuiClock
    extends JTextField
{
    public GuiClock(GoColor color)
    {
        super(COLUMNS);
        GuiUtil.setEditableFalse(this);
        setHorizontalAlignment(SwingConstants.CENTER);
        setMinimumSize(getPreferredSize());
        m_color = color;
        setText("00:00");
    }

    public final void setText(String text)
    {
        super.setText(text);
        String toolTip;
        if (m_color == BLACK)
            toolTip = i18n("TT_INFOPANEL_TIME_BLACK");
        else
            toolTip = i18n("TT_INFOPANEL_TIME_WHITE");
        if (text.length() > COLUMNS)
            toolTip = toolTip + " (" + text + ")";
        setToolTipText(toolTip);
    }

    private static final int COLUMNS = 8;

    private final GoColor m_color;
}

class Prisoners
    extends JPanel
{
    public Prisoners(GoColor color)
    {
        m_color = color;
        Icon icon;
        if (color == BLACK)
            icon = GuiUtil.getIcon("gogui-black-16x16", i18n("LB_BLACK"));
        else
            icon = GuiUtil.getIcon("gogui-white-16x16", i18n("LB_WHITE"));
        JLabel labelStone = new JLabel(icon);
        add(labelStone, BorderLayout.WEST);
        m_text = new JLabel();
        add(m_text, BorderLayout.CENTER);
        setCount(0);
    }

    public final void setCount(int n)
    {
        m_text.setText(Integer.toString(n));
        String tip;
        if (m_color == BLACK)
        {
            if (n == 1)
                tip = i18n("TT_INFOPANEL_PRISONER_BLACK_ONE");
            else
                tip = MessageFormat.format(i18n("TT_INFOPANEL_PRISONER_BLACK"), n);
        }
        else
        {
            if (n == 1)
                tip = i18n("TT_INFOPANEL_PRISONER_WHITE_ONE");
            else
                tip = MessageFormat.format(i18n("TT_INFOPANEL_PRISONER_WHITE"), n);
        }
        setToolTipText(tip);
    }

    private final JLabel m_text;

    private final GoColor m_color;
}
