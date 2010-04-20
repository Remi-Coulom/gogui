// Field.java

package net.sf.gogui.boardpainter;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.LineMetrics;
import java.awt.geom.Point2D;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;

/** State of a field on the board. */
public class Field
    implements ConstField
{
    public Field()
    {
    }

    public void clearInfluence()
    {
        m_influenceSet = false;
        m_influence = 0;
    }

    public void draw(Graphics graphics, int size, int x, int y,
                     Image boardImage, int boardWidth)
    {
        if (! graphics.hitClip(x, y, size, size))
            return;
        m_graphics = graphics.create(x, y, size, size);
        if (m_graphics instanceof Graphics2D)
            m_graphics2D = (Graphics2D)m_graphics;
        else
            m_graphics2D = null;
        m_size = size;
        if (m_fieldColor != null)
            drawFieldColor();
        if (m_territory != EMPTY && m_graphics2D == null)
            drawTerritoryGraphics();
        if (m_color != EMPTY)
            drawStone(m_color, false);
        if (m_ghostStone != null)
            drawStone(m_ghostStone, true);
        if (m_label != null && ! m_label.equals(""))
            drawLabel(x, y, graphics, boardImage, boardWidth);
        if (m_territory != EMPTY && m_graphics2D != null)
            drawTerritoryGraphics2D();
        if (m_influenceSet)
            drawInfluence();
        drawMarks();
        if (m_crossHair)
            drawCrossHair();
        if (m_lastMoveMarker)
            drawLastMoveMarker();
        if (m_select)
            drawSelect();
        if (m_cursor)
            drawCursor();
        m_graphics = null;
    }

    public GoColor getColor()
    {
        return m_color;
    }

    public boolean getCursor()
    {
        return m_cursor;
    }

    public boolean getCrossHair()
    {
        return m_crossHair;
    }

    public Color getFieldBackground()
    {
        return m_fieldColor;
    }

    public boolean getMark()
    {
        return m_mark;
    }

    public boolean getMarkCircle()
    {
        return m_markCircle;
    }

    public boolean getMarkSquare()
    {
        return m_markSquare;
    }

    public boolean getMarkTriangle()
    {
        return m_markTriangle;
    }

    public boolean getSelect()
    {
        return m_select;
    }

    /** @see #setGhostStone */
    public GoColor getGhostStone()
    {
        return m_ghostStone;
    }

    public static int getStoneMargin(int size)
    {
        return size / 17;
    }

    public String getLabel()
    {
        return m_label;
    }

    public GoColor getTerritory()
    {
        return m_territory;
    }

    public boolean isInfluenceSet()
    {
        return m_influenceSet;
    }

    public void setFieldBackground(Color color)
    {
        m_fieldColor = color;
    }

    public void setColor(GoColor color)
    {
        m_color = color;
    }

    public void setCrossHair(boolean crossHair)
    {
        m_crossHair = crossHair;
    }

    public void setCursor(boolean cursor)
    {
        m_cursor = cursor;
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

    public void setMark(boolean mark)
    {
        m_mark = mark;
    }

    public void setMarkCircle(boolean mark)
    {
        m_markCircle = mark;
    }

    public void setMarkSquare(boolean mark)
    {
        m_markSquare = mark;
    }

    public void setMarkTriangle(boolean mark)
    {
        m_markTriangle = mark;
    }

    public void setSelect(boolean select)
    {
        m_select = select;
    }

    /** Set a shadow stone at this field.
        A shadow stone is a stone which is drawn with some transparency.
        It can be used to display stones which are not part of the current
        board position (e.g. search variations)
        @param color The color of the shadow stone or null to remove a
        shadow stone. */
    public void setGhostStone(GoColor color)
    {
        m_ghostStone = color;
    }

    public void setLabel(String s)
    {
        m_label = s;
    }

    public void setTerritory(GoColor color)
    {
        assert color != null;
        m_territory = color;
    }

    private boolean m_crossHair;

    private boolean m_cursor;

    private boolean m_lastMoveMarker;

    private boolean m_mark;

    private boolean m_markCircle;

    private boolean m_markSquare;

    private boolean m_markTriangle;

    private boolean m_influenceSet;

    private boolean m_select;

    private static int s_cachedFontFieldSize;

    private int m_paintSizeBlack;

    private int m_paintSizeWhite;

    private int m_size;

    private double m_influence;

    private static final AlphaComposite COMPOSITE_4
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);

    private static final AlphaComposite COMPOSITE_5
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    private static final AlphaComposite COMPOSITE_6
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);

    private static final AlphaComposite COMPOSITE_7
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);

    private static final AlphaComposite COMPOSITE_8
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    private static final AlphaComposite COMPOSITE_95
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f);

    private static final AlphaComposite COMPOSITE_97
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.97f);

    private static final Stroke THICK_STROKE
        = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);

    private String m_label = "";

    private Color m_fieldColor;

    private GoColor m_territory = EMPTY;

    private static final Color COLOR_INFLUENCE_BLACK = Color.gray;

    private static final Color COLOR_INFLUENCE_WHITE = Color.white;

    private static final Color COLOR_LAST_MOVE
        = Color.decode("#888888");

    private static final Color COLOR_MARK = Color.decode("#4040ff");

    private static final Color COLOR_STONE_BLACK = Color.decode("#3b3d3a");

    private static final Color COLOR_STONE_BLACK_BRIGHT
        = Color.decode("#99998c");

    private static final Color COLOR_STONE_WHITE = Color.decode("#d3d7cf");

    private static final Color COLOR_STONE_WHITE_BRIGHT
        = Color.decode("#ffffff");

    private static Font s_cachedFont;

    private GoColor m_color = EMPTY;

    private GoColor m_ghostStone;

    private Graphics m_graphics;

    private Graphics2D m_graphics2D;

    private RadialGradientPaint m_paintBlack;

    private RadialGradientPaint m_paintWhite;

    private void drawCircle(Color color)
    {
        m_graphics.setColor(color);
        int d = m_size * 4 / 10;
        int w = m_size - 2 * d;
        m_graphics.fillOval(d, d, w, w);
    }

    private void drawCrossHair()
    {
        setComposite(COMPOSITE_7);
        int d = m_size / 5;
        int center = m_size / 2;
        m_graphics.setColor(Color.red);
        m_graphics.drawLine(d, center, m_size - d, center);
        m_graphics.drawLine(center, d, center, m_size - d);
        m_graphics.setPaintMode();
    }

    private void drawCursor()
    {
        setComposite(COMPOSITE_7);
        int d = m_size / 6;
        int w = m_size;
        int d2 = 2 * d;
        m_graphics.setColor(COLOR_LAST_MOVE);
        Stroke oldStroke = null;
        if (m_graphics2D != null && m_size > 10)
        {
            oldStroke = m_graphics2D.getStroke();
            m_graphics2D.setStroke(THICK_STROKE);
        }
        m_graphics.drawLine(d, d, d2, d);
        m_graphics.drawLine(d, d, d, d2);
        m_graphics.drawLine(d, w - d2 - 1, d, w - d - 1);
        m_graphics.drawLine(d, w - d - 1, d2, w - d - 1);
        m_graphics.drawLine(w - d2 - 1, d, w - d - 1, d);
        m_graphics.drawLine(w - d - 1, d, w - d - 1, d2);
        m_graphics.drawLine(w - d - 1, w - d - 1, w - d - 1, w - d2 - 1);
        m_graphics.drawLine(w - d - 1, w - d - 1, w - d2 - 1, w - d - 1);
        if (oldStroke != null)
            m_graphics2D.setStroke(oldStroke);
        m_graphics.setPaintMode();
    }

    private void drawFieldColor()
    {
        setComposite(COMPOSITE_5);
        m_graphics.setColor(m_fieldColor);
        m_graphics.fillRect(0, 0, m_size, m_size);
        m_graphics.setPaintMode();
    }

    private void drawInfluence()
    {
        double d = Math.abs(m_influence);
        if (d < 0.01)
            return;
        setComposite(COMPOSITE_6);
        if (m_influence > 0)
            m_graphics.setColor(COLOR_INFLUENCE_BLACK);
        else
            m_graphics.setColor(COLOR_INFLUENCE_WHITE);
        int dd = (int)(m_size * (0.38 + (1 - d) * 0.62));
        int width = m_size - dd;
        m_graphics.fillRect(dd / 2, dd / 2, width, width);
    }

    private void drawLabel(int fieldX, int fieldY, Graphics boardGraphics,
                           Image boardImage, int boardWidth)
    {
        setComposite(COMPOSITE_97);
        setFont(m_graphics, m_size);
        FontMetrics fontMetrics = m_graphics.getFontMetrics();
        LineMetrics lineMetrics =
            fontMetrics.getLineMetrics(m_label, m_graphics);
        int width = fontMetrics.stringWidth(m_label);
        int height = fontMetrics.getHeight();
        int ascent = (int)lineMetrics.getAscent();
        int x = Math.max((m_size - width) / 2, 0);
        int y = (ascent + m_size) / 2;
        if (m_ghostStone == null)
        {
            if (m_color == BLACK)
                m_graphics.setColor(Color.white);
            else
                m_graphics.setColor(Color.black);
        }
        else
        {
            if (m_ghostStone == BLACK)
                m_graphics.setColor(Color.white);
            else
                m_graphics.setColor(Color.black);
        }
        Rectangle oldClip = m_graphics.getClipBounds();
        width = Math.min(width, (int)(0.95 * m_size));
        m_graphics.setClip(x, y - ascent, width, height);
        if (m_color == EMPTY && m_ghostStone == null)
        {
            Rectangle oldBoardClip = boardGraphics.getClipBounds();
            boardGraphics.setClip(fieldX + x, fieldY + y - ascent,
                                  width, height);
            boardGraphics.drawImage(boardImage, 0, 0, boardWidth, boardWidth,
                                    null);
            boardGraphics.setClip(oldBoardClip);
            m_graphics.setColor(Color.black);
        }
        m_graphics.drawString(m_label, x, y);
        m_graphics.setClip(oldClip);
    }

    private void drawLastMoveMarker()
    {
        setComposite(COMPOSITE_7);
        drawCircle(COLOR_LAST_MOVE);
        m_graphics.setPaintMode();
    }

    private void drawMarks()
    {
        setComposite(COMPOSITE_95);
        int d = m_size / 4;
        int width = m_size - 2 * d;
        m_graphics.setColor(COLOR_MARK);
        Stroke oldStroke = null;
        if (m_graphics2D != null && m_size > 10)
        {
            oldStroke = m_graphics2D.getStroke();
            m_graphics2D.setStroke(THICK_STROKE);
        }
        if (m_mark)
        {
            m_graphics.drawLine(d, d, d + width, d + width);
            m_graphics.drawLine(d, d + width, d + width, d);
        }
        if (m_markCircle)
            m_graphics.drawOval(d, d, width - 1, width - 1);
        if (m_markSquare)
            m_graphics.drawRect(d, d, width - 1, width - 1);
        if (m_markTriangle)
        {
            int height = (int)(0.866 * width);
            int top = (int)(0.866 * (width - height) / 2);
            int bottom = top + height - 1;
            m_graphics.drawLine(d, d + bottom, d + width / 2, d + top);
            m_graphics.drawLine(d + width / 2, d + top,
                                d + width, d + bottom);
            m_graphics.drawLine(d + width, d + bottom, d, d + bottom);
        }
        if (oldStroke != null)
            m_graphics2D.setStroke(oldStroke);
        m_graphics.setPaintMode();
    }

    private void drawSelect()
    {
        setComposite(COMPOSITE_95);
        drawCircle(COLOR_MARK);
        m_graphics.setPaintMode();
    }

    private void drawStone(GoColor color, boolean isGhostStone)
    {
        if (color == BLACK)
            drawStone(color, COLOR_STONE_BLACK, COLOR_STONE_BLACK_BRIGHT,
                      isGhostStone);
        else if (color == WHITE)
            drawStone(color, COLOR_STONE_WHITE, COLOR_STONE_WHITE_BRIGHT,
                      isGhostStone);
    }

    private void drawStone(GoColor color, Color colorNormal,
                           Color colorBright, boolean isGhostStone)
    {
        int margin = getStoneMargin(m_size);
        if (m_graphics2D != null && m_size >= 7)
        {
            RadialGradientPaint paint =
                getPaint(color, m_size, colorNormal, colorBright);
            m_graphics2D.setPaint(paint);
        }
        else
        {
            m_graphics.setColor(colorNormal);
        }
        if (isGhostStone)
            setComposite(COMPOSITE_8);
        m_graphics.fillOval(margin, margin,
                            m_size - 2 * margin, m_size - 2 * margin);
    }

    private void drawTerritoryGraphics()
    {
        if (m_territory == BLACK)
            m_graphics.setColor(Color.darkGray);
        else
        {
            assert m_territory == WHITE;
            m_graphics.setColor(Color.lightGray);
        }
        m_graphics.fillRect(0, 0, m_size, m_size);
    }

    private void drawTerritoryGraphics2D()
    {
        setComposite(COMPOSITE_4);
        if (m_territory == BLACK)
            m_graphics2D.setColor(Color.darkGray);
        else
        {
            assert m_territory == WHITE;
            m_graphics2D.setColor(Color.white);
        }
        m_graphics2D.fillRect(0, 0, m_size, m_size);
        m_graphics2D.setPaintMode();
    }

    private RadialGradientPaint getPaint(GoColor color, int size,
                                         Color colorNormal,
                                         Color colorBright)
    {
        RadialGradientPaint paint;
        int paintSize;
        if (color == BLACK)
        {
            paint = m_paintBlack;
            paintSize = m_paintSizeBlack;
        }
        else
        {
            assert color == WHITE;
            paint = m_paintWhite;
            paintSize = m_paintSizeWhite;
        }
        if (size == paintSize && paint != null)
            return paint;
        Point2D.Double center = new Point2D.Double(0.43 * size, 0.21 * size);
        Point2D.Double radius1 = new Point2D.Double(0.47 * size, -0.15 * size);
        Point2D.Double radius2 = new Point2D.Double(0.08 * size, 0.25 * size);
        double focus = -0.4;
        paint = new RadialGradientPaint(center, radius1, radius2, focus,
                                        colorBright, colorNormal);
        if (color == BLACK)
        {
            m_paintBlack = paint;
            m_paintSizeBlack = size;
        }
        else
        {
            m_paintWhite = paint;
            m_paintSizeWhite = size;
        }
        return paint;
    }

    private void setComposite(AlphaComposite composite)
    {
        if (m_graphics2D != null)
            m_graphics2D.setComposite(composite);
    }

    private static void setFont(Graphics graphics, int fieldSize)
    {
        if (s_cachedFont != null && s_cachedFontFieldSize == fieldSize)
        {
            graphics.setFont(s_cachedFont);
            return;
        }
        int fontSize;
        if (fieldSize < 29)
            fontSize = (int)(0.45 * fieldSize);
        else if (fieldSize < 40)
            fontSize = 13;
        else
            fontSize = (int)(13 + 0.15 * (fieldSize - 40));
        s_cachedFont = new Font("Dialog", Font.PLAIN, fontSize);
        s_cachedFontFieldSize = fieldSize;
        graphics.setFont(s_cachedFont);
    }
}
