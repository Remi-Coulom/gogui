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


import java.awt.image.*;
 
//-----------------------------------------------------------------------------

class RadialGradientPaint
    implements Paint
{
    public RadialGradientPaint(double x, double y, java.awt.Color pointColor,
                               Point2D radius, java.awt.Color backgroundColor)
    {
        assert(radius.distance(0, 0) > 0);
        m_point = new Point2D.Double(x, y);
        m_pointColor = pointColor;
        m_radius = radius;
        m_backgroundColor = backgroundColor;
    }
    
    public PaintContext createContext(ColorModel colorModel,
                                      Rectangle deviceBounds,
                                      Rectangle2D userBounds,
                                      AffineTransform xform,
                                      RenderingHints hints)
    {
        Point2D transformedPoint = xform.transform(m_point, null);
        Point2D transformedRadius = xform.deltaTransform(m_radius, null);
        return new RadialGradientContext(transformedPoint, m_pointColor,
                                         transformedRadius, m_backgroundColor);
    }
    
    public int getTransparency()
    {
        int a1 = m_pointColor.getAlpha();
        int a2 = m_backgroundColor.getAlpha();
        return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
    }

    private Point2D m_point;

    private Point2D m_radius;

    private java.awt.Color m_pointColor;

    private java.awt.Color m_backgroundColor;
}

//-----------------------------------------------------------------------------

class RadialGradientContext
    implements PaintContext
{
    public RadialGradientContext(Point2D p, java.awt.Color c1, Point2D r,
                                 java.awt.Color c2)
    {
        m_point = p;
        mC1 = c1;
        m_radius = r;
        mC2 = c2;
    }
    
    public void dispose()
    {
    }
    
    public ColorModel getColorModel()
    {
        return ColorModel.getRGBdefault();
    }
  
    public Raster getRaster(int x, int y, int w, int h)
    {
        WritableRaster raster =
            getColorModel().createCompatibleWritableRaster(w, h);
        
        int[] data = new int[w * h * 4];
        for (int j = 0; j < h; j++)
        {
            for (int i = 0; i < w; i++)
            {
                double distance = m_point.distance(x + i, y + j);
                double radius = m_radius.distance(0, 0);
                double ratio = distance / radius;
                if (ratio > 1.0)
                    ratio = 1.0;
                
                int base = (j * w + i) * 4;
                data[base + 0] = (int)(mC1.getRed() + ratio *
                                       (mC2.getRed() - mC1.getRed()));
                data[base + 1] = (int)(mC1.getGreen() + ratio *
                                       (mC2.getGreen() - mC1.getGreen()));
                data[base + 2] = (int)(mC1.getBlue() + ratio *
                                       (mC2.getBlue() - mC1.getBlue()));
                data[base + 3] = (int)(mC1.getAlpha() + ratio *
                                       (mC2.getAlpha() - mC1.getAlpha()));
            }
        }
        raster.setPixels(0, 0, w, h, data);
        
        return raster;
    }

    private Point2D m_point;

    private Point2D m_radius;

    private java.awt.Color mC1;

    private java.awt.Color mC2;
}

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
                public void mouseClicked(MouseEvent event)
                {
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
        if (m_fieldColor != null)
        {
            if (graphics2D != null)
            {
                AlphaComposite composite =
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f); 
                graphics2D.setComposite(composite);
            }
            graphics.setColor(m_fieldColor);
            graphics.fillRect(0, 0, size.width, size.height);
        }
        if (m_territory != go.Color.EMPTY && graphics2D == null)
        {
            if (m_territory == go.Color.BLACK)
                graphics.setColor(java.awt.Color.darkGray);
            else if (m_territory == go.Color.WHITE)
                graphics.setColor(java.awt.Color.lightGray);
            graphics.fillRect(0, 0, size.width, size.height);
        }
        if (m_color == go.Color.BLACK)
            drawStone(graphics, java.awt.Color.black, java.awt.Color.gray);
        else if (m_color == go.Color.WHITE)
            drawStone(graphics, new java.awt.Color(0.930f, 0.895f, 0.867f),
                      new java.awt.Color(1.0f, 1.0f, 1.0f));
        if (m_territory != go.Color.EMPTY && graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f); 
            graphics2D.setComposite(composite);
            if (m_territory == go.Color.BLACK)
                graphics.setColor(java.awt.Color.black);
            else if (m_territory == go.Color.WHITE)
                graphics.setColor(java.awt.Color.white);
            graphics.fillRect(0, 0, size.width, size.height);
        }
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        if (m_influenceSet)
            drawInfluence(graphics, m_influence);
        if (m_markup)
            drawMarkup(graphics);
        if (m_crossHair)
            drawCrossHair(graphics);
        if (m_lastMoveMarker)
            drawLastMoveMarker(graphics);
        if (m_select)
            drawSelect(graphics);
        if (graphics2D != null)
            graphics.setPaintMode();
        if (! m_string.equals(""))
            drawString(graphics);
        if (graphics2D != null)
        {
            AlphaComposite composite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 
            graphics2D.setComposite(composite);
        }
        if (isFocusOwner() && m_board.getShowCursor())
            drawFocus(graphics);
        if (graphics2D != null)
            graphics.setPaintMode();
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

    private void drawInfluence(Graphics graphics, double influence)
    {
        Dimension size = getSize();
        double d = Math.abs(influence);
        if (d < 0.01)
            return;
        if (influence > 0)
            graphics.setColor(m_influenceBlackColor);
        else
            graphics.setColor(m_influenceWhiteColor);
        int dd = (int)(size.width * (0.31 + (1 - d) * 0.69));
        int width = size.width - dd + 1;
        graphics.fillRect(dd / 2, dd / 2, width, width);
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

    private void drawStone(Graphics graphics, java.awt.Color color,
                           java.awt.Color colorBright)
    {
        Dimension size = getSize();
        int width = size.width;
        int height = size.height;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
        {
            RadialGradientPaint paint =
                new RadialGradientPaint(width / 3, height / 3,
                                       colorBright,
                                       new Point2D.Double(width / 4 , height / 4),
                                       color);
            graphics2D.setPaint(paint);
            graphics.fillOval(1, 1, width - 2, height - 2);
        }
        else
        {
            graphics.setColor(color);
            graphics.fillOval(1, 1, width - 2, height - 2);
        }
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
