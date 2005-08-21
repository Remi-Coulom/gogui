//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Information necessary to undo a move. */
class MoveRecord
{
    public MoveRecord(GoColor oldToMove, Move m, GoColor old,
                      ArrayList killed, ArrayList suicide)
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

    public GoColor getOldColor()
    {
        return m_old;
    }

    public GoColor getOldToMove()
    {
        return m_oldToMove;
    }

    public ArrayList getKilled()
    {
        return m_killed;
    }

    public ArrayList getSuicide()
    {
        return m_suicide;
    }

    private final GoColor m_old;

    private final GoColor m_oldToMove;

    private final Move m_move;

    private final ArrayList m_killed;

    private final ArrayList m_suicide;
}

//----------------------------------------------------------------------------

/** Go board. */
public final class Board
{
    public static final int RULES_UNKNOWN = 0;

    public static final int RULES_CHINESE = 1;

    public static final int RULES_JAPANESE = 2;

    public static final int NUMBER_ROTATIONS = 8;

    public Board(int boardSize)
    {
        initSize(boardSize);
    }

    public boolean bothPassed()
    {
        if (m_moveNumber < 2)
            return false;
        return (getInternalMove(m_moveNumber - 1).getPoint() == null
                && getInternalMove(m_moveNumber - 2).getPoint() == null);
    }

