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
    extends JComponent
    implements FocusListener, KeyListener, MouseListener
{
    Field(gui.Board board, go.Point p, boolean isHandicap)
    {
        m_board = board;
        m_color = go.Color.EMPTY;
        m_territory = go.Color.EMPTY;
        m_point = p;
        m_isHandicap = isHandicap;
        Dimension size = m_board.getPreferredFieldSize();
        setPreferredSize(size);
        Font font = UIManager.getFont("Label.font");        
        if (font != null)
        {
            font = font.deriveFont(Font.BOLD);
            if (font != null)
                setFont(font);
        }
        setMinimumSize(new Dimension(3, 3));
        setBorder(null);
        addFocusListener(this);
        addKeyListener(this);
        addMouseListener(this);
    }

    public void clearInfluence()
    {
        m_influenceSet = false;
        m_influence = 0;
    }

    public void focusGained(FocusEvent event)
    {
        m_board.setFocusPoint(m_point);
        repaint();
    }

    public void focusLost(FocusEvent event)
    {
        repaint();
    }

    public go.Color getColor()
    {
        return m_color;
    }

    public boolean getMarkup()
    {
        return m_markup;
    }

    public boolean getSelect()
    {
        return m_select;
    }

    public String getString()
    {
        return m_string;
    }

    public go.Color getTerritory()
    {
        return m_territory;
    }

    public void keyPressed(KeyEvent event)
    {
        int code = event.getKeyCode();
        int modifiers = event.getModifiers();
        int mask = (ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK
                    | ActionEvent.META_MASK);
        boolean modifiedSelect = ((modifiers & mask) != 0);
        if (code == KeyEvent.VK_ENTER)
            m_board.fieldClicked(m_point, modifiedSelect);
    }

    public void keyReleased(KeyEvent event)
    {
    }

    public void keyTyped(KeyEvent event)
    {
    }

    public void mouseClicked(MouseEvent event)
    {
        if (event.getClickCount() == 2)
            m_board.fieldClicked(m_point, true);
        else
        {            
            int modifiers = event.getModifiers();
            int mask = (ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK
                        | ActionEvent.META_MASK);
            boolean modifiedSelect = ((modifiers & mask) != 0);
            m_board.fieldClicked(m_point, modifiedSelect);
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
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
        if (m_territory == go.Color.BLACK)
            drawInfluence(graphics, 1.0);
        else if (m_territory == go.Color.WHITE)
            drawInfluence(graphics, -1.0);
        else if (m_influenceSet)
            drawInfluence(graphics, m_influence);
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

    public void setTerritory(go.Color color)
    {
        m_territory = color;
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

    private go.Color m_territory;

    private static java.awt.Color m_influenceBlackColor
        = new java.awt.Color(96, 96, 96);

    private static java.awt.Color m_influenceWhiteColor
        = new java.awt.Color(224, 224, 224);

    private go.Color m_color;

    private go.Point m_point;

    private gui.Board m_board;

    private void drawCircle(Graphics g, java.awt.Color color, boolean fill)
    {
        g.setColor(color);
        Dimension size = getSize();
        int d = size.width * 36 / 100;
        int w = size.width - 2 * d;
        if (fill)
            g.fillOval(d, d, w, w);
        else
            g.drawOval(d, d, w, w);
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
        int d = size.width / 6;
        int w = size.width;
        g.setColor(java.awt.Color.red);
        g.drawLine(d, d, 2 * d, d);
        g.drawLine(d, d, d, 2 * d);
        g.drawLine(d, w - 2 * d - 1, d, w - d - 1);
        g.drawLine(d, w - d - 1, 2 * d, w - d - 1);
        g.drawLine(w - 2 * d - 1, d, w - d - 1, d);
        g.drawLine(w - d - 1, d, w - d - 1, 2 * d);
        g.drawLine(w - d - 1, w - d - 1, w - d - 1, w - 2 * d - 1);
        g.drawLine(w - d - 1, w - d - 1, w - 2 * d - 1, w - d - 1);
    }

    private void drawGrid(Graphics g)
    {
        int boardSize = m_board.getBoard().getSize();
        Dimension size = getSize();
        int w2 = size.width / 2;
        g.setColor(java.awt.Color.darkGray);
        int xMin = 0;
        int xMax = size.width - 1;
        int yMin = 0;
        int yMax = size.height - 1;
        int x = m_point.getX();
        int y = m_point.getY();
        if (x == 0)
            xMin = w2;
        else if (x == boardSize - 1)
            xMax = w2;
        if (y == boardSize - 1)
            yMin = w2;
        else if (y == 0)
            yMax = w2;
        g.drawLine(xMin, w2, xMax, w2);
        g.drawLine(w2, yMin, w2, yMax);
        if (m_isHandicap)
        {
            int r = size.width / 10;
            g.fillOval(w2 - r, w2 - r, 2 * r + 1, 2 * r + 1);
        }
    }

    private void drawInfluence(Graphics g, double influence)
    {
        Dimension size = getSize();
        double d = Math.abs(influence);
        if (d < 0.01)
            return;
        if (influence > 0)
            g.setColor(m_influenceBlackColor);
        else
            g.setColor(m_influenceWhiteColor);
        int dd = (int)(size.width * (0.31 + (1 - d) * 0.69));
        int width = size.width - dd + 1;
        g.fillRect(dd / 2, dd / 2, width, width);
    }

    private void drawLastMoveMarker(Graphics g)
    {
        drawCircle(g, java.awt.Color.red, true);
    }

    private void drawMarkup(Graphics g)
    {
        Dimension size = getSize();
        int d = size.width / 4;
        int width = size.width - 2 * d;
        g.setColor(java.awt.Color.blue);
        g.drawRect(d, d, width, width);
        g.drawRect(d + 1, d + 1, width - 2, width - 2);
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
        else if (m_color == go.Color.BLACK)
            g.setColor(java.awt.Color.white);
        else
            g.setColor(java.awt.Color.white);
        g.drawString(m_string, x, y);
    }
}

//-----------------------------------------------------------------------------
