// BoardPainter.java

package net.sf.gogui.boardpainter;

import net.sf.gogui.go.BoardConstants;
import net.sf.gogui.go.GoPoint;

import java.awt.*;
import java.net.URL;

import static net.sf.gogui.go.GoColor.EMPTY;

/** Draws a board. */
public class BoardPainterRect
        extends BoardPainter {
    public BoardPainterRect() {
        System.out.println("Using Rect Painter");
        loadBackground("net/sf/gogui/images/wood.png");
    }

    public Point getCenter(int x, int y) {
        Point point = getLocation(x, y);
        point.x += m_fieldSize / 2;
        point.y += m_fieldSize / 2;
        return point;
    }

    public Point getLocation(int x, int y) {
        if (m_flipVertical)
            x = m_size - 1 - x;
        if (!m_flipHorizontal)
            y = m_size - 1 - y;
        Point point = new Point();
        point.x = m_fieldOffset + x * m_fieldSize;
        point.y = m_fieldOffset + y * m_fieldSize;
        return point;
    }

    public GoPoint getPoint(Point point) {
        if (m_fieldSize == 0)
            return null;
        int x = (int) point.getX() - m_fieldOffset;
        int y = (int) point.getY() - m_fieldOffset;
        if (x < 0 || y < 0)
            return null;
        x = x / m_fieldSize;
        y = y / m_fieldSize;
        if (x >= m_size || y >= m_size)
            return null;
        if (m_flipVertical)
            x = m_size - 1 - x;
        if (!m_flipHorizontal)
            y = m_size - 1 - y;
        return GoPoint.get(x, y);
    }

    protected void drawFields(Graphics graphics, ConstField[][] field) {
        assert field.length == m_size;
        for (int x = 0; x < m_size; ++x) {
            assert field[x].length == m_size;
            for (int y = 0; y < m_size; ++y) {
                Point location = getLocation(x, y);
                field[x][y].draw(graphics, m_fieldSize, location.x,
                        location.y, m_image, m_width);
            }
        }
    }

    protected void drawGrid(Graphics graphics) {
        if (m_fieldSize < 2)
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
        if (m_fieldSize <= 7)
            return;
        else if (m_fieldSize <= 33)
            r = 1;
        else if (m_fieldSize <= 60)
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
        if (m_fieldSize < 15)
            return;
        graphics.setColor(m_gridLabelColor);
        setFont(graphics, m_fieldSize);
        int offset = (m_fieldSize + m_fieldOffset) / 2;
        Point point;
        for (int x = 0; x < m_size; ++x) {
            String string = GoPoint.xToString(x);
            point = getLocation(x, 0);
            if (m_flipHorizontal)
                point.y -= offset;
            else
                point.y += offset;
            drawLabel(graphics, point, string);
            point = getLocation(x, m_size - 1);
            if (m_flipHorizontal)
                point.y += offset;
            else
                point.y -= offset;
            drawLabel(graphics, point, string);
        }
        for (int y = 0; y < m_size; ++y) {
            String string = Integer.toString(y + 1);
            point = getLocation(0, y);
            if (m_flipVertical)
                point.x += offset;
            else
                point.x -= offset;
            drawLabel(graphics, point, string);
            point = getLocation(m_size - 1, y);
            if (m_flipVertical)
                point.x -= offset;
            else
                point.x += offset;
            drawLabel(graphics, point, string);
        }
    }

    protected void drawShadows(Graphics graphics, ConstField[][] field) {
        if (m_fieldSize <= 5)
            return;
        Graphics2D graphics2D =
                graphics instanceof Graphics2D ? (Graphics2D) graphics : null;
        if (graphics2D == null)
            return;
        graphics2D.setComposite(COMPOSITE_3);
        int size = m_fieldSize - 2 * Field.getStoneMargin(m_fieldSize);
        int offsetX = getShadowOffset() / 2; // Relates to stone gradient
        int offsetY = getShadowOffset();
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y) {
                if (field[x][y].getColor() == EMPTY)
                    continue;
                Point location = getCenter(x, y);
                graphics.setColor(Color.black);
                graphics.fillOval(location.x - size / 2 + offsetX,
                        location.y - size / 2 + offsetY,
                        size, size);
            }
        graphics.setPaintMode();
    }

    protected void drawLabel(Graphics graphics, Point location,
                             String string) {
        FontMetrics metrics = graphics.getFontMetrics();
        int stringWidth = metrics.stringWidth(string);
        int stringHeight = metrics.getAscent();
        int x = Math.max((m_fieldSize - stringWidth) / 2, 0);
        int y = stringHeight + (m_fieldSize - stringHeight) / 2;
        graphics.drawString(string, location.x + x, location.y + y);
    }
}
