//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.UIManager;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.utils.SquareLayout;
import net.sf.gogui.gui.GuiUtils;

//----------------------------------------------------------------------------

/** Graphical display of a Go board. */
public final class GuiBoard
    extends JPanel
    implements FocusListener, Printable
{
    /** Callback for clicks on a field. */
    public interface Listener
    {
        void fieldClicked(GoPoint point, boolean modifiedSelect);

        void contextMenu(GoPoint point, Component invoker, int x, int y);
    }

    public GuiBoard(Board board, boolean fastPaint)
    {
        m_board = board;
        m_fastPaint = fastPaint;
        setPreferredFieldSize();
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/images/wood.png");
        if (url != null)
            m_image = new ImageIcon(url).getImage();
        else
            m_image = null;
        initSize(m_board.getSize());
    }

    /** Clear everything but stones. */
    public void clearAll()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            clearInfluence(p);
            setFieldBackground(p, null);
        }
        clearAllCrossHair();
        clearAllMarkup();
        clearAllSelect();
        clearAllLabels();
        clearAllTerritory();
        clearLastMove();
    }

    public void clearAllCrossHair()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setCrossHair(m_board.getPoint(i), false);
    }

    public void clearAllMarkup()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint point = m_board.getPoint(i);
            setMark(point, false);
            setMarkCircle(point, false);
            setMarkSquare(point, false);
            setMarkTriangle(point, false);
        }
    }

    public void clearAllSelect()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setSelect(m_board.getPoint(i), false);
    }

    public void clearAllLabels()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setLabel(m_board.getPoint(i), "");
    }

    public void clearAllTerritory()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setTerritory(m_board.getPoint(i), GoColor.EMPTY);
    }

    public void clearInfluence(GoPoint p)
    {
        getField(p).clearInfluence();
    }

    public void contextMenu(GoPoint point)
    {
        m_panel.contextMenu(point);
    }

    public void focusGained(FocusEvent event)
    {
        if (getShowCursor())
            setFocus(m_focusPoint, true);
    }

    public void focusLost(FocusEvent event)
    {
        if (getShowCursor())
            setFocus(m_focusPoint, false);
    }

    public void fieldClicked(GoPoint p, boolean modifiedSelect)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p, modifiedSelect);
    }

    public int getBoardSize()
    {
        return m_size;
    }

    /** If the JComponent is needed.
        @todo Should be fixed, GuiField is not for public access.
    */
    private GuiField getField(GoPoint p)
    {
        assert(p != null);
        return m_field[p.getX()][p.getY()];
    }

    public Dimension getFieldSize()
    {
        int size = m_panel.getFieldSize();
        return new Dimension(size, size);
    }

    public String getLabel(GoPoint point)
    {
        return getField(point).getString();
    }

    public Point getLocationOnScreen(GoPoint point)
    {
        Point center = m_panel.getCenter(point.getX(), point.getY());
        Point location = m_panel.getLocationOnScreen();
        location.x += center.x;
        location.y += center.y;
        return location;
    }

    public boolean getMark(GoPoint point)
    {
        return getField(point).getMark();
    }

    public boolean getMarkCircle(GoPoint point)
    {
        return getField(point).getMarkCircle();
    }

    public boolean getMarkSquare(GoPoint point)
    {
        return getField(point).getMarkSquare();
    }

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

    public boolean getSelect(GoPoint point)
    {
        return getField(point).getSelect();
    }

    public boolean getShowCursor()
    {
        return m_showCursor;
    }

    public void initFocus()
    {
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getMove(moveNumber - 1);
            GoPoint lastMove = m.getPoint();
            if (lastMove != null && m.getColor() != GoColor.EMPTY)
                setFocusPoint(lastMove);
        }
        else if (m_board.getInternalNumberMoves() == 0)
        {
            if (m_size > 0)
            {
                m_focusPoint = null;
                setFocusPoint(GoPoint.create(m_size / 2, m_size / 2));
            }
        }
    }

    public void initSize(int size)
    {
        m_size = size;
        m_field = new GuiField[size][size];
        removeAll();
        setLayout(new SquareLayout());
        m_panel = new BoardPanel();
        add(m_panel);
        m_panel.requestFocusInWindow();
        KeyAdapter keyAdapter = new KeyAdapter()
            {
                public void keyPressed(KeyEvent event)
                {
                    GuiBoard.this.keyPressed(event);
                }
            };
        m_panel.addKeyListener(keyAdapter);
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
                public void mousePressed(MouseEvent event)
                {
                    GoPoint point = m_panel.getPoint(event);
                    m_pointPressed = point;
                    if (point == null)
                        return;
                    // mousePressed and mouseReleased (platform dependency)
                    if (event.isPopupTrigger())
                    {
                        contextMenu(point);
                        m_pointPressed = null;
                        return;
                    }
                }

                public void mouseReleased(MouseEvent event)
                {                    
                    GoPoint point = m_panel.getPoint(event);
                    if (point == null || point != m_pointPressed)
                    {
                        m_pointPressed = null;
                        return;
                    }
                    m_pointPressed = null;
                    if (event.isPopupTrigger())
                    {
                        contextMenu(point);
                        return;
                    }
                    int button = event.getButton();
                    int count = event.getClickCount();
                    if (button != MouseEvent.BUTTON1)
                        return;
                    if (count == 2)
                        fieldClicked(point, true);
                    else
                    {            
                        int modifiers = event.getModifiers();
                        int mask = (ActionEvent.CTRL_MASK
                                    | ActionEvent.ALT_MASK
                                    | ActionEvent.META_MASK);
                        boolean modifiedSelect = ((modifiers & mask) != 0);
                        fieldClicked(point, modifiedSelect);
                    }
                }

                private GoPoint m_pointPressed;
            };
        m_panel.addMouseListener(mouseAdapter);
        m_panel.setOpaque(false);
        for (int y = size - 1; y >= 0; --y)
        {
            for (int x = 0; x < size; ++x)
            {
                GoPoint p = GoPoint.create(x, y);
                GuiField field = new GuiField(this, m_fastPaint);
                m_field[x][y] = field;
            }
        }
        m_lastMove = null;
        initFocus();
        updateFromGoBoard();
        revalidate();
    }

    /** Mark point of last move on the board.
        The last move marker will be removed, if the move parameter is null
        or a pass move.
    */
    public void markLastMove(Move move)
    {
        GoPoint point = null;
        if (move != null)
            point = move.getPoint();
        clearLastMove();
        m_lastMove = point;
        if (m_lastMove != null)
        {
            GuiField field = getField(m_lastMove);
            field.setLastMoveMarker(true);
            repaint(point);
            m_lastMove = point;
        }
        initFocus();
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

    public void resetBoard()
    {
        if (! m_needsReset)
            return;
        clearAll();
        m_needsReset = false;
    }

    public void scoreBegin(GoPoint[] isDeadStone)
    {
        m_board.scoreBegin(isDeadStone);
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                setCrossHair(isDeadStone[i], true);
        calcScore();
    }

    public void scoreSetDead(GoPoint p)
    {
        GoColor c = m_board.getColor(p);
        if (c == GoColor.EMPTY)
            return;
        ArrayList stones = new ArrayList(m_board.getNumberPoints());
        m_board.getStones(p, c, stones);
        boolean dead = ! m_board.scoreGetDead((GoPoint)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint stone = (GoPoint)stones.get(i);
            m_board.scoreSetDead(stone, dead);
            setCrossHair(stone, dead);
        }
        calcScore();
    }

    public void setColor(GoPoint point, GoColor color)
    {
        GuiField field = getField(point);
        if (field.getColor() != color)
        {
            field.setColor(color);
            m_panel.repaintWithShadow(point);
        }
    }

    public void setFont(Graphics graphics, int fieldSize)
    {
        if (m_cachedFont != null && m_cachedFontFieldSize == fieldSize)
        {
            graphics.setFont(m_cachedFont);
            return;
        }
        Font font = UIManager.getFont("Label.font");
        if (font != null)
        {
            FontMetrics metrics = graphics.getFontMetrics(font);
            double scale = (double)fieldSize / metrics.getAscent() / 2.3;
            if (scale < 0.95)
            {
                int size = font.getSize();
                Font derivedFont
                    = font.deriveFont(Font.BOLD, (float)(size * scale));
                if (derivedFont != null)
                    font = derivedFont;
            }
            else
                font = font.deriveFont(Font.BOLD);
        }
        m_cachedFont = font;
        m_cachedFontFieldSize = fieldSize;
        graphics.setFont(font);
    }

    public void setFieldBackground(GoPoint point, Color color)
    {
        GuiField field = getField(point);
        if ((field.getFieldBackground() == null && color != null)
            || (field.getFieldBackground() != null
                && ! field.getFieldBackground().equals(color)))
        {
            field.setFieldBackground(color);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setCrossHair(GoPoint point, boolean crossHair)
    {
        GuiField field = getField(point);
        if (field.getCrossHair() != crossHair)
        {
            field.setCrossHair(crossHair);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setInfluence(GoPoint point, double value)
    {
        getField(point).setInfluence(value);
        m_needsReset = true;
        repaint(point);
    }

    public void setLabel(GoPoint point, String label)
    {
        GuiField field = getField(point);
        if ((field.getString() == null && label != null)
            || (field.getString() != null
                && ! field.getString().equals(label)))
        {
            field.setString(label);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setListener(Listener l)
    {
        m_listener = l;
    }

    public void setMark(GoPoint point, boolean mark)
    {
        GuiField field = getField(point);
        if (field.getMark() != mark)
        {
            getField(point).setMark(mark);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setMarkCircle(GoPoint point, boolean mark)
    {
        GuiField field = getField(point);
        if (field.getMarkCircle() != mark)
        {
            getField(point).setMarkCircle(mark);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setMarkSquare(GoPoint point, boolean mark)
    {
        GuiField field = getField(point);
        if (field.getMarkSquare() != mark)
        {
            getField(point).setMarkSquare(mark);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setMarkTriangle(GoPoint point, boolean mark)
    {
        GuiField field = getField(point);
        if (field.getMarkTriangle() != mark)
        {
            getField(point).setMarkTriangle(mark);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setPreferredFieldSize(Dimension size)
    {
        m_preferredFieldSize = size;
    }

    public void setShowCursor(boolean showCursor)
    {
        if (m_showCursor)
            setFocus(m_focusPoint, false);
        m_showCursor = showCursor;
        if (m_showCursor)
            setFocus(m_focusPoint, true);
        m_panel.requestFocusInWindow();
    }

    public void setShowGrid(boolean showGrid)
    {
        if (showGrid != m_showGrid)
        {
            m_showGrid = showGrid;
            initSize(m_size);
        }
    }

    public void setSelect(GoPoint point, boolean select)
    {
        GuiField field = getField(point);
        if (field.getSelect() != select)
        {
            getField(point).setSelect(select);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void setTerritory(GoPoint point, GoColor color)
    {
        GuiField field = getField(point);
        if (field.getTerritory() != color)
        {
            field.setTerritory(color);
            m_needsReset = true;
            repaint(point);
        }
    }

    public void updateFromGoBoard()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            updateFromGoBoard(m_board.getPoint(i));
    }

    public void updateFromGoBoard(GoPoint point)
    {
        GoColor color = m_board.getColor(point);
        setColor(point, color);
    }

    private class BoardPanel
        extends JPanel
    {
        public BoardPanel()
        {
            int preferredFieldSize = getPreferredFieldSize().width;
            int preferredSize;
            int minimumSize;
            if (m_showGrid)
            {
                preferredSize = preferredFieldSize * (m_size + 2);
                minimumSize = 4 * (m_size + 2);
            }
            else
            {
                preferredSize =
                    preferredFieldSize * m_size + preferredFieldSize / 2;
                minimumSize = 4 * m_size + 2;
            }
            setPreferredSize(new Dimension(preferredSize, preferredSize));
            setMinimumSize(new Dimension(minimumSize, minimumSize));
            setFocusable(true);
            addFocusListener(GuiBoard.this);
        }

        public void contextMenu(GoPoint point)
        {
            if (m_listener == null)
                return;
            Point center = getCenter(point.getX(), point.getY());
            m_listener.contextMenu(point, this, center.x, center.y);
        }

        public int getFieldSize()
        {
            return m_fieldSize;
        }

        public GoPoint getPoint(MouseEvent event)
        {
            int eventX = (int)event.getPoint().getX();
            int eventY = (int)event.getPoint().getY();
            int x = (eventX - m_fieldOffset) / m_fieldSize;
            int y = (eventY - m_fieldOffset) / m_fieldSize;
            y = m_size - y - 1;
            if (x >= 0 && x < m_size && y >= 0 && y < m_size)
                return GoPoint.create(x, y);
            return null;
        }

        public void paintComponent(Graphics graphics)
        {
            /*
            System.err.println("BoardPanel.paintComponent"
                               + " x=" + graphics.getClipBounds().x
                               + " y=" + graphics.getClipBounds().y
                               + " w=" + graphics.getClipBounds().width
                               + " h=" + graphics.getClipBounds().height);
            */
            if (! m_fastPaint)
                GuiUtils.setAntiAlias(graphics);
            int width = getWidth();
            assert(width == getHeight());
            if (m_showGrid)
            {
                m_fieldSize = width / (m_size + 2);
                m_fieldOffset =
                    (width - (m_size + 2) * m_fieldSize) / 2
                    + m_fieldSize;
            }
            else
            {
                // Minimum border 1/5 field size
                m_fieldSize = (5 * width) / (5 * m_size + 2);
                m_fieldOffset = (width - m_size * m_fieldSize) / 2;
            }
            drawBackground(graphics);
            drawGrid(graphics);
            if (m_showGrid)
                drawLabels(graphics);
            drawShadows(graphics);
            drawFields(graphics);
        }

        public void paintImmediately(GoPoint point)
        {
            Point location = getLocation(point.getX(), point.getY());
            Rectangle dirty = new Rectangle();
            dirty.x = location.x;
            dirty.y = location.y;
            dirty.width = m_fieldSize;
            dirty.height = m_fieldSize;
            paintImmediately(dirty);
        }

        public void repaint(GoPoint point)
        {
            Point location = getLocation(point.getX(), point.getY());
            Rectangle dirty = new Rectangle();
            dirty.x = location.x;
            dirty.y = location.y;
            dirty.width = m_fieldSize;
            dirty.height = m_fieldSize;
            repaint(dirty);
        }

        public void repaintWithShadow(GoPoint point)
        {
            Point location = getLocation(point.getX(), point.getY());
            Rectangle dirty = new Rectangle();
            dirty.x = location.x;
            dirty.y = location.y;
            int offset = getShadowOffset()
                - GuiField.getStoneMargin(m_fieldSize);
            dirty.width = m_fieldSize + offset;
            dirty.height = m_fieldSize + offset;
            repaint(dirty);
        }

        private int m_fieldSize;

        private int m_fieldOffset;

        private void drawFields(Graphics graphics)
        {
            for (int x = 0; x < m_size; ++x)
                for (int y = 0; y < m_size; ++y)
                {
                    Point location = getLocation(x, y);
                    Graphics newGraphics =
                        graphics.create(location.x, location.y,
                                        m_fieldSize, m_fieldSize);
                    m_field[x][y].draw(newGraphics, m_fieldSize);
                }
        }

        private void drawBackground(Graphics graphics)
        {
            int width = getWidth();
            if (m_image != null && ! m_fastPaint)
                graphics.drawImage(m_image, 0, 0, width, width, null);
            else
            {
                graphics.setColor(new Color(212, 167, 102));
                graphics.fillRect(0, 0, width, width);
            }
        }

        private void drawGrid(Graphics graphics)
        {
            if (m_fieldSize < 2)
                return;
            graphics.setColor(Color.darkGray);
            for (int y = 0; y < m_size; ++y)
            {
                Point left = getCenter(0, y);
                Point right = getCenter(m_size - 1, y);
                graphics.drawLine(left.x, left.y, right.x, right.y);
            }
            for (int x = 0; x < m_size; ++x)
            {
                Point top = getCenter(x, 0);
                Point bottom = getCenter(x, m_size - 1);
                graphics.drawLine(top.x, top.y, bottom.x, bottom.y);
            }
            int r = m_fieldSize / 10;
            for (int x = 0; x < m_size; ++x)
                if (m_board.isHandicapLine(x))
                    for (int y = 0; y < m_size; ++y)
                        if (m_board.isHandicapLine(y))
                        {
                            Point point = getCenter(x, y);
                            graphics.fillOval(point.x - r, point.y - r,
                                              2 * r + 1, 2 * r + 1);
                        }
        }

        private void drawLabels(Graphics graphics)
        {
            graphics.setColor(Color.darkGray);
            graphics.setFont(UIManager.getFont("Label.font"));
            int stringWidth = graphics.getFontMetrics().stringWidth("XX");
            if (m_fieldSize < stringWidth)
                return;
            char c = 'A';
            for (int x = 0; x < m_size; ++x)
            {
                String string = Character.toString(c);
                drawLabel(graphics, getLocation(x, -1), string);
                drawLabel(graphics, getLocation(x, m_size), string);
                    ++c;
                if (c == 'I')
                    ++c;
            }
            for (int y = 0; y < m_size; ++y)
            {
                String string = Integer.toString(y + 1);
                drawLabel(graphics, getLocation(-1, y), string);
                drawLabel(graphics, getLocation(m_size, y), string);
            }
        }

        private void drawShadows(Graphics graphics)
        {
            if (m_fieldSize <= 5)
                return;
            Graphics2D graphics2D =
                graphics instanceof Graphics2D ? (Graphics2D)graphics : null;
            if (graphics2D == null || m_fastPaint)
                return;
            graphics2D.setComposite(m_composite3);
            int size = m_fieldSize - 2 * GuiField.getStoneMargin(m_fieldSize);
            int offset = getShadowOffset();
            for (int i = 0; i < m_board.getNumberPoints(); ++i)
            {
                GoPoint point = m_board.getPoint(i);
                if (getField(point).getColor() == GoColor.EMPTY)
                    continue;
                Point location = getCenter(point.getX(), point.getY());
                graphics.setColor(Color.black);
                graphics.fillOval(location.x - size / 2 + offset,
                                  location.y - size / 2 + offset,
                                  size, size);
            }
            graphics.setPaintMode();
        }

        private void drawLabel(Graphics graphics, Point location,
                               String string)
        {
            FontMetrics metrics = graphics.getFontMetrics();
            int stringWidth = metrics.stringWidth(string);
            int stringHeight = metrics.getAscent();
            int x = Math.max((m_fieldSize - stringWidth) / 2, 0);
            int y = stringHeight + (m_fieldSize - stringHeight) / 2;
            graphics.drawString(string, location.x + x, location.y + y);
        }

        private Point getCenter(int x, int y)
        {            
            Point point = getLocation(x, y);
            point.x += m_fieldSize / 2;
            point.y += m_fieldSize / 2;
            return point;
        }

        private Point getLocation(int x, int y)
        {            
            Point point = new Point();
            point.x = m_fieldOffset + x * m_fieldSize;
            point.y = m_fieldOffset + (m_size - y - 1) * m_fieldSize;
            return point;
        }
    }

    private final boolean m_fastPaint;

    private boolean m_needsReset;

    private boolean m_showCursor = true;

    private boolean m_showGrid = true;

    private int m_cachedFontFieldSize;

    private int m_size;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private GoPoint m_focusPoint;

    private GoPoint m_lastMove;

    private final Board m_board;

    private static final AlphaComposite m_composite3
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);

    private Dimension m_minimumFieldSize;

    private Dimension m_preferredFieldSize;

    private GuiField m_field[][];

    private Font m_cachedFont;

    private final Image m_image;

    private BoardPanel m_panel;

    private Listener m_listener;

    private void calcScore()
    {
        m_board.calcScore();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            GoColor c = m_board.getScore(p);
            setTerritory(p, c);
        }
    }

    private void clearLastMove()
    {
        if (m_lastMove != null)
        {
            GuiField field = getField(m_lastMove);
            field.setLastMoveMarker(false);
            repaint(m_lastMove);
            m_lastMove = null;
        }
    }

    private int getShadowOffset()
    {
        Rectangle grid = getBounds();
        int width = grid.width / (m_size + 2);
        int size = width - 2 * GuiField.getStoneMargin(width);
        return size / 12;
    }

    private boolean isHandicapLineOrEdge(int line)
    {
        return m_board.isHandicapLine(line) || m_board.isEdgeLine(line);
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
            if (getShowCursor() && m_focusPoint != null)
                fieldClicked(m_focusPoint, modifiedSelect);
            return;
        }        
        if ((modifiers & ActionEvent.CTRL_MASK) != 0
            || ! getShowCursor() || m_focusPoint == null)
            return;
        boolean shiftModifier = ((modifiers & ActionEvent.SHIFT_MASK) != 0);
        GoPoint point = m_focusPoint;
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
        setFocusPoint(point);
    }

    private void repaint(GoPoint point)
    {
        m_panel.repaint(point);
    }

    private void setFocus(GoPoint point, boolean focus)
    {
        if (point == null)
            return;
        GuiField field = getField(point);
        if (field.getFocus() != focus)
        {
            field.setFocus(focus);
            repaint(point);
        }
    }

    private void setFocusPoint(GoPoint point)
    {
        if (point != null && ! m_board.contains(point))
            point = null;
        if (m_focusPoint != point)
        {
            if (getShowCursor())
            {
                setFocus(m_focusPoint, false);            
                setFocus(point, true);
            }
            m_focusPoint = point;
        }
        m_panel.requestFocusInWindow();
    }

    private void setPreferredFieldSize()
    {
        int size = (int)((double)GuiUtils.getDefaultMonoFontSize() * 2.2);
        if (size % 2 == 0)
            ++size;
        m_preferredFieldSize = new Dimension(size, size);
        size = GuiUtils.getDefaultMonoFontSize();
        if (size % 2 == 0)
            ++size;
        m_minimumFieldSize = new Dimension(size, size);
    }
}

//----------------------------------------------------------------------------
