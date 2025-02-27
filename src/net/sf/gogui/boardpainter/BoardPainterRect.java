// BoardPainter.java

package net.sf.gogui.boardpainter;

import net.sf.gogui.go.GoPoint;

import java.awt.*;

import static net.sf.gogui.go.GoColor.EMPTY;

/** Draws a board. */
public class BoardPainterRect
        extends BoardPainter {
    public BoardPainterRect() {
        System.out.println("Using Rect Painter");
        m_geometry = "rect";
        loadBackground("net/sf/gogui/images/wood.png");
    }

    protected void calcCellSize(int imageWidth, int fieldLength, float borderSize)
    {
        m_cellSize = Math.round((float)Math.floor(imageWidth / (fieldLength + 2 * borderSize)));
    }

    protected void calcFieldOffset(int imageWidth, int fieldLength, int cellSize)
    {
        m_fieldOffset = (imageWidth - fieldLength * cellSize) / 2;
    }

    public Point getCenter(int x, int y) {
        Point point = getLocation(x, y);
        point.x += m_cellSize / 2;
        point.y += m_cellSize / 2;
        return point;
    }

    public Point getLocation(int x, int y) {
        if (m_flipVertical)
            x = m_size - 1 - x;
        if (!m_flipHorizontal)
            y = m_size - 1 - y;
        Point point = new Point();
        point.x = m_fieldOffset + x * m_cellSize;
        point.y = m_fieldOffset + y * m_cellSize;
        return point;
    }

    public GoPoint getPoint(Point point) {
        if (m_cellSize == 0)
            return null;
        int x = (int) point.getX() - m_fieldOffset;
        int y = (int) point.getY() - m_fieldOffset;
        if (x < 0 || y < 0)
            return null;
        x = x / m_cellSize;
        y = y / m_cellSize;
        if (x >= m_size || y >= m_size)
            return null;
        if (m_flipVertical)
            x = m_size - 1 - x;
        if (!m_flipHorizontal)
            y = m_size - 1 - y;
        return GoPoint.get(x, y);
    }

    protected void drawGrid(Graphics graphics) {
        if (m_cellSize < 2)
            return;
        graphics.setColor(Color.darkGray);
        if (graphics instanceof Graphics2D) {
            // Temporarily disable antialiasing, which causes lines to
            // appear too thick with OpenJDK (version 6b09)
            Graphics2D graphics2D = (Graphics2D) graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        for (int y = 0; y < m_size; ++y) {
            if (y == 0 || y == m_size - 1)
                graphics.setColor(Color.black);
            else
                graphics.setColor(m_gridColor);
            Point left = getCenter(0, y);
            Point right = getCenter(m_size - 1, y);
            graphics.drawLine(left.x, left.y, right.x, right.y);
        }
        for (int x = 0; x < m_size; ++x) {
            if (x == 0 || x == m_size - 1)
                graphics.setColor(Color.black);
            else
                graphics.setColor(m_gridColor);
            Point top = getCenter(x, 0);
            Point bottom = getCenter(x, m_size - 1);
            graphics.drawLine(top.x, top.y, bottom.x, bottom.y);
        }
        if (graphics instanceof Graphics2D) {
            Graphics2D graphics2D = (Graphics2D) graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        int r;
        if (m_cellSize <= 7)
            return;
        else if (m_cellSize <= 33)
            r = 1;
        else if (m_cellSize <= 60)
            r = 2;
        else
            r = 3;
        for (int x = 0; x < m_size; ++x)
            if (m_constants.isHandicapLine(x))
                for (int y = 0; y < m_size; ++y)
                    if (m_constants.isHandicapLine(y)) {
                        Point point = getCenter(x, y);
                        graphics.fillOval(point.x - r, point.y - r,
                                2 * r + 1, 2 * r + 1);
                    }
    }

    protected void drawGridLabels(Graphics graphics) {
        drawGridLabels(graphics, m_fieldOffset);
    }

    protected Polygon getPolygon(int x, int y) { return null; }

    /**
     * Offset of the board from the border of the screen
     */
    private int m_fieldOffset;
}
