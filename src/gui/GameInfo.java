//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import game.*;
import go.*;
import utils.*;

//-----------------------------------------------------------------------------

class GameInfo
    extends JPanel
    implements ActionListener
{
    public GameInfo(TimeControl timeControl)
    {
        super(new GridLayout(2, 0, utils.GuiUtils.PAD, 0));
        m_timeControl = timeControl;

        add(createLeftAlignedLabel("To play"));
        add(createLeftAlignedLabel("Moves"));
        add(createLeftAlignedLabel("Last move"));
        add(createLeftAlignedLabel("Captured B"));
        add(createLeftAlignedLabel("Captured W"));
        add(createLeftAlignedLabel("Time B"));
        add(createLeftAlignedLabel("Time W"));

        m_move = createEntry();
        add(m_move);
        m_number = createEntry();
        add(m_number);
        m_last = createEntry();
        add(m_last);
        m_captB = createEntry();
        add(m_captB);
        m_captW = createEntry();
        add(m_captW);
        m_timeB = createEntry();
        m_timeB.setText("00:00");
        add(m_timeB);
        m_timeW = createEntry();
        m_timeW.setText("00:00");
        add(m_timeW);

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
        int moveNumber = node.getMoveNumber();
        String numberString = Integer.toString(moveNumber);
        int movesLeft = node.getMovesLeft();
        if (movesLeft > 0)
            numberString += "/" + (moveNumber + movesLeft);
        m_number.setText(numberString);
        String lastMove = "";
        Move move = node.getMove();
        if (move != null)
        {
            go.Color c = move.getColor();
            go.Point p = move.getPoint();
            lastMove = (c == go.Color.BLACK ? "B" : "W") + " ";
            if (p == null)
                lastMove += "PASS";
            else
                lastMove += p.toString();
        }
        m_last.setText(lastMove);
        String timeLeftBlack = node.getTimeLeftFomatted(go.Color.BLACK);
        String timeLeftWhite = node.getTimeLeftFomatted(go.Color.WHITE);
        if (timeLeftBlack != null)
            m_timeB.setText(timeLeftBlack);
        if (timeLeftWhite != null)
            m_timeW.setText(timeLeftWhite);
    }

    private go.Board m_board;

    private JLabel m_move;

    private JLabel m_number;

    private JLabel m_last;

    private JLabel m_captB;

    private JLabel m_captW;

    private JLabel m_timeB;

    private JLabel m_timeW;

    private TimeControl m_timeControl;

    private JLabel createLeftAlignedLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JLabel createEntry()
    {
        JLabel label = createLeftAlignedLabel(" ");
        label.setBorder(BorderFactory.createLoweredBevelBorder());
        return label;
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
}

//-----------------------------------------------------------------------------
