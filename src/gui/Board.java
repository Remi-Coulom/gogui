//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.net.URL;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import go.Move;
import utils.GuiUtils;

//----------------------------------------------------------------------------

class BoardLabel
    extends JLabel
{
    public BoardLabel(String text)
    {
        super(text, JLabel.CENTER);
        setForeground(java.awt.Color.DARK_GRAY);
    }

    public void setShow(boolean show)
    {
        m_show = show;
    }

    public void paintComponent(Graphics graphics)
    {
        if (! m_show)
            return;
        int stringWidth = graphics.getFontMetrics().stringWidth("XX");
        if (getSize().width < stringWidth)
            return;
        super.paintComponent(graphics);
    }

    private boolean m_show = true;
}

//----------------------------------------------------------------------------

public class Board
    extends JPanel
    implements FocusListener, Printable
{
    public interface Listener
    {
        void fieldClicked(go.Point p, boolean modifiedSelect);
    }

    public Board(go.Board board, boolean fastPaint)
    {
        m_board = board;
        m_fastPaint = fastPaint;
        setPreferredFieldSize();
        URL url = getClass().getClassLoader().getResource("images/wood.png");
        if (url != null)
            m_image = new ImageIcon(url).getImage();
        initSize(m_board.getSize());
        setFocusable(true);
        addFocusListener(this);
        setFocusPoint(m_focusPoint);
    }

    public void clearAll()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            clearInfluence(p);
            setFieldBackground(p, null);
        }
        clearAllCrossHair();
        clearAllMarkup();
        clearAllSelect();
        clearAllStrings();
        clearAllTerritory();
        clearLastMove();
        if (m_variationShown)
        {
            updateFromGoBoard();
            m_variationShown = false;
        }
    }

    public void clearAllCrossHair()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setCrossHair(m_board.getPoint(i), false);
    }

    public void clearAllMarkup()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setMarkup(m_board.getPoint(i), false);
    }

    public void clearAllSelect()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setSelect(m_board.getPoint(i), false);
    }

    public void clearAllStrings()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setString(m_board.getPoint(i), "");
    }

    public void clearAllTerritory()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setTerritory(m_board.getPoint(i), go.Color.EMPTY);
    }

    public void clearInfluence(go.Point p)
    {
        getField(p).clearInfluence();
    }

    public void focusGained(FocusEvent event)
    {
        setFocusPoint(m_focusPoint);
    }

    public void focusLost(FocusEvent event)
    {
    }

    public void fieldClicked(go.Point p, boolean modifiedSelect)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p, modifiedSelect);
    }

    public go.Board getBoard()
    {
        return m_board;
    }

    public Dimension getFieldSize()
    {
        int width = m_field[0][0].getWidth();
        return new Dimension(width, width);
    }

    public java.awt.Point getLocationOnScreen(go.Point point)
    {
        Field field = getField(point);
        java.awt.Point location = field.getLocationOnScreen();
        Dimension size = field.getSize();
        location.x += size.width / 2;
        location.y += size.height / 2;
        return location;
    }

    public boolean[][] getMarkups()
    {
        int size = m_board.getSize();
        boolean[][] result = new boolean[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            result[point.getX()][point.getY()] = getField(point).getMarkup();
        }
        return result;
    }

    public Dimension getMinimumFieldSize()
    {
        return m_minimumFieldSize;
    }

    public Dimension getPreferredFieldSize()
    {
        return m_preferredFieldSize;
    }

    public boolean[][] getSelects()
    {
        int size = m_board.getSize();
        boolean[][] result = new boolean[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            result[point.getX()][point.getY()] = getField(point).getSelect();
        }
        return result;
    }

    public boolean getShowCursor()
    {
        return m_showCursor;
    }

    public String[][] getStrings()
    {
        int size = m_board.getSize();
        String[][] result = new String[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            result[point.getX()][point.getY()] = getField(point).getString();
        }
        return result;
    }

    public void initSize(int size)
    {
        m_board.initSize(size);
        m_field = new Field[size][size];
        removeAll();
        setOpaque(false);        
        setLayout(new GridLayout(size + 2, size + 2));
        m_labels.clear();
        addColumnLabels(size);
        for (int y = size - 1; y >= 0; --y)
        {
            String text = Integer.toString(y + 1);
            addRowLabel(text);
            for (int x = 0; x < size; ++x)
            {
                go.Point p = m_board.getPoint(x, y);
                Field field = new Field(this, p, m_fastPaint);
                add(field);
                m_field[x][y] = field;
                KeyListener keyListener = new KeyAdapter()
                    {
                        public void keyPressed(KeyEvent event)
                        {
                            Board.this.keyPressed(event);
                        }
                    };
                field.addKeyListener(keyListener);
            }
            addRowLabel(text);
        }
        addColumnLabels(size);
        m_focusPoint = new go.Point(size / 2, size / 2);
        m_lastMove = null;
        revalidate();
        repaint();
    }

    public void markLastMove(go.Point point)
    {
        clearLastMove();
        m_lastMove = point;
        if (m_lastMove != null)
        {
            Field field = getField(m_lastMove);
            field.setLastMoveMarker(true);
            field.repaint();
            m_lastMove = point;
        }
        setFocus();
    }

    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null && ! m_fastPaint)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(graphics);
        int width = getSize().width;
        int size = m_board.getSize();
        if (width >= (size + 2) * 2)
            drawGrid(graphics);
        if (width > (size + 2) * 5)
            drawShadows(graphics);
    }

    public void paintImmediately(go.Point point)
    {
        Field field = getField(point);
        Rectangle rect = field.getVisibleRect();
        int offset = getShadowOffset();
        rect.width += offset;
        rect.height += offset;
        field.paintImmediately(rect);
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
        return;
    }

    public void scoreBegin(go.Point[] isDeadStone)
    {
        m_board.scoreBegin(isDeadStone);
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                setCrossHair(isDeadStone[i], true);
        calcScore();
    }

    public void scoreSetDead(go.Point p)
    {
        go.Color c = m_board.getColor(p);
        if (c == go.Color.EMPTY)
            return;
        Vector stones = new Vector(m_board.getNumberPoints());
        m_board.getStones(p, c, stones);
        boolean dead = ! m_board.scoreGetDead((go.Point)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            go.Point stone = (go.Point)stones.get(i);
            m_board.scoreSetDead(stone, dead);
            setCrossHair(stone, dead);
        }
        calcScore();
    }

    public void setPreferredFieldSize(Dimension size)
    {
        m_preferredFieldSize = size;
        int boardSize = m_board.getSize();
        for (int x = 0; x < boardSize; ++x)
            for (int y = 0; y < boardSize; ++y)
                m_field[x][y].setPreferredSize(m_preferredFieldSize);
    }

    public void setFocus()
    {
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getMove(moveNumber - 1);
            go.Point lastMove = m.getPoint();
            if (lastMove != null && m.getColor() != go.Color.EMPTY)
                setFocusPoint(lastMove);
        }
        else if (m_board.getInternalNumberMoves() == 0)
        {
            int size = m_board.getSize();
            if (size > 0)
                setFocusPoint(m_board.getPoint(size / 2, size / 2));
        }
    }

    public void setFocusPoint(go.Point point)
    {
        if (! m_board.contains(point))
            return;
        getField(point).requestFocusInWindow();
        m_focusPoint.set(point.getX(), point.getY());
    }

    public void setFieldBackground(go.Point p, java.awt.Color color)
    {
        getField(p).setFieldBackground(color);
        m_needsReset = true;
    }

    public void setCrossHair(go.Point p, boolean crossHair)
    {
        getField(p).setCrossHair(crossHair);
        m_needsReset = true;
    }

    public void setInfluence(go.Point p, double value)
    {
        getField(p).setInfluence(value);
        m_needsReset = true;
    }

    public void setListener(Listener l)
    {
        m_listener = l;
    }

    public void setMarkup(go.Point p, boolean markup)
    {
        getField(p).setMarkup(markup);
        m_needsReset = true;
    }

    public void setShowCursor(boolean showCursor)
    {
        m_showCursor = showCursor;
    }

    public void setShowGrid(boolean showGrid)
    {
        for (int i = 0; i < m_labels.size(); ++i)
            ((BoardLabel)m_labels.get(i)).setShow(showGrid);
    }

    public void setSelect(go.Point p, boolean select)
    {
        getField(p).setSelect(select);
        m_needsReset = true;
    }

    public void setString(go.Point p, String s)
    {
        getField(p).setString(s);
        m_needsReset = true;
    }

    public void setTerritory(go.Point p, go.Color color)
    {
        getField(p).setTerritory(color);
        m_needsReset = true;
    }

    public void showBWBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            String s = board[p.getX()][p.getY()].toLowerCase();
            if (s.equals("b") || s.equals("black"))
                setTerritory(p, go.Color.BLACK);
            else if (s.equals("w") || s.equals("white"))
                setTerritory(p, go.Color.WHITE);
            else
                setTerritory(p, go.Color.EMPTY);
        }
    }

    public void showChildrenMoves(Vector childrenMoves)
    {
        clearAllStrings();
        int numberMarked = 0;
        char label = 'A';
        for (int i = 0; i < childrenMoves.size(); ++i)
        {
            go.Point point = (go.Point)childrenMoves.get(i);
            String s = getField(point).getString();
            if (! s.equals(""))
            {
                if (! s.endsWith("."))
                    setString(point, s + ".");
                continue;
            };
            if (numberMarked >= 26)
                setString(point, "+");
            else
                setString(point, Character.toString(label));
            if (numberMarked < 26)
                ++label;
            ++numberMarked;            
        }
    }

    public void showColorBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            String s = board[p.getX()][p.getY()];
            java.awt.Color c = null;
            if (s.equals("blue"))
                c = java.awt.Color.blue;
            else if (s.equals("cyan"))
                c = java.awt.Color.cyan;
            else if (s.equals("green"))
                c = java.awt.Color.green;
            else if (s.equals("gray"))
                c = java.awt.Color.lightGray;
            else if (s.equals("magenta"))
                c = java.awt.Color.magenta;
            else if (s.equals("pink"))
                c = java.awt.Color.pink;
            else if (s.equals("red"))
                c = java.awt.Color.red;
            else if (s.equals("yellow"))
                c = java.awt.Color.yellow;
            else if (s.equals("black"))
                c = java.awt.Color.black;
            else if (s.equals("white"))
                c = java.awt.Color.white;
            setFieldBackground(p, c);
        }
    }

    public void showDoubleBoard(double[][] board, double scale)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            double d = board[p.getX()][p.getY()] * scale;
            setInfluence(p, d);
        }
    }

    public void showPointList(go.Point pointList[])
    {
        clearAllMarkup();
        for (int i = 0; i < pointList.length; ++i)
        {
            go.Point p = pointList[i];
            if (p != null)
                setMarkup(p, true);
        }
    }

    public void showPointStringList(Vector pointList, Vector stringList)
    {
        clearAllStrings();
        for (int i = 0; i < pointList.size(); ++i)
        {
            go.Point point = (go.Point)pointList.get(i);
            String string = (String)stringList.get(i);
            if (point != null)
                setString(point, string);
        }
    }

    public void showStringBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            setString(p, board[p.getX()][p.getY()]);
        }
    }

    public void showVariation(go.Move[] variation)
    {
        clearAllStrings();
        updateFromGoBoard();
        for (int i = 0; i < variation.length; ++i)
        {
            go.Move move = variation[i];
            if (move.getPoint() != null)
            {
                setColor(move.getPoint(), move.getColor());
                setString(move.getPoint(), Integer.toString(i + 1));
            }
        }
        m_variationShown = true;
    }

    public void updateFromGoBoard()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            updateFromGoBoard(m_board.getPoint(i));
    }

    public void updateFromGoBoard(go.Point point)
    {
        Field field = getField(point);
        go.Color color = m_board.getColor(point);
        go.Color oldColor = field.getColor();
        if (color != oldColor)
        {
            setColor(point, color);
            field.repaint();
        }
    }

    private boolean m_fastPaint;

    private boolean m_needsReset;

    private boolean m_showCursor = true;

    private boolean m_variationShown;

    private int m_cachedFontFieldSize;

    private go.Point m_focusPoint;

    private go.Point m_lastMove;

    private go.Board m_board;

    private static final AlphaComposite m_composite3
        = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);

    private Dimension m_minimumFieldSize;

    private Dimension m_preferredFieldSize;

    private Field m_field[][];

    private Font m_cachedFont;

    private Image m_image;

    private Listener m_listener;

    private Vector m_labels = new Vector(100, 100);

    private void addColumnLabels(int size)
    {
        add(Box.createHorizontalGlue());
        char c = 'A';
        for (int x = 0; x < size; ++x)
        {
            BoardLabel label = new BoardLabel(new Character(c).toString());
            m_labels.add(label);
            add(label);
            ++c;
            if (c == 'I')
                ++c;
        }
        add(Box.createHorizontalGlue());
    }

    private void addRowLabel(String text)
    {
        BoardLabel label = new BoardLabel(text);
        add(label);
        m_labels.add(label);
    }

    private void calcScore()
    {
        m_board.calcScore();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            go.Color c = m_board.getScore(p);
            setTerritory(p, c);
        }
    }

    private void clearLastMove()
    {
        if (m_lastMove != null)
        {
            Field field = getField(m_lastMove);
            field.setLastMoveMarker(false);
            field.repaint();
            m_lastMove = null;
        }
    }

    private void drawBackground(Graphics graphics)
    {
        int size = m_board.getSize();
        int width = getSize().width;
        if (m_image != null && ! m_fastPaint)
            graphics.drawImage(m_image, 0, 0, width, width, null);
        else
        {
            graphics.setColor(new java.awt.Color(212, 167, 102));
            graphics.fillRect(0, 0, width, width);
        }
    }

    private void drawGrid(Graphics graphics)
    {
        int size = m_board.getSize();
        int width = getSize().width;
        graphics.setColor(java.awt.Color.darkGray);
        for (int y = 0; y < size; ++y)
        {
            java.awt.Point left = getScreenLocation(0, y);
            java.awt.Point right = getScreenLocation(size - 1, y);
            graphics.drawLine(left.x, left.y, right.x, right.y);
        }
        for (int x = 0; x < size; ++x)
        {
            java.awt.Point top = getScreenLocation(x, 0);
            java.awt.Point bottom = getScreenLocation(x, size - 1);
            graphics.drawLine(top.x, top.y, bottom.x, bottom.y);
        }
        int r = width / (size + 2) / 10;
        for (int x = 0; x < size; ++x)
            if (m_board.isHandicapLine(x))
                for (int y = 0; y < size; ++y)
                    if (m_board.isHandicapLine(y))
                    {
                        java.awt.Point point = getScreenLocation(x, y);
                        graphics.fillOval(point.x - r, point.y - r,
                                          2 * r + 1, 2 * r + 1);
                    }
    }

    private void drawShadows(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D == null || m_fastPaint)
            return;
        graphics2D.setComposite(m_composite3);
        Rectangle grid = getBounds();
        int width = grid.width / (m_board.getSize() + 2);
        int size = width - 2 * Field.getStoneMargin(width);
        int offset = getShadowOffset();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            if (getField(point).getColor() == go.Color.EMPTY)
                continue;
            java.awt.Point location = getScreenLocation(point.getX(),
                                                        point.getY());
            graphics.setColor(java.awt.Color.black);
            graphics.fillOval(location.x - size / 2 + offset,
                              location.y - size / 2 + offset,
                              size, size);
        }
        graphics.setPaintMode();
    }

    private Field getField(go.Point p)
    {
        assert(p != null);
        return m_field[p.getX()][p.getY()];
    }

    private void setColor(go.Point p, go.Color color)
    {
        getField(p).setColor(color);
    }

    private int getShadowOffset()
    {
        Rectangle grid = getBounds();
        int width = grid.width / (m_board.getSize() + 2);
        int size = width - 2 * Field.getStoneMargin(width);
        return size / 12;
    }

    private java.awt.Point getScreenLocation(int x, int y)
    {
        int size = m_board.getSize();
        assert(x >= 0 && x < size);
        assert(y >= 0 && y < size);
        int width = getSize().width;
        int offset = width / (size + 2) / 2;
        int screenX = ((x + 1) * width) / (size + 2) + offset;
        int screenY = ((size - y) * width) / (size + 2) + offset;
        return new java.awt.Point(screenX, screenY);
    }

    private void keyPressed(KeyEvent event)
    {
        int code = event.getKeyCode();
        int modifiers = event.getModifiers();
        int size = m_board.getSize();
        if ((modifiers & ActionEvent.CTRL_MASK) != 0)
            return;
        boolean shiftModifier = ((modifiers & ActionEvent.SHIFT_MASK) != 0);
        if (code == KeyEvent.VK_DOWN)
        {
            if (shiftModifier)
                do
                    m_focusPoint.down();
                while (! m_board.isHandicapLine(m_focusPoint.getY())
                       && ! m_board.isEdgeLine(m_focusPoint.getY()));
            else
                m_focusPoint.down();
        }
        else if (code == KeyEvent.VK_UP)
        {
            if (shiftModifier)
                do
                    m_focusPoint.up(size);
                while (! m_board.isHandicapLine(m_focusPoint.getY())
                       && ! m_board.isEdgeLine(m_focusPoint.getY()));
            else
                m_focusPoint.up(size);
        }
        else if (code == KeyEvent.VK_LEFT)
        {
            if (shiftModifier)
                do
                    m_focusPoint.left();
                while (! m_board.isHandicapLine(m_focusPoint.getX())
                       && ! m_board.isEdgeLine(m_focusPoint.getX()));
            else
                m_focusPoint.left();
        }
        else if (code == KeyEvent.VK_RIGHT)
        {
            if (shiftModifier)
                do
                    m_focusPoint.right(size);
                while (! m_board.isHandicapLine(m_focusPoint.getX())
                       && ! m_board.isEdgeLine(m_focusPoint.getX()));
            else
                m_focusPoint.right(size);
        }
        setFocusPoint(m_focusPoint);
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
