//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

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

    /** Play move or setup stone.
        @param placement The placement to play.
    */
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

    /** Get points adjacent to a point.
        @param point The point.
        @return List of points adjacent.
    */
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

    /** Get number of black captured stones.
        @return The total number of black stones captured by all moves played.
    */
    public int getCapturedB()
    {
        return m_capturedB;
    }

    /** Get number of white captured stones.
        @return The total number of white stones captured by all moves played.
    */
    public int getCapturedW()
    {
        return m_capturedW;
    }

    /** Get state of a point on the board.
        @return GoColor.BLACK, GoColor.WHITE or GoColor.EMPTY
    */
    public GoColor getColor(GoPoint p)
    {
        return m_color[p.getX()][p.getY()];
    }

    /** Get location of handicap stones.
        @param n The number of handicap stones.
        @return List of points (go.Point) corresponding to the handicap
        stone locations.
        @see BoardConstants#getHandicapStones
    */
    public ArrayList getHandicapStones(int n)
    {
        return m_constants.getHandicapStones(n);
    }

    /** Get location of handicap stones for a given board size.
        @param n The number of handicap stones.
        @param size The board size.
        @return List of points (go.Point) corresponding to the handicap
        stone locations.
        @see BoardConstants#getHandicapStones
    */
    public static ArrayList getHandicapStones(int size, int n)
    {
        return new BoardConstants(size).getHandicapStones(n);
    }

    /** Opponent stones captured in last move.
        Does not include player stones killed by suicide.
        Requires that there is a last move (or setup stone).
        @return List of opponent stones (go.Point) captured in last move;
        empty if none were killed or if last placement was a setup stone.
        @see #getSuicide()
    */
    public ArrayList getKilled()
    {
        assert(getNumberPlacements() > 0);
        StackEntry entry = (StackEntry)m_stack.get(getNumberPlacements() - 1);
        return new ArrayList(entry.m_killed);
    }

    /** Get a placement (move or setup stone) from the sequence of placements
        played so far.
        @param i The number of the placement (starting with zero).
        @return The placement with the given number.
        @see #getNumberPlacements()
    */
    public Placement getPlacement(int i)
    {
        return ((StackEntry)m_stack.get(i)).m_placement;
    }

    /** Get a point on the board.
        Can be used for iterating over all points.
        @param i The index of the point between 0 and size * size - 1.
        @return The point with the given index.
        @see #getNumberPoints()
    */
    public GoPoint getPoint(int i)
    {
        return m_constants.getPoint(i);
    }

    /** Get the number of placements (moves or setup stones) played so far.
        @return The number of placements.
        @see #getPlacement
    */
    public int getNumberPlacements()
    {
        return m_stack.size();
    }

    /** Get the number of points on the board.
        @return The number of points on the board (size * size).
        @see #getPoint
    */
    public int getNumberPoints()
    {
        return m_constants.getNumberPoints();
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

    /** Player stones killed by suicide in last move.
        Requires that there is a last move (or setup stone).
        @return List of stones (go.Point) killed by suicide in last move,
        including the stone played; empty if no stones were killed by suicide
        or if last placement was a setup stone..
        @see #getKilled()
    */
    public ArrayList getSuicide()
    {
        assert(getNumberPlacements() > 0);
        StackEntry entry = (StackEntry)m_stack.get(getNumberPlacements() - 1);
        return new ArrayList(entry.m_suicide);
    }

    /** Get color to move.
        @return The color to move.
    */
    public GoColor getToMove()
    {
        return m_toMove;
    }

    /** Initialize the board for a given board size.
        For changing the board size.
        Also calls newGame().
        @param size The new board size.
    */
    public void init(int size)
    {
        m_size = size;
        m_color = new GoColor[m_size][m_size];
        m_mark = new Marker(m_size);
        m_constants = new BoardConstants(size);
        newGame();
    }

    /** Check if a move would capture anything (including suicide).
        @param point The point to check.
        @param toMove The player color.
        @return true, if a move on the given point by the given player would
        capture any opponent stones, or be a suicide move.
    */
    public boolean isCaptureOrSuicide(GoPoint point, GoColor toMove)
    {
        if (getColor(point) != GoColor.EMPTY)
            return false;
        play(point, toMove);
        int n = getNumberPlacements();
        StackEntry entry = (StackEntry)m_stack.get(n - 1);
        boolean result = (entry.m_suicide.size() > 0
                          || entry.m_killed.size() > 0);
        undo();
        return result;
    }

    /** Check if a point is a handicap point.
        @param point The point to check.
        @return true, if the given point is a handicap point.
        @see BoardConstants#isHandicap
    */
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

    /** Check if any placements (moves or setup stones) were made on the
        board.
    */
    public boolean isModified()
    {
        return (m_stack.size() > 0);
    }

    /** Check if a point would be a suicide move.
        @param point The point to check.
        @param toMove The player color to check.
        @return true, if a move at the given point by the given player
        would be a suicide move.
    */
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

    /** Start a new game.
        Takes back the effects of any placements (moves or setup stones)
        on the board.
    */
    public void newGame()
    {
        for (int i = 0; i < getNumberPoints(); ++i)
            setColor(getPoint(i), GoColor.EMPTY);
        m_stack.clear();        
        m_capturedB = 0;
        m_capturedW = 0;
        m_toMove = GoColor.BLACK;
        m_koPoint = null;
    }

    /** Play a move.
        @param point The location of the move.
        @param color The player who played the move.
        @see #play(Move)
    */
    public void play(GoPoint point, GoColor color)
    {
        play(Move.get(point, color));
    }

    /** Play a move.
        Never fails, even if ko rule is violated, suicide or play on occupied
        points. For example, when loading an SGF file with illegal moves,
        we still want to be able to load and execute the moves.
        A move will place a stone of the given color, capture all dead
        blocks adjacent to the stone, capture the block the stone is part of
        if it was a suicide move and switches the color to move.
        @param move The move (location and player)
    */
    public void play(Move move)
    {
        doPlacement(new Placement(move.getPoint(), move.getColor(), false));
    }

    /** Change the color to move.
        @param toMove The new color to move.
    */
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

    /** Undo the last placement (move or setup stone).
        Restores any stones removed by the last placement (captured or
        suicide) if it was a move and restore the color who was to move before
        the placement.
    */
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
        @see #undo()
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
            else if (adjColor.equals(color))
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

    private boolean isDead(GoPoint p, GoColor color, ArrayList stones)
    {
        GoColor c = getColor(p);
        if (c == GoColor.EMPTY)
            return false;
        if (! c.equals(color))
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

