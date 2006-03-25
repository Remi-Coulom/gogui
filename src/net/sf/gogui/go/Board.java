//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Go board. */
public final class Board
{
    public static final int RULES_UNKNOWN = 0;

    public static final int RULES_CHINESE = 1;

    public static final int RULES_JAPANESE = 2;

    public Board(int boardSize)
    {
        initSize(boardSize);
    }

    /** Check for two consecutive passes.
        @return true, if the last two moves were pass moves
    */
    public boolean bothPassed()
    {
        int moveNumber = getMoveNumber();
        if (moveNumber < 2)
            return false;
        return (getMove(moveNumber - 1).getPoint() == null
                && getMove(moveNumber - 2).getPoint() == null);
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

    /** Check if board contains a point.
        @param The point to check
        @return true, if the point is on the board
    */
    public boolean contains(GoPoint point)
    {
        int size = getSize();
        return (point.getX() < size && point.getY() < size);
    }

    public ArrayList getAdjacentPoints(GoPoint point)
    {
        final int maxAdjacent = 4;
        ArrayList result = new ArrayList(maxAdjacent);
        int x = point.getX();
        int y = point.getY();
        if (x > 0)
            result.add(GoPoint.get(x - 1, y));
        if (x < m_size - 1)
            result.add(GoPoint.get(x + 1, y));
        if (y > 0)
            result.add(GoPoint.get(x, y - 1));
        if (y < m_size - 1)
            result.add(GoPoint.get(x, y + 1));
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
        return new BoardConstants(size).getHandicapStones(n);
    }

    public Move getMove(int i)
    {
        return ((MoveRecord)m_moves.get(i)).m_move;
    }

    public GoPoint getPoint(int i)
    {
        return m_allPoints[i];
    }

    public int getMoveNumber()
    {
        return m_moves.size();
    }

    public int getNumberPoints()
    {
        return m_allPoints.length;
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
        m_constants = new BoardConstants(size);
        initAllPoints();
        newGame();
    }

    public boolean isHandicap(GoPoint point)
    {
        return m_constants.isHandicap(point);
    }

    /** Check if move would violate the simple Ko rule.
        Assumes other color to move than the color of the last move.
        @param point The point to check
        @return true, if a move at this point would violate the simple ko rule
    */
    public boolean isKo(GoPoint point)
    {
        return point == m_koPoint;
    }

    public boolean isModified()
    {
        return (m_moves.size() > 0);
    }

    public boolean isSuicide(GoPoint point, GoColor toMove)
    {
        if (getColor(point) != GoColor.EMPTY)
            return false;
        play(point, toMove);
        int moveNumber = getMoveNumber();
        MoveRecord moveRecord = (MoveRecord)m_moves.get(moveNumber - 1);
        boolean result = (moveRecord.m_suicide.size() > 0);
        undo();
        return result;
    }

    public void newGame()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            setColor(m_allPoints[i], GoColor.EMPTY);
        m_moves.clear();        
        m_capturedB = 0;
        m_capturedW = 0;
        m_toMove = GoColor.BLACK;
        m_koPoint = null;
    }

    public void play(GoPoint point, GoColor color)
    {
        play(Move.get(point, color));
    }

    /** Play a move.
        Never fails, even if ko rule is violated, suicide or play on occupied
        points. For example, when loading an SGF file with illegal moves,
        we still want to be able to load and execute the moves.
    */
    public void play(Move m)
    {
        GoPoint p = m.getPoint();
        GoColor color = m.getColor();
        GoColor otherColor = color.otherColor();
        ArrayList killed = new ArrayList();
        ArrayList suicide = new ArrayList();
        GoColor old = GoColor.EMPTY;
        GoPoint oldKoPoint = m_koPoint;
        m_koPoint = null;
        if (p != null)
        {
            old = getColor(p);
            setColor(p, color);
            if (color != GoColor.EMPTY)
            {
                ArrayList adj = getAdjacentPoints(p);
                for (int i = 0; i < adj.size(); ++i)
                {
                    int killedSize = killed.size();
                    checkKill((GoPoint)(adj.get(i)), otherColor, killed);
                    if (killed.size() == killedSize + 1)
                        m_koPoint = (GoPoint)killed.get(killedSize);
                }
                checkKill(p, color, suicide);
                if (m_koPoint != null && ! isSingleStoneSingleLib(p, color))
                    m_koPoint = null;
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
        m_moves.add(new MoveRecord(m_toMove, m, old, killed, suicide,
                                   oldKoPoint));
        m_toMove = otherColor;        
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

    public void undo()
    {
        if (getMoveNumber() == 0)
            return;
        int index = getMoveNumber() - 1;
        MoveRecord r = (MoveRecord)m_moves.get(index);
        m_moves.remove(index);
        Move m = r.m_move;
        GoColor c = m.getColor();
        GoColor otherColor = c.otherColor();
        GoPoint p = m.getPoint();
        if (p != null)
        {
            ArrayList suicide = r.m_suicide;
            for (int i = 0; i < suicide.size(); ++i)
            {
                GoPoint stone = (GoPoint)suicide.get(i);
                setColor(stone, c);
            }
            setColor(p, r.m_oldColor);
            ArrayList killed = r.m_killed;
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
        m_toMove = r.m_oldToMove;
        m_koPoint = r.m_oldKoPoint;
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

    /** Information necessary to undo a move. */
    private static class MoveRecord
    {
        public MoveRecord(GoColor oldToMove, Move m, GoColor oldColor,
                          ArrayList killed, ArrayList suicide,
                          GoPoint oldKoPoint)
        {
            m_oldColor = oldColor;
            m_oldToMove = oldToMove;
            m_move = m;
            m_killed = killed;
            m_suicide = suicide;
            m_oldKoPoint = oldKoPoint;
        }

        public final GoPoint m_oldKoPoint;

        /** Old stone color of field.
            Needed in case move was played on a non-empty point.
        */
        public final GoColor m_oldColor;

        public final GoColor m_oldToMove;

        public final Move m_move;

        public final ArrayList m_killed;

        public final ArrayList m_suicide;
    }

    private boolean m_mark[][];

    private boolean m_dead[][];

    private int m_size;

    private int m_capturedB;

    private int m_capturedW;

    private final ArrayList m_moves = new ArrayList(361);

    private GoColor m_color[][];

    private GoColor m_score[][];

    private GoColor m_toMove;

    private BoardConstants m_constants;

    private GoPoint m_koPoint;

    private GoPoint m_allPoints[];

    private boolean isSingleStoneSingleLib(GoPoint point, GoColor color)
    {
        if (getColor(point) != color)
            return false;
        ArrayList adj = getAdjacentPoints(point);
        int lib = 0;
        for (int i = 0; i < adj.size(); ++i)
        {
            GoColor adjColor = getColor((GoPoint)adj.get(i));
            if (adjColor == GoColor.EMPTY)
            {
                ++lib;
                if (lib > 1)
                    return false;
            }
            else if (adjColor == color)
                return false;
        }
        return true;
    }

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

    private void initAllPoints()
    {
        m_allPoints = new GoPoint[m_size * m_size];
        int i = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
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
