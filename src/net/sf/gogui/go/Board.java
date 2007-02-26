//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

/** Go board. */
public final class Board
    implements ConstBoard
{
    /** Constructor.
        @param boardSize The board size (number of points per row / column)
        in the range from one to GoPoint.MAXSIZE
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
        int n = getNumberMoves();
        return (n >= 2
                && getStackEntry(n - 1).m_move.getPoint() == null
                && getStackEntry(n - 2).m_move.getPoint() == null);
    }

    /** Check if board contains a point.
        @param point The point to check
        @return true, if the point is on the board
    */
    public boolean contains(GoPoint point)
    {
        return point.isOnBoard(getSize());
    }

    /** Get points adjacent to a point.
        @param point The point.
        @return List of points adjacent.
    */
    public ConstPointList getAdjacentPoints(GoPoint point)
    {
        final int maxAdjacent = 4;
        PointList result = new PointList(maxAdjacent);
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

    /** Get number of captured stones.
        @return The total number of stones of the given color captured by
        opponent moves or by suicide.
    */
    public int getCaptured(GoColor c)
    {
        assert(c.isBlackWhite());
        return m_captured[c.toInteger()];
    }

    /** Get state of a point on the board.
        @return GoColor.BLACK, GoColor.WHITE or GoColor.EMPTY
    */
    public GoColor getColor(GoPoint p)
    {
        return m_color[p.getX()][p.getY()];
    }

    /** Get location of handicap stones for a given board size.
        @param n The number of handicap stones.
        @param size The board size.
        @return List of points (go.Point) corresponding to the handicap
        stone locations.
        @see BoardConstants#getHandicapStones
    */
    public static ConstPointList getHandicapStones(int size, int n)
    {
        return BoardConstants.get(size).getHandicapStones(n);
    }

    /** Opponent stones captured in last move.
        Does not include player stones killed by suicide.
        Requires that there is a last move (or setup stone).
        @return List of opponent stones (go.Point) captured in last move;
        empty if none were killed or if last action was a setup stone.
        @see #getSuicide()
    */
    public ConstPointList getKilled()
    {
        int n = getNumberMoves();
        assert(n > 0);
        return getStackEntry(n - 1).m_killed;
    }

    /** Return last move.
        @return Last move or null if no action was done yet or last
        action was not a move.
    */
    public Move getLastMove()
    {
        int n = getNumberMoves();
        if (n == 0)
            return null;
        return getStackEntry(n - 1).m_move;
    }

    /** Get the number of moves played so far.
        @return The number of moves.
        @see #getMove
    */
    public int getNumberMoves()
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

    /** Get a move from the sequence of moves played so far.
        @param i The number of the move (starting with zero).
        @return The move with the given number.
        @see #getNumberMoves()
    */
    public Move getMove(int i)
    {
        return ((StackEntry)m_stack.get(i)).m_move;
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

    public ConstPointList getSetup(GoColor c)
    {
        return m_setup[c.toInteger()];
    }

    public GoColor getSetupPlayer()
    {
        return m_setupPlayer;
    }

    /** Get board size.
        @return The board size.
    */
    public int getSize()
    {
        return m_size;
    }

    /** Get stones of a block. */
    public void getStones(GoPoint p, GoColor color, PointList stones)
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
        or if last action was a setup stone..
        @see #getKilled()
    */
    public ConstPointList getSuicide()
    {
        int n = getNumberMoves();
        assert(n > 0);
        return getStackEntry(n - 1).m_suicide;
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
        Also calls clear().
        @param size The new board size (number of points per
        row / column) in the range from one to GoPoint.MAXSIZE
    */
    public void init(int size)
    {
        m_size = size;
        m_color = new GoColor[m_size][m_size];
        m_mark = new Marker(m_size);
        m_constants = BoardConstants.get(size);
        clear();
    }

    /** Check if a move would capture anything (including suicide).
        @param c The player color.
        @param p The point to check.
        @return true, if a move on the given point by the given player would
        capture any opponent stones, or be a suicide move.
    */
    public boolean isCaptureOrSuicide(GoColor c, GoPoint p)
    {
        if (getColor(p) != GoColor.EMPTY)
            return false;
        play(c, p);
        boolean result = (getKilled().size() > 0 || getSuicide().size() > 0);
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

    /** Check if any actions (moves or setup stones) were made on the
        board.
    */
    public boolean isModified()
    {
        return (m_stack.size() > 0
                || m_setup[GoColor.BLACK.toInteger()].size() > 0
                || m_setup[GoColor.WHITE.toInteger()].size() > 0
                || m_toMove != GoColor.BLACK);
    }

    public boolean isSetupHandicap()
    {
        return m_isSetupHandicap;
    }

    /** Check if a point would be a suicide move.
        @param c The player color to check.
        @param p The point to check.
        @return true, if a move at the given point by the given player
        would be a suicide move.
    */
    public boolean isSuicide(GoColor c, GoPoint p)
    {
        if (getColor(p) != GoColor.EMPTY)
            return false;
        play(c, p);
        boolean result = (getSuicide().size() > 0);
        undo();
        return result;
    }

    /** Clear board.
        Takes back the effects of any moves or setup stones on the board.
    */
    public void clear()
    {
        for (int i = 0; i < getNumberPoints(); ++i)
            setColor(getPoint(i), GoColor.EMPTY);
        m_stack.clear();        
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
        {
            int index = c.toInteger();
            m_setup[index].clear();
            m_captured[index] = 0;
        }
        m_toMove = GoColor.BLACK;
        m_koPoint = null;
        m_isSetupHandicap = false;
        m_setupPlayer = null;
    }

    /** Play a move.
        @param color The player who played the move.
        @param point The location of the move.
        @see #play(Move)
    */
    public void play(GoColor color, GoPoint point)
    {
        play(Move.get(color, point));
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
        StackEntry entry = new StackEntry(move);
        entry.execute(this);
        m_stack.add(entry);
    }

    /** Change the color to move.
        @param toMove The new color to move.
    */
    public void setToMove(GoColor toMove)
    {
        m_toMove = toMove;
    }

    /** Setup position.
        Clears the board and move history and sets up a position.
        @param black Black stones to add on the board.
        @param white White stones to add on the board.
        @param toMove Color to play
    */
    public void setup(ConstPointList black, ConstPointList white,
                      GoColor player)
    {
        clear();
        m_koPoint = null;
        m_setupPlayer = player;
        if (m_setupPlayer != null)
            m_toMove = player;
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
        {
            ConstPointList stones = (c == GoColor.BLACK ? black : white);
            int index = c.toInteger();
            if (stones != null)
            {
                for (int i = 0; i < stones.size(); ++i)
                    setColor(stones.get(i), c);
                m_setup[index] = new PointList(stones);
            }
            else
                m_setup[index] = new PointList();
        }
    }

    public void setupHandicap(ConstPointList points)
    {
        setup(points, null, GoColor.WHITE);
        m_isSetupHandicap = true;
    }

    /** Undo the last move.
        Restores any stones removed by the last move (captured or
        suicide) and the color who was to move before the move.
    */
    public void undo()
    {
        int index = getNumberMoves() - 1;
        assert(index >= 0);
        getStackEntry(index).undo(this);
        m_stack.remove(index);
    }

    /** Undo a number of moves.
        @param n Number of moves to undo. Must be between 0
        and getNumberMoves().
        @see #undo()
    */
    public void undo(int n)
    {
        assert(n >= 0);
        assert(n <= getNumberMoves());
        for (int i = 0; i < n; ++i)
            undo();
    }

    private static class StackEntry
    {
        public final Move m_move;

        public GoPoint m_oldKoPoint;

        public GoColor m_oldColor;

        public GoColor m_oldToMove;

        public PointList m_killed;

        public PointList m_suicide;

        public StackEntry(Move move)
        {
            m_move = move;
        }

        public void execute(Board board)
        {
            GoPoint p = m_move.getPoint();
            GoColor c = m_move.getColor();
            GoColor otherColor = c.otherColor();
            m_killed = new PointList();
            m_suicide = new PointList();
            m_oldKoPoint = board.m_koPoint;
            board.m_koPoint = null;
            if (p != null)
            {
                m_oldColor = board.getColor(p);
                board.setColor(p, c);
                assert(c != GoColor.EMPTY);
                ConstPointList adj = board.getAdjacentPoints(p);
                for (int i = 0; i < adj.size(); ++i)
                {
                    int killedSize = m_killed.size();
                    board.checkKill(adj.get(i), otherColor, m_killed);
                    if (m_killed.size() == killedSize + 1)
                        board.m_koPoint = m_killed.get(killedSize);
                }
                board.checkKill(p, c, m_suicide);
                if (board.m_koPoint != null
                    && ! board.isSingleStoneSingleLib(p, c))
                    board.m_koPoint = null;
                board.m_captured[c.toInteger()] += m_suicide.size();
                board.m_captured[otherColor.toInteger()] += m_killed.size();
            }
            m_oldToMove = board.m_toMove;
            board.m_toMove = otherColor;        
        }

        protected void undo(Board board)
        {
            GoPoint p = m_move.getPoint();
            if (p != null)
            {
                GoColor c = m_move.getColor();
                GoColor otherColor = c.otherColor();
                for (int i = 0; i < m_suicide.size(); ++i)
                {
                    GoPoint stone = m_suicide.get(i);
                    board.setColor(stone, c);
                }
                board.setColor(p, m_oldColor);
                for (int i = 0; i < m_killed.size(); ++i)
                {
                    GoPoint stone = m_killed.get(i);
                    board.setColor(stone, otherColor);
                }
                board.m_captured[c.toInteger()] -= m_suicide.size();
                board.m_captured[otherColor.toInteger()] -= m_killed.size();
            }
            board.m_toMove = m_oldToMove;
            board.m_koPoint = m_oldKoPoint;
        }
    }

    private Marker m_mark;

    private int m_size;

    private int[] m_captured = { 0, 0 };

    private final ArrayList m_stack = new ArrayList(361);

    private GoColor[][] m_color;

    private GoColor m_toMove;

    private GoColor m_setupPlayer;

    private BoardConstants m_constants;

    private GoPoint m_koPoint;

    private PointList[] m_setup = { new PointList(), new PointList() };

    private boolean m_isSetupHandicap;

    private StackEntry getStackEntry(int i)
    {
        return (StackEntry)m_stack.get(i);
    }

    private boolean isSingleStoneSingleLib(GoPoint point, GoColor color)
    {
        if (getColor(point) != color)
            return false;
        ConstPointList adj = getAdjacentPoints(point);
        int lib = 0;
        for (int i = 0; i < adj.size(); ++i)
        {
            GoColor adjColor = getColor(adj.get(i));
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

    private void checkKill(GoPoint p, GoColor color, PointList killed)
    {
        assert(m_mark.isCleared());
        PointList stones = new PointList();
        if (isDead(p, color, stones))
        {
            killed.addAll(stones);
            for (int i = 0; i < stones.size(); ++i)
                setColor(stones.get(i), GoColor.EMPTY);
        }
        m_mark.set(stones, false);
        assert(m_mark.isCleared());
    }

    private void findStones(GoPoint p, GoColor color, PointList stones)
    {
        GoColor c = getColor(p);
        if (c != color)
            return;
        if (m_mark.get(p))
            return;
        m_mark.set(p, true);
        stones.add(p);
        ConstPointList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            findStones(adj.get(i), color, stones);
    }

    private boolean isDead(GoPoint p, GoColor color, PointList stones)
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
        ConstPointList adj = getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isDead(adj.get(i), color, stones))
                return false;
        return true;
    }

    private void setColor(GoPoint p, GoColor c)
    {
        assert(p != null);
        m_color[p.getX()][p.getY()] = c;
    }
}
