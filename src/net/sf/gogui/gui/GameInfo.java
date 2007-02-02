//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.Icon;
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
        Box boxBlack = Box.createVerticalBox();
        panel.add(boxBlack);
        m_iconBlack = new JLabel(GuiUtil.getIcon("gogui-black-32x32",
                                                 "Black"));
        m_iconBlack.setAlignmentX(Component.CENTER_ALIGNMENT);
        boxBlack.add(m_iconBlack);
        boxBlack.add(GuiUtil.createFiller());
        m_clockBlack = new GuiClock(GoColor.BLACK);
        m_clockBlack.setAlignmentX(Component.CENTER_ALIGNMENT);
        boxBlack.add(m_clockBlack);
        m_prisonersWhite = new Prisoners(GoColor.WHITE);
        boxBlack.add(m_prisonersWhite);

        Box boxWhite = Box.createVerticalBox();
        panel.add(boxWhite);
        m_iconWhite = new JLabel(GuiUtil.getIcon("gogui-white-32x32",
                                                 "White"));
        m_iconWhite.setAlignmentX(Component.CENTER_ALIGNMENT);
        boxWhite.add(m_iconWhite);
        boxWhite.add(GuiUtil.createFiller());
        m_clockWhite = new GuiClock(GoColor.WHITE);
        m_clockWhite.setAlignmentX(Component.CENTER_ALIGNMENT);
        boxWhite.add(m_clockWhite);
        m_prisonersBlack = new Prisoners(GoColor.BLACK);
        boxWhite.add(m_prisonersBlack);

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
        ConstGameInformation info = tree.getGameInformationConst();
        updatePlayerToolTip(m_iconBlack, info.getPlayer(GoColor.BLACK),
                            info.getRank(GoColor.BLACK), "Black");
        updatePlayerToolTip(m_iconWhite, info.getPlayer(GoColor.WHITE),
                            info.getRank(GoColor.WHITE), "White");
        m_prisonersBlack.setCount(board.getCapturedBlack());
        m_prisonersWhite.setCount(board.getCapturedWhite());
        // Usually time left information is stored in a node only for the
        // player who moved, so we check the father node too
        ConstNode father = node.getFatherConst();
        if (father != null)
            updateTimeFromNode(father);
        updateTimeFromNode(node);
    }

    public void updateTimeFromClock(ConstClock clock)
    {
        updateTimeFromClock(clock, GoColor.BLACK);
        updateTimeFromClock(clock, GoColor.WHITE);
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

    private final GuiClock m_clockBlack;

    private final GuiClock m_clockWhite;

    private JLabel m_iconBlack;

    private JLabel m_iconWhite;

    private Prisoners m_prisonersBlack;

    private Prisoners m_prisonersWhite;

    private final Game m_game;

    private final UpdateTimeRunnable m_updateTime = new UpdateTimeRunnable();

    private void updatePlayerToolTip(JLabel label, String player, String rank,
                                     String color)
    {
        StringBuffer buffer = new StringBuffer(128);
        buffer.append("Player ");
        buffer.append(color);
        buffer.append(" (");
        if (player != null && ! player.trim().equals(""))
        {
            buffer.append(player);
            if (rank != null && ! rank.trim().equals(""))
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

    private void updateTimeFromClock(ConstClock clock, GoColor color)
    {
        String text = clock.getTimeString(color);
        if (text == null)
            text = " ";
        if (color == GoColor.BLACK)
            m_clockBlack.setText(text);
        else
            m_clockWhite.setText(text);
    }

    private void updateTimeFromNode(ConstNode node)
    {
        double timeLeftBlack = node.getTimeLeft(GoColor.BLACK);
        int movesLeftBlack = node.getMovesLeft(GoColor.BLACK);
        if (! Double.isNaN(timeLeftBlack))
            m_clockBlack.setText(Clock.getTimeString(timeLeftBlack,
                                                     movesLeftBlack));
        double timeLeftWhite = node.getTimeLeft(GoColor.WHITE);
        int movesLeftWhite = node.getMovesLeft(GoColor.WHITE);
        if (! Double.isNaN(timeLeftWhite))
            m_clockWhite.setText(Clock.getTimeString(timeLeftWhite,
                                                     movesLeftWhite));
    }
}

class GuiClock
    extends JTextField
{
    public GuiClock(GoColor color)
    {
        super(11);
        m_color = color;
        setEditable(false);
        setFocusable(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        GuiUtil.setMonospacedFont(this);
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

    private GoColor m_color;
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
