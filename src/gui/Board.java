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
import java.net.*;
import java.util.*;
import javax.swing.*;
import go.*;

//-----------------------------------------------------------------------------

class BoardLabel
    extends JLabel
{
    public BoardLabel(String text)
    {
        super(text, JLabel.CENTER);
        setForeground(java.awt.Color.DARK_GRAY);
    }
}

public class Board
    extends JPanel
    implements FocusListener, Printable, KeyListener
{
    public interface Listener
    {
        void fieldClicked(go.Point p, boolean modifiedSelect);
    }

    public Board(go.Board board)
    {
        super(new GridBagLayout());
        m_board = board;
        setPreferredFieldSize();
        URL url = getClass().getClassLoader().getResource("images/wood.png");
        m_image = new ImageIcon(url);
        initSize(m_board.getSize());
        setFocusable(true);
        addFocusListener(this);
    }

    public void clearAll()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            clearInfluence(p);
            setFieldBackground(p, null);
        }
        clearAllCrossHair();
        clearAllMarkup();
        clearAllSelect();
        clearAllStrings();
        drawLastMove();
        if (m_variationShown)
        {
            update();
            m_variationShown = false;
        }
    }

    public void clearAllCrossHair()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setCrossHair(m_board.getPoint(i), false);
    }

    public void clearAllMarkup()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setMarkup(m_board.getPoint(i), false);
    }

    public void clearAllSelect()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setSelect(m_board.getPoint(i), false);
    }

    public void clearAllStrings()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setString(m_board.getPoint(i), "");
    }

    public void clearInfluence(go.Point p)
    {
        getField(p).clearInfluence();
    }

    public void fieldClicked(go.Point p, boolean modifiedSelect)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p, modifiedSelect);
    }

    public void focusGained(FocusEvent event)
    {
        setFocusPoint(m_focusPoint);
    }

    public void focusLost(FocusEvent event)
    {
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
        m_field = new Field[size][size];
        removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(new GridLayout(size, size));
        panel.setOpaque(false);
        add(panel);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridheight = constraints.gridwidth = size;
        constraints.weightx = constraints.weighty = (double)size / (size + 1);
        GridBagLayout gridBag = (GridBagLayout)getLayout();
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
                field.addKeyListener(this);
            }
        }
        m_focusPoint = new go.Point(size / 2, size / 2);
        m_lastMove = null;
        revalidate();
        repaint();
    }

    public void keyPressed(KeyEvent event)
    {
        int code = event.getKeyCode();
        int modifiers = event.getModifiers();
        int size = m_board.getSize();
        if ((modifiers & ActionEvent.CTRL_MASK) != 0)
            return;
        boolean shiftModifier = ((modifiers & ActionEvent.SHIFT_MASK) != 0);
        if (code == KeyEvent.VK_DOWN)
        {
            if (shiftModifier)
                do
                    m_focusPoint.down();
                while (! m_board.isHandicapLine(m_focusPoint.getY())
                       && ! m_board.isEdgeLine(m_focusPoint.getY()));
            else
                m_focusPoint.down();
        }
        else if (code == KeyEvent.VK_UP)
        {
            if (shiftModifier)
                do
                    m_focusPoint.up(size);
                while (! m_board.isHandicapLine(m_focusPoint.getY())
                       && ! m_board.isEdgeLine(m_focusPoint.getY()));
            else
                m_focusPoint.up(size);
        }
        else if (code == KeyEvent.VK_LEFT)
        {
            if (shiftModifier)
                do
                    m_focusPoint.left();
                while (! m_board.isHandicapLine(m_focusPoint.getX())
                       && ! m_board.isEdgeLine(m_focusPoint.getX()));
            else
                m_focusPoint.left();
        }
        else if (code == KeyEvent.VK_RIGHT)
        {
            if (shiftModifier)
                do
                    m_focusPoint.right(size);
                while (! m_board.isHandicapLine(m_focusPoint.getX())
                       && ! m_board.isEdgeLine(m_focusPoint.getX()));
            else
                m_focusPoint.right(size);
        }
        setFocusPoint(m_focusPoint);
    }

    public void keyReleased(KeyEvent event)
    {
    }

    public void keyTyped(KeyEvent event)
    {
    }

    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        Dimension size = getSize();
        graphics.drawImage(m_image.getImage(), 0, 0, size.width, size.height,
                           null);
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

    public void scoreBegin(go.Point[] isDeadStone)
    {
        m_board.scoreBegin(isDeadStone);
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                setCrossHair(isDeadStone[i], true);
        calcScore();
    }

    public void scoreSetDead(go.Point p)
    {
        go.Color c = m_board.getColor(p);
        if (c == go.Color.EMPTY)
            return;
        Vector stones = new Vector(m_board.getNumberPoints());
        m_board.getStones(p, c, stones);
        boolean dead = ! m_board.scoreGetDead((go.Point)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            go.Point stone = (go.Point)stones.get(i);
            m_board.scoreSetDead(stone, dead);
            setCrossHair(stone, dead);
        }
        calcScore();
    }

    public void setFocus()
    {
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getMove(moveNumber - 1);
            go.Point lastMove = m.getPoint();
            if (lastMove != null && m.getColor() != go.Color.EMPTY)
                setFocusPoint(lastMove);
        }
        else if (m_board.getInternalNumberMoves() == 0)
        {
            int size = m_board.getSize();
            setFocusPoint(m_board.getPoint(size / 2, size / 2));
        }
    }

    public void setFocusPoint(go.Point point)
    {
        if (! m_board.contains(point))
            return;
        getField(point).requestFocus();
        m_focusPoint.set(point.getX(), point.getY());
    }

    public void setFieldBackground(go.Point p, java.awt.Color color)
    {
        getField(p).setFieldBackground(color);
    }

    public void setCrossHair(go.Point p, boolean crossHair)
    {
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

    public void setShowLastMove(boolean showLastMove)
    {
        clearLastMove();
        m_showLastMove = showLastMove;
        drawLastMove();
    }

    public void setMarkup(go.Point p, boolean markup)
    {
        getField(p).setMarkup(markup);
    }

    public void setSelect(go.Point p, boolean select)
    {
        getField(p).setSelect(select);
    }

    public void setString(go.Point p, String s)
    {
        getField(p).setString(s);
    }

    public void showBWBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            String s = board[p.getX()][p.getY()].toLowerCase();
            if (s.equals("b") || s.equals("black"))
                setInfluence(p, 1);
            else if (s.equals("w") || s.equals("white"))
                setInfluence(p, -1);
            else
                setInfluence(p, 0);
        }
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
        clearAllMarkup();
        for (int i = 0; i < pointList.length; ++i)
        {
            go.Point p = pointList[i];
            if (p != null)
                setMarkup(p, true);
        }
    }

    public void showPointStringList(Vector pointList, Vector stringList)
    {
        clearAllStrings();
        for (int i = 0; i < pointList.size(); ++i)
        {
            go.Point point = (go.Point)pointList.get(i);
            String string = (String)stringList.get(i);
            if (point != null)
                setString(point, string);
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

    public void showVariation(go.Point[] variation, go.Color toMove)
    {
        clearAllStrings();
        for (int i = 0; i < variation.length; ++i)
        {
            go.Point point = variation[i];
            if (point != null)
            {
                setColor(point, toMove);
                setString(point, Integer.toString(i + 1));
            }
            toMove = toMove.otherColor();
        }
        m_variationShown = true;
    }

    public void update()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            Field field = getField(point);
            go.Color color = m_board.getColor(point);
            go.Color oldColor = field.getColor();
            if (color != oldColor)
            {
                setColor(point, color);
                field.repaint();
            }
        }
        drawLastMove();
    }

    private boolean m_showLastMove = true;

    private boolean m_variationShown;

    private go.Point m_focusPoint;

    private go.Point m_lastMove;

    private Dimension m_preferredFieldSize;

    private go.Board m_board;

    private ImageIcon m_image;

    private Field m_field[][];

    private Listener m_listener;

    private void addColumnLabels(GridBagLayout gridBag, int size, int y)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridheight = constraints.gridwidth = 1;
        constraints.weightx = constraints.weighty = 0.5 / (size + 1);
        char c = 'A';
        for (int x = 0; x < size; ++x)
        {
            JLabel label = new BoardLabel(new Character(c).toString());
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
        constraints.weightx = constraints.weighty = 0.5 / (size + 1);
        for (int y = 0; y < size; ++y)
        {
            String text = Integer.toString(y + 1);
            JLabel label = new BoardLabel(text);
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

    private void clearLastMove()
    {
        if (m_lastMove != null)
        {
            Field field = getField(m_lastMove);
            field.setLastMoveMarker(false);
            field.repaint();
            m_lastMove = null;
        }
    }

    private void drawLastMove()
    {
        if (m_showLastMove)
            clearLastMove();
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getMove(moveNumber - 1);
            go.Point lastMove = m.getPoint();
            if (lastMove != null && m.getColor() != go.Color.EMPTY)
            {
                if (m_showLastMove)
                {
                    Field field = getField(lastMove);
                    field.setLastMoveMarker(true);
                    field.repaint();
                    m_lastMove = lastMove;
                }
            }
        }
        setFocus();
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
