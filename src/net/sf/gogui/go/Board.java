// Board.java

package net.sf.gogui.go;

import java.util.ArrayList;
import java.util.Iterator;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import static net.sf.gogui.go.GoColor.BLACK_WHITE;

/** Go board. */
public final class Board
    implements ConstBoard
{
    public class BoardIterator
        implements Iterator<GoPoint>
    {
        public boolean hasNext()
        {
            return m_iterator.hasNext();
        }

        public GoPoint next()
        {
            return m_iterator.next();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private final Iterator<GoPoint> m_iterator =
            m_constants.getPoints().iterator();
    }

    /** Constructor.
        @param boardSize The board size (number of points per row / column)
        in the range from one to GoPoint.MAX_SIZE */
    public Board(int boardSize)
    {
        init(boardSize);
    }

    /** Check for two consecutive passes.
        @return true, if the last two moves were pass moves */
    public boolean bothPassed()
    {
        int n = getNumberMoves();
        return (n >= 2
                && m_stack.get(n - 1).m_move.getPoint() == null
                && m_stack.get(n - 2).m_move.getPoint() == null);
    }

    /** Check if board contains a point.
        @param point The point to check
        @return true, if the point is on the board */
    public boolean contains(GoPoint point)
    {
        return point.isOnBoard(getSize());
    }

    /** Get points adjacent to a point.
        @param point The point.
        @return List of points adjacent. */
    public ConstPointList getAdjacent(GoPoint point)
    {
        return m_constants.getAdjacent(point);
    }

    /** Get number of captured stones.
        @return The total number of stones of the given color captured by
        opponent moves or by suicide. */
    public int getCaptured(GoColor c)
    {
        return m_captured.get(c);
    }

    /** Get state of a point on the board.
        @return BLACK, WHITE or EMPTY */
    public GoColor getColor(GoPoint p)
    {
        return m_color[p.getIndex()];
    }

    /** Get location of handicap stones for a given board size.
        @param n The number of handicap stones.
        @param size The board size.
        @return List of points (go.Point) corresponding to the handicap
        stone locations.
        @see BoardConstants#getHandicapStones */
    public static ConstPointList getHandicapStones(int size, int n)
    {
        return BoardConstants.get(size).getHandicapStones(n);
    }

    /** Opponent stones captured in last move.
        Does not include player stones killed by suicide.
        Requires that there is a last move (or setup stone).
        @return List of opponent stones (go.Point) captured in last move;
        empty if none were killed or there is no last move.
        @see #getSuicide() */
    public ConstPointList getKilled()
    {
        int n = getNumberMoves();
        assert n > 0;
        return m_stack.get(n - 1).m_killed;
    }

    /** Return last move.
        @return Last move or null if there is no last move. */
    public Move getLastMove()
    {
        int n = getNumberMoves();
        if (n == 0)
            return null;
        return m_stack.get(n - 1).m_move;
    }

    /** Get the number of moves played so far.
        @return The number of moves.
        @see #getMove */
    public int getNumberMoves()
    {
        return m_stack.size();
    }

    /** Get a move from the sequence of moves played so far.
        @param i The number of the move (starting with zero).
        @return The move with the given number.
        @see #getNumberMoves() */
    public Move getMove(int i)
    {
        return m_stack.get(i).m_move;
    }

    /** Get initial setup stones of a color.
        @param c Black or White.
        @return Initial stones of this color placed on the board by calling
        <code>setup</code>.
        @see #setup */
    public ConstPointList getSetup(GoColor c)
    {
        return m_setup.get(c);
    }

    /** Get player of initial setup position.
        @return Player of initial setup position as used in the call to
        <code>setup</code>; <code>null</code> means unknown player color.
        @see #setup */
    public GoColor getSetupPlayer()
    {
        return m_setupPlayer;
    }

    /** Get board size.
        @return The board size. */
    public int getSize()
    {
        return m_size;
    }

    /** Get stones of a block. */
    public void getStones(GoPoint p, GoColor color, PointList stones)
    {
        assert m_mark.isCleared();
        findStones(p, color, stones);
        m_mark.clear(stones);
        //assert m_mark.isCleared();
    }

    /** Player stones killed by suicide in last move.
        Requires that there is a last move (or setup stone).
        @return List of stones (go.Point) killed by suicide in last move,
        including the stone played; empty if no stones were killed by suicide
        or if there is no last move.
        @see #getKilled() */
    public ConstPointList getSuicide()
    {
        int n = getNumberMoves();
        assert n > 0;
        return m_stack.get(n - 1).m_suicide;
    }

    /** Get color to move.
        @return The color to move. */
    public GoColor getToMove()
    {
        return m_toMove;
    }

    /** Initialize the board for a given board size.
        For changing the board size.
        Also calls clear().
        @param size The new board size (number of points per
        row / column) in the range from one to GoPoint.MAX_SIZE */
    public void init(int size)
    {
        m_size = size;
        m_mark = new Marker(m_size);
        m_constants = BoardConstants.get(size);
        clear();
    }

    /** Check if a move would capture anything (including suicide).
        @param c The player color.
        @param p The point to check.
        @return true, if a move on the given point by the given player would
        capture any opponent stones, or be a suicide move. */
    public boolean isCaptureOrSuicide(GoColor c, GoPoint p)
    {
        if (getColor(p) != EMPTY)
            return false;
        play(c, p);
        boolean result = (getKilled().size() > 0 || getSuicide().size() > 0);
        undo();
        return result;
    }

    /** Check if a point is a handicap point.
        @param point The point to check.
        @return true, if the given point is a handicap point.
        @see BoardConstants#isHandicap */
    public boolean isHandicap(GoPoint point)
    {
        return m_constants.isHandicap(point);
    }

    /** Check if move would violate the simple Ko rule.
        Assumes other color to move than the color of the last move.
        @param point The point to check
        @return true, if a move at this point would violate the simple ko
        rule */
    public boolean isKo(GoPoint point)
    {
        return point == m_koPoint;
    }

    /** Check if any moves were played or setup stones placed on the board. */
    public boolean isModified()
    {
        return (! m_stack.isEmpty()
                || m_setup.get(BLACK).size() > 0
                || m_setup.get(WHITE).size() > 0
                || m_toMove != BLACK);
    }

    /** Check if the initial setup position was a handicap.
        @return <code>true</code>, if the initial position was setup by
        calling setupHandicap, <code>false</code> otherwise.
        @see #setupHandicap */
    public boolean isSetupHandicap()
    {
        return m_isSetupHandicap;
    }

    /** Check if a point would be a suicide move.
        @param c The player color to check.
        @param p The point to check.
        @return true, if a move at the given point by the given player
        would be a suicide move. */
    public boolean isSuicide(GoColor c, GoPoint p)
    {
        if (getColor(p) != EMPTY)
            return false;
        play(c, p);
        boolean result = (getSuicide().size() > 0);
        undo();
        return result;
    }

    public Iterator<GoPoint> iterator()
    {
        return new BoardIterator();
    }

    /** Clear board.
        Takes back the effects of any moves or setup stones on the board. */
    public void clear()
    {
        for (GoPoint p : this)
            setColor(p, EMPTY);
        m_stack.clear();
        for (GoColor c : BLACK_WHITE)
        {
            m_setup.get(c).clear();
            m_captured.set(c, 0);
        }
        m_toMove = BLACK;
        m_koPoint = null;
        m_isSetupHandicap = false;
        m_setupPlayer = null;
    }

    /** Play a move.
        @param color The player who played the move.
        @param point The location of the move.
        @see #play(Move) */
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
        @param move The move (location and player) */
    public void play(Move move)
    {
        StackEntry entry = new StackEntry(move);
        entry.execute(this);
        m_stack.add(entry);
    }

    /** Change the color to move.
        @param toMove The new color to move. */
    public void setToMove(GoColor toMove)
    {
        m_toMove = toMove;
    }

    /** Setup position.
        Clears the board and move history and sets up a position.
        @param black Black stones to add on the board.
        @param white White stones to add on the board.
        @param player Color to play */
    public void setup(ConstPointList black, ConstPointList white,
                      GoColor player)
    {
        clear();
        m_koPoint = null;
        m_setupPlayer = player;
        if (m_setupPlayer != null)
            m_toMove = player;
        for (GoColor c : BLACK_WHITE)
        {
            ConstPointList stones = (c == BLACK ? black : white);
            if (stones == null)
                m_setup.set(c, new PointList());
            else
            {
                for (GoPoint p : stones)
                    setColor(p, c);
                m_setup.set(c, new PointList(stones));
            }
        }
    }

    /** Setup initial handicap stones.
        This function is similar to an initial setup with only black stones,
        but it is remembered that the setup was a handicap and it can later
        be checked with <code>isSetupHandicap</code>.
        @see #isSetupHandicap */
    public void setupHandicap(ConstPointList points)
    {
        setup(points, null, WHITE);
        m_isSetupHandicap = true;
    }

    /** Undo the last move.
        Restores any stones removed by the last move (captured or
        suicide) and the color who was to move before the move. */
    public void undo()
    {
        int index = getNumberMoves() - 1;
        assert index >= 0;
        m_stack.get(index).undo(this);
        m_stack.remove(index);
    }

    /** Undo a number of moves.
        @param n Number of moves to undo. Must be between 0
        and getNumberMoves().
        @see #undo() */
    public void undo(int n)
    {
        assert n >= 0;
        assert n <= getNumberMoves();
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
                assert c != EMPTY;
                for (GoPoint adj : board.getAdjacent(p))
                {
                    int killedSize = m_killed.size();
                    if (board.getColor(adj) == otherColor)
                        board.checkKill(adj, m_killed);
                    if (m_killed.size() == killedSize + 1)
                        board.m_koPoint = m_killed.get(killedSize);
                }
                board.checkKill(p, m_suicide);
                if (board.m_koPoint != null
                    && ! board.isSingleStoneSingleLib(p, c))
                    board.m_koPoint = null;
                board.m_captured.set(c,
                                     board.m_captured.get(c)
                                     + m_suicide.size());
                board.m_captured.set(otherColor,
                                     board.m_captured.get(otherColor)
                                     + m_killed.size());
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
                for (GoPoint stone : m_suicide)
                    board.setColor(stone, c);
                board.setColor(p, m_oldColor);
                for (GoPoint stone : m_killed)
                    board.setColor(stone, otherColor);
                board.m_captured.set(c,
                                     board.m_captured.get(c)
                                     - m_suicide.size());
                board.m_captured.set(otherColor,
                                     board.m_captured.get(otherColor)
                                     - m_killed.size());
            }
            board.m_toMove = m_oldToMove;
            board.m_koPoint = m_oldKoPoint;
        }
    }

    private Marker m_mark;

    private int m_size;

    private final BlackWhiteSet<Integer> m_captured
        = new BlackWhiteSet<Integer>(0, 0);

    private final ArrayList<StackEntry> m_stack
        = new ArrayList<StackEntry>(361);

    /** Temporary variable reused for efficiency. */
    private final PointList m_checkKillStones = new PointList();

    /** Temporary variable reused for efficiency. */
    private final PointList m_checkKillStack = new PointList();

    private GoColor[] m_color = new GoColor[GoPoint.NUMBER_INDEXES];

    private GoColor m_toMove;

    private GoColor m_setupPlayer;

    private BoardConstants m_constants;

    private GoPoint m_koPoint;

    private final BlackWhiteSet<PointList> m_setup
        = new BlackWhiteSet<PointList>(new PointList(), new PointList());

    private boolean m_isSetupHandicap;

    private boolean isSingleStoneSingleLib(GoPoint point, GoColor color)
    {
        if (getColor(point) != color)
            return false;
        int lib = 0;
        for (GoPoint adj : getAdjacent(point))
        {
            GoColor adjColor = getColor(adj);
            if (adjColor == EMPTY)
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

    private void checkKill(GoPoint point, PointList killed)
    {
        assert m_mark.isCleared();
        GoColor color = getColor(point);
        assert color != EMPTY;
        m_checkKillStack.clear();
        m_checkKillStack.add(point);
        m_mark.set(point);
        m_checkKillStones.clear();
        boolean isDead = true;
        // Recursion is unrolled using a stack for efficiency
        while (isDead && ! m_checkKillStack.isEmpty())
        {
            GoPoint p = m_checkKillStack.pop();
            assert getColor(p) == color;
            m_checkKillStones.add(p);
            ConstPointList adjacent = getAdjacent(p);
            int nuAdjacent = adjacent.size();
            // Don't use an iterator for efficiency
            for (int i = 0; i < nuAdjacent; ++i)
            {
                GoPoint adj = adjacent.get(i);
                GoColor c = getColor(adj);
                if (c == EMPTY)
                {
                    isDead = false;
                    break;
                }
                if (m_mark.get(adj) || ! c.equals(color))
                    continue;
                m_checkKillStack.add(adj);
                m_mark.set(adj);
            }
        }
        if (isDead)
        {
            killed.addAll(m_checkKillStones);
            int nuKillStones = m_checkKillStones.size();
            // Don't use an iterator for efficiency
            for (int i = 0; i < nuKillStones; ++i)
                setColor(m_checkKillStones.get(i), EMPTY);
        }
        m_mark.clear(m_checkKillStack);
        m_mark.clear(m_checkKillStones);
        //assert m_mark.isCleared();
    }

    private void findStones(GoPoint p, GoColor color, PointList stones)
    {
        if (getColor(p) != color)
            return;
        if (m_mark.get(p))
            return;
        m_mark.set(p);
        stones.add(p);
        for (GoPoint adj : getAdjacent(p))
            findStones(adj, color, stones);
    }

    private void setColor(GoPoint p, GoColor c)
    {
        assert p != null;
        m_color[p.getIndex()] = c;
    }
}
