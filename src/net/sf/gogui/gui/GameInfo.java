//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Clock;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

/** Panel displaying information about the current position. */
public class GameInfo
    extends JPanel
{
    /** Constructor.
        @param clock Clock to register as listener or null.
    */
    public GameInfo(Game game)
    {
        super(new GridLayout(0, 2, GuiUtil.SMALL_PAD, GuiUtil.SMALL_PAD));
        m_move = addEntry("To play:");
        m_number = addEntry("Moves:");
        m_last = addEntry("Last move:");
        m_variation = addEntry("Variation:");
        m_captB = addEntry("Captured Black:");
        m_captW = addEntry("Captured White:");
        m_timeB = addEntry("Time Black:");
        m_timeW = addEntry("Time White:");
        m_timeB.setText("00:00");
        m_timeW.setText("00:00");
        Clock.Listener listener = new Clock.Listener() {
                public void clockChanged(ConstClock clock)
                {
                    updateTimeFromClock(clock);
                }
            };
        game.setClockListener(listener);
    }

    public void fastUpdateMoveNumber(String text)
    {
        m_number.setText(text);
        m_number.paintImmediately(m_number.getVisibleRect());
    }

    public void update(ConstNode node, ConstBoard board)
    {
        if (board.getToMove() == GoColor.BLACK)
            m_move.setText("Black");
        else
            m_move.setText("White");
        int capturedB = board.getCapturedB();
        if (capturedB == 0)
            m_captB.setText("");
        else
            m_captB.setText(Integer.toString(capturedB));
        int capturedW = board.getCapturedW();
        if (capturedW == 0)
            m_captW.setText("");
        else
            m_captW.setText(Integer.toString(capturedW));
        updateMoveNumber(node);
        String lastMove = "";
        Move move = node.getMove();
        if (move != null)
        {
            GoColor c = move.getColor();
            GoPoint p = move.getPoint();
            lastMove = (c == GoColor.BLACK ? "B " : "W ");
            if (p == null)
                lastMove += "PASS";
            else
                lastMove += p.toString();
        }
        m_last.setText(lastMove);
        m_variation.setText(NodeUtil.getVariationString(node));
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

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final JTextField m_move;

    private final JTextField m_number;

    private final JTextField m_last;

    private final JTextField m_captB;

    private final JTextField m_captW;

    private final JTextField m_timeB;

    private final JTextField m_timeW;

    private final JTextField m_variation;

    private JTextField addEntry(String text)
    {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        add(label);
        JTextField entry = new JTextField(" ");
        entry.setHorizontalAlignment(SwingConstants.LEFT);
        entry.setEditable(false);
        entry.setFocusable(false);
        add(entry);
        return entry;
    }

    private void updateMoveNumber(ConstNode node)
    {
        int moveNumber = NodeUtil.getMoveNumber(node);
        int movesLeft = NodeUtil.getMovesLeft(node);
        if (moveNumber == 0 && movesLeft == 0)
            m_number.setText("");
        else
        {
            String numberString = Integer.toString(moveNumber);
            if (movesLeft > 0)
                numberString += "/" + (moveNumber + movesLeft);
            m_number.setText(numberString);
        }
    }

    private void updateTimeFromClock(ConstClock clock, GoColor color)
    {
        String text = clock.getTimeString(color);
        if (text == null)
            text = " ";
        if (color == GoColor.BLACK)
            m_timeB.setText(text);
        else
            m_timeW.setText(text);
    }

    private void updateTimeFromNode(ConstNode node)
    {
        double timeLeftBlack = node.getTimeLeft(GoColor.BLACK);
        int movesLeftBlack = node.getMovesLeft(GoColor.BLACK);
        if (! Double.isNaN(timeLeftBlack))
            m_timeB.setText(Clock.getTimeString(timeLeftBlack,
                                                movesLeftBlack));
        double timeLeftWhite = node.getTimeLeft(GoColor.WHITE);
        int movesLeftWhite = node.getMovesLeft(GoColor.WHITE);
        if (! Double.isNaN(timeLeftWhite))
            m_timeW.setText(Clock.getTimeString(timeLeftWhite,
                                                movesLeftWhite));
    }
}

