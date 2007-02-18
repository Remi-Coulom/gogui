//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstGameInformation;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Clock;
import net.sf.gogui.game.Game;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.util.StringUtil;

/** Panel displaying information about the current position. */
public class GameInfo
    extends JPanel
{
    public GameInfo(Game game)
    {        
        setBorder(GuiUtil.createEmptyBorder());
        JPanel panel =
            new JPanel(new GridLayout(0, 2, GuiUtil.PAD, GuiUtil.PAD));
        add(panel, BorderLayout.CENTER);
        m_game = game;
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
        {
            int index = c.toInteger();
            Box box = Box.createVerticalBox();
            panel.add(box);
            ImageIcon icon;
            if (c == GoColor.BLACK)
                icon = GuiUtil.getIcon("gogui-black-32x32", "Black");
            else
                icon = GuiUtil.getIcon("gogui-white-32x32", "White");
            m_icon[index] = new JLabel(icon);
            m_icon[index].setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(m_icon[index]);
            box.add(GuiUtil.createFiller());
            m_clock[index] = new GuiClock(c);
            m_clock[index].setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(m_clock[index]);
            GoColor otherColor = c.otherColor();
            int otherColorIndex = otherColor.toInteger();
            m_prisoners[otherColorIndex] = new Prisoners(otherColor);
            box.add(m_prisoners[otherColorIndex]);
        }
        Clock.Listener listener = new Clock.Listener() {
                public void clockChanged(ConstClock clock)
                {
                    SwingUtilities.invokeLater(m_updateTime);
                }
            };
        game.setClockListener(listener);
    }

    public void update(ConstGame game)
    {
        ConstBoard board = game.getBoard();
        ConstNode node = game.getCurrentNode();
        ConstGameTree tree = game.getTree();
        ConstGameInformation info = tree.getGameInformationConst(node);
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
        {
            int index = c.toInteger();
            updatePlayerToolTip(m_icon[index], info.getPlayer(c),
                                info.getRank(c), c.getCapitalizedName());
            m_prisoners[index].setCount(board.getCaptured(c));
        }
        // Usually time left information is stored in a node only for the
        // player who moved, so we check the father node too
        ConstNode father = node.getFatherConst();
        if (father != null)
            updateTimeFromNode(father);
        updateTimeFromNode(node);
    }

    public void updateTimeFromClock(ConstClock clock)
    {
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
            updateTimeFromClock(clock, c);
    }

    private class UpdateTimeRunnable
        implements Runnable
    {
        public void run()
        {
            updateTimeFromClock(m_game.getClock());
        }
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final GuiClock[] m_clock = new GuiClock[2];

    private JLabel[] m_icon = new JLabel[2];

    private Prisoners[] m_prisoners = new Prisoners[2];

    private final Game m_game;

    private final UpdateTimeRunnable m_updateTime = new UpdateTimeRunnable();

    private void updatePlayerToolTip(JLabel label, String player, String rank,
                                     String color)
    {
        StringBuffer buffer = new StringBuffer(128);
        buffer.append(color);
        buffer.append(" player");
        buffer.append(" (");
        if (! StringUtil.isEmpty(player))
        {
            buffer.append(player);
            if (! StringUtil.isEmpty(rank))
            {
                buffer.append(" ");
                buffer.append(rank);
            }
        }
        else
            buffer.append("unknown");
        buffer.append(")");
        label.setToolTipText(buffer.toString());
    }

    private void updateTimeFromClock(ConstClock clock, GoColor c)
    {
        assert(c.isBlackWhite());
        String text = clock.getTimeString(c);
        if (text == null)
            text = " ";
        m_clock[c.toInteger()].setText(text);
    }

    private void updateTimeFromNode(ConstNode node)
    {
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
        {
            double timeLeft = node.getTimeLeft(c);
            int movesLeft = node.getMovesLeft(c);
            if (! Double.isNaN(timeLeft))
            {
                String text = Clock.getTimeString(timeLeft, movesLeft);
                m_clock[c.toInteger()].setText(text);
            }
        }
    }
}

class GuiClock
    extends JTextField
{
    public GuiClock(GoColor color)
    {
        super(11);
        GuiUtil.setMonospacedFont(this);
        GuiUtil.setEditableFalse(this);
        setHorizontalAlignment(SwingConstants.CENTER);
        setMinimumSize(getPreferredSize());
        if (color == GoColor.BLACK)
            setToolTipText("Time for Black");
        else
            setToolTipText("Time for White");
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID
}

class Prisoners
    extends JPanel
{
    public Prisoners(GoColor color)
    {
        m_color = color;
        Icon icon;
        if (color == GoColor.BLACK)
            icon = GuiUtil.getIcon("gogui-black-16x16", "Black");
        else
            icon = GuiUtil.getIcon("gogui-white-16x16", "White");
        JLabel labelStone = new JLabel(icon);
        add(labelStone, BorderLayout.WEST);
        m_text = new JLabel();
        add(m_text, BorderLayout.CENTER);
        setCount(0);
    }

    public void setCount(int n)
    {
        m_text.setText(Integer.toString(n));
        StringBuffer buffer = new StringBuffer(64);
        buffer.append(n);
        if (m_color == GoColor.BLACK)
            buffer.append(" black");
        else
            buffer.append(" white");
        if (n == 1)
            buffer.append(" stone captured");
        else
            buffer.append(" stones captured");
        setToolTipText(buffer.toString());
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JLabel m_text;

    private GoColor m_color;
}
