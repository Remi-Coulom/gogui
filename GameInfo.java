//=============================================================================
// $Id$
// $Source$
//=============================================================================

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import board.*;
import utils.*;

//=============================================================================

class GameInfo
    extends JPanel
    implements ActionListener
{
    public GameInfo(Board board, TimeControl timeControl)
    {
        m_board = board;
        m_timeControl = timeControl;

        setLayout(new GridLayout(2, 0, utils.GuiUtils.PAD, 0));

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
        setTime(board.Color.BLACK);
        setTime(board.Color.WHITE);
    }

    public void update()
    {
        if (m_board.getToMove() == board.Color.BLACK)
            m_move.setText("Black");
        else
            m_move.setText("White");
        String capturedB = Integer.toString(m_board.getCapturedB());
        m_captB.setText(capturedB);
        String capturedW = Integer.toString(m_board.getCapturedW());
        m_captW.setText(capturedW);
        int moveNumber = m_board.getMoveNumber();
        m_number.setText(Integer.toString(moveNumber));
        String lastMove;
        if (moveNumber == 0)
            lastMove = "";
        else
        {
            Move m = m_board.getMove(moveNumber - 1);
            board.Color c = m.getColor();
            board.Point p = m.getPoint();
            lastMove = (c == board.Color.BLACK ? "B" : "W") + " ";
            if (p == null)
                lastMove += "PASS";
            else
                lastMove += p.toString();
        }
        m_last.setText(lastMove);
    }

    private Board m_board;
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

    private void setTime(board.Color c)
    {
        String text = m_timeControl.getTimeString(c);
        if (text == null)
            text = " ";
        if (c == board.Color.BLACK)
            m_timeB.setText(text);
        else
            m_timeW.setText(text);
    }
}

//=============================================================================
