//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import game.Node;
import game.NodeUtils;
import go.Move;

//----------------------------------------------------------------------------

/** Panel displaying information about the current position. */
public class GameInfo
    extends JPanel
    implements ActionListener
{
    public GameInfo(Clock clock)
    {
        super(new GridLayout(0, 2, GuiUtils.SMALL_PAD, 0));
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

    public void update(Node node, go.Board board)
    {
        if (board.getToMove() == go.Color.BLACK)
            m_move.setText("Black");
        else
            m_move.setText("White");
        String capturedB = Integer.toString(board.getCapturedB());
        m_captB.setText(capturedB);
        String capturedW = Integer.toString(board.getCapturedW());
        m_captW.setText(capturedW);
        updateMoveNumber(node);
        String lastMove = "";
        Move move = node.getMove();
        if (move != null)
        {
            go.Color c = move.getColor();
            go.Point p = move.getPoint();
            lastMove = (c == go.Color.BLACK ? "Black" : "White") + " ";
            if (p == null)
                lastMove += "PASS";
            else
                lastMove += p.toString();
        }
        m_last.setText(lastMove);
        m_variation.setText(NodeUtils.getVariationString(node));
        double timeLeftBlack = node.getTimeLeft(go.Color.BLACK);
        int movesLeftBlack = node.getMovesLeft(go.Color.BLACK);
        if (! Double.isNaN(timeLeftBlack))
            m_timeB.setText(Clock.getTimeString(timeLeftBlack,
                                                movesLeftBlack));
        double timeLeftWhite = node.getTimeLeft(go.Color.WHITE);
        int movesLeftWhite = node.getMovesLeft(go.Color.WHITE);
        if (! Double.isNaN(timeLeftWhite))
            m_timeW.setText(Clock.getTimeString(timeLeftWhite,
                                                movesLeftWhite));
    }

    public void updateTime()
    {
        updateTime(go.Color.BLACK);
        updateTime(go.Color.WHITE);
    }

    private JTextField m_move;

    private JTextField m_number;

    private JTextField m_last;

    private JTextField m_captB;

    private JTextField m_captW;

    private JTextField m_timeB;

    private JTextField m_timeW;

    private JTextField m_variation;

    private Clock m_clock;

    private JTextField addEntry(String text)
    {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        add(label);
        JTextField entry = new JTextField(" ");
        entry.setHorizontalAlignment(SwingConstants.LEFT);
        entry.setBorder(BorderFactory.createLoweredBevelBorder());
        entry.setEditable(false);
        add(entry);
        return entry;
    }

    private void updateMoveNumber(Node node)
    {
        int moveNumber = NodeUtils.getMoveNumber(node);
        String numberString = Integer.toString(moveNumber);
        int movesLeft = NodeUtils.getMovesLeft(node);
        if (movesLeft > 0)
            numberString += "/" + (moveNumber + movesLeft);
        m_number.setText(numberString);
    }

    private void updateTime(go.Color color)
    {
        String text = m_clock.getTimeString(color);
        if (text == null)
            text = " ";
        if (color == go.Color.BLACK)
            m_timeB.setText(text);
        else
            m_timeW.setText(text);
    }
}

//----------------------------------------------------------------------------
