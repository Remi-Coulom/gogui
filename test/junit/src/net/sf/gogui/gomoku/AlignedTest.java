package net.sf.gogui.gomoku;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

public class AlignedTest extends junit.framework.TestCase
{
	public static void main(String args[])
	{
		junit.textui.TestRunner.run(suite());
	}

	public static junit.framework.Test suite()
	{
		return new junit.framework.TestSuite(AlignedTest.class);
	}


	public void testAlignedHorizontally() {
		Board b = new Board(19);
		b.play(GoColor.WHITE, GoPoint.get(5, 10));
		b.play(GoColor.WHITE, GoPoint.get(6, 10));
		b.play(GoColor.WHITE, GoPoint.get(7, 10));
		b.play(GoColor.WHITE, GoPoint.get(8, 10));
		//b.play(GoColor.WHITE, GoPoint.get(9, 10));
		Move lastMove = Move.get(GoColor.WHITE, 9, 10);
		assertTrue(CheckAlignment.areNAligned(b, lastMove, 5));
	}
	
	public void testAlignedDiagonal() {
		Board b = new Board(19);
		b.play(GoColor.BLACK, GoPoint.get(18, 18));
		b.play(GoColor.BLACK, GoPoint.get(17, 17));
		b.play(GoColor.BLACK, GoPoint.get(16, 16));
		b.play(GoColor.BLACK, GoPoint.get(15, 15));
		//b.play(GoColor.BLACK, GoPoint.get(14, 14));
		Move lastMove = Move.get(GoColor.BLACK, 14, 14);
		assertTrue(CheckAlignment.areFiveAligned(b, lastMove));
	}


	public void testAlignedFromCenter() {
		Board b = new Board(19);
		b.play(GoColor.BLACK, GoPoint.get(15, 10));
		b.play(GoColor.BLACK, GoPoint.get(15, 9));
		//b.play(GoColor.BLACK, GoPoint.get(15, 8));
		b.play(GoColor.BLACK, GoPoint.get(15, 7));
		b.play(GoColor.BLACK, GoPoint.get(15, 6));
		b.play(GoColor.BLACK, GoPoint.get(15, 5));
		Move lastMove = Move.get(GoColor.BLACK, 15,8);
		assertTrue(CheckAlignment.areAtLeastFiveAligned(b, lastMove));
	}

	public void testNonAlignedBlackWhite() {
		Board b = new Board(19);
		b.play(GoColor.BLACK, GoPoint.get(5, 10));
		b.play(GoColor.WHITE, GoPoint.get(6, 10));
		b.play(GoColor.BLACK, GoPoint.get(7, 10));
		b.play(GoColor.WHITE, GoPoint.get(8, 10));
		//b.play(GoColor.BLACK, GoPoint.get(9, 10));
		Move lastMove = Move.get(GoColor.BLACK, 9, 10);
		assertFalse(CheckAlignment.areAtLeastFiveAligned(b, lastMove));
	}

	public void testBorder() {
		Board b = new Board(19);
		b.play(GoColor.WHITE, GoPoint.get(3, 2));
		b.play(GoColor.WHITE, GoPoint.get(3, 1));
		b.play(GoColor.WHITE, GoPoint.get(3, 0));
		assertTrue(CheckAlignment.areNAligned(b, Move.get(GoColor.WHITE, 3, 2), 3));
		assertFalse(CheckAlignment.areAtLeastFiveAligned(b,Move.get(GoColor.WHITE, 3, 0)));
	}
}
