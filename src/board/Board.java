//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package board;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.print.*;
import java.util.*;
import javax.swing.*;

//-----------------------------------------------------------------------------

class MoveRecord
{
    public MoveRecord(Color oldToMove, Move m, Color old, Vector killed,
                      Vector suicide)
    {
        m_old = old;
        m_oldToMove = oldToMove;
        m_move = m;
        m_killed = killed;
        m_suicide = suicide;
    }

    public Move getMove()
    {
        return m_move;
    }

    public Color getOldColor()
    {
        return m_old;
    }

    public Color getOldToMove()
    {
        return m_oldToMove;
    }

    public Vector getKilled()
    {
        return m_killed;
    }

    public Vector getSuicide()
    {
        return m_suicide;
    }

    private Color m_old;
    private Color m_oldToMove;
    private Move m_move;
    private Vector m_killed;
    private Vector m_suicide;
}

//-----------------------------------------------------------------------------

public class Board
    extends JPanel
    implements Printable
{
    public interface Listener
    {
        void fieldClicked(Point p);
    }

    public static final int RULES_JAPANESE = 0;

    public static final int RULES_CHINESE = 1;

    public Board(int boardSize)
    {
        setPreferredFieldSize();
        initSize(boardSize);
    }

    public boolean bothPassed()
    {
        if (m_moveNumber < 2)
            return false;
        return (getInternalMove(m_moveNumber - 1).getPoint() == null
                 && getInternalMove(m_moveNumber - 2).getPoint() == null);
    }

    public void clearAll()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
            clearInfluence(p);
            setFieldBackground(p, null);
            setMarkup(p, false);
            setString(p, "");
        }
        clearAllCrossHair();
    }

    public void clearAllCrossHair()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            setCrossHair(m_allPoints[i], false);
    }

    public void clearInfluence(Point p)
    {
        getField(p).clearInfluence();
    }

    public void fieldClicked(Point p)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p);
    }

    public Vector getAdjacentPoints(Point p)
    {
        Vector result = new Vector(4);
        int x = p.getX();
        int y = p.getY();
        if (x > 0)
            result.add(m_point[x - 1][y]);
        if (x < m_boardSize - 1)
            result.add(m_point[x + 1][y]);
        if (y > 0)
            result.add(m_point[x][y - 1]);
        if (y < m_boardSize - 1)
            result.add(m_point[x][y + 1]);
        return result;
    }

    public int getBoardSize()
    {
        return m_boardSize;
    }

    public int getCapturedB()
    {
        return m_capturedB;
    }

    public int getCapturedW()
    {
        return m_capturedW;
    }

    public Color getColor(Point p)
    {
        return m_color[p.getX()][p.getY()];
    }

    public Vector getHandicapStones(int n)
    {
        Vector result = new Vector(9);
        if (n == 0)
            return result;
        if (m_handicapLine1 < 0)
            return null;
        if (n > 4 && m_handicapLine2 < 0)
            return null;
        if (n >= 1)
            result.add(new Point(m_handicapLine1, m_handicapLine1));
        if (n >= 2)
            result.add(new Point(m_handicapLine3, m_handicapLine3));
        if (n >= 3)
            result.add(new Point(m_handicapLine1, m_handicapLine3));
        if (n >= 4)
            result.add(new Point(m_handicapLine3, m_handicapLine1));
        if (n >= 5)
            if (n % 2 != 0)
            {
                result.add(new Point(m_handicapLine2, m_handicapLine2));
                --n;
            }
        if (n >= 5)
            result.add(new Point(m_handicapLine1, m_handicapLine2));
        if (n >= 6)
            result.add(new Point(m_handicapLine3, m_handicapLine2));
        if (n >= 7)
            result.add(new Point(m_handicapLine2, m_handicapLine1));
        if (n >= 8)
            result.add(new Point(m_handicapLine2, m_handicapLine3));
        return result;
    }

    public float getKomi()
    {
        return m_komi;
    }

    public Move getMove(int i)
    {
        return ((MoveRecord)m_moves.get(i + m_setupNumber)).getMove();
    }

    public Point getPoint(int i)
    {
        return m_allPoints[i];
    }

    public int getMoveNumber()
    {
        return m_moveNumber - m_setupNumber;
    }

    public int getNumberPoints()
    {
        return m_allPoints.length;
    }

    public int getNumberSavedMoves()
    {
        return m_moves.size() - m_setupNumber;
    }

    public Dimension getPreferredFieldSize()
    {
        return m_preferredFieldSize;
    }

    public int getRules()
    {
        return m_rules;
    }

    public Vector getSetupStonesBlack()
    {
        return m_setupStonesBlack;
    }

    public Vector getSetupStonesWhite()
    {
        return m_setupStonesWhite;
    }

    public Color getToMove()
    {
        return m_toMove;
    }

    public void initSize(int size)
    {
        m_boardSize = size;
        m_color = new Color[m_boardSize][m_boardSize];
        m_field = new Field[m_boardSize][m_boardSize];
        m_mark = new boolean[m_boardSize][m_boardSize];
        m_dead = new boolean[m_boardSize][m_boardSize];
        m_score = new Color[m_boardSize][m_boardSize];
        m_point = new Point[m_boardSize][m_boardSize];
        m_capturedB = 0;
        m_capturedW = 0;
        initAllPoints();
        m_handicapLine1 = -1;
        m_handicapLine2 = -1;
        m_handicapLine3 = -1;
        if (m_boardSize >= 13)
        {
            m_handicapLine1 = 3;
            m_handicapLine3 = m_boardSize - 4;
        }
        else if (m_boardSize >= 8)
        {
            m_handicapLine1 = 2;
            m_handicapLine3 = m_boardSize - 3;
        }
        if (m_boardSize >= 11 && m_boardSize % 2 != 0)
            m_handicapLine2 = m_boardSize / 2;

        removeAll();
        setLayout(new GridLayout(m_boardSize + 2, m_boardSize + 2));
        addColumnLabels();
        for (int y = m_boardSize - 1; y >= 0; --y)
        {
            String yLabel = Integer.toString(y + 1);
            add(new JLabel(yLabel, JLabel.CENTER));
            for (int x = 0; x < m_boardSize; ++x)
            {
                Point p = m_point[x][y];
                Field field = new Field(this, p, isHandicap(p));
                add(field);
                m_field[x][y] = field;
            }
            add(new JLabel(yLabel, JLabel.CENTER));
        }
        addColumnLabels();
        m_lastMove = null;
        newGame();
        revalidate();
        repaint();
    }

    public boolean isModified()
    {
        return (m_moves.size() > 0 || m_setupNumber > 0);
    }

    public void newGame()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            setColor(m_allPoints[i], Color.EMPTY);
        m_moves.clear();
        m_moveNumber = 0;
        m_setupNumber = 0;
        m_toMove = Color.BLACK;
        drawLastMove();
    }

    public void play(Move m)
    {
        board.Point p = m.getPoint();
        Color color = m.getColor();
        Color otherColor = color.otherColor();
        Vector killed = new Vector();
        Vector suicide = new Vector();
        Color old = Color.EMPTY;
        if (p != null)
        {
            old = getColor(p);
            setColor(p, color);
            if (color != Color.EMPTY)
            {
                Vector adj = getAdjacentPoints(p);
                for (int i = 0; i < adj.size(); ++i)
                    checkKill((Point)(adj.get(i)), otherColor, killed);
                checkKill(p, color, suicide);
                if (color == Color.BLACK)
                {
                    m_capturedB += suicide.size();
                    m_capturedW += killed.size();
                }
                else
                {
                    m_capturedW += suicide.size();
                    m_capturedB += killed.size();
                }
            }
        }
        if (m_moveNumber == m_moves.size()
             || ! m.equals(getInternalMove(m_moveNumber)))
        {
            m_moves.setSize(m_moveNumber);
            m_moves.add(new MoveRecord(m_toMove, m, old, killed, suicide));
        }
        ++m_moveNumber;
        m_toMove = otherColor;        
        drawLastMove();
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

    public void scoreBegin()
    {
        clearAll();
        for (int i = 0; i < m_allPoints.length; ++i)
            setDead(m_allPoints[i], false);
        calcScore();
    }

    public void scoreSetDead(Point p)
    {
        Color c = getColor(p);
        if (c == Color.EMPTY)
            return;
        assert(isMarkCleared());
        Vector stones = new Vector(getNumberPoints());
        findStones(p, c, stones);
        boolean dead = ! getDead((Point)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            Point stone = (Point)stones.get(i);
            setDead(stone, dead);
            setCrossHair(stone, dead);
            setMark(stone, false);
        }
        assert(isMarkCleared());
        calcScore();
    }

    public Score scoreGet()
    {
        Score s = new Score();
        s.m_rules = m_rules;        
        s.m_capturedBlack = m_capturedB;
        s.m_capturedWhite = m_capturedW;
        int areaDiff = 0;
        int territoryDiff = 0;
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
            Color c = getColor(p);
            Color sc = getScore(p);
            if (sc == Color.BLACK)
            {
                ++s.m_areaBlack;
                ++areaDiff;
            }
            else if (sc == Color.WHITE)
            {
                ++s.m_areaWhite;
                --areaDiff;
            }
            if (c == Color.EMPTY)
            {
                if (sc == Color.BLACK)
                {
                    ++s.m_territoryBlack;
                    ++territoryDiff;
                }
                else if (sc == Color.WHITE)
                {
                    ++s.m_territoryWhite;
                    --territoryDiff;
                }
            }
            if (c == Color.BLACK && sc == Color.WHITE)
            {
                ++s.m_capturedBlack;
                ++s.m_territoryWhite;
                --territoryDiff;
            }
            if (c == Color.WHITE && sc == Color.BLACK)
            {
                ++s.m_capturedWhite;
                ++s.m_territoryBlack;
                ++territoryDiff;
            }
        }
        s.m_resultChinese = areaDiff - m_komi;
        s.m_resultJapanese =
            s.m_capturedWhite - s.m_capturedBlack + territoryDiff - m_komi;
        if (m_rules == RULES_JAPANESE)
            s.m_result = s.m_resultJapanese;
        else
        {
            assert(m_rules == RULES_CHINESE);
            s.m_result = s.m_resultChinese;
        }
        return s;
    }

    public void setFieldBackground(Point p, java.awt.Color color)
    {
        getField(p).setFieldBackground(color);
    }

    public void setCrossHair(Point p, boolean crossHair)
    {
        if (m_lastMove != null)
        {
            Field f = getField(m_lastMove);
            f.setCrossHair(false);
            m_lastMove = null;
        }
        getField(p).setCrossHair(crossHair);
    }

    public void setInfluence(Point p, double value)
    {
        getField(p).setInfluence(value);
    }

    public void setKomi(float komi)
    {
        m_komi = komi;
    }

    public void setListener(Listener l)
    {
        m_listener = l;
    }

    public void setMarkup(Point p, boolean markup)
    {
        getField(p).setMarkup(markup);
    }

    public void setString(Point p, String s)
    {
        getField(p).setString(s);
    }

    public void setToMove(Color toMove)
    {
        m_toMove = toMove;
    }

    /** Set a stone on the board.
        Will remove dead stones.
        Requires: getMoveNumber() == 0
    */
    public void setup(Move m)
    {
        assert(getMoveNumber() == 0);
        play(m);
        if (m.getColor() == Color.BLACK)
            m_setupStonesBlack.add(m.getPoint());
        else if (m.getColor() == Color.WHITE)
            m_setupStonesWhite.add(m.getPoint());
        else
            assert(false);
        ++m_setupNumber;
    }

    public void showColorBoard(String[][] board)
    {
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
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
            setFieldBackground(p, c);
        }
    }

    public void showDoubleBoard(double[][] board, double scale)
    {
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
            double d = board[p.getX()][p.getY()] * scale;
            setInfluence(p, d);
        }
    }

    public void showPointList(Point pointList[])
    {
        for (int i = 0; i < pointList.length; ++i)
        {
            Point p = pointList[i];
            if (p != null)
                setMarkup(p, true);
        }
    }

    public void showStringBoard(String[][] board)
    {
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
            setString(p, board[p.getX()][p.getY()]);
        }
    }

    public void setRules(int rules)
    {
        assert(rules == RULES_JAPANESE || rules == RULES_CHINESE);
        m_rules = rules;
    }

    public void undo()
    {
        if (getMoveNumber() == 0)
            return;
        --m_moveNumber;
        MoveRecord r = (MoveRecord)m_moves.get(m_moveNumber);
        Move m = r.getMove();
        Color c = m.getColor();
        Color otherColor = c.otherColor();
        Point p = m.getPoint();
        if (p != null)
        {
            Vector suicide = r.getSuicide();
            for (int i = 0; i < suicide.size(); ++i)
            {
                Point stone = (Point)suicide.get(i);
                setColor(stone, c);
            }
            setColor(p, r.getOldColor());
            Vector killed = r.getKilled();
            for (int i = 0; i < killed.size(); ++i)
            {
                Point stone = (Point)killed.get(i);
                setColor(stone, otherColor);
            }
            if (c == Color.BLACK)
            {
                m_capturedB -= suicide.size();
                m_capturedW -= killed.size();
            }
            else
            {
                m_capturedW -= suicide.size();
                m_capturedB -= killed.size();
            }
        }
        m_toMove = r.getOldToMove();
        drawLastMove();
    }
    
    private boolean m_mark[][];
    private boolean m_dead[][];
    private int m_boardSize;
    private int m_capturedB;
    private int m_capturedW;
    private int m_handicapLine1;
    private int m_handicapLine2;
    private int m_handicapLine3;
    private int m_moveNumber;
    private int m_rules = RULES_CHINESE;
    private int m_setupNumber;
    private float m_komi = 5.5f;
    private Dimension m_preferredFieldSize;
    private Vector m_moves = new Vector(361, 361);
    private Vector m_setupStonesBlack = new Vector();
    private Vector m_setupStonesWhite = new Vector();
    private Color m_color[][];
    private Color m_score[][];
    private Color m_toMove;
    private Field m_field[][];
    private Listener m_listener;
    private Point m_allPoints[];
    private Point m_point[][];
    private Point m_lastMove;

    private void addColumnLabels()
    {
        add(Box.createHorizontalGlue());
        char c = 'A';
        for (int x = 0; x < m_boardSize; ++x)
        {
            add(new JLabel(new Character(c).toString(), JLabel.CENTER));
            ++c;
            if (c == 'I')
                ++c;
        }
        add(Box.createHorizontalGlue());
    }

    private void calcScore()
    {
        assert(isMarkCleared());
        boolean allEmpty = true;
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i]; 
            Color c = getColor(p);
            if (c != Color.EMPTY)
                allEmpty = false;
            if (c != Color.EMPTY)
            {
                if (! getDead(p))
                    setScore(p, c);
            }
            else
                setScore(p, Color.EMPTY);
        }
        if (allEmpty)
            return;
        Vector territory = new Vector(getNumberPoints());
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
            if (! getMark(p))
            {
                territory.clear();
                if (isTerritory(p, territory, Color.BLACK))
                {
                    for (int j = 0; j < territory.size(); ++j)
                        setScore((Point)territory.get(j), Color.BLACK);
                }
                else
                {
                    for (int j = 0; j < territory.size(); ++j)
                        setMark((Point)territory.get(j), false);
                    if (isTerritory(p, territory, Color.WHITE))
                    {
                        for (int j = 0; j < territory.size(); ++j)
                            setScore((Point)territory.get(j), Color.WHITE);
                    }
                    else
                    {
                        for (int j = 0; j < territory.size(); ++j)
                            setMark((Point)territory.get(j), false);
                    }
                }
            }
        }
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            Point p = m_allPoints[i];
            setMark(p, false);
            Color c = getScore(p);
            if (c == Color.BLACK)
                setInfluence(p, 1.0);
            else if (c == Color.WHITE)
                setInfluence(p, -1.0);
            else
                setInfluence(p, 0);
        }
        assert(isMarkCleared());
    }

    private void checkKill(Point p, Color color, Vector killed)
    {
        assert(isMarkCleared());
        Vector stones = new Vector();
        if (isDead(p, color, stones))
        {
            killed.addAll(stones);
            for (int i = 0; i < stones.size(); ++i)
                setColor((Point)stones.get(i), Color.EMPTY);
        }
        for (int i = 0; i < stones.size(); ++i)
            setMark((Point)stones.get(i), false);
        assert(isMarkCleared());
    }

    private void findStones(Point p, Color color, Vector stones)
    {
        Color c = getColor(p);
        if (c != color)
            return;
        if (getMark(p))
            return;
        setMark(p, true);
        stones.add(p);
        Vector adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            findStones((Point)(adj.get(i)), color, stones);
    }

    private void drawLastMove()
    {
        if (m_lastMove != null)
        {
            Field f = getField(m_lastMove);
            f.setCrossHair(false);
            m_lastMove = null;
        }
        if (getMoveNumber() > 0)
        {
            Move m = getInternalMove(m_moveNumber - 1);
            m_lastMove = m.getPoint();
            if (m_lastMove != null && m.getColor() != Color.EMPTY)
            {
                Field f = m_field[m_lastMove.getX()][m_lastMove.getY()];
                f.setCrossHair(true);
            }
        }
    }

    private boolean getDead(Point p)
    {
        return m_dead[p.getX()][p.getY()];
    }

    private Field getField(Point p)
    {
        assert(p != null);
        return m_field[p.getX()][p.getY()];
    }

    private Move getInternalMove(int i)
    {
        return ((MoveRecord)m_moves.get(i)).getMove();
    }

    private boolean getMark(Point p)
    {
        return m_mark[p.getX()][p.getY()];
    }

    private Color getScore(Point p)
    {
        return m_score[p.getX()][p.getY()];
    }

    private void initAllPoints()
    {
        m_allPoints = new Point[m_boardSize * m_boardSize];
        int i = 0;
        for (int x = 0; x < m_boardSize; ++x)
            for (int y = 0; y < m_boardSize; ++y)
            {
                Point p = new Point(x, y);
                m_allPoints[i++] = p;
                m_point[x][y] = p;
            }
    }

    private boolean isDead(Point p, Color color, Vector stones)
    {
        Color c = getColor(p);
        if (c == Color.EMPTY)
            return false;
        if (c != color)
            return true;
        if (getMark(p))
            return true;
        setMark(p, true);
        stones.add(p);
        Vector adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isDead((Point)(adj.get(i)), color, stones))
                return false;
        return true;
    }

    private boolean isHandicap(Point p)
    {
        int x = p.getX();
        int y = p.getY();
        return (isOnHandicapLine(x) && isOnHandicapLine(y));
    }

    private boolean isMarkCleared()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            if (getMark(m_allPoints[i]))
                 return false;
        return true;
    }    

    private boolean isOnHandicapLine(int i)
    {
        return (i == m_handicapLine1
                || i == m_handicapLine2
                || i == m_handicapLine3);
    }

    private boolean isTerritory(Point p, Vector territory, Color color)
    {
        Color c = getColor(p);
        if (c == color.otherColor() && ! getDead(p))
            return false;
        if (c == color)
        {
            if (getDead(p))
                return false;
            else
                return true;
        }
        if (getMark(p))
            return true;
        setMark(p, true);
        territory.add(p);
        Vector adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isTerritory((Point)(adj.get(i)), territory, color))
                return false;
        return true;
    }

    private void setColor(Point p, Color color)
    {
        m_color[p.getX()][p.getY()] = color;
        getField(p).setColor(color);
    }

    private void setDead(Point p, boolean value)
    {
        m_dead[p.getX()][p.getY()] = value;
    }

    private void setMark(Point p, boolean value)
    {
        m_mark[p.getX()][p.getY()] = value;
    }

    private void setPreferredFieldSize()
    {
        int size;
        Font font = UIManager.getFont("Label.font");        
        if (font != null)
            size = (int)((double)font.getSize() * 2.5);
        else
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            size = screenSize.height / 30;
        }
        if (size % 2 == 0)
            ++size;
        m_preferredFieldSize = new Dimension(size, size);
    }

    private void setScore(Point p, Color c)
    {
        m_score[p.getX()][p.getY()] = c;
    }
}

//-----------------------------------------------------------------------------
