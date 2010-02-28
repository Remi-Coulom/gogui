// GuiBoard.java

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import javax.swing.JPanel;
import net.sf.gogui.boardpainter.BoardPainter;
import net.sf.gogui.boardpainter.ConstField;
import net.sf.gogui.boardpainter.Field;
import net.sf.gogui.go.BoardConstants;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.ObjectUtil;

/** Graphical display of a Go board.
    This class does not use go.Board, so it can be used with other board
    implementations. It uses go.GoPoint for coordinates. */
public final class GuiBoard
    extends JPanel
    implements ConstGuiBoard, Printable
{
    /** Callback for clicks on a field. */
    public interface Listener
    {
        /** Callback for click on a field.
            This callback is triggered with mouse clicks or the enter key
            if the cursor is shown.
            @param p The point clicked.
            @param modifiedSelect Modified select. True if the click was a
            double click or with the right mouse button or if a modifier key
            (Ctrl, Alt, Meta) was pressed while clicking, as long as it was
            not a (platform-dependent) popup menu trigger. */
        void fieldClicked(GoPoint p, boolean modifiedSelect);

        /** Callback for context menu.
            This callback is triggered with mouse clicks that trigger
            popup menus (platform-dependent).
            @param point The point clicked.
            @param invoker The awt.Component that was clicked on.
            @param x The x coordinate on the invoker component.
            @param y The y coordinate on the invoker component. */
        void contextMenu(GoPoint point, Component invoker, int x, int y);
    }

    /** Constructor.
        @param size The board size. */
    public GuiBoard(int size)
    {
        m_painter = new BoardPainter();
        setPreferredFieldSize();
        initSize(size);
    }

    /** Clear every kind of markup. */
    public void clearAll()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                setFieldBackground(GoPoint.get(x, y), null);
        clearAllCrossHair();
        clearAllMarkup();
        clearAllSelect();
        clearAllInfluence();
        clearAllLabels();
        clearAllGhostStones();
        clearAllTerritory();
        clearLastMove();
    }

    /** Clear all crosshairs. */
    public void clearAllCrossHair()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                setCrossHair(GoPoint.get(x, y), false);
    }

    public void clearAllInfluence()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                clearInfluence(GoPoint.get(x, y));
    }

    /** Clear all markup.
        Clears mark, circle, square, triangle on all points. */
    public void clearAllMarkup()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                setMark(point, false);
                setMarkCircle(point, false);
                setMarkSquare(point, false);
                setMarkTriangle(point, false);
            }
    }

    /** Clear all selected points. */
    public void clearAllSelect()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                setSelect(GoPoint.get(x, y), false);
    }

    /** Clear all labels. */
    public void clearAllLabels()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                setLabel(GoPoint.get(x, y), "");
    }

    /** Clear all shadow stones. */
    public void clearAllGhostStones()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                setGhostStone(GoPoint.get(x, y), null);
    }

    /** Clear all territory. */
    public void clearAllTerritory()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                setTerritory(GoPoint.get(x, y), EMPTY);
    }

    /** Clear influence. */
    public void clearInfluence(GoPoint point)
    {
        getField(point).clearInfluence();
        repaint(point);
    }

    /** Trigger the context menu callback at the listener. */
    public void contextMenu(GoPoint point)
    {
        m_panel.contextMenu(point);
    }

    /** Get current board size. */
    public int getBoardSize()
    {
        return m_size;
    }

    /** Return a field.
        Returns only a const interface to the field, the field state should
        be modified using GuiBoard functions to guarantee the UI repaint after
        field changes. */
    public ConstField getFieldConst(GoPoint p)
    {
        return getField(p);
    }

    public Dimension getFieldSize()
    {
        int size = m_painter.getFieldSize();
        return new Dimension(size, size);
    }

    /** Get label.
        @param point The point.
        @return Label or null if point has no label. */
    public String getLabel(GoPoint point)
    {
        return getField(point).getLabel();
    }

    /** Get location on screen for a point.
        @param point The point.
        @return Location on screen of center of point. */
    public Point getLocationOnScreen(GoPoint point)
    {
        Point center = m_painter.getCenter(point.getX(), point.getY());
        Point location = m_panel.getLocationOnScreen();
        location.x += center.x;
        location.y += center.y;
        return location;
    }

    /** Check if point is marked.
        This unspecified mark uses a diagonal cross.
        @param point The point.
        @return true, if point is marked. */
    public boolean getMark(GoPoint point)
    {
        return getField(point).getMark();
    }

    /** Check if point is marked with a circle.
        @param point The point.
        @return true, if point is marked with a circle. */
    public boolean getMarkCircle(GoPoint point)
    {
        return getField(point).getMarkCircle();
    }

    /** Check if point is marked with a square.
        @param point The point.
        @return true, if point is marked with a square. */
    public boolean getMarkSquare(GoPoint point)
    {
        return getField(point).getMarkSquare();
    }

    /** Check if point is marked with a triangle.
        @param point The point.
        @return true, if point is marked with a triangle. */
    public boolean getMarkTriangle(GoPoint point)
    {
        return getField(point).getMarkTriangle();
    }

    public Dimension getMinimumFieldSize()
    {
        return m_minimumFieldSize;
    }

    public Dimension getPreferredFieldSize()
    {
        return m_preferredFieldSize;
    }

    /** Check if point is selected.
        @param point The point.
        @return true, if point is selected. */
    public boolean getSelect(GoPoint point)
    {
        return getField(point).getSelect();
    }

    /** Check if cursor is shown.
        @return true, if cursor is shown. */
    public boolean getShowCursor()
    {
        return m_showCursor;
    }

    public boolean getShowGrid()
    {
        return m_showGrid;
    }

    /** Change the board size.
        @param size The new board size. */
    public void initSize(int size)
    {
        assert size > 0 && size <= GoPoint.MAX_SIZE;
        m_size = size;
        m_constants = BoardConstants.get(size);
        m_field = new Field[size][size];
        removeAll();
        m_cursor = null;
        setLayout(new SquareLayout());
        m_panel = new BoardPanel();
        m_panel.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    if (getShowCursor())
                        setCursor(m_cursor, true);
                }

                public void focusLost(FocusEvent event) {
                    if (getShowCursor())
                        setCursor(m_cursor, false);
                }
            });
        addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    m_panel.requestFocusInWindow();
                }
            });
        add(m_panel);
        m_panel.requestFocusInWindow();
        m_panel.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent event) {
                    GuiBoard.this.keyPressed(event);
                }
            });
        m_panel.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    GoPoint point = m_panel.getPoint(e);
                    if (point == null)
                        return;
                    // mousePressed and mouseReleased (platform dependency)
                    if (e.isPopupTrigger())
                    {
                        contextMenu(point);
                        return;
                    }
                    int button = e.getButton();
                    int count = e.getClickCount();
                    if (button != MouseEvent.BUTTON1)
                        return;
                    if (count == 2)
                        fieldClicked(point, true);
                    else
                    {
                        int modifiers = e.getModifiers();
                        int mask = (ActionEvent.CTRL_MASK
                                    | ActionEvent.ALT_MASK
                                    | ActionEvent.META_MASK);
                        boolean modifiedSelect = ((modifiers & mask) != 0);
                        fieldClicked(point, modifiedSelect);
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    GoPoint point = m_panel.getPoint(e);
                    if (point == null)
                        return;
                    if (e.isPopupTrigger())
                    {
                        contextMenu(point);
                        return;
                    }
                }
            });
        m_panel.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    m_panel.setToolTipText(null);
                    GoPoint point = m_panel.getPoint(e);
                    if (point == null)
                        return;
                    String label = getField(point).getLabel();
                    if (label != null && label.length() > 3)
                        m_panel.setToolTipText(label);
                }
            });
        for (int y = size - 1; y >= 0; --y)
            for (int x = 0; x < size; ++x)
                m_field[x][y] = new Field();
        m_lastMove = null;
        setCursor(GoPoint.get(m_size / 2, m_size / 2));
        revalidate();
        m_dirty = new Rectangle(0, 0, getWidth(), getHeight());
        repaint();
    }

    /** Mark point of last move on the board.
        The last move marker will be removed, if the parameter is null. */
    public void markLastMove(GoPoint point)
    {
        clearLastMove();
        m_lastMove = point;
        if (m_lastMove != null)
        {
            Field field = getField(m_lastMove);
            field.setLastMoveMarker(true);
            repaint(point);
            m_lastMove = point;
        }
    }

    public void paintImmediately(GoPoint point)
    {
        m_panel.paintImmediately(point);
    }

    public int print(Graphics g, PageFormat format, int page)
        throws PrinterException
    {
        if (page >= 1)
        {
            return Printable.NO_SUCH_PAGE;
        }
        double width = getSize().width;
        double height = getSize().height;
        double pageWidth = format.getImageableWidth();
        double pageHeight = format.getImageableHeight();
        double scale = 1;
        if (width >= pageWidth)
            scale = pageWidth / width;
        double xSpace = (pageWidth - width * scale) / 2;
        double ySpace = (pageHeight - height * scale) / 2;
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(format.getImageableX() + xSpace,
                      format.getImageableY() + ySpace);
        g2d.scale(scale, scale);
        print(g2d);
        return Printable.PAGE_EXISTS;
    }

    /** Set or remove stone.
        @param point The point.
        @param color The stone color or EMPTY to remove a stone,
        if existing. */
    public void setColor(GoPoint point, GoColor color)
    {
        Field field = getField(point);
        if (field.getColor() != color)
        {
            field.setColor(color);
            m_panel.repaintWithShadow(point);
        }
    }

    /** Set the cursor.
        @param point New location of the cursor. */
    public void setCursor(GoPoint point)
    {
        if (point != null && ! point.isOnBoard(m_size))
            point = null;
        if (! GoPoint.equals(m_cursor, point))
        {
            setCursor(m_cursor, false);
            if (getShowCursor())
                setCursor(point, true);
            m_cursor = point;
        }
    }

    /** Set the field background color.
        @param point The location of the field.
        @param color The color. */
    public void setFieldBackground(GoPoint point, Color color)
    {
        Field field = getField(point);
        if ((field.getFieldBackground() == null && color != null)
            || (field.getFieldBackground() != null
                && ! field.getFieldBackground().equals(color)))
        {
            field.setFieldBackground(color);
            repaint(point);
        }
    }

    /** Set crosshair.
        @param point The point.
        @param crossHair True to set, false to remove crosshair. */
    public void setCrossHair(GoPoint point, boolean crossHair)
    {
        Field field = getField(point);
        if (field.getCrossHair() != crossHair)
        {
            field.setCrossHair(crossHair);
            repaint(point);
        }
    }

    public void setGhostStone(GoPoint point, GoColor color)
    {
        Field field = getField(point);
        if (! ObjectUtil.equals(field.getGhostStone(), color))
        {
            field.setGhostStone(color);
            m_panel.repaintWithShadow(point);
        }
    }

    /** Set influence value.
        @param point The point.
        @param value The influence value between -1 and 1. */
    public void setInfluence(GoPoint point, double value)
    {
        getField(point).setInfluence(value);
        repaint(point);
    }

    /** Set label.
        @param point The point.
        @param label The label. Should not be longer than 3 characters to
        avoid clipping. null to remove label. */
    public void setLabel(GoPoint point, String label)
    {
        Field field = getField(point);
        if ((field.getLabel() == null && label != null)
            || (field.getLabel() != null
                && ! field.getLabel().equals(label)))
        {
            field.setLabel(label);
            repaint(point);
        }
    }

    /** Set the listener.
        @param listener The new listener; null to set no listener. */
    public void setListener(Listener listener)
    {
        m_listener = listener;
    }

    /** Set markup.
        This unspecified markup uses a diagonal cross.
        @param point The point.
        @param mark True to set, false to remove. */
    public void setMark(GoPoint point, boolean mark)
    {
        Field field = getField(point);
        if (field.getMark() != mark)
        {
            getField(point).setMark(mark);
            repaint(point);
        }
    }

    /** Set circle markup.
        @param point The point.
        @param mark True to set, false to remove. */
    public void setMarkCircle(GoPoint point, boolean mark)
    {
        Field field = getField(point);
        if (field.getMarkCircle() != mark)
        {
            getField(point).setMarkCircle(mark);
            repaint(point);
        }
    }

    /** Set square markup.
        @param point The point.
        @param mark True to set, false to remove. */
    public void setMarkSquare(GoPoint point, boolean mark)
    {
        Field field = getField(point);
        if (field.getMarkSquare() != mark)
        {
            getField(point).setMarkSquare(mark);
            repaint(point);
        }
    }

    /** Set triangle markup.
        @param point The point.
        @param mark True to set, false to remove. */
    public void setMarkTriangle(GoPoint point, boolean mark)
    {
        Field field = getField(point);
        if (field.getMarkTriangle() != mark)
        {
            getField(point).setMarkTriangle(mark);
            repaint(point);
        }
    }

    public void setPreferredFieldSize(Dimension size)
    {
        m_preferredFieldSize = size;
        m_panel.setPreferredFieldSize();
    }

    /** Set point selection markup.
        @param point The point.
        @param select True to set, false to remove. */
    public void setSelect(GoPoint point, boolean select)
    {
        Field field = getField(point);
        if (field.getSelect() != select)
        {
            getField(point).setSelect(select);
            repaint(point);
        }
    }

    /** Enable or disable cursor.
        @param showCursor true to enable cursor. */
    public void setShowCursor(boolean showCursor)
    {
        setCursor(m_cursor, false);
        m_showCursor = showCursor;
        if (m_showCursor)
            setCursor(m_cursor, true);
        m_panel.requestFocusInWindow();
    }

    /** Enable or disable grid coordinates.
        @param showGrid true to enable grid coordinates. */
    public void setShowGrid(boolean showGrid)
    {
        if (showGrid != m_showGrid)
        {
            m_showGrid = showGrid;
            m_dirty = new Rectangle(0, 0, getWidth(), getHeight());
            repaint();
        }
    }

    /** Set territory.
        @param point The point.
        @param color The territory color for this point; EMPTY for
        no territory. */
    public void setTerritory(GoPoint point, GoColor color)
    {
        Field field = getField(point);
        if (field.getTerritory() != color)
        {
            field.setTerritory(color);
            repaint(point);
        }
    }

    private class BoardPanel
        extends JPanel
    {
        public BoardPanel()
        {
            setPreferredFieldSize();
            setFocusable(true);
            setOpaque(true);
        }

        public void contextMenu(GoPoint point)
        {
            if (m_listener == null)
                return;
            Point center = m_painter.getCenter(point.getX(), point.getY());
            m_listener.contextMenu(point, this, center.x, center.y);
        }

        public GoPoint getPoint(MouseEvent event)
        {
            return m_painter.getPoint(event.getPoint());
        }

        public void paintComponent(Graphics graphics)
        {
            if (DEBUG_REPAINT)
                System.err.println("BoardPanel.paintComponent "
                                   + graphics.getClipBounds().x + " "
                                   + graphics.getClipBounds().y + " "
                                   + graphics.getClipBounds().width + " "
                                   + graphics.getClipBounds().height);
            int width = getWidth();
            int height = getHeight();
            if (m_image == null || width != m_imageWidth
                || height != m_imageHeight)
            {
                if (DEBUG_REPAINT)
                    System.err.println("createImage " + width + " " + height);
                m_image = createImage(width, height);
                m_imageWidth = width;
                m_imageHeight = height;
                m_dirty = new Rectangle(0, 0, width, height);
            }
            drawImage();
            graphics.drawImage(m_image, 0, 0, null);
        }

        public void paintImmediately(GoPoint point)
        {
            if (DEBUG_REPAINT)
                System.err.println("paintImmediately " + point);
            Point location = m_painter.getLocation(point.getX(), point.getY());
            Rectangle dirty = new Rectangle();
            dirty.x = location.x;
            dirty.y = location.y;
            int offset = m_painter.getShadowOffset()
                - Field.getStoneMargin(m_painter.getFieldSize());
            dirty.width = m_painter.getFieldSize() + offset;
            dirty.height = m_painter.getFieldSize() + offset;
            addDirty(dirty);
            Rectangle oldDirty = m_dirty;
            m_dirty = dirty;
            paintImmediately(dirty);
            m_dirty = oldDirty;
        }

        public void repaint(GoPoint point)
        {
            if (DEBUG_REPAINT)
                System.err.println("repaint " + point);
            Point location = m_painter.getLocation(point.getX(), point.getY());
            Rectangle dirty = new Rectangle();
            dirty.x = location.x;
            dirty.y = location.y;
            dirty.width = m_painter.getFieldSize();
            dirty.height = m_painter.getFieldSize();
            addDirty(dirty);
            repaint(dirty);
        }

        public void repaintWithShadow(GoPoint point)
        {
            if (DEBUG_REPAINT)
                System.err.println("repaintWithShadow " + point);
            Point location = m_painter.getLocation(point.getX(), point.getY());
            Rectangle dirty = new Rectangle();
            dirty.x = location.x;
            dirty.y = location.y;
            int offset = m_painter.getShadowOffset()
                - Field.getStoneMargin(m_painter.getFieldSize());
            dirty.width = m_painter.getFieldSize() + offset;
            dirty.height = m_painter.getFieldSize() + offset;
            addDirty(dirty);
            repaint(dirty);
        }

        public final void setPreferredFieldSize()
        {
            int preferredFieldSize = getPreferredFieldSize().width;
            setPreferredSize(BoardPainter.getPreferredSize(preferredFieldSize,
                                                          m_size, m_showGrid));
            int minimumSize = 4 * m_size + 2;
            setMinimumSize(new Dimension(minimumSize, minimumSize));
        }
    }

    private static final boolean DEBUG_REPAINT = false;

    private boolean m_showCursor = true;

    private boolean m_showGrid = true;

    private int m_imageHeight;

    private int m_imageWidth;

    private int m_size;

    private BoardConstants m_constants;

    private BoardPanel m_panel;

    private Dimension m_minimumFieldSize;

    private Dimension m_preferredFieldSize;

    private GoPoint m_cursor;

    private GoPoint m_lastMove;

    private final BoardPainter m_painter;

    private Field m_field[][];

    private Image m_image;

    private Listener m_listener;

    private Rectangle m_dirty = new Rectangle();

    private void addDirty(Rectangle rectangle)
    {
        if (m_dirty == null)
            m_dirty = rectangle;
        else
            m_dirty.add(rectangle);
    }

    private void clearLastMove()
    {
        if (m_lastMove != null)
        {
            Field field = getField(m_lastMove);
            field.setLastMoveMarker(false);
            repaint(m_lastMove);
            m_lastMove = null;
        }
    }

    private void drawImage()
    {
        if (m_image == null || m_dirty == null)
            return;
        if (DEBUG_REPAINT)
            System.err.println("BoardPanel.drawImage " + m_dirty.x + " "
                               + m_dirty.y + " " + m_dirty.width + " "
                               + m_dirty.height);
        Graphics graphics = m_image.getGraphics();
        graphics.setClip(m_dirty);
        m_painter.draw(graphics, m_field, m_imageWidth, m_showGrid);
        m_dirty = null;
    }

    private void fieldClicked(GoPoint p, boolean modifiedSelect)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p, modifiedSelect);
    }

    private Field getField(GoPoint p)
    {
        assert p != null;
        return m_field[p.getX()][p.getY()];
    }

    private boolean isHandicapLineOrEdge(int line)
    {
        return m_constants.isHandicapLine(line)
            || m_constants.isEdgeLine(line);
    }

    private void keyPressed(KeyEvent event)
    {
        int code = event.getKeyCode();
        int modifiers = event.getModifiers();
        if (code == KeyEvent.VK_ENTER)
        {
            int mask = (ActionEvent.CTRL_MASK
                        | ActionEvent.ALT_MASK
                        | ActionEvent.META_MASK);
            boolean modifiedSelect = ((modifiers & mask) != 0);
            if (getShowCursor() && m_cursor != null)
                fieldClicked(m_cursor, modifiedSelect);
            return;
        }
        if ((modifiers & ActionEvent.CTRL_MASK) != 0
            || ! getShowCursor() || m_cursor == null)
            return;
        boolean shiftModifier = ((modifiers & ActionEvent.SHIFT_MASK) != 0);
        GoPoint point = m_cursor;
        if (code == KeyEvent.VK_DOWN)
        {
            point = point.down();
            if (shiftModifier)
                while (! isHandicapLineOrEdge(point.getY()))
                    point = point.down();
        }
        else if (code == KeyEvent.VK_UP)
        {
            point = point.up(m_size);
            if (shiftModifier)
                while (! isHandicapLineOrEdge(point.getY()))
                    point = point.up(m_size);
        }
        else if (code == KeyEvent.VK_LEFT)
        {
            point = point.left();
            if (shiftModifier)
                while (! isHandicapLineOrEdge(point.getX()))
                    point = point.left();
        }
        else if (code == KeyEvent.VK_RIGHT)
        {
            point = point.right(m_size);
            if (shiftModifier)
                while (! isHandicapLineOrEdge(point.getX()))
                    point = point.right(m_size);
        }
        setCursor(point);
    }

    private void repaint(GoPoint point)
    {
        m_panel.repaint(point);
    }

    private void setCursor(GoPoint point, boolean cursor)
    {
        if (point == null)
            return;
        Field field = getField(point);
        if (field.getCursor() != cursor)
        {
            field.setCursor(cursor);
            repaint(point);
        }
    }

    private void setPreferredFieldSize()
    {
        int size = (int)((double)GuiUtil.getDefaultMonoFontSize() * 2.5);
        if (size % 2 == 0)
            ++size;
        m_preferredFieldSize = new Dimension(size, size);
        size = GuiUtil.getDefaultMonoFontSize();
        if (size % 2 == 0)
            ++size;
        m_minimumFieldSize = new Dimension(size, size);
    }
}
