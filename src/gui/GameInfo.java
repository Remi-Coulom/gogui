//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import game.*;
import go.*;
import utils.*;

//----------------------------------------------------------------------------

public class GameInfo
    extends JPanel
    implements ActionListener
{
    public GameInfo(TimeControl timeControl)
    {
        super(new GridLayout(0, 2, GuiUtils.SMALL_PAD, 0));
        m_timeControl = timeControl;
        m_move = addEntry("To Play");
        m_number = addEntry("Moves");
        m_last = addEntry("Last Move");
        m_captB = addEntry("Captured Black");
        m_captW = addEntry("Captured White");
        m_timeB = addEntry("Time Black");
        m_timeW = addEntry("Time White");
        m_timeB.setText("00:00");
        m_timeW.setText("00:00");
        new javax.swing.Timer(1000, this).start();
    }

    public void actionPerformed(ActionEvent evt)
    {
        if (m_timeControl.isRunning())
        {
            setTime(go.Color.BLACK);
            setTime(go.Color.WHITE);
        }
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
        double timeLeftBlack = node.getTimeLeftBlack();
        int movesLeftBlack = node.getMovesLeftBlack();
        if (! Double.isNaN(timeLeftBlack))
            m_timeB.setText(TimeControl.getTimeString(timeLeftBlack,
                                                      movesLeftBlack));
        double timeLeftWhite = node.getTimeLeftWhite();
        int movesLeftWhite = node.getMovesLeftWhite();
        if (! Double.isNaN(timeLeftWhite))
            m_timeW.setText(TimeControl.getTimeString(timeLeftWhite,
                                                      movesLeftWhite));
    }

    private JLabel m_move;

    private JLabel m_number;

    private JLabel m_last;

    private JLabel m_captB;

    private JLabel m_captW;

    private JLabel m_timeB;

    private JLabel m_timeW;

    private TimeControl m_timeControl;

    private JLabel addEntry(String text)
    {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        add(label);
        JLabel entry = new JLabel(" ");
        entry.setHorizontalAlignment(SwingConstants.LEFT);
        entry.setBorder(BorderFactory.createLoweredBevelBorder());
        add(entry);
        return entry;
    }

    private void setTime(go.Color c)
    {
        String text = m_timeControl.getTimeString(c);
        if (text == null)
            text = " ";
        if (c == go.Color.BLACK)
            m_timeB.setText(text);
        else
            m_timeW.setText(text);
    }

    private void updateMoveNumber(Node node)
    {
        int moveNumber = node.getMoveNumber();
        String numberString = Integer.toString(moveNumber);
        int movesLeft = node.getMovesLeft();
        if (movesLeft > 0)
            numberString += "/" + (moveNumber + movesLeft);
        m_number.setText(numberString);
    }
}

//----------------------------------------------------------------------------
