// BoardPainter.java

package net.sf.gogui.boardpainter;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.net.URL;

import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.BoardConstants;

/** Draws a board. */
public abstract class BoardPainter
{
    protected void loadBackground(String filename)
    {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(filename);
        if (url == null)
            m_image = null;
        else
            m_image = loadImage(url);
    }

    /** Get preferred board size given a preferred field size.
     The drawer can draw any board size. The border has a variable size
     to ensure that all fields have exactly the same size (in pixels).
     If a preferred field size is known (e.g. from a different board size,
     or from the last settings), then using the board size returned by this
     function will draw the board such that the field size is exactly the
     preferred one. */
    public static Dimension getPreferredSize(int preferredWidthSize, int preferredHeightSize,
                                             Dimension boardDimension, boolean showGrid)
    {
        double borderXSize;
        double borderYSize;
        if (showGrid) {
            borderXSize = BORDER_SIZE * preferredWidthSize;
            borderYSize = BORDER_SIZE * preferredHeightSize;
        }
        else {
            borderXSize = BORDER_SIZE_NOGRID * preferredWidthSize;
            borderYSize = BORDER_SIZE_NOGRID * preferredHeightSize;
        }
        int preferredWSize = (preferredWidthSize * boardDimension.width
                + 2 * Math.round((float)Math.ceil(borderXSize)));
        int preferredHSize = (preferredHeightSize * boardDimension.height
                + 2 * Math.round((float)Math.ceil(borderYSize)));
        return new Dimension(preferredWSize, preferredHSize);
    }

    /** Draw a board into graphics object.
        @param graphics The graphics object.
        @param field The fields.
        @param width The width/height of the image.
        @param showGrid Show grid coordinates. */
    public void draw(Graphics graphics, ConstField[][] field, int width,
                     boolean showGrid)
    {
        if (graphics instanceof Graphics2D)
        {
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        }
        m_width = width;
        m_size = field.length;
        if (m_constants == null || m_constants.getSize() != m_size)
            m_constants = BoardConstants.get(m_size);
        assert m_size <= GoPoint.MAX_SIZE;
        float borderSize;
        if (showGrid)
            borderSize = BORDER_SIZE;
        else
            borderSize = BORDER_SIZE_NOGRID;
        calcCellSize(width, m_size, borderSize);
        calcFieldOffset(width, m_size, m_cellSize);
        drawBackground(graphics);
        drawGrid(graphics);
        if (showGrid)
            drawGridLabels(graphics);
        drawShadows(graphics, field);
        drawFields(graphics, field);
    }

    protected void drawBackground(Graphics graphics) {
        if (m_image == null) {
            graphics.setColor(new Color(212, 167, 102));
            graphics.fillRect(0, 0, m_width, m_width);
        } else
            graphics.drawImage(m_image, 0, 0, m_width, m_width, null);
    }

    protected abstract void drawFields(Graphics graphics, ConstField[][] field);

    protected abstract void drawGrid(Graphics graphics);

    protected abstract void drawGridLabels(Graphics graphics);

    protected abstract void drawShadows(Graphics graphics, ConstField[][] field);

    public int getShadowOffset()
    {
        return (m_cellSize - 2 * Field.getStoneMargin(m_cellSize)) / 12;
    }

    protected abstract void drawLabel(Graphics graphics, Point location, String string);

    public abstract Point getCenter(int x, int y);

    protected abstract void calcCellSize(int imageWidth, int fieldLength, float borderSize);

    protected abstract void calcFieldOffset(int imageWidth, int fieldLength, int cellSize);

    public int getCellSize()
    {
        return m_cellSize;
    }

    /** Get the coordinates in pixels of the center of the cell at x, y.
     *  @param x The x-coordinate of the cell (from 0 to m_size - 1).
     *  @param y The y-coordinate of the cell (from 0 to m_size - 1).
     *  @return The coordinates in pixels of the center of the cell.
     * */
    public abstract Point getLocation(int x, int y);

    /**
     * Get the GoPoint at the given pixel coordinates.
     * @param point The pixel coordinates.
     * @return The GoPoint at the given pixel coordinates, or null if outside the board.
     */
    public abstract GoPoint getPoint(Point point);

    protected static Image loadImage(URL url)
    {
        Image image = Toolkit.getDefaultToolkit().getImage(url);
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        try
        {
            mediaTracker.waitForID(0);
        }
        catch (InterruptedException e)
        {
            return null;
        }
        return image;
    }

    protected static void setFont(Graphics graphics, int fieldSize)
    {
        if (s_cachedFont != null && s_cachedFontFieldSize == fieldSize)
        {
            graphics.setFont(s_cachedFont);
            return;
        }
        int fontSize;
        if (fieldSize < 29)
            fontSize = (int)(0.33 * fieldSize);
        else if (fieldSize < 40)
            fontSize = 10;
        else
            fontSize = (int)(10 + 0.1 * (fieldSize - 40));
        s_cachedFont = new Font("SansSerif", Font.PLAIN, fontSize);
        s_cachedFontFieldSize = fieldSize;
        graphics.setFont(s_cachedFont);
    }

    public void setOrientation(boolean flipHorizontal, boolean flipVertical) {
        m_flipHorizontal = flipHorizontal;
        m_flipVertical = flipVertical;
    }

    /** Preferred border size (in fraction of field size) if grid is drawn. */
    protected static final float BORDER_SIZE = 0.6f;

    /** Preferred border size (in fraction of field size) if grid is drawn. */
    protected static final float BORDER_SIZE_NOGRID = 0.2f;

    /**
     * Size of one cell in pixels.
     */
    protected int m_cellSize;

    protected boolean m_flipHorizontal = false;

    protected boolean m_flipVertical = false;

    protected int m_size;

    protected int m_width;

    protected static int s_cachedFontFieldSize;

    protected static final AlphaComposite COMPOSITE_3
            = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);

    protected BoardConstants m_constants;

    protected final Color m_gridLabelColor = new Color(96, 96, 96);

    protected final Color m_gridColor = new Color(80, 80, 80);

    protected static Font s_cachedFont;

    protected Image m_image;
}
