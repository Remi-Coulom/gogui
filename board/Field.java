//=============================================================================
// $Id$
// $Source$
//=============================================================================

package board;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.util.*;
import javax.swing.*;

//=============================================================================

class Field
    extends JButton
    implements ActionListener
{
    Field(Board board, Point p, boolean isHandicap)
    {
        m_board = board;
        m_color = Color.EMPTY;
        m_point = p;
        m_isHandicap = isHandicap;
        Dimension size = m_board.getPreferredFieldSize();
        setPreferredSize(size);
        setMinimumSize(new Dimension(3, 3));
        setBorder(null);
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent event)
    {
        m_board.fieldClicked(m_point);
    }

    public void clearInfluence()
    {
        if (m_influenceSet)
        {
            m_influenceSet = false;
            m_influence = 0;
            repaint();
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Dimension size = getSize();
        if (m_fieldColor != null)
            g.setColor(m_fieldColor);
        else
            g.setColor(m_boardColor);
        g.fillRect(0, 0, size.width, size.height);        
        if (m_color == Color.BLACK)
            drawStone(g, java.awt.Color.black);
        else if (m_color == Color.WHITE)
            drawStone(g, java.awt.Color.white);
        else
            drawGrid(g);
        if (m_influenceSet)
            drawInfluence(g);
        if (m_markup)
            drawMarkup(g);
        if (m_crossHair)
            drawCrossHair(g);
        if (! m_string.equals(""))
            drawString(g);
    }

    public void setFieldBackground(java.awt.Color color)
    {
        if (m_fieldColor != color)
        {
            m_fieldColor = color;
            repaint();
        }
    }

    public void setColor(Color color)
    {
        if (m_color != color)
        {
            m_color = color;
            repaint();
        }
    }

    public void setCrossHair(boolean crossHair)
    {
        if (m_crossHair == crossHair)
            return;
        m_crossHair = crossHair;
        repaint();
    }

    public void setInfluence(double value)
    {
        if (value > 1.)
            value = 1.;
        else if (value < -1.)
            value = -1.;
        repaint();
        m_influence = value;
        m_influenceSet = true;
    }

    public void setMarkup(boolean markup)
    {
        if (m_markup == markup)
            return;
        m_markup = markup;
        repaint();
    }

    public void setString(String s)
    {
        if (! m_string.equals(s))
        {
            m_string = s;
            repaint();
        }
    }

    private boolean m_isHandicap;
    private boolean m_crossHair;
    private boolean m_markup;
    private boolean m_influenceSet;
    private double m_influence;
    private String m_string = "";
    private static java.awt.Color m_boardColor
        = new java.awt.Color(224, 160, 96);
    private java.awt.Color m_fieldColor;
    private static java.awt.Color m_influenceBlackColor
        = new java.awt.Color(255, 63, 63);
    private static java.awt.Color m_influenceWhiteColor
        = new java.awt.Color(0, 255, 127);
    private Color m_color;
    private Point m_point;
    private Board m_board;

    private void drawCrossHair(Graphics g)
    {
        Dimension size = getSize();
        int dx = size.width / 4;
        int dy = size.height / 4;
        g.setColor(java.awt.Color.magenta);
        g.drawLine(dx, size.height / 2, size.width - dx, size.height / 2);
        g.drawLine(size.width / 2, dy, size.width / 2, size.height - dy);
    }

    private void drawGrid(Graphics g)
    {
        int boardSize = m_board.getBoardSize();
        Dimension size = getSize();
        int halfWidth = size.width / 2;
        int halfHeight = size.height / 2;
        g.setColor(java.awt.Color.darkGray);
        int xMin = 0;
        int xMax = size.width;
        int yMin = 0;
        int yMax = size.height;
        int x = m_point.getX();
        int y = m_point.getY();
        if (x == 0)
            xMin = halfWidth;
        else if (x == boardSize - 1)
            xMax = halfWidth;
        if (y == boardSize - 1)
            yMin = halfHeight;
        else if (y == 0)
            yMax = halfHeight;
        g.drawLine(xMin, halfHeight, xMax, halfHeight);
        g.drawLine(halfWidth, yMin, halfWidth, yMax);
        if (m_isHandicap)
        {
            int radiusX = size.width / 10;
            int radiusY = size.height / 10;
            g.fillOval(halfWidth - radiusX, halfHeight - radiusY,
                       2 * radiusX + 1, 2 * radiusY + 1);
        }
    }

    private void drawInfluence(Graphics g)
    {
        Dimension size = getSize();
        double d = Math.abs(m_influence);
        if (d < 0.01)
            return;
        if (m_influence > 0)
            g.setColor(m_influenceBlackColor);
        else
            g.setColor(m_influenceWhiteColor);
        int dx = (int)(size.width * (0.29 + (1 - d) * 0.71));
        int dy = (int)(size.height * (0.29 + (1 - d) * 0.71));
        g.fillRect(dx / 2, dy / 2, size.width - dx + 1, size.height - dy + 1);
    }

    private void drawMarkup(Graphics g)
    {
        Dimension size = getSize();
        int dx = size.width / 2;
        int dy = size.height / 2;
        g.setColor(java.awt.Color.blue);
        g.drawRect(dx / 2, dy / 2, size.width - dx + 1, size.height - dy + 1);
    }

    private void drawStone(Graphics g, java.awt.Color c)
    {
        Dimension size = getSize();
        g.setColor(c);
        g.fillOval(0, 0, size.width, size.height);
    }

    private void drawString(Graphics g)
    {
        Dimension size = getSize();
        int stringWidth = g.getFontMetrics().stringWidth(m_string);
        int stringHeight = g.getFont().getSize();
        int x = (size.width - stringWidth) / 2;
        int y = stringHeight + (size.height - stringHeight) / 2;
        if (m_color == Color.WHITE)
            g.setColor(java.awt.Color.black);
        else
            g.setColor(java.awt.Color.white);
        g.drawString(m_string, x, y);
    }
}

//=============================================================================
