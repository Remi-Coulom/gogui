//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;
import java.util.Random;
import net.sf.gogui.util.ObjectUtil;

/** Go board. */
public final class Board
    implements ConstBoard
{
    public interface Action
    {
        UndoableAction createUndoableAction();
    }

    public static class Play
        implements Action
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

        public UndoableAction createUndoableAction()
        {
            return new UndoablePlay(this);
        }

        private final Move m_move;
    }

    public static class Setup
        implements Action
    {
        public Setup(ConstPointList black, ConstPointList white)
        {
            this(black, white, null);
        }

        public Setup(ConstPointList black, ConstPointList white,
                     ConstPointList empty)
        {
            this(black, white, empty, null);
        }

        public Setup(ConstPointList black, ConstPointList white,
                     ConstPointList empty, GoColor toMove)
        {
            if (black != null && black.size() > 0)
                m_stones[GoColor.BLACK.toInteger()] = new PointList(black);
            if (white != null && white.size() > 0)
                m_stones[GoColor.WHITE.toInteger()] = new PointList(white);
            if (empty != null && empty.size() > 0)
                m_stones[GoColor.EMPTY.toInteger()] = new PointList(empty);
            m_toMove = toMove;
        }

        public boolean equals(Object object)
        {
            if (object == null || object.getClass() != getClass())
                return false;        
            Setup setup = (Setup)object;
            for (GoColor c = GoColor.BLACK; c != null;
                 c = c.getNextBlackWhiteEmpty())
            {
                int index = c.toInteger();
                if (! ObjectUtil.equals(setup.m_stones[index],
                                        m_stones[index]))
                    return false;
            }
            return true;
        }

        /** Get location of added or removed stones.
            @param c The color of the added stones or GoColor.EMPTY for
            removed stones.
        */
        public ConstPointList getStones(GoColor c)
        {
            ConstPointList stones = m_stones[c.toInteger()];
            if (stones == null)
                return PointList.getEmptyList();
            return stones;
        }

        public GoColor getToMove()
        {
            return m_toMove;
        }

        /** Check if setup contains any added or removed stones. */
        public boolean hasStones()
        {
            for (GoColor c = GoColor.BLACK; c != null;
                 c = c.getNextBlackWhiteEmpty())
            {
                int index = c.toInteger();
                if (m_stones[index] != null && m_stones[index].size() > 0)
                    return true;
            }
            return false;
        }

        public UndoableAction createUndoableAction()
        {
            return new UndoableSetup(this);
        }

        private PointList[] m_stones = new PointList[3];

        private GoColor m_toMove;
    }

    /** Handicap stones placement action.
        This action is only allowed as the first action on the board.
    */
    public static class SetupHandicap
        extends Setup
    {
        public SetupHandicap(ConstPointList points)
        {
            super(points, null, null, GoColor.WHITE);
        }

        public ConstPointList getHandicapStones()
        {
            return getStones(GoColor.BLACK);
        }
    }

    public static abstract class UndoableAction
    {
        protected abstract void execute(Board board);

        protected abstract void undo(Board board);
    }

    public static class UndoablePlay
        extends UndoableAction
    {
        public UndoablePlay(Play play)
        {
            m_play = play;
        }

        protected void execute(Board board)
        {
            GoPoint p = m_play.getMove().getPoint();
            GoColor c = m_play.getMove().getColor();
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
            GoPoint p = m_play.getMove().getPoint();
            if (p != null)
            {
                GoColor c = m_play.getMove().getColor();
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

        private final Play m_play;

        private GoPoint m_oldKoPoint;

        private GoColor m_oldColor;

        private GoColor m_oldToMove;

        private PointList m_killed;

        private PointList m_suicide;
    }

    public static class UndoableSetup
        extends UndoableAction
    {
        public UndoableSetup(Setup setup)
        {
            m_setup = setup;
        }

        protected void execute(Board board)
        {
            m_oldKoPoint = board.m_koPoint;
            board.m_koPoint = null;
            m_oldToMove = board.m_toMove;
            if (m_setup.getToMove() != null)
                board.m_toMove = m_setup.getToMove();
            m_oldColor = new ArrayList();
            for (GoColor c = GoColor.BLACK; c != null;
                 c = c.getNextBlackWhiteEmpty())
                setup(board, c, m_setup.getStones(c));
        }

        protected void undo(Board board)
        {
            for (GoColor c = GoColor.EMPTY; c != null;
                 c = c.getPreviousBlackWhiteEmpty())
                undoSetup(board, c, m_setup.getStones(c));
            board.m_koPoint = m_oldKoPoint;
            board.m_toMove = m_oldToMove;
        }

        private Setup m_setup;

        private ArrayList m_oldColor;

        private GoPoint m_oldKoPoint;

        private GoColor  m_oldToMove;

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
        int n = getNumberActions();
        return (n >= 2
                && getAction(n - 1) instanceof Play
                && ((Play)getAction(n - 1)).getMove().getPoint() == null
                && getAction(n - 2) instanceof Play
                && ((Play)getAction(n - 2)).getMove().getPoint() == null);
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
        @param action The action to play.
    */
    public void doAction(Action action)
    {
        assert(! (action instanceof SetupHandicap)
               || getNumberActions() == 0);
        UndoableAction undoableAction = action.createUndoableAction();
        undoableAction.execute(this);
        m_actions.add(action);
        m_undoableActions.add(undoableAction);
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
        int numberActions = getNumberActions();
        assert(numberActions > 0);
        int index = numberActions - 1;
        Action action = (Action)m_actions.get(index);
        if (action instanceof Play)
            return ((UndoablePlay)m_undoableActions.get(index)).m_killed;
        return PointList.getEmptyList();
    }

    /** Return last move.
        @return Last move or null if no action was done yet or last
        action was not a move.
    */
    public Move getLastMove()
    {
        int n = getNumberActions();
        if (n > 0 && getAction(n - 1) instanceof Play)
            return ((Play)getAction(n - 1)).getMove();
        else
            return null;
    }

    /** Get the number of actions (moves or setup stones) played so far.
        @return The number of actions.
        @see #getAction
    */
    public int getNumberActions()
    {
        return m_actions.size();
    }

    /** Get the number of points on the board.
        @return The number of points on the board (size * size).
        @see #getPoint
    */
    public int getNumberPoints()
    {
        return m_constants.getNumberPoints();
    }

    /** Get a hash code for the current position.
        Does not take into account who is to play.
    */
    public long getPositionHashCode()
    {
        return m_positionHashCode;
    }

    /** Get a action (move or setup stone) from the sequence of actions
        played so far.
        @param i The number of the action (starting with zero).
        @return The action with the given number.
        @see #getNumberActions()
    */
    public Action getAction(int i)
    {
        return (Action)m_actions.get(i);
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
        or if last action was a setup stone..
        @see #getKilled()
    */
    public ConstPointList getSuicide()
    {
        int numberActions = getNumberActions();
        assert(numberActions > 0);
        int index = numberActions - 1;
        Action action = (Action)m_actions.get(index);
        if (action instanceof Play)
            return ((UndoablePlay)m_undoableActions.get(index)).m_suicide;
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
        return (m_actions.size() > 0);
    }

    /** Check if a point would be a suicide move.
        @param point The point to check.
        @param c The player color to check.
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

    /** Start a new game.
        Takes back the effects of any actions (moves or setup stones)
        on the board.
    */
    public void newGame()
    {
        m_positionHashCode = m_randomNumbersBoardSize[m_size];
        for (int i = 0; i < getNumberPoints(); ++i)
            setColor(getPoint(i), GoColor.EMPTY);
        m_actions.clear();        
        m_undoableActions.clear();        
        for (GoColor c = GoColor.BLACK; c != null; c = c.getNextBlackWhite())
            m_captured[c.toInteger()] = 0;
        m_toMove = GoColor.BLACK;
        m_koPoint = null;
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
        doAction(new Play(move));
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

    public void setup(ConstPointList black, ConstPointList white,
                      ConstPointList empty)
    {
        setup(black, white, empty, null);
    }

    /** Add setup stones.
        @param black Black stones to add on the board.
        @param white White stones to add on the board.
        @param empty Stones to remove from the board.
        @param toMove New color to play (if null, color to play will not
        change)
    */
    public void setup(ConstPointList black, ConstPointList white,
                      ConstPointList empty, GoColor toMove)
    {
        doAction(new Setup(black, white, empty, toMove));
    }

    public void setupHandicap(ConstPointList points)
    {
        doAction(new SetupHandicap(points));
    }

    /** Undo the last action (move or setup stone).
        Restores any stones removed by the last action (captured or
        suicide) if it was a move and restore the color who was to move before
        the action.
    */
    public void undo()
    {
        int index = getNumberActions() - 1;
        assert(index >= 0);
        ((UndoableAction)m_undoableActions.get(index)).undo(this);
        m_actions.remove(index);
        m_undoableActions.remove(index);
    }

    /** Undo a number of moves or setup stones.
        @param n Number of moves to undo. Must be between 0
        and getNumberActions().
        @see #undo()
    */
    public void undo(int n)
    {
        assert(n >= 0);
        assert(n <= getNumberActions());
        for (int i = 0; i < n; ++i)
            undo();
    }

    private Marker m_mark;

    private int m_size;

    private int[] m_captured = { 0, 0 };

    private long m_positionHashCode;

    private final ArrayList m_actions = new ArrayList(361);

    private final ArrayList m_undoableActions = new ArrayList(361);

    private GoColor[][] m_color;

    private GoColor m_toMove;

    private BoardConstants m_constants;

    private GoPoint m_koPoint;

    /** Black stone random numbers for computing the position hash code. */
    private static long[][] m_randomNumbersBlack;

    /** White stone random numbers for computing the position hash code. */
    private static long[][] m_randomNumbersWhite;

    /** Board size random numbers for computing the position hash code. */
    private static long[] m_randomNumbersBoardSize;

    {
        Random random = new Random(1);
        m_randomNumbersBoardSize = new long[GoPoint.MAXSIZE];
        for (int i = 0; i < GoPoint.MAXSIZE; ++i)
            m_randomNumbersBoardSize[i] = random.nextLong();
        m_randomNumbersBlack = new long[GoPoint.MAXSIZE][GoPoint.MAXSIZE];
        m_randomNumbersWhite = new long[GoPoint.MAXSIZE][GoPoint.MAXSIZE];
        for (int x = 0; x < GoPoint.MAXSIZE; ++x)
            for (int y = 0; y < GoPoint.MAXSIZE; ++y)
            {
                m_randomNumbersBlack[x][y] = random.nextLong();
                m_randomNumbersWhite[x][y] = random.nextLong();
            }
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

    private void setColor(GoPoint point, GoColor color)
    {
        assert(point != null);
        int x = point.getX();
        int y = point.getY();
        GoColor oldColor = m_color[x][y]; 
        if (oldColor == GoColor.BLACK)
            m_positionHashCode ^= m_randomNumbersBlack[x][y];
        else
            m_positionHashCode ^= m_randomNumbersWhite[x][y];
        m_color[x][y] = color;
        if (color == GoColor.BLACK)
            m_positionHashCode ^= m_randomNumbersBlack[x][y];
        else
            m_positionHashCode ^= m_randomNumbersWhite[x][y];
    }
}
