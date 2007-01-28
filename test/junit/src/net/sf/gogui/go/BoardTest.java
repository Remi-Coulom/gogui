//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

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
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        assertFalse(board.bothPassed());
        board.play(null, GoColor.WHITE);
        assertFalse(board.bothPassed());
        board.play(null, GoColor.BLACK);
        assertTrue(board.bothPassed());
        board.play(null, GoColor.WHITE);
        assertTrue(board.bothPassed());
    }

    public void testCapture()
    {
        Board board = new Board(19);
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        board.play(GoPoint.get(1, 0), GoColor.BLACK);
        board.play(GoPoint.get(0, 1), GoColor.WHITE);
        board.play(GoPoint.get(1, 1), GoColor.WHITE);
        board.play(GoPoint.get(2, 0), GoColor.WHITE);
        assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(0, 0)));
        assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(1, 0)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 1)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(1, 1)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 0)));
        assertEquals(2, board.getCapturedBlack());
        assertEquals(0, board.getCapturedWhite());
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

    /** Test Board.getKilled() */
    public void testGetKilled()
    {
        Board board = new Board(19);
        // 4 . . . . .
        // 3 . . O . .
        // 2 O O @ O .
        // 1 @ @ . . .
        //   A B C D E
        board.setup(GoPoint.get(0, 0), GoColor.BLACK);
        board.setup(GoPoint.get(1, 0), GoColor.BLACK);
        board.setup(GoPoint.get(2, 1), GoColor.BLACK);
        board.setup(GoPoint.get(0, 1), GoColor.WHITE);
        board.setup(GoPoint.get(1, 1), GoColor.WHITE);
        board.setup(GoPoint.get(2, 2), GoColor.WHITE);
        board.setup(GoPoint.get(3, 1), GoColor.WHITE);
        board.play(GoPoint.get(2, 0), GoColor.WHITE);
        ConstPointList killed = board.getKilled();
        assertEquals(3, killed.size());
        assertTrue(killed.contains(GoPoint.get(0, 0)));
        assertTrue(killed.contains(GoPoint.get(1, 0)));
        assertTrue(killed.contains(GoPoint.get(2, 1)));
        board.undo();
        board.setup(GoPoint.get(3, 0), GoColor.WHITE);
        killed = board.getKilled();
        assertEquals(0, killed.size());
        board.undo();
        board.play(GoPoint.get(3, 0), GoColor.WHITE);
        assertTrue(board.getKilled().isEmpty());
    }

    /** Test Board.getSuicide() */
    public void testGetSuicide()
    {
        Board board = new Board(19);
        // 4 . . . .
        // 3 O . . .
        // 2 @ O . .
        // 1 . @ O .
        //   A B C D
        board.setup(GoPoint.get(0, 1), GoColor.BLACK);
        board.setup(GoPoint.get(1, 0), GoColor.BLACK);
        board.setup(GoPoint.get(0, 2), GoColor.WHITE);
        board.setup(GoPoint.get(1, 1), GoColor.WHITE);
        board.setup(GoPoint.get(2, 0), GoColor.WHITE);
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        ConstPointList suicide = board.getSuicide();
        assertEquals(3, suicide.size());
        assertTrue(suicide.contains(GoPoint.get(0, 0)));
        assertTrue(suicide.contains(GoPoint.get(0, 1)));
        assertTrue(suicide.contains(GoPoint.get(1, 0)));
        board.undo();
        board.setup(GoPoint.get(0, 0), GoColor.BLACK);
        assertTrue(board.getSuicide().isEmpty());
    }

    public void testIsSuicide()
    {
        Board board = new Board(19);
        assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
        board.play(GoPoint.get(0, 1), GoColor.BLACK);
        assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
        board.play(GoPoint.get(1, 1), GoColor.BLACK);
        assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
        board.play(GoPoint.get(2, 0), GoColor.BLACK);
        assertFalse(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
        board.play(GoPoint.get(1, 0), GoColor.WHITE);
        assertTrue(board.isSuicide(GoPoint.get(0, 0), GoColor.WHITE));
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        assertTrue(board.isSuicide(GoPoint.get(1, 0), GoColor.WHITE));
    }

    public void testGetLastMove()
    {
        Board board = new Board(19);
        assertNull(board.getLastMove());
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        assertEquals(Move.get(0, 0, GoColor.BLACK), board.getLastMove());
        board.setup(GoPoint.get(1, 1), GoColor.BLACK);        
        assertNull(board.getLastMove());
    }

    /** Test that playing on a occupied field does not fail.
        Board.play spciefies that a play never fails.
        Also tests that the old stone is correctly restored.
    */
    public void testPlayOnOccupied()
    {
        Board board = new Board(19);
        GoPoint point = GoPoint.get(0, 0);
        board.play(point, GoColor.WHITE);
        board.play(point, GoColor.BLACK);
        board.undo();
        assertEquals(GoColor.WHITE, board.getColor(point));
    }

    /** Test that setup does not cause suicide. */
    public void testSetupSuicide()
    {
        Board board = new Board(19);
        board.play(GoPoint.get(1, 0), GoColor.BLACK);
        board.play(GoPoint.get(0, 1), GoColor.BLACK);
        board.setup(GoPoint.get(0, 0), GoColor.WHITE);
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 0)));
    }

    /** Test that setup does not capture anything. */
    public void testSetupCapture()
    {
        Board board = new Board(19);
        board.play(GoPoint.get(1, 0), GoColor.BLACK);
        board.play(GoPoint.get(2, 0), GoColor.WHITE);
        board.play(GoPoint.get(0, 1), GoColor.BLACK);
        board.play(GoPoint.get(1, 1), GoColor.WHITE);
        board.setup(GoPoint.get(0, 0), GoColor.WHITE);
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(0, 0)));
    }

    public void testToMove()
    {
        Board board = new Board(19);
        assertEquals(GoColor.BLACK, board.getToMove());
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        assertEquals(GoColor.WHITE, board.getToMove());
        // Setup should not change to move
        board.setup(GoPoint.get(1, 1), GoColor.BLACK);        
        assertEquals(GoColor.WHITE, board.getToMove());
    }

    public void testUndo()
    {
        Board board = new Board(19);
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        board.undo();
        assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(0, 0)));
        assertEquals(GoColor.BLACK, board.getToMove());
    }
}