    public void calcScore()
    {
        assert(isMarkCleared());
        boolean allEmpty = true;
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            GoPoint p = m_allPoints[i]; 
            GoColor c = getColor(p);
            setScore(p, GoColor.EMPTY);
            if (c != GoColor.EMPTY)
            {
                allEmpty = false;
                if (! scoreGetDead(p))
                    setScore(p, c);
            }
        }
        if (allEmpty)
            return;
        ArrayList territory = new ArrayList(getNumberPoints());
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            GoPoint p = m_allPoints[i];
            if (! getMark(p))
            {
                territory.clear();
                if (isTerritory(p, territory, GoColor.BLACK))
                {
                    for (int j = 0; j < territory.size(); ++j)
                        setScore((GoPoint)territory.get(j), GoColor.BLACK);
                }
                else
                {
                    setMark(territory, false);
                    if (isTerritory(p, territory, GoColor.WHITE))
                    {
                        for (int j = 0; j < territory.size(); ++j)
                            setScore((GoPoint)territory.get(j),
                                     GoColor.WHITE);
                    }
                    else
                    {
                        setMark(territory, false);
                    }
                }
            }
        }
        clearMark();
    }

    public boolean contains(GoPoint point)
    {
        int size = getSize();
        return (point.getX() < size && point.getY() < size);
    }

    public ArrayList getAdjacentPoints(GoPoint p)
    {
        final int maxAdjacent = 4;
        ArrayList result = new ArrayList(maxAdjacent);
        int x = p.getX();
        int y = p.getY();
        if (x > 0)
            result.add(getPoint(x - 1, y));
        if (x < m_size - 1)
            result.add(getPoint(x + 1, y));
        if (y > 0)
            result.add(getPoint(x, y - 1));
        if (y < m_size - 1)
            result.add(getPoint(x, y + 1));
        return result;
    }

    public GoColor getScore(GoPoint p)
    {
        return m_score[p.getX()][p.getY()];
    }

    public int getSize()
    {
        return m_size;
    }

    public int getCapturedB()
    {
        return m_capturedB;
    }

    public int getCapturedW()
    {
        return m_capturedW;
    }

    public GoColor getColor(GoPoint p)
    {
        return m_color[p.getX()][p.getY()];
    }

    public ArrayList getHandicapStones(int n)
    {
        return m_constants.getHandicapStones(n);
    }

    public static ArrayList getHandicapStones(int size, int n)
    {
        return new Constants(size).getHandicapStones(n);
    }

    public Move getMove(int i)
    {
        return ((MoveRecord)m_moves.get(i + m_setupNumber)).getMove();
    }

    public GoPoint getPoint(int i)
    {
        return m_allPoints[i];
    }

    public Move getInternalMove(int i)
    {
        return ((MoveRecord)m_moves.get(i)).getMove();
    }

    public int getInternalNumberMoves()
    {
        return m_setupNumber;
    }

    public int getMoveNumber()
    {
        return m_moveNumber - m_setupNumber;
    }

    public int getNumberPoints()
    {
        return m_allPoints.length;
    }

    public ArrayList getSetupStonesBlack()
    {
        return m_setupStonesBlack;
    }

    public ArrayList getSetupStonesWhite()
    {
        return m_setupStonesWhite;
    }

    public void getStones(GoPoint p, GoColor color, ArrayList stones)
    {
        assert(isMarkCleared());
        findStones(p, color, stones);
        setMark(stones, false);
        assert(isMarkCleared());
    }

    public GoColor getToMove()
    {
        return m_toMove;
    }

    public void initSize(int size)
    {
        m_size = size;
        m_color = new GoColor[m_size][m_size];
        m_mark = new boolean[m_size][m_size];
        m_dead = new boolean[m_size][m_size];
        m_score = new GoColor[m_size][m_size];
        m_capturedB = 0;
        m_capturedW = 0;
        m_constants = new Constants(size);
        initAllPoints();
        newGame();
    }

    public boolean isEdgeLine(int i)
    {
        return (i == 0 || i == m_size - 1);
    }

    public boolean isHandicapLine(int i)
    {
        return (i == m_constants.m_handicapLine1
                || i == m_constants.m_handicapLine2
                || i == m_constants.m_handicapLine3);
    }

    public boolean isHandicap(GoPoint p)
    {
        int x = p.getX();
        int y = p.getY();
        return (isHandicapLine(x) && isHandicapLine(y));
    }

    public boolean isModified()
    {
        return (m_moves.size() > 0 || m_setupNumber > 0);
    }

    public boolean isSuicide(GoPoint point, GoColor toMove)
    {
        if (getColor(point) != GoColor.EMPTY)
            return false;
        play(point, toMove);
        MoveRecord moveRecord = (MoveRecord)m_moves.get(m_moveNumber - 1);
        boolean result = (moveRecord.getSuicide().size() > 0);
        undo();
        return result;
    }

    public void newGame()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            setColor(m_allPoints[i], GoColor.EMPTY);
        m_moves.clear();
        m_moveNumber = 0;
        m_setupNumber = 0;
        m_capturedB = 0;
        m_capturedW = 0;
        m_toMove = GoColor.BLACK;
        m_setupStonesBlack.clear();
        m_setupStonesWhite.clear();
    }

    public void play(GoPoint point, GoColor color)
    {
        play(Move.create(point, color));
    }

    public void play(Move m)
    {
        GoPoint p = m.getPoint();
        GoColor color = m.getColor();
        GoColor otherColor = color.otherColor();
        ArrayList killed = new ArrayList();
        ArrayList suicide = new ArrayList();
        GoColor old = GoColor.EMPTY;
        if (p != null)
        {
            old = getColor(p);
            setColor(p, color);
            if (color != GoColor.EMPTY)
            {
                ArrayList adj = getAdjacentPoints(p);
                for (int i = 0; i < adj.size(); ++i)
                    checkKill((GoPoint)(adj.get(i)), otherColor, killed);
                checkKill(p, color, suicide);
                if (color == GoColor.BLACK)
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
        assert(m_moves.size() == m_moveNumber);
        m_moves.add(new MoveRecord(m_toMove, m, old, killed, suicide));
        ++m_moveNumber;
        m_toMove = otherColor;        
    }

    public GoPoint rotate(int rotationIndex, GoPoint point)
    {
        assert(rotationIndex < NUMBER_ROTATIONS);
        if (point == null)
            return null;
        int x = point.getX();
        int y = point.getY();
        int size = getSize();
        switch (rotationIndex)
        {
        case 0:
            return getPoint(x, y);
        case 1:
            return getPoint(size - x - 1, y);
        case 2:
            return getPoint(x, size - y - 1);
        case 3:
            return getPoint(y, x);
        case 4:
            return getPoint(size - y - 1, x);
        case 5:
            return getPoint(y, size - x - 1);
        case 6:
            return getPoint(size - x - 1, size - y - 1);
        case 7:
            return getPoint(size - y - 1, size - x - 1);
        default:
            return getPoint(x, y);
        }
    }

    public void scoreBegin(GoPoint[] isDeadStone)
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            scoreSetDead(m_allPoints[i], false);
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                scoreSetDead(isDeadStone[i], true);
        calcScore();
    }

    /** Mark point as dead for scoring. */
    public void scoreSetDead(GoPoint p, boolean value)
    {
        m_dead[p.getX()][p.getY()] = value;
    }

    public Score scoreGet(double komi, int rules)
    {
        Score s = new Score();
        s.m_rules = rules;
        s.m_komi = komi;
        s.m_capturedBlack = m_capturedB;
        s.m_capturedWhite = m_capturedW;
        int areaDiff = 0;
        int territoryDiff = 0;
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            GoPoint p = m_allPoints[i];
            GoColor c = getColor(p);
            GoColor sc = getScore(p);
            if (sc == GoColor.BLACK)
            {
                ++s.m_areaBlack;
                ++areaDiff;
            }
            else if (sc == GoColor.WHITE)
            {
                ++s.m_areaWhite;
                --areaDiff;
            }
            if (c == GoColor.EMPTY)
            {
                if (sc == GoColor.BLACK)
                {
                    ++s.m_territoryBlack;
                    ++territoryDiff;
                }
                else if (sc == GoColor.WHITE)
                {
                    ++s.m_territoryWhite;
                    --territoryDiff;
                }
            }
            if (c == GoColor.BLACK && sc == GoColor.WHITE)
            {
                ++s.m_capturedBlack;
                ++s.m_territoryWhite;
                --territoryDiff;
            }
            if (c == GoColor.WHITE && sc == GoColor.BLACK)
            {
                ++s.m_capturedWhite;
                ++s.m_territoryBlack;
                ++territoryDiff;
            }
        }
        s.m_resultChinese = areaDiff - komi;
        s.m_resultJapanese =
            s.m_capturedWhite - s.m_capturedBlack + territoryDiff - komi;
        if (rules == RULES_JAPANESE)
            s.m_result = s.m_resultJapanese;
        else
            s.m_result = s.m_resultChinese;
        return s;
    }

    public boolean scoreGetDead(GoPoint p)
    {
        return m_dead[p.getX()][p.getY()];
    }

    public void setToMove(GoColor toMove)
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
        if (m.getColor() == GoColor.BLACK)
            m_setupStonesBlack.add(m.getPoint());
        else if (m.getColor() == GoColor.WHITE)
            m_setupStonesWhite.add(m.getPoint());
        else
            assert(false);
        ++m_setupNumber;
    }

    public void undo()
    {
        if (getMoveNumber() == 0)
            return;
        --m_moveNumber;
        MoveRecord r = (MoveRecord)m_moves.get(m_moveNumber);
        m_moves.remove(m_moveNumber);
        Move m = r.getMove();
        GoColor c = m.getColor();
        GoColor otherColor = c.otherColor();
        GoPoint p = m.getPoint();
        if (p != null)
        {
            ArrayList suicide = r.getSuicide();
            for (int i = 0; i < suicide.size(); ++i)
            {
                GoPoint stone = (GoPoint)suicide.get(i);
                setColor(stone, c);
            }
            setColor(p, r.getOldColor());
            ArrayList killed = r.getKilled();
            for (int i = 0; i < killed.size(); ++i)
            {
                GoPoint stone = (GoPoint)killed.get(i);
                setColor(stone, otherColor);
            }
            if (c == GoColor.BLACK)
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
    }

    /** Undo a number of moves.
        @param n Number of moves to undo. Must be between 0
        and getMoveNumber().
    */
    public void undo(int n)
    {
        assert(n >= 0);
        assert(n <= getMoveNumber());
        for (int i = 0; i < n; ++i)
            undo();
    }

    /** Some values that are constant for a given board size. */
    private static class Constants
    {
        public Constants(int size)
        {
            m_size = size;
            m_handicapLine1 = -1;
            m_handicapLine2 = -1;
            m_handicapLine3 = -1;
            if (size >= 13)
            {
                m_handicapLine1 = 3;
                m_handicapLine3 = size - 4;
            }
            else if (size >= 8)
            {
                m_handicapLine1 = 2;
                m_handicapLine3 = size - 3;
            }
            if (size >= 11 && size % 2 != 0)
                m_handicapLine2 = size / 2;
        }

        public ArrayList getHandicapStones(int n)
        {
            ArrayList result = new ArrayList(9);
            if (n == 0)
                return result;
            int line1 = m_handicapLine1;
            int line2 = m_handicapLine2;
            int line3 = m_handicapLine3;
            if (line1 < 0)
                return null;
            if (n > 4 && line2 < 0)
                return null;
            if (n >= 1)
                result.add(getPoint(line1, line1));
            if (n >= 2)
                result.add(getPoint(line3, line3));
            if (n >= 3)
                result.add(getPoint(line1, line3));
            if (n >= 4)
                result.add(getPoint(line3, line1));
            if (n >= 5)
                if (n % 2 != 0)
                {
                    result.add(getPoint(line2, line2));
                    --n;
                }
            if (n >= 5)
                result.add(getPoint(line1, line2));
            if (n >= 6)
                result.add(getPoint(line3, line2));
            if (n >= 7)
                result.add(getPoint(line2, line1));
            if (n >= 8)
                result.add(getPoint(line2, line3));
            return result;
        }

        private int m_size;

        private int m_handicapLine1;

        private int m_handicapLine2;

        private int m_handicapLine3;

        private GoPoint getPoint(int x, int y)
        {
            assert(x >= 0);
            assert(x < m_size);
            assert(y >= 0);
            assert(y < m_size);
            return GoPoint.create(x, y);
        }
    }
    
    private boolean m_mark[][];

    private boolean m_dead[][];

    private int m_size;

    private int m_capturedB;

    private int m_capturedW;

    private int m_moveNumber;

    private int m_setupNumber;

    private final ArrayList m_moves = new ArrayList(361);

    private final ArrayList m_setupStonesBlack = new ArrayList();

    private final ArrayList m_setupStonesWhite = new ArrayList();

    private GoColor m_color[][];

    private GoColor m_score[][];

    private GoColor m_toMove;

    private Constants m_constants;

    private GoPoint m_allPoints[];

    private void checkKill(GoPoint p, GoColor color, ArrayList killed)
    {
        assert(isMarkCleared());
        ArrayList stones = new ArrayList();
        if (isDead(p, color, stones))
        {
            killed.addAll(stones);
            for (int i = 0; i < stones.size(); ++i)
                setColor((GoPoint)stones.get(i), GoColor.EMPTY);
        }
        setMark(stones, false);
        assert(isMarkCleared());
    }

    private void clearMark()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
        {
            GoPoint p = m_allPoints[i];
            setMark(p, false);
        }
    }

    private void findStones(GoPoint p, GoColor color, ArrayList stones)
    {
        GoColor c = getColor(p);
        if (c != color)
            return;
        if (getMark(p))
            return;
        setMark(p, true);
        stones.add(p);
        ArrayList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            findStones((GoPoint)(adj.get(i)), color, stones);
    }

    private boolean getMark(GoPoint p)
    {
        return m_mark[p.getX()][p.getY()];
    }

    private GoPoint getPoint(int x, int y)
    {
        return m_constants.getPoint(x, y);
    }

    private void initAllPoints()
    {
        m_allPoints = new GoPoint[m_size * m_size];
        int i = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
            {
                GoPoint point = getPoint(x, y);
                m_allPoints[i++] = point;
            }
    }

    private boolean isDead(GoPoint p, GoColor color, ArrayList stones)
    {
        GoColor c = getColor(p);
        if (c == GoColor.EMPTY)
            return false;
        if (c != color)
            return true;
        if (getMark(p))
            return true;
        setMark(p, true);
        stones.add(p);
        ArrayList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isDead((GoPoint)(adj.get(i)), color, stones))
                return false;
        return true;
    }

    private boolean isMarkCleared()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            if (getMark(m_allPoints[i]))
                 return false;
        return true;
    }    

    private boolean isTerritory(GoPoint p, ArrayList territory, GoColor color)
    {
        GoColor c = getColor(p);
        if (c == color.otherColor() && ! scoreGetDead(p))
            return false;
        if (c == color)
            return (! scoreGetDead(p));
        if (getMark(p))
            return true;
        setMark(p, true);
        territory.add(p);
        ArrayList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isTerritory((GoPoint)(adj.get(i)), territory, color))
                return false;
        return true;
    }

    private void setColor(GoPoint point, GoColor color)
    {
        assert(point != null);
        m_color[point.getX()][point.getY()] = color;
    }

    private void setMark(GoPoint p, boolean value)
    {
        m_mark[p.getX()][p.getY()] = value;
    }

    private void setMark(ArrayList points, boolean value)
    {
        int size = points.size();
        for (int i = 0; i < size; ++i)
            setMark((GoPoint)points.get(i), value);
    }

    private void setScore(GoPoint p, GoColor c)
    {
        assert(c != null);
        m_score[p.getX()][p.getY()] = c;
    }
}

//----------------------------------------------------------------------------
