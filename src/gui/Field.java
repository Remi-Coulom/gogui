//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.util.*;
import javax.swing.*;
import go.*;

//-----------------------------------------------------------------------------

public class Field
    extends JButton
    implements ActionListener, FocusListener
{
    Field(gui.Board board, go.Point p, boolean isHandicap)
    {
        m_board = board;
        m_color = go.Color.EMPTY;
        m_point = p;
        m_isHandicap = isHandicap;
        Dimension size = m_board.getPreferredFieldSize();
        setPreferredSize(size);
        setMinimumSize(new Dimension(3, 3));
        setBorder(null);
        setOpaque(false);
        addActionListener(this);
        addFocusListener(this);
    }

    public void actionPerformed(ActionEvent event)
    {
        int modifiers = event.getModifiers();
        m_board.fieldClicked(m_point, modifiers);
    }

    public void clearInfluence()
    {
        m_influenceSet = false;
        m_influence = 0;
    }

    public void focusGained(FocusEvent event)
    {
        m_board.setFocusPoint(m_point);
    }

    public void focusLost(FocusEvent event)
    {
    }

    public go.Color getColor()
    {
        return m_color;
    }

    public void paintComponent(Graphics graphics)
    {
        Dimension size = getSize();
        if (m_fieldColor != null)
        {
            graphics.setColor(m_fieldColor);
            graphics.fillRect(0, 0, size.width, size.height);
        }
        if (m_color == go.Color.BLACK)
            drawStone(graphics, java.awt.Color.black);
        else if (m_color == go.Color.WHITE)
            drawStone(graphics, java.awt.Color.white);
        else
            drawGrid(graphics);
        if (m_influenceSet)
            drawInfluence(graphics);
        if (m_markup)
            drawMarkup(graphics);
        if (m_crossHair)
            drawCrossHair(graphics);
        if (m_lastMoveMarker)
            drawLastMoveMarker(graphics);
        if (m_select)
            drawSelect(graphics);
        if (! m_string.equals(""))
            drawString(graphics);
        if (isFocusOwner())
            drawFocus(graphics);
    }

    public void setFieldBackground(java.awt.Color color)
    {
        m_fieldColor = color;
    }

    public void setColor(go.Color color)
    {
        m_color = color;
    }

    public void setCrossHair(boolean crossHair)
    {
        m_crossHair = crossHair;
    }

    public void setInfluence(double value)
    {
        if (value > 1.)
            value = 1.;
        else if (value < -1.)
            value = -1.;
        m_influence = value;
        m_influenceSet = true;
    }

    public void setLastMoveMarker(boolean lastMoveMarker)
    {
        m_lastMoveMarker = lastMoveMarker;
    }

    public void setMarkup(boolean markup)
    {
        m_markup = markup;
    }

    public void setSelect(boolean select)
    {
        m_select = select;
    }

    public void setString(String s)
    {
        m_string = s;
    }

    private boolean m_crossHair;

    private boolean m_isHandicap;

    private boolean m_lastMoveMarker;

    private boolean m_markup;

    private boolean m_influenceSet;

    private boolean m_select;

    private double m_influence;

    private String m_string = "";

    private java.awt.Color m_fieldColor;

    private static java.awt.Color m_influenceBlackColor
        = new java.awt.Color(96, 96, 96);

    private static java.awt.Color m_influenceWhiteColor
        = new java.awt.Color(224, 224, 224);

    private go.Color m_color;

    private go.Point m_point;

    private gui.Board m_board;

    private void drawCircle(Graphics g, java.awt.Color color, boolean fill)
    {
        Dimension size = getSize();
        int dx = size.width / 4;
        int dy = size.height / 4;
        g.setColor(color);
        int radiusX = size.width / 6;
        int radiusY = size.height / 6;
        int x = size.width / 2 - radiusX;
        int y = size.height / 2 - radiusY;
        int width = 2 * radiusX + 1;
        int height = 2 * radiusY + 1;
        if (fill)
            g.fillOval(x, y, width, height);
        else
            g.drawOval(x, y, width, height);
    }

    private void drawCrossHair(Graphics g)
    {
        Dimension size = getSize();
        int dx = size.width / 5;
        int dy = size.height / 5;
        g.setColor(java.awt.Color.red);
        g.drawLine(dx, size.height / 2, size.width - dx, size.height / 2);
        g.drawLine(size.width / 2, dy, size.width / 2, size.height - dy);
    }

    private void drawFocus(Graphics g)
    {
        Dimension size = getSize();
        int dx = size.width / 6;
        int dy = size.height / 6;
        g.setColor(java.awt.Color.blue);
        g.drawLine(dx, dy, 2 * dx, dy);
        g.drawLine(dx, dy, dx, 2 * dy);
        g.drawLine(dx, size.height - 2 * dy, dx, size.height - dy);
        g.drawLine(dx, size.height - dy, 2 * dx, size.height - dy);
        g.drawLine(size.width - 2 * dx, dy, size.width - dx, dy);
        g.drawLine(size.width - dx, dy, size.width - dx, 2 * dy);
        g.drawLine(size.width - dx, size.height - dy,
                   size.width - dx, size.height - 2 * dy);
        g.drawLine(size.width - dx, size.height - dy,
                   size.width - 2 * dx, size.height - dy);
    }

    private void drawGrid(Graphics g)
    {
        int boardSize = m_board.getBoard().getSize();
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
        int dx = (int)(size.width * (0.31 + (1 - d) * 0.69));
        int dy = (int)(size.height * (0.31 + (1 - d) * 0.69));
        g.fillRect(dx / 2, dy / 2, size.width - dx + 1, size.height - dy + 1);
    }

    private void drawLastMoveMarker(Graphics g)
    {
        drawCircle(g, java.awt.Color.red, true);
    }

    private void drawMarkup(Graphics g)
    {
        Dimension size = getSize();
        int dx = size.width / 2;
        int dy = size.height / 2;
        g.setColor(java.awt.Color.blue);
        g.drawRect(dx / 2, dy / 2, size.width - dx + 1, size.height - dy + 1);
    }

    private void drawSelect(Graphics g)
    {
        drawCircle(g, java.awt.Color.blue, true);
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
        if (m_color == go.Color.WHITE)
            g.setColor(java.awt.Color.black);
        else
            g.setColor(java.awt.Color.white);
        g.drawString(m_string, x, y);
    }
}

//-----------------------------------------------------------------------------
