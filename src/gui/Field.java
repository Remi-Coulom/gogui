//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import utils.RadialGradientPaint;
 
//----------------------------------------------------------------------------

/** Component representing a field on the board.
    The implementation assumes that the size of the component is a square,
    which is automatically guaranteed if the board uses SquareLayout.
*/
public class Field
    extends JComponent
    implements FocusListener
{
    public Field(gui.Board board, go.Point point, boolean fastPaint)
    {
        m_board = board;
        m_point = point;
        m_fastPaint = fastPaint;
        setPreferredSize(m_board.getPreferredFieldSize());
        setMinimumSize(m_board.getMinimumFieldSize());
        addFocusListener(this);
        KeyAdapter keyAdapter = new KeyAdapter()
            {
                public void keyPressed(KeyEvent event)
                {
                    int code = event.getKeyCode();
                    int modifiers = event.getModifiers();
                    final int mask = (ActionEvent.CTRL_MASK
                                      | ActionEvent.ALT_MASK
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
                        final int mask = (ActionEvent.CTRL_MASK
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

    public static int getStoneMargin(int size)
    {
        return size / 17;
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
        m_graphics = graphics;
        if (m_fastPaint)
            m_graphics2D = null;
        else
            m_graphics2D = (Graphics2D)graphics;
        m_size = getSize().width;
        if (m_fieldColor != null)
            drawFieldColor();
        if (m_territory != go.Color.EMPTY && m_graphics2D == null)
            drawTerritoryGraphics();
        if (m_color != go.Color.EMPTY)
            drawStone();
        if (m_territory != go.Color.EMPTY && m_graphics2D != null)
            drawTerritoryGraphics2D();
        if (m_influenceSet)
            drawInfluence();
        if (m_markup)
            drawMarkup();
        if (m_crossHair)
            drawCrossHair();
        if (m_lastMoveMarker)
            drawLastMoveMarker();
        if (m_select)
            drawSelect();
        if (! m_string.equals(""))
            drawString();
        if (isFocusOwner() && m_board.getShowCursor())
            drawFocus();
        m_graphics = null;
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
        assert(color != null);
        m_territory = color;
    }

    private boolean m_crossHair;

    private boolean m_fastPaint;

    private boolean m_lastMoveMarker;

    private boolean m_markup;

    private boolean m_influenceSet;

    private boolean m_select;

    private int m_paintSize;

    private int m_size;

    private double m_influence;

    private static final AlphaComposite m_composite5
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    private static final AlphaComposite m_composite7
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); 

    private String m_string = "";

    private java.awt.Color m_fieldColor;

    private go.Color m_territory = go.Color.EMPTY;

    private static final java.awt.Color m_influenceBlackColor
        = java.awt.Color.gray;

    private static final java.awt.Color m_influenceWhiteColor
        = java.awt.Color.white;

    private static final java.awt.Color m_lastMoveMarkerColor
        = java.awt.Color.decode("#b61a17");

    private static final java.awt.Color m_colorBlackStone
        = java.awt.Color.decode("#030303");

    private static final java.awt.Color m_colorBlackStoneBright
        = java.awt.Color.decode("#666666");

    private static final java.awt.Color m_colorWhiteStone
        = java.awt.Color.decode("#d7d0c9");

    private static final java.awt.Color m_colorWhiteStoneBright
        = java.awt.Color.decode("#f6eee6");

    private go.Color m_color = go.Color.EMPTY;

    private go.Point m_point;

    private gui.Board m_board;

    private Graphics m_graphics;

    private Graphics2D m_graphics2D;

    private RadialGradientPaint m_paintBlack;

    private RadialGradientPaint m_paintWhite;

    private void drawCircle(java.awt.Color color)
    {
        m_graphics.setColor(color);
        int d = m_size * 36 / 100;
        int w = m_size - 2 * d;
        m_graphics.fillOval(d, d, w, w);
    }

    private void drawCrossHair()
    {
        setComposite(m_composite7);
        int d = m_size / 5;
        int center = m_size / 2;
        m_graphics.setColor(java.awt.Color.red);
        m_graphics.drawLine(d, center, m_size - d, center);
        m_graphics.drawLine(center, d, center, m_size - d);
        m_graphics.setPaintMode();
    }

    private void drawFieldColor()
    {
        setComposite(m_composite5);
        m_graphics.setColor(m_fieldColor);
        m_graphics.fillRect(0, 0, m_size, m_size);
        m_graphics.setPaintMode();
    }

    private void drawFocus()
    {
        setComposite(m_composite5);
        int d = m_size / 6;
        int w = m_size;
        int d2 = 2 * d;
        m_graphics.setColor(java.awt.Color.red);
        m_graphics.drawLine(d, d, d2, d);
        m_graphics.drawLine(d, d, d, d2);
        m_graphics.drawLine(d, w - d2 - 1, d, w - d - 1);
        m_graphics.drawLine(d, w - d - 1, d2, w - d - 1);
        m_graphics.drawLine(w - d2 - 1, d, w - d - 1, d);
        m_graphics.drawLine(w - d - 1, d, w - d - 1, d2);
        m_graphics.drawLine(w - d - 1, w - d - 1, w - d - 1, w - d2 - 1);
        m_graphics.drawLine(w - d - 1, w - d - 1, w - d2 - 1, w - d - 1);
        m_graphics.setPaintMode();
    }

    private void drawInfluence()
    {
        double d = Math.abs(m_influence);
        if (d < 0.01)
            return;
        setComposite(m_composite7);
        if (m_influence > 0)
            m_graphics.setColor(m_influenceBlackColor);
        else
            m_graphics.setColor(m_influenceWhiteColor);
        int dd = (int)(m_size * (0.38 + (1 - d) * 0.62));
        int width = m_size - dd;
        m_graphics.fillRect(dd / 2, dd / 2, width, width);
    }

    private void drawLastMoveMarker()
    {
        setComposite(m_composite7);
        drawCircle(m_lastMoveMarkerColor);
        m_graphics.setPaintMode();
    }

    private void drawMarkup()
    {
        setComposite(m_composite7);
        int d = m_size / 4;
        int width = m_size - 2 * d;
        m_graphics.setColor(java.awt.Color.blue);
        m_graphics.drawRect(d, d, width, width);
        m_graphics.drawRect(d + 1, d + 1, width - 2, width - 2);
        m_graphics.setPaintMode();
    }

    private void drawSelect()
    {
        setComposite(m_composite7);
        drawCircle(java.awt.Color.blue);
        m_graphics.setPaintMode();
    }

    private void drawStone()
    {
        if (m_color == go.Color.BLACK)
            drawStone(m_colorBlackStone, m_colorBlackStoneBright);
        else if (m_color == go.Color.WHITE)
            drawStone(m_colorWhiteStone, m_colorWhiteStoneBright);
    }

    private void drawStone(java.awt.Color colorNormal,
                           java.awt.Color colorBright)
    {
        int margin = getStoneMargin(m_size);
        if (m_graphics2D != null && m_size >= 7)
        {
            RadialGradientPaint paint =
                getPaint(m_color, m_size, colorNormal, colorBright);
            m_graphics2D.setPaint(paint);
        }
        else
        {
            m_graphics.setColor(colorNormal);
        }
        m_graphics.fillOval(margin, margin,
                            m_size - 2 * margin, m_size - 2 * margin);
    }

    private void drawString()
    {
        m_board.setFont(m_graphics, m_size);
        FontMetrics metrics = m_graphics.getFontMetrics();
        int stringWidth = metrics.stringWidth(m_string);
        int stringHeight = metrics.getAscent();
        int x = Math.max((m_size - stringWidth) / 2, 0);
        int y = stringHeight + (m_size - stringHeight) / 2;
        if (m_color == go.Color.WHITE)
            m_graphics.setColor(java.awt.Color.black);
        else
            m_graphics.setColor(java.awt.Color.white);
        Rectangle clip = null;
        if (stringWidth > 0.9 * m_size)
        {
            clip = m_graphics.getClipBounds();
            m_graphics.setClip(clip.x, clip.y,
                               (int)(0.9 * clip.width), clip.height);
        }
        m_graphics.drawString(m_string, x, y);
        if (clip != null)
            m_graphics.setClip(clip.x, clip.y, clip.width, clip.height);
    }

    private void drawTerritoryGraphics()
    {
        if (m_territory == go.Color.BLACK)
            m_graphics.setColor(java.awt.Color.darkGray);
        else
        {
            assert(m_territory == go.Color.WHITE);
            m_graphics.setColor(java.awt.Color.lightGray);
        }
        m_graphics.fillRect(0, 0, m_size, m_size);
    }

    private void drawTerritoryGraphics2D()
    {
        setComposite(m_composite5);
        if (m_territory == go.Color.BLACK)
            m_graphics2D.setColor(java.awt.Color.darkGray);
        else
        {
            assert(m_territory == go.Color.WHITE);
            m_graphics2D.setColor(java.awt.Color.white);
        }
        m_graphics2D.fillRect(0, 0, m_size, m_size);
        m_graphics2D.setPaintMode();
    }

    private RadialGradientPaint getPaint(go.Color color, int size,
                                         java.awt.Color colorNormal,
                                         java.awt.Color colorBright)
    {
        RadialGradientPaint paint;
        if (color == go.Color.BLACK)
             paint = m_paintBlack;
        else
        {
            assert(color == go.Color.WHITE);
            paint = m_paintWhite;
        }
        if (size == m_paintSize && paint != null)
            return paint;
        int radius = Math.max(size / 3, 1);
        int center = size / 3;
        Point2D.Double centerPoint =
            new Point2D.Double(center, center);
        Point2D.Double radiusPoint =
            new Point2D.Double(radius, radius);
        paint = new RadialGradientPaint(centerPoint, colorBright,
                                        radiusPoint, colorNormal);
        if (color == go.Color.BLACK)
            m_paintBlack = paint;
        else
            m_paintWhite = paint;
        m_paintSize = size;
        return paint;
    }

    private void setComposite(AlphaComposite composite)
    {
        if (m_graphics2D != null)
            m_graphics2D.setComposite(composite);
    }
}

//----------------------------------------------------------------------------
