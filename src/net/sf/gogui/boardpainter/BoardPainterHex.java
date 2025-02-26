// BoardPainter.java

package net.sf.gogui.boardpainter;

import net.sf.gogui.go.GoPoint;

import java.awt.*;

import static net.sf.gogui.go.GoColor.EMPTY;

/** Draws a board. */
public class BoardPainterHex
    extends BoardPainter
{
    public BoardPainterHex()
    {
        System.out.println("Using Hex Painter");
        loadBackground("net/sf/gogui/images/wood.png");
    }

    protected void calcCellSize(int imageWidth, int fieldLength, float borderSize)
    {
        m_cellSize = Math.round(imageWidth / (1.5f * (fieldLength + 2 * borderSize)));
    }

    protected void calcFieldOffset(int imageWidth, int fieldLength, int cellSize)
    {
        m_fieldOffsetX = Math.round((imageWidth - (cellSize * fieldLength + cellSize * fieldLength / 2f)) / 2f);
        m_fieldOffsetY = Math.round((imageWidth - cellSize * fieldLength) / 2f);
    }

    public Point getCenter(int x, int y)
    {
        Point point = getLocation(x, y);
        point.x += m_cellSize / 2;
        point.y += m_cellSize / 2;
        return point;
    }

    public Point getLocation(int x, int y)
    {
        if (m_flipVertical)
            x = m_size - 1 - x;
        if (! m_flipHorizontal)
            y = m_size - 1 - y;
        Point point = new Point();
        point.x = m_fieldOffsetX + x * m_cellSize + y * m_cellSize / 2;
        point.y = m_fieldOffsetY + Math.round((y * 0.75f * m_cellSize / SQRT));
        return point;
    }

    public GoPoint getPoint(Point point)
    {
        if (m_cellSize == 0 || m_hexes == null)
            return null;
        /*int x = (int)point.getX() - m_fieldOffsetX;
        int y = (int)point.getY() - m_fieldOffsetY;

        y = (int) (y / Math.round((0.75f * m_cellSize / SQRT)));
        if (y % 2 == 1)
            x -= m_cellSize / 2;
        x = x / m_cellSize - y / 2;*/

        int x = -1;
        int y = -1;
        for (int i = 0; i < m_hexes.length; i++)
        {
            if (m_hexes[i].contains(point))
            {
                x = i % m_size;
                y = i / m_size;
                break;
            }
        }
        System.out.println("x: " + x + " y: " + y);

        if (x == -1 || y == -1)
            return null;

        if (m_flipVertical)
            x = m_size - 1 - x;
        if (! m_flipHorizontal)
            y = m_size - 1 - y;

        return GoPoint.get(x, y);
    }

    protected void drawFields(Graphics graphics, ConstField[][] field)
    {
        assert field.length == m_size;
        for (int x = 0; x < m_size; ++x)
        {
            assert field[x].length == m_size;
            for (int y = 0; y < m_size; ++y)
            {
                Point location = getLocation(x, y);
                field[x][y].draw(graphics, m_cellSize, location.x,
                                 location.y, m_image, m_width);
            }
        }
    }

    protected void drawGrid(Graphics graphics)
    {
        if (m_cellSize < 2)
            return;
        graphics.setColor(Color.darkGray);
        if (graphics instanceof Graphics2D)
        {
            // Temporarily disable antialiasing, which causes lines to
            // appear too thick with OpenJDK (version 6b09)
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        graphics.setColor(Color.black);
        m_hexes = new Polygon[m_size * m_size];
        for (int y = 0; y < m_size; ++y)
        {
            for (int x = 0; x < m_size; ++x)
            {
                // Calc the index for a bottom left 0;0 board
                int index = x + (m_size - 1 - y) * m_size;
                m_hexes[index] = getHex(getLocation(x, y), m_cellSize);
                graphics.drawPolygon(m_hexes[index]);
            }
        }

        if (graphics instanceof Graphics2D)
        {
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private Polygon getHex(Point point, int size)
    {
        int[] xpoints = new int[6];
        int[] ypoints = new int[6];

        float center_x = size / 2f;
        float center_y = size / 2f;
        float offset = size / 2f;

        xpoints[0] = Math.round(center_x);               ypoints[0] = Math.round((center_y - offset / SQRT));
        xpoints[1] = Math.round(center_x + offset);      ypoints[1] = Math.round((center_y - offset / SQRT / 2));
        xpoints[2] = Math.round(center_x + offset);      ypoints[2] = Math.round((center_y + offset / SQRT / 2));
        xpoints[3] = Math.round(center_x);               ypoints[3] = Math.round((center_y + offset / SQRT));
        xpoints[4] = Math.round(center_x - offset);      ypoints[4] = Math.round((center_y + offset / SQRT / 2));
        xpoints[5] = Math.round(center_x - offset);      ypoints[5] = Math.round((center_y - offset / SQRT / 2));


        /*xpoints[0] = center_x - offset;     ypoints[0] = center_y;
        xpoints[1] = center_x - offset / 2; ypoints[1] = center_y + offset;
        xpoints[2] = center_x + offset / 2; ypoints[2] = center_y + offset;
        xpoints[3] = center_x + offset;     ypoints[3] = center_y;
        xpoints[4] = center_x + offset / 2; ypoints[4] = center_y - offset;
        xpoints[5] = center_x - offset / 2; ypoints[5] = center_y - offset;*/

        Polygon hex = new Polygon(xpoints, ypoints, 6);
        hex.translate(point.x, point.y);

        return hex;

        /*int[] xpoints = new int[4];
        int[] ypoints = new int[4];

        int center_x = size / 2;
        int center_y = size / 2;
        int offset = size / 2;

        xpoints[0] = center_x - offset; ypoints[0] = center_y;
        xpoints[1] = center_x;          ypoints[1] = center_y + offset;
        xpoints[2] = center_x + offset; ypoints[2] = center_y;
        xpoints[3] = center_x;          ypoints[3] = center_y - offset;

        Polygon square = new Polygon(xpoints, ypoints, 4);
        square.translate(point.x, point.y);
        return square;*/
    }

    protected void drawGridLabels(Graphics graphics)
    {
        if (m_cellSize < 15)
            return;
        graphics.setColor(m_gridLabelColor);
        setFont(graphics, m_cellSize);
        int offset = (m_cellSize + m_fieldOffsetX) / 2;
        Point point;
        for (int x = 0; x < m_size; ++x)
        {
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
        for (int y = 0; y < m_size; ++y)
        {
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

    protected void drawShadows(Graphics graphics, ConstField[][] field)
    {
        if (m_cellSize <= 5)
            return;
        Graphics2D graphics2D =
            graphics instanceof Graphics2D ? (Graphics2D)graphics : null;
        if (graphics2D == null)
            return;
        graphics2D.setComposite(COMPOSITE_3);
        int size = m_cellSize - 2 * Field.getStoneMargin(m_cellSize);
        int offsetX = getShadowOffset() / 2; // Relates to stone gradient
        int offsetY = getShadowOffset();
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
            {
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
                           String string)
    {
        FontMetrics metrics = graphics.getFontMetrics();
        int stringWidth = metrics.stringWidth(string);
        int stringHeight = metrics.getAscent();
        int x = Math.max((m_cellSize - stringWidth) / 2, 0);
        int y = stringHeight + (m_cellSize - stringHeight) / 2;
        graphics.drawString(string, location.x + x, location.y + y);
    }

    private static final float SQRT = (float) (Math.sqrt(3) / 2f);

    /** Offsets of the board from the border of the screen */
    private int m_fieldOffsetX;
    private int m_fieldOffsetY;

    /** Hexagons field */
    private Polygon[] m_hexes;
}
