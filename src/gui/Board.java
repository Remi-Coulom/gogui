//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.print.*;
import java.util.*;
import javax.swing.*;
import go.*;

//-----------------------------------------------------------------------------

public class Board
    extends JPanel
    implements Printable
{
    public interface Listener
    {
        void fieldClicked(go.Point p);
    }

    public Board(go.Board board)
    {
        m_board = board;
        setPreferredFieldSize();
        initSize(m_board.getSize());
    }

    public void clearAll()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            clearInfluence(p);
            setFieldBackground(p, null);
            setMarkup(p, false);
            setString(p, "");
        }
        clearAllCrossHair();
    }

    public void clearAllCrossHair()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setCrossHair(m_board.getPoint(i), false);
    }

    public void clearInfluence(go.Point p)
    {
        getField(p).clearInfluence();
    }

    public void fieldClicked(go.Point p)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p);
    }

    public go.Board getBoard()
    {
        return m_board;
    }

    public Dimension getPreferredFieldSize()
    {
        return m_preferredFieldSize;
    }

    public void initSize(int size)
    {
        m_board.initSize(size);
        m_lastMove = null;
        m_field = new Field[size][size];
        removeAll();
        GridBagLayout gridBag = new GridBagLayout();
        setLayout(gridBag);
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(size, size));
        panel.setBackground(m_boardColor);
        add(panel);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridheight = constraints.gridwidth = size;
        constraints.weightx = constraints.weighty = (double)size / (size + 2);
        gridBag.setConstraints(panel, constraints);
        addColumnLabels(gridBag, size, 0);
        addColumnLabels(gridBag, size, size + 1);
        addRowLabels(gridBag, size, 0);
        addRowLabels(gridBag, size, size + 1);
        for (int y = size - 1; y >= 0; --y)
        {
            for (int x = 0; x < size; ++x)
            {
                go.Point p = m_board.getPoint(x, y);
                Field field = new Field(this, p, m_board.isHandicap(p));
                panel.add(field);
                m_field[x][y] = field;
            }
        }
        revalidate();
        repaint();
    }

    public int print(Graphics g, PageFormat format, int page)
        throws PrinterException
    {
        if (page >= 1)
        {
            return Printable.NO_SUCH_PAGE;
        }
        double width = getSize().width;
        double height = getSize().height;
        double pageWidth = format.getImageableWidth();
        double pageHeight = format.getImageableHeight();
        double scale = 1;
        if (width >= pageWidth)
            scale = pageWidth / width;
        double xSpace = (pageWidth - width * scale) / 2;
        double ySpace = (pageHeight - height * scale) / 2;
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(format.getImageableX() + xSpace,
                      format.getImageableY() + ySpace);
        g2d.scale(scale, scale);
        print(g2d);
        return Printable.PAGE_EXISTS;
    }

    public void scoreSetDead(go.Point p)
    {
        go.Color c = m_board.getColor(p);
        if (c == go.Color.EMPTY)
            return;
        Vector stones = new Vector(m_board.getNumberPoints());
        m_board.getStones(p, c, stones);
        boolean dead = ! m_board.getDead((go.Point)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            go.Point stone = (go.Point)stones.get(i);
            m_board.setDead(stone, dead);
            setCrossHair(stone, dead);
        }
        calcScore();
    }

    public void setFieldBackground(go.Point p, java.awt.Color color)
    {
        getField(p).setFieldBackground(color);
    }

    public void setCrossHair(go.Point p, boolean crossHair)
    {
        if (m_lastMove != null)
        {
            Field f = getField(m_lastMove);
            f.setCrossHair(false);
            m_lastMove = null;
        }
        getField(p).setCrossHair(crossHair);
    }

    public void setInfluence(go.Point p, double value)
    {
        getField(p).setInfluence(value);
    }

    public void setListener(Listener l)
    {
        m_listener = l;
    }

    public void setMarkup(go.Point p, boolean markup)
    {
        getField(p).setMarkup(markup);
    }

    public void setString(go.Point p, String s)
    {
        getField(p).setString(s);
    }

    public void showColorBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            String s = board[p.getX()][p.getY()];
            java.awt.Color c = null;
            if (s.equals("blue"))
                c = java.awt.Color.blue;
            else if (s.equals("cyan"))
                c = java.awt.Color.cyan;
            else if (s.equals("green"))
                c = java.awt.Color.green;
            else if (s.equals("gray"))
                c = java.awt.Color.lightGray;
            else if (s.equals("magenta"))
                c = java.awt.Color.magenta;
            else if (s.equals("pink"))
                c = java.awt.Color.pink;
            else if (s.equals("red"))
                c = java.awt.Color.red;
            else if (s.equals("yellow"))
                c = java.awt.Color.yellow;
            setFieldBackground(p, c);
        }
    }

    public void showDoubleBoard(double[][] board, double scale)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            double d = board[p.getX()][p.getY()] * scale;
            setInfluence(p, d);
        }
    }

    public void showPointList(go.Point pointList[])
    {
        for (int i = 0; i < pointList.length; ++i)
        {
            go.Point p = pointList[i];
            if (p != null)
                setMarkup(p, true);
        }
    }

    public void showStringBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            setString(p, board[p.getX()][p.getY()]);
        }
    }

    public void update()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            setColor(p, m_board.getColor(p));
        }
        drawLastMove();
    }

    private static java.awt.Color m_boardColor
        = new java.awt.Color(224, 160, 96);

    private Dimension m_preferredFieldSize;

    private go.Board m_board;

    private Field m_field[][];

    private Listener m_listener;

    private go.Point m_lastMove;

    private void addColumnLabels(GridBagLayout gridBag, int size, int y)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridheight = constraints.gridwidth = 1;
        constraints.weightx = constraints.weighty = 1d / (size + 2);
        char c = 'A';
        for (int x = 0; x < size; ++x)
        {
            JLabel label = new JLabel(new Character(c).toString(),
                                      JLabel.CENTER);
            add(label);
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = x + 1;
            constraints.gridy = y;
            gridBag.setConstraints(label, constraints);
            ++c;
            if (c == 'I')
                ++c;
        }
    }

    private void addRowLabels(GridBagLayout gridBag, int size, int x)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridheight = constraints.gridwidth = 1;
        constraints.weightx = constraints.weighty = 1d / (size + 2);
        for (int y = 0; y < size; ++y)
        {
            String text = Integer.toString(y + 1);
            JLabel label = new JLabel(text, JLabel.CENTER);
            add(label);
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = x;
            constraints.gridy = size - y;
            gridBag.setConstraints(label, constraints);
        }
    }

    private void calcScore()
    {
        m_board.calcScore();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            go.Color c = m_board.getScore(p);
            if (c == go.Color.BLACK)
                setInfluence(p, 1.0);
            else if (c == go.Color.WHITE)
                setInfluence(p, -1.0);
            else
                setInfluence(p, 0);
        }
    }

    private void drawLastMove()
    {
        if (m_lastMove != null)
        {
            Field f = getField(m_lastMove);
            f.setCrossHair(false);
            m_lastMove = null;
        }
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getInternalMove(moveNumber - 1);
            m_lastMove = m.getPoint();
            if (m_lastMove != null && m.getColor() != go.Color.EMPTY)
            {
                Field f = m_field[m_lastMove.getX()][m_lastMove.getY()];
                f.setCrossHair(true);
            }
        }
    }

    private Field getField(go.Point p)
    {
        assert(p != null);
        return m_field[p.getX()][p.getY()];
    }

    private void setColor(go.Point p, go.Color color)
    {
        getField(p).setColor(color);
    }

    private void setPreferredFieldSize()
    {
        int size;
        Font font = UIManager.getFont("Label.font");        
        if (font != null)
            size = (int)((double)font.getSize() * 2.5);
        else
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            size = screenSize.height / 30;
        }
        if (size % 2 == 0)
            ++size;
        m_preferredFieldSize = new Dimension(size, size);
    }
}

//-----------------------------------------------------------------------------
