package net.sf.gogui.gomoku;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

public class ForbiddenRenjuMoveTest{
	

	public static Test suite() {
		TestSuite suite = new TestSuite(ForbiddenRenjuMoveTest.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(RenjuPatternTest.suite());
		//$JUnit-END$
		return new junit.framework.TestSuite(ForbiddenRenjuMoveTest.class);
	}
	
	public void testIsOpenFour() {
	    Board b = new Board(8);
	    b.play(GoColor.BLACK, GoPoint.get(1, 1));
	    b.play(GoColor.BLACK, GoPoint.get(2, 2));
	    b.play(GoColor.BLACK, GoPoint.get(4, 4));
	  //  RenjuPattern.
	}
	public void testIsFork() {
		Board b = new Board(12);
		b.play(GoColor.WHITE, GoPoint.get(1, 1));
		b.play(GoColor.BLACK, GoPoint.get(2, 1));
		b.play(GoColor.BLACK, GoPoint.get(3, 1));
		b.play(GoColor.BLACK, GoPoint.get(6, 1));
		b.play(GoColor.BLACK, GoPoint.get(8, 1));
		b.play(GoColor.BLACK, GoPoint.get(9, 1));
		b.play(GoColor.WHITE, GoPoint.get(10, 1));
	}

}