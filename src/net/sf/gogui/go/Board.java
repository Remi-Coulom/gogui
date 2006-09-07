//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Go board. */
public final class Board
    implements ConstBoard
{
    /** Constant for unknown rules. */
    public static final int RULES_UNKNOWN = 0;

    /** Constant for Chinese rules.
        Indicates that area counting (stones and territory) is used for the
        score.
    */
    public static final int RULES_CHINESE = 1;

    /** Constant for Japanese rules.
        Indicates that territory counting (territory and prisoners) is used
        for the score.
    */
    public static final int RULES_JAPANESE = 2;

    /** Constructor.
        @param boardSize The board size (number of points per row / column).
    */
    public Board(int boardSize)
    {
        init(boardSize);
    }

    /** Check for two consecutive passes.
        @return true, if the last two moves were pass moves
    */
    public boolean bothPassed()
    {
        int n = getNumberPlacements();
        return (n >= 2
                && getPlacement(n - 1).isPassMove()
                && getPlacement(n - 2).isPassMove());
    }

    /** Check if board contains a point.
        @param point The point to check
        @return true, if the point is on the board
    */
    public boolean contains(GoPoint point)
    {
        return point.isOnBoard(getSize());
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

    public Placement getPlacement(int i)
    {
        return ((StackEntry)m_stack.get(i)).m_placement;
    }

    public GoPoint getPoint(int i)
    {
        return m_allPoints[i];
    }

    public int getNumberPlacements()
    {
        return m_stack.size();
    }

    public int getNumberPoints()
    {
        return m_allPoints.length;
    }

    /** Get board size.
        @return The board size.
    */
    public int getSize()
    {
        return m_size;
    }

    public void getStones(GoPoint p, GoColor color, ArrayList stones)
    {
        assert(m_mark.isCleared());
        findStones(p, color, stones);
        m_mark.set(stones, false);
        assert(m_mark.isCleared());
    }

    public GoColor getToMove()
    {
        return m_toMove;
    }

    public void init(int size)
    {
        m_size = size;
        m_color = new GoColor[m_size][m_size];
        m_mark = new Marker(m_size);
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
        return (m_stack.size() > 0);
    }

    public boolean isSuicide(GoPoint point, GoColor toMove)
    {
        if (getColor(point) != GoColor.EMPTY)
            return false;
        play(point, toMove);
        int n = getNumberPlacements();
        StackEntry entry = (StackEntry)m_stack.get(n - 1);
        boolean result = (entry.m_suicide.size() > 0);
        undo();
        return result;
    }

    public void newGame()
    {
        for (int i = 0; i < m_allPoints.length; ++i)
            setColor(m_allPoints[i], GoColor.EMPTY);
        m_stack.clear();        
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
    public void play(Move move)
    {
        doPlacement(new Placement(move.getPoint(), move.getColor(), false));
    }

    public void setToMove(GoColor toMove)
    {
        m_toMove = toMove;
    }

    /** Add a setup stone.
        A setup stone differs from a move in that no stones are captured and
        the color to move does not switched.
        @param p the point
        @param color the color; GoColor.EMPTY for removing a stone
    */
    public void setup(GoPoint p, GoColor color)
    {
        doPlacement(new Placement(p, color, true));
    }

    /** Undo last move or setup stone. */
    public void undo()
    {
        assert(getNumberPlacements() > 0);
        int index = getNumberPlacements() - 1;
        StackEntry entry = (StackEntry)m_stack.get(index);
        m_stack.remove(index);
        Placement placement = entry.m_placement;
        GoColor c = placement.getColor();
        GoColor otherColor = c.otherColor();
        GoPoint p = placement.getPoint();
        if (p != null)
        {
            ArrayList suicide = entry.m_suicide;
            for (int i = 0; i < suicide.size(); ++i)
            {
                GoPoint stone = (GoPoint)suicide.get(i);
                setColor(stone, c);
            }
            setColor(p, entry.m_oldColor);
            ArrayList killed = entry.m_killed;
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
        m_toMove = entry.m_oldToMove;
        m_koPoint = entry.m_oldKoPoint;
    }

    /** Undo a number of moves or setup stones.
        @param n Number of moves to undo. Must be between 0
        and getNumberPlacements().
    */
    public void undo(int n)
    {
        assert(n >= 0);
        assert(n <= getNumberPlacements());
        for (int i = 0; i < n; ++i)
            undo();
    }

    /** Information necessary to undo a move or setup stone. */
    private static class StackEntry
    {
        public StackEntry(GoColor oldToMove, Placement placement,
                          GoColor oldColor, ArrayList killed,
                          ArrayList suicide, GoPoint oldKoPoint)
        {
            m_oldColor = oldColor;
            m_oldToMove = oldToMove;
            m_placement = placement;
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

        public final Placement m_placement;

        public final ArrayList m_killed;

        public final ArrayList m_suicide;
    }

    private Marker m_mark;

    private int m_size;

    private int m_capturedB;

    private int m_capturedW;

    private final ArrayList m_stack = new ArrayList(361);

    private GoColor m_color[][];

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
        assert(m_mark.isCleared());
        ArrayList stones = new ArrayList();
        if (isDead(p, color, stones))
        {
            killed.addAll(stones);
            for (int i = 0; i < stones.size(); ++i)
                setColor((GoPoint)stones.get(i), GoColor.EMPTY);
        }
        m_mark.set(stones, false);
        assert(m_mark.isCleared());
    }

    public void doPlacement(Placement placement)
    {
        GoPoint p = placement.getPoint();
        GoColor color = placement.getColor();
        boolean isSetup = placement.isSetup();
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
            if (! isSetup)
            {
                assert(color != GoColor.EMPTY);
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
        m_stack.add(new StackEntry(m_toMove, placement, old, killed,
                                   suicide, oldKoPoint));
        if (! isSetup)
            m_toMove = otherColor;        
    }

    private void findStones(GoPoint p, GoColor color, ArrayList stones)
    {
        GoColor c = getColor(p);
        if (c != color)
            return;
        if (m_mark.get(p))
            return;
        m_mark.set(p, true);
        stones.add(p);
        ArrayList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            findStones((GoPoint)(adj.get(i)), color, stones);
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
        if (m_mark.get(p))
            return true;
        m_mark.set(p, true);
        stones.add(p);
        ArrayList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isDead((GoPoint)(adj.get(i)), color, stones))
                return false;
        return true;
    }

    private void setColor(GoPoint point, GoColor color)
    {
        assert(point != null);
        m_color[point.getX()][point.getY()] = color;
    }
}

//----------------------------------------------------------------------------
