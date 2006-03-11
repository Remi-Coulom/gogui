//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

//----------------------------------------------------------------------------

/** Panel displaying information about the current position. */
public class GameInfo
    extends JPanel
    implements ActionListener
{
    public GameInfo(Clock clock)
    {
        super(new GridLayout(0, 2, GuiUtils.SMALL_PAD, GuiUtils.SMALL_PAD));
        m_clock = clock;
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
        new javax.swing.Timer(1000, this).start();
    }

    public void actionPerformed(ActionEvent evt)
    {
        if (m_clock.isRunning())
            updateTime();
    }

    public void fastUpdateMoveNumber(Node node)
    {
        updateMoveNumber(node);
        m_number.paintImmediately(m_number.getVisibleRect());
    }

    public void update(Node node, Board board)
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
        m_variation.setText(NodeUtils.getVariationString(node));
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

    public void updateTime()
    {
        updateTime(GoColor.BLACK);
        updateTime(GoColor.WHITE);
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

    private final Clock m_clock;

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

    private void updateMoveNumber(Node node)
    {
        int moveNumber = NodeUtils.getMoveNumber(node);
        int movesLeft = NodeUtils.getMovesLeft(node);
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

    private void updateTime(GoColor color)
    {
        String text = m_clock.getTimeString(color);
        if (text == null)
            text = " ";
        if (color == GoColor.BLACK)
            m_timeB.setText(text);
        else
            m_timeW.setText(text);
    }
}

//----------------------------------------------------------------------------
