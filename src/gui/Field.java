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
import utils.RadialGradientContext;
import utils.RadialGradientPaint;
 
//-----------------------------------------------------------------------------

public class Field
    extends JComponent
    implements FocusListener
{
    public Field(gui.Board board, go.Point p)
    {
        m_board = board;
        m_color = go.Color.EMPTY;
        m_territory = go.Color.EMPTY;
        m_point = p;
        Dimension size = m_board.getPreferredFieldSize();
        setPreferredSize(size);
        setMinimumSize(m_board.getMinimumFieldSize());
        Font font = UIManager.getFont("Label.font");        
        if (font != null)
        {
            font = font.deriveFont(Font.BOLD);
            if (font != null)
                setFont(font);
        }
        setBorder(null);
        addFocusListener(this);
        KeyAdapter keyAdapter = new KeyAdapter()
            {
                public void keyPressed(KeyEvent event)
                {
                    int code = event.getKeyCode();
                    int modifiers = event.getModifiers();
                    int mask = (ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK
                                | ActionEvent.META_MASK);
                    boolean modifiedSelect = ((modifiers & mask) != 0);
                    if (code == KeyEvent.VK_ENTER && m_board.getShowCursor())
                        m_board.fieldClicked(m_point, modifiedSelect);
                }
            };
        addKeyListener(keyAdapter);
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
                public void mouseReleased(MouseEvent event)
                {
                    java.awt.Point point = event.getPoint();
                    if (! contains((int)point.getX(), (int)point.getY()))
                        return;
                    if (event.getClickCount() == 2)
                        m_board.fieldClicked(m_point, true);
                    else
                    {            
                        int modifiers = event.getModifiers();
                        int mask = (ActionEvent.CTRL_MASK
                                    | ActionEvent.ALT_MASK
                                    | ActionEvent.META_MASK);
                        boolean modifiedSelect = ((modifiers & mask) != 0);
                        m_board.fieldClicked(m_point, modifiedSelect);
                    }
                }
            };
        addMouseListener(mouseAdapter);
    }

    public void clearInfluence()
    {
        m_influenceSet = false;
        m_influence = 0;
    }

    public void focusGained(FocusEvent event)
    {
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

    public int getStoneMargin()
    {
        return getSize().width / 15;
    }

    public String getString()
    {
        return m_string;
    }

    public go.Color getTerritory()
    {
        return m_territory;
    }

    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        Dimension size = getSize();
        drawFieldColor(graphics);
        if (graphics2D == null)
            drawTerritoryGraphics(graphics);
        drawStone(graphics);
        if (graphics2D != null)
            drawTerritoryGraphics2D(graphics2D);
        drawInfluence(graphics);
        drawMarkup(graphics);
        drawCrossHair(graphics);
        drawLastMoveMarker(graphics);
        drawSelect(graphics);
        drawString(graphics);
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

    private boolean m_lastMoveMarker;

    private boolean m_markup;

    private boolean m_influenceSet;

    private boolean m_select;

    private double m_influence;

    private String m_string = "";

    private java.awt.Color m_fieldColor;

    private go.Color m_territory;

    private static java.awt.Color m_influenceBlackColor
        = java.awt.Color.gray;

    private static java.awt.Color m_influenceWhiteColor
        = java.awt.Color.white;

    private go.Color m_color;

    private go.Point m_point;

    private gui.Board m_board;

    private void drawCircle(Graphics graphics, java.awt.Color color,
                            boolean fill)
    {
        graphics.setColor(color);
        Dimension size = getSize();
        int d = size.width * 36 / 100;
        int w = size.width - 2 * d;
        if (fill)
            graphics.fillOval(d, d, w, w);
        else
            graphics.drawOval(d, d, w, w);
    }

    private void drawCrossHair(Graphics graphics)
    {
        if (! m_crossHair)
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        Dimension size = getSize();
        int dx = size.width / 5;
        int dy = size.height / 5;
        graphics.setColor(java.awt.Color.red);
        graphics.drawLine(dx, size.height / 2,
                          size.width - dx, size.height / 2);
        graphics.drawLine(size.width / 2, dy,
                          size.width / 2, size.height - dy);
        graphics.setPaintMode();
    }

    private void drawFieldColor(Graphics graphics)
    {
        if (m_fieldColor != null)
        {
            Graphics2D graphics2D = (Graphics2D)graphics;
            if (graphics2D != null)
            {
                AlphaComposite composite =
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f); 
                graphics2D.setComposite(composite);
            }
            graphics.setColor(m_fieldColor);
            int size = getSize().width;
            graphics.fillRect(0, 0, size, size);
            graphics.setPaintMode();
        }
    }

    private void drawFocus(Graphics graphics)
    {
        if (! isFocusOwner() || ! m_board.getShowCursor())
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f); 
            graphics2D.setComposite(composite);
        }
        Dimension size = getSize();
        int d = size.width / 6;
        int w = size.width;
        graphics.setColor(java.awt.Color.red);
        graphics.drawLine(d, d, 2 * d, d);
        graphics.drawLine(d, d, d, 2 * d);
        graphics.drawLine(d, w - 2 * d - 1, d, w - d - 1);
        graphics.drawLine(d, w - d - 1, 2 * d, w - d - 1);
        graphics.drawLine(w - 2 * d - 1, d, w - d - 1, d);
        graphics.drawLine(w - d - 1, d, w - d - 1, 2 * d);
        graphics.drawLine(w - d - 1, w - d - 1, w - d - 1, w - 2 * d - 1);
        graphics.drawLine(w - d - 1, w - d - 1, w - 2 * d - 1, w - d - 1);
        graphics.setPaintMode();
    }

    private void drawInfluence(Graphics graphics)
    {
        if (! m_influenceSet)
            return;
        double d = Math.abs(m_influence);
        if (d < 0.01)
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        if (m_influence > 0)
            graphics.setColor(m_influenceBlackColor);
        else
            graphics.setColor(m_influenceWhiteColor);
        int size = getSize().width;
        int dd = (int)(size * (0.38 + (1 - d) * 0.62));
        int width = size - dd;
        graphics.fillRect(dd / 2, dd / 2, width, width);
    }

    private void drawLastMoveMarker(Graphics graphics)
    {
        if (! m_lastMoveMarker)
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        drawCircle(graphics, java.awt.Color.red, true);
        graphics.setPaintMode();
    }

    private void drawMarkup(Graphics graphics)
    {
        if (! m_markup)
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        Dimension size = getSize();
        int d = size.width / 4;
        int width = size.width - 2 * d;
        graphics.setColor(java.awt.Color.blue);
        graphics.drawRect(d, d, width, width);
        graphics.drawRect(d + 1, d + 1, width - 2, width - 2);
        graphics.setPaintMode();
    }

    private void drawSelect(Graphics graphics)
    {
        if (! m_select)
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        drawCircle(graphics, java.awt.Color.blue, true);
        graphics.setPaintMode();
    }

    private void drawStone(Graphics graphics)
    {
        int size = getSize().width;
        if (m_color == go.Color.BLACK)
            drawStone(graphics, java.awt.Color.decode("#030303"),
                      java.awt.Color.decode("#666666"), size / 3);
        else if (m_color == go.Color.WHITE)
            drawStone(graphics, java.awt.Color.decode("#d7d0c9"),
                      java.awt.Color.decode("#f6eee6"), size / 3);
    }

    private void drawStone(Graphics graphics, java.awt.Color color,
                           java.awt.Color colorBright, int radius)
    {
        int size = getSize().width;
        int margin = getStoneMargin();
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            int center = size / 3;
            radius = Math.max(radius, 1);
            RadialGradientPaint paint =
                new RadialGradientPaint(new Point2D.Double(center, center),
                                        colorBright,
                                        new Point2D.Double(radius, radius),
                                        color);
            graphics2D.setPaint(paint);
        }
        else
        {
            graphics.setColor(color);
        }
        graphics.fillOval(margin, margin,
                          size - 2 * margin, size - 2 * margin);
    }

    private void drawString(Graphics g)
    {
        if (m_string.equals(""))
            return;
        Dimension size = getSize();
        int stringWidth = g.getFontMetrics().stringWidth(m_string);
        int stringHeight = g.getFont().getSize();
        int x = Math.max((size.width - stringWidth) / 2, 0);
        int y = stringHeight + (size.height - stringHeight) / 2;
        if (m_color == go.Color.WHITE)
            g.setColor(java.awt.Color.black);
        else if (m_color == go.Color.BLACK)
            g.setColor(java.awt.Color.white);
        else
            g.setColor(java.awt.Color.white);
        g.drawString(m_string, x, y);
    }

    private void drawTerritoryGraphics(Graphics graphics)
    {
        if (m_territory != go.Color.EMPTY)
        {
            if (m_territory == go.Color.BLACK)
                graphics.setColor(java.awt.Color.darkGray);
            else if (m_territory == go.Color.WHITE)
                graphics.setColor(java.awt.Color.lightGray);
            int size = getSize().width;
            graphics.fillRect(0, 0, size, size);
        }
    }

    private void drawTerritoryGraphics2D(Graphics2D graphics2D)
    {
        if (m_territory != go.Color.EMPTY)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f); 
            graphics2D.setComposite(composite);
            if (m_territory == go.Color.BLACK)
                graphics2D.setColor(java.awt.Color.darkGray);
            else if (m_territory == go.Color.WHITE)
                graphics2D.setColor(java.awt.Color.white);
            int size = getSize().width;
            graphics2D.fillRect(0, 0, size, size);
            graphics2D.setPaintMode();
        }
    }
}

//-----------------------------------------------------------------------------
