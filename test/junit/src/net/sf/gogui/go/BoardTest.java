//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

//----------------------------------------------------------------------------

public class BoardTest
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
        assertEquals(2, board.getCapturedB());
        assertEquals(0, board.getCapturedW());
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

    public void testUndo()
    {
        Board board = new Board(19);
        board.play(GoPoint.get(0, 0), GoColor.BLACK);
        board.undo();
        assertEquals(GoColor.EMPTY, board.getColor(GoPoint.get(0, 0)));
    }
}

//----------------------------------------------------------------------------
