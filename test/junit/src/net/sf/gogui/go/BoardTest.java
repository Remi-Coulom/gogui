// BoardTest.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;

public final class BoardTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(BoardTest.class);
    }

    public void testBothPassed()
    {
        Board board = new Board(19);
        assertFalse(board.bothPassed());
        board.play(BLACK, GoPoint.get(0, 0));
        assertFalse(board.bothPassed());
        board.play(WHITE, null);
        assertFalse(board.bothPassed());
        board.play(BLACK, null);
        assertTrue(board.bothPassed());
        board.play(WHITE, null);
        assertTrue(board.bothPassed());
    }

    public void testCapture()
    {
        Board board = new Board(19);
        board.play(BLACK, GoPoint.get(0, 0));
        board.play(BLACK, GoPoint.get(1, 0));
        board.play(WHITE, GoPoint.get(0, 1));
        board.play(WHITE, GoPoint.get(1, 1));
        board.play(WHITE, GoPoint.get(2, 0));
        assertEquals(EMPTY, board.getColor(GoPoint.get(0, 0)));
        assertEquals(EMPTY, board.getColor(GoPoint.get(1, 0)));
        assertEquals(WHITE, board.getColor(GoPoint.get(0, 1)));
        assertEquals(WHITE, board.getColor(GoPoint.get(1, 1)));
        assertEquals(WHITE, board.getColor(GoPoint.get(2, 0)));
        assertEquals(2, board.getCaptured(BLACK));
        assertEquals(0, board.getCaptured(WHITE));
    }

    public void testContains()
    {
        Board board = new Board(19);
        assertTrue(board.contains(GoPoint.get(0, 0)));
        assertTrue(board.contains(GoPoint.get(0, 18)));
        assertTrue(board.contains(GoPoint.get(18, 0)));
        assertTrue(board.contains(GoPoint.get(18, 18)));
        assertFalse(board.contains(GoPoint.get(0, 19)));
        assertFalse(board.contains(GoPoint.get(19, 0)));
        assertFalse(board.contains(GoPoint.get(19, 19)));
        assertFalse(board.contains(GoPoint.get(20, 20)));
    }

    /** Test Board.getKilled(). */
    public void testGetKilled()
    {
        Board board = new Board(19);
        // 4 . . . . .
        // 3 . . O . .
        // 2 O O @ O .
        // 1 @ @ . . .
        //   A B C D E
        PointList black = new PointList();
        PointList white = new PointList();
        black.add(GoPoint.get(0, 0));
        black.add(GoPoint.get(1, 0));
        black.add(GoPoint.get(2, 1));
        white.add(GoPoint.get(0, 1));
        white.add(GoPoint.get(1, 1));
        white.add(GoPoint.get(2, 2));
        white.add(GoPoint.get(3, 1));
        board.setup(black, white, BLACK);
        board.play(WHITE, GoPoint.get(2, 0));
        ConstPointList killed = board.getKilled();
        assertEquals(3, killed.size());
        assertTrue(killed.contains(GoPoint.get(0, 0)));
        assertTrue(killed.contains(GoPoint.get(1, 0)));
        assertTrue(killed.contains(GoPoint.get(2, 1)));
        board.undo();
        board.play(WHITE, GoPoint.get(3, 0));
        assertTrue(board.getKilled().isEmpty());
    }

    /** Test Board.getSuicide(). */
    public void testGetSuicide()
    {
        Board board = new Board(19);
        // 4 . . . .
        // 3 O . . .
        // 2 @ O . .
        // 1 . @ O .
        //   A B C D
        PointList black = new PointList();
        PointList white = new PointList();
        black.add(GoPoint.get(0, 1));
        black.add(GoPoint.get(1, 0));
        white.add(GoPoint.get(0, 2));
        white.add(GoPoint.get(1, 1));
        white.add(GoPoint.get(2, 0));
        board.setup(black, white, BLACK);
        board.play(BLACK, GoPoint.get(0, 0));
        ConstPointList suicide = board.getSuicide();
        assertEquals(3, suicide.size());
        assertTrue(suicide.contains(GoPoint.get(0, 0)));
        assertTrue(suicide.contains(GoPoint.get(0, 1)));
        assertTrue(suicide.contains(GoPoint.get(1, 0)));
    }

    /** Test Board.isKo(). */
    public void testIsKo()
    {
        Board board = new Board(19);
        // 3 . . . .
        // 2 @ O . .
        // 1 . @ O .
        //   A B C D
        PointList black = new PointList();
        PointList white = new PointList();
        black.add(GoPoint.get(0, 1));
        black.add(GoPoint.get(1, 0));
        white.add(GoPoint.get(1, 1));
        white.add(GoPoint.get(2, 0));
        board.setup(black, white, BLACK);
        assertFalse(board.isKo(GoPoint.get(0, 0)));
        board.play(WHITE, GoPoint.get(0, 0));
        assertTrue(board.isKo(GoPoint.get(1, 0)));
        board.play(BLACK, GoPoint.get(5, 5));
        assertFalse(board.isKo(GoPoint.get(1, 0)));
        board.undo();
        assertTrue(board.isKo(GoPoint.get(1, 0)));
    }

    public void testIsSuicide()
    {
        Board board = new Board(19);
        assertFalse(board.isSuicide(WHITE, GoPoint.get(0, 0)));
        board.play(BLACK, GoPoint.get(0, 1));
        assertFalse(board.isSuicide(WHITE, GoPoint.get(0, 0)));
        board.play(BLACK, GoPoint.get(1, 1));
        assertFalse(board.isSuicide(WHITE, GoPoint.get(0, 0)));
        board.play(BLACK, GoPoint.get(2, 0));
        assertFalse(board.isSuicide(WHITE, GoPoint.get(0, 0)));
        board.play(WHITE, GoPoint.get(1, 0));
        assertTrue(board.isSuicide(WHITE, GoPoint.get(0, 0)));
        board.play(BLACK, GoPoint.get(0, 0));
        assertTrue(board.isSuicide(WHITE, GoPoint.get(1, 0)));
    }

    public void testGetLastMove()
    {
        Board board = new Board(19);
        assertNull(board.getLastMove());
        board.play(BLACK, GoPoint.get(0, 0));
        assertEquals(Move.get(BLACK, 0, 0), board.getLastMove());
        board.setup(new PointList(GoPoint.get(1, 1)), null, BLACK);
        assertNull(board.getLastMove());
    }

    /** Test that playing on a occupied field does not fail.
        Board.play spciefies that a play never fails.
        Also tests that the old stone is correctly restored. */
    public void testPlayOnOccupied()
    {
        Board board = new Board(19);
        GoPoint point = GoPoint.get(0, 0);
        board.play(WHITE, point);
        board.play(BLACK, point);
        board.undo();
        assertEquals(WHITE, board.getColor(point));
    }

    /** Test that setup does not cause suicide. */
    public void testSetupSuicide()
    {
        Board board = new Board(19);
        board.play(BLACK, GoPoint.get(1, 0));
        board.play(BLACK, GoPoint.get(0, 1));
        board.setup(null, new PointList(GoPoint.get(0, 0)), BLACK);
        assertEquals(WHITE, board.getColor(GoPoint.get(0, 0)));
    }

    /** Test that setup does not capture anything. */
    public void testSetupCapture()
    {
        Board board = new Board(19);
        board.play(BLACK, GoPoint.get(1, 0));
        board.play(WHITE, GoPoint.get(2, 0));
        board.play(BLACK, GoPoint.get(0, 1));
        board.play(WHITE, GoPoint.get(1, 1));
        board.setup(null, new PointList(GoPoint.get(0, 0)), BLACK);
        assertEquals(WHITE, board.getColor(GoPoint.get(0, 0)));
    }

    public void testSetupHandicap()
    {
        Board board = new Board(19);
        PointList stones = new PointList();
        stones.add(GoPoint.get(4, 4));
        stones.add(GoPoint.get(5, 5));
        board.setupHandicap(stones);
        assertEquals(WHITE, board.getToMove());
        assertEquals(BLACK, board.getColor(GoPoint.get(4, 4)));
        assertEquals(BLACK, board.getColor(GoPoint.get(5, 5)));
        assertEquals(stones, board.getSetup(BLACK));
    }

    public void testToMove()
    {
        Board board = new Board(19);
        assertEquals(BLACK, board.getToMove());
        board.play(BLACK, GoPoint.get(0, 0));
        assertEquals(WHITE, board.getToMove());
        board.setup(new PointList(GoPoint.get(1, 1)), null, BLACK);
        assertEquals(BLACK, board.getToMove());
    }

    public void testUndo()
    {
        Board board = new Board(19);
        board.play(BLACK, GoPoint.get(0, 0));
        board.undo();
        assertEquals(EMPTY, board.getColor(GoPoint.get(0, 0)));
        assertEquals(BLACK, board.getToMove());
    }
}
