//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;
import net.sf.gogui.util.ObjectUtil;

/** Go board. */
public final class Board
    implements ConstBoard
{
    public static abstract class Placement
    {
        protected abstract void execute(Board board);

        protected abstract void undo(Board board);
    }

    public static class Play
        extends Placement
    {
        public Play(Move move)
        {
            m_move = move;
        }

        public boolean equals(Object object)
        {
            if (object == null || object.getClass() != getClass())
                return false;        
            Play play = (Play)object;
            return (play.m_move == m_move);
        }

        public Move getMove()
        {
            return m_move;
        }

        protected void execute(Board board)
        {
            GoPoint p = m_move.getPoint();
            GoColor c = m_move.getColor();
            GoColor otherColor = c.otherColor();
            m_killed = new PointList();
            m_suicide = new PointList();
            GoPoint m_oldKoPoint = board.m_koPoint;
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
                if (c == GoColor.BLACK)
                {
                    board.m_capturedBlack += m_suicide.size();
                    board.m_capturedWhite += m_killed.size();
                }
                else
                {
                    board.m_capturedWhite += m_suicide.size();
                    board.m_capturedBlack += m_killed.size();
                }
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
                if (c == GoColor.BLACK)
                {
                    board.m_capturedBlack -= m_suicide.size();
                    board.m_capturedWhite -= m_killed.size();
                }
                else
                {
                    board.m_capturedWhite -= m_suicide.size();
                    board.m_capturedBlack -= m_killed.size();
                }
            }
            board.m_toMove = m_oldToMove;
            board.m_koPoint = m_oldKoPoint;
        }

        private final Move m_move;

        private GoPoint m_oldKoPoint;

        private GoColor m_oldColor;

        private GoColor m_oldToMove;

        private PointList m_killed;

        private PointList m_suicide;
    }

    public static class Setup
        extends Placement
    {
        public Setup(ConstPointList black, ConstPointList white)
        {
            this(black, white, null);
        }

        public Setup(ConstPointList black, ConstPointList white,
                     ConstPointList empty)
        {
            if (black != null && black.size() > 0)
                m_black = new PointList(black);
            if (white != null && white.size() > 0)
                m_white = new PointList(white);
            if (empty != null && empty.size() > 0)
                m_empty = new PointList(empty);
        }

        public boolean equals(Object object)
        {
            if (object == null || object.getClass() != getClass())
                return false;        
            Setup setup = (Setup)object;
            return (ObjectUtil.equals(setup.m_black, m_black)
                    && ObjectUtil.equals(setup.m_white, m_white)
                    && ObjectUtil.equals(setup.m_empty, m_empty));
        }

        public ConstPointList getEmpty()
        {
            if (m_empty == null)
                return PointList.getEmptyList();
            return m_empty;
        }

        public ConstPointList getWhite()
        {
            if (m_white == null)
                return PointList.getEmptyList();
            return m_white;
        }

        public ConstPointList getBlack()
        {
            if (m_black == null)
                return PointList.getEmptyList();
            return m_black;
        }

        protected void execute(Board board)
        {
            m_oldKoPoint = board.m_koPoint;
            board.m_koPoint = null;
            m_oldColor = new ArrayList();
            setup(board, GoColor.BLACK, m_black);
            setup(board, GoColor.WHITE, m_white);
            setup(board, GoColor.EMPTY, m_empty);
        }

        protected void undo(Board board)
        {
            undoSetup(board, GoColor.EMPTY, m_empty);
            undoSetup(board, GoColor.WHITE, m_white);
            undoSetup(board, GoColor.BLACK, m_black);
            board.m_koPoint = m_oldKoPoint;
        }

        private PointList m_black;

        private PointList m_white;

        private PointList m_empty;

        private ArrayList m_oldColor;

        private GoPoint m_oldKoPoint;

        private void setup(Board board, GoColor c, ConstPointList points)
        {
            if (points == null)
                return;
            for (int i = 0; i < points.size(); ++i)
            {
                GoPoint p = points.get(i);
                m_oldColor.add(board.getColor(p));
                board.setColor(p, c);
            }
        }

        private void undoSetup(Board board, GoColor c, ConstPointList points)
        {
            if (points == null)
                return;
            for (int i = points.size() - 1; i >= 0; --i)
            {
                GoPoint p = points.get(i);
                int index = m_oldColor.size() - 1;
                board.setColor(p, (GoColor)m_oldColor.get(index));
                m_oldColor.remove(index);
            }
        }
    }

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
        int n = getNumberPlacements();
        return (n >= 2
                && getPlacement(n - 1) instanceof Play
                && ((Play)getPlacement(n - 1)).getMove().getPoint() == null
                && getPlacement(n - 2) instanceof Play
                && ((Play)getPlacement(n - 2)).getMove().getPoint() == null);
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
        placement.execute(this);
        m_stack.add(placement);
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

    /** Get number of black captured stones.
        @return The total number of black stones captured by all moves played.
    */
    public int getCapturedBlack()
    {
        return m_capturedBlack;
    }

    /** Get number of white captured stones.
        @return The total number of white stones captured by all moves played.
    */
    public int getCapturedWhite()
    {
        return m_capturedWhite;
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
    public ConstPointList getHandicapStones(int n)
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
    public static ConstPointList getHandicapStones(int size, int n)
    {
        return BoardConstants.get(size).getHandicapStones(n);
    }

    /** Opponent stones captured in last move.
        Does not include player stones killed by suicide.
        Requires that there is a last move (or setup stone).
        @return List of opponent stones (go.Point) captured in last move;
        empty if none were killed or if last placement was a setup stone.
        @see #getSuicide()
    */
    public ConstPointList getKilled()
    {
        int numberPlacements = getNumberPlacements();
        assert(numberPlacements > 0);
        Placement placement = (Placement)m_stack.get(numberPlacements - 1);
        if (placement instanceof Play)
            return ((Play)placement).m_killed;
        return PointList.getEmptyList();
    }

    /** Return last move.
        @return Last move or null if no placement was done yet or last
        placement was not a move.
    */
    public Move getLastMove()
    {
        int n = getNumberPlacements();
        if (n > 0 && getPlacement(n - 1) instanceof Play)
            return ((Play)getPlacement(n - 1)).getMove();
        else
            return null;
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

    /** Get a placement (move or setup stone) from the sequence of placements
        played so far.
        @param i The number of the placement (starting with zero).
        @return The placement with the given number.
        @see #getNumberPlacements()
    */
    public Placement getPlacement(int i)
    {
        return (Placement)m_stack.get(i);
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

    /** Get board size.
        @return The board size.
    */
    public int getSize()
    {
        return m_size;
    }

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
        or if last placement was a setup stone..
        @see #getKilled()
    */
    public ConstPointList getSuicide()
    {
        int numberPlacements = getNumberPlacements();
        assert(numberPlacements > 0);
        Placement placement = (Placement)m_stack.get(numberPlacements - 1);
        if (placement instanceof Play)
            return ((Play)placement).m_suicide;
        return PointList.getEmptyList();
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
        @param size The new board size (number of points per
        row / column) in the range from one to GoPoint.MAXSIZE
    */
    public void init(int size)
    {
        m_size = size;
        m_color = new GoColor[m_size][m_size];
        m_mark = new Marker(m_size);
        m_constants = BoardConstants.get(size);
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
        boolean result = (getSuicide().size() > 0);
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
        m_capturedBlack = 0;
        m_capturedWhite = 0;
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
        doPlacement(new Play(move));
    }

    /** Change the color to move.
        @param toMove The new color to move.
    */
    public void setToMove(GoColor toMove)
    {
        m_toMove = toMove;
    }

    public void setup(ConstPointList black, ConstPointList white)
    {
        setup(black, white, null);
    }

    /** Add setup stones. */
    public void setup(ConstPointList black, ConstPointList white,
                      ConstPointList empty)
    {
        doPlacement(new Setup(black, white, empty));
    }

    /** Undo the last placement (move or setup stone).
        Restores any stones removed by the last placement (captured or
        suicide) if it was a move and restore the color who was to move before
        the placement.
    */
    public void undo()
    {
        int index = getNumberPlacements() - 1;
        assert(index >= 0);
        Placement placement = (Placement)m_stack.get(index);
        placement.undo(this);
        m_stack.remove(index);
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

    private Marker m_mark;

    private int m_size;

    private int m_capturedBlack;

    private int m_capturedWhite;

    private final ArrayList m_stack = new ArrayList(361);

    private GoColor m_color[][];

    private GoColor m_toMove;

    private BoardConstants m_constants;

    private GoPoint m_koPoint;

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

    private void setColor(GoPoint point, GoColor color)
    {
        assert(point != null);
        m_color[point.getX()][point.getY()] = color;
    }
}

