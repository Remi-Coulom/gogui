package net.sf.gogui.gomoku;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

public class RenjuPatternTest extends junit.framework.TestCase{

	public static Test suite() {
		TestSuite suite = new TestSuite(RenjuPatternTest.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(AlignedTest.suite());
		//$JUnit-END$
		return new junit.framework.TestSuite(RenjuPatternTest.class);
	}
	
	public void testIsDoubleFourTrue() {
        System.out.println("\n\nis double four true");
	    Board b = new Board(19);
	    b.play(GoColor.BLACK, GoPoint.get(6, 6));
	    b.play(GoColor.BLACK, GoPoint.get(6, 5));
	    b.play(GoColor.BLACK, GoPoint.get(6, 3));
	    b.play(GoColor.BLACK, GoPoint.get(5, 4));
	    b.play(GoColor.BLACK, GoPoint.get(4, 4));
	    b.play(GoColor.BLACK, GoPoint.get(7, 4));
	    RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(6, 4)));
	    assertTrue(rj.isDoubleFour());
	}
	
	public void testIsDoubleFourFalse() {
        System.out.println("\n\nis double four false");
	    Board b = new Board(19);
	    b.play(GoColor.BLACK, GoPoint.get(6, 6));
        b.play(GoColor.BLACK, GoPoint.get(6, 5));
        b.play(GoColor.BLACK, GoPoint.get(6, 3));
        b.play(GoColor.WHITE, GoPoint.get(6, 2));
        RenjuPattern rj1 = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(6, 4)));
        assertFalse(rj1.isDoubleFour());
        b.play(GoColor.BLACK, GoPoint.get(5, 4));
        b.play(GoColor.BLACK, GoPoint.get(4, 4));
        b.play(GoColor.BLACK, GoPoint.get(7, 4));
        b.play(GoColor.WHITE, GoPoint.get(6, 7));
        b.play(GoColor.WHITE, GoPoint.get(3, 4));
        b.play(GoColor.WHITE, GoPoint.get(8, 4));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(6, 4)));
        assertFalse(rj.isDoubleFour());
	}
    
    public void testIsOpenFourTrue() {
        System.out.println("\n\nis open four true");
        Board b = new Board(8);
        b.play(GoColor.BLACK, GoPoint.get(1, 1));
        b.play(GoColor.BLACK, GoPoint.get(2, 2));
        b.play(GoColor.BLACK, GoPoint.get(4, 4));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK,  GoPoint.get(3, 3)));
        assertTrue(rj.isDoubleFour()); //And not isOpenFour because isDoubleFour includes isOpenFour test
    }

    public void testIsOpenFourFalse() {
        System.out.println("\n\nis open four false");
        Board b = new Board(8);
        b.play(GoColor.WHITE, GoPoint.get(0, 0));
        b.play(GoColor.BLACK, GoPoint.get(1, 1));
        b.play(GoColor.BLACK, GoPoint.get(2, 2));
        b.play(GoColor.BLACK, GoPoint.get(4, 4));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK,  GoPoint.get(3, 3)));
        assertFalse(rj.isDoubleFour());
    }
    public void testIsDoubleOpenThree() {
        System.out.println("\n\nis double open three true");
        Board b = new Board(11);
        b.play(GoColor.BLACK, GoPoint.get(2, 2));
        b.play(GoColor.BLACK, GoPoint.get(5, 2));
        b.play(GoColor.BLACK, GoPoint.get(3, 3));
        b.play(GoColor.BLACK, GoPoint.get(3, 4));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK,GoPoint.get(3, 2)));
        assertTrue(rj.isDoubleOpenThree());
    }
    public void testIsDoubleFourAligned() {
        System.out.println("\n\nis double four while aligned");
        Board b = new Board(10);
        b.play(GoColor.WHITE, GoPoint.get(0, 0));
        b.play(GoColor.BLACK, GoPoint.get(1, 0));
        b.play(GoColor.BLACK, GoPoint.get(2, 0));
        b.play(GoColor.BLACK, GoPoint.get(4, 0));
        b.play(GoColor.BLACK, GoPoint.get(7, 0));
        b.play(GoColor.BLACK, GoPoint.get(8, 0));
        b.play(GoColor.WHITE, GoPoint.get(9, 0));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(5, 0)));
        assertTrue(rj.isDoubleFour());
    } 
    
    public void testIsDoubleFourAligned2() {
        System.out.println("\n\nis double four while aligned2");
        Board b = new Board(10);
        b.play(GoColor.WHITE, GoPoint.get(0, 0));
        b.play(GoColor.BLACK, GoPoint.get(1, 0));
        b.play(GoColor.BLACK, GoPoint.get(3, 0));
        //b.play(GoColor.BLACK, GoPoint.get(4, 0));
        b.play(GoColor.BLACK, GoPoint.get(5, 0));
        b.play(GoColor.BLACK, GoPoint.get(7, 0));
        b.play(GoColor.WHITE, GoPoint.get(9, 0));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(4, 0)));
        assertTrue(rj.isDoubleFour());
    }

    public void testIsDoubleOpenThreeFalse() {
        System.out.println("\n\nis double open three false");
        Board b = new Board(11);
        b.play(GoColor.BLACK, GoPoint.get(2, 2));
        b.play(GoColor.BLACK, GoPoint.get(5, 2));
        b.play(GoColor.BLACK, GoPoint.get(3, 3));
        b.play(GoColor.BLACK, GoPoint.get(3, 4));
        b.play(GoColor.WHITE, GoPoint.get(3, 5));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK,GoPoint.get(3, 2)));
        assertFalse(rj.isDoubleOpenThree());
    }
    
    public void testIsFork() {
        System.out.println("\n\nis fork");
        Board b = new Board(12);
        b.play(GoColor.WHITE, GoPoint.get(1, 1));
        b.play(GoColor.BLACK, GoPoint.get(2, 1));
        b.play(GoColor.BLACK, GoPoint.get(3, 1));
        b.play(GoColor.BLACK, GoPoint.get(6, 1));
        b.play(GoColor.BLACK, GoPoint.get(8, 1));
        b.play(GoColor.BLACK, GoPoint.get(9, 1));
        b.play(GoColor.WHITE, GoPoint.get(10, 1));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(5, 1)));
        assertTrue(rj.isFork());
    }
    
    public void testIsForbiddenMove() {
        System.out.println("\n\nis forbidden move");
        Board b = new Board(15);
        b.play(GoColor.BLACK, GoPoint.get(4, 5));
        b.play(GoColor.BLACK, GoPoint.get(3, 3));
        b.play(GoColor.BLACK, GoPoint.get(5, 3));
        b.play(GoColor.BLACK, GoPoint.get(4, 2));
        RenjuPattern rj1 = new RenjuPattern(b, Move.get(GoColor.BLACK,  GoPoint.get(4, 3)));
        assertTrue(rj1.isForbiddenMove());
    }
    
    public void testIsNotForbidden() {
        System.out.println("\n\nis not forbidden move (because of fork 4-3)");
        //Does not work because it's 4/4 when I put the stone on
        Board b = new Board(11);
        b.play(GoColor.BLACK, GoPoint.get(1,4));
        b.play(GoColor.BLACK, GoPoint.get(5, 4));
        b.play(GoColor.BLACK, GoPoint.get(6, 4));
        b.play(GoColor.BLACK, GoPoint.get(8, 4));
        b.play(GoColor.BLACK, GoPoint.get(5, 5));
        b.play(GoColor.BLACK, GoPoint.get(3, 3));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(4, 4)));
        assertFalse(rj.isForbiddenMove());
    }
    
    public void testIsNotForbidden2() {
        System.out.println("\n\nis not forbidden move 2 (because of 6 alignment)");
        Board b = new Board(11);
        b.play(GoColor.WHITE, GoPoint.get(1, 1));
        b.play(GoColor.BLACK, GoPoint.get(2, 2));
        b.play(GoColor.BLACK, GoPoint.get(3, 3));
        b.play(GoColor.BLACK, GoPoint.get(4, 4));
        b.play(GoColor.BLACK, GoPoint.get(7, 7));
        b.play(GoColor.BLACK, GoPoint.get(6, 4));
        b.play(GoColor.BLACK, GoPoint.get(6, 4));
        b.play(GoColor.BLACK, GoPoint.get(6, 3));
        b.play(GoColor.BLACK, GoPoint.get(6, 2));
        b.play(GoColor.WHITE, GoPoint.get(6, 1));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(6, 6)));
        assertFalse(rj.isForbiddenMove());
    }
    
    public void testIsNotForbidden3() {
        System.out.println("\n\nis not forbidden move 3");
        Board b = new Board(11);
        b.play(GoColor.WHITE, GoPoint.get(1, 8));
        b.play(GoColor.WHITE, GoPoint.get(2, 7));
        b.play(GoColor.WHITE, GoPoint.get(3, 6));
        b.play(GoColor.WHITE, GoPoint.get(1, 3));
        b.play(GoColor.WHITE, GoPoint.get(2, 1));
        b.play(GoColor.WHITE, GoPoint.get(3, 3));
        b.play(GoColor.WHITE, GoPoint.get(3, 4));
        b.play(GoColor.WHITE, GoPoint.get(5, 3));
        b.play(GoColor.WHITE, GoPoint.get(5, 4));
        b.play(GoColor.WHITE, GoPoint.get(6, 1));
        b.play(GoColor.WHITE, GoPoint.get(7, 3));
    //    b.play(GoColor.BLACK, GoPoint.get(2, 2));
        b.play(GoColor.BLACK, GoPoint.get(2, 3));
        b.play(GoColor.BLACK, GoPoint.get(2, 4));
        b.play(GoColor.BLACK, GoPoint.get(3, 5));
        b.play(GoColor.BLACK, GoPoint.get(4, 3));
        b.play(GoColor.BLACK, GoPoint.get(4, 4));
        b.play(GoColor.BLACK, GoPoint.get(5, 5));
        b.play(GoColor.BLACK, GoPoint.get(6, 4));
        b.play(GoColor.BLACK, GoPoint.get(6, 3));
    //    b.play(GoColor.BLACK, GoPoint.get(6, 2));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(4, 5)));
        assertFalse(rj.isForbiddenMove());
    }
    
    public void testStopRecursivity() {
        System.out.println("\n\nstop recursivity");
        Board b = new Board(7);
        b.play(GoColor.WHITE, GoPoint.get(1, 0));
        b.play(GoColor.WHITE, GoPoint.get(0, 1));
        b.play(GoColor.WHITE, GoPoint.get(5, 0));
        b.play(GoColor.WHITE, GoPoint.get(0, 5));
        b.play(GoColor.WHITE, GoPoint.get(1, 6));
        b.play(GoColor.WHITE, GoPoint.get(6, 1));
        b.play(GoColor.WHITE, GoPoint.get(6, 5));
        b.play(GoColor.WHITE, GoPoint.get(5, 6));
        b.play(GoColor.BLACK, GoPoint.get(2, 1));
        b.play(GoColor.BLACK, GoPoint.get(3, 1));
        b.play(GoColor.BLACK, GoPoint.get(4, 1));
        b.play(GoColor.BLACK, GoPoint.get(2, 5));
        b.play(GoColor.BLACK, GoPoint.get(3, 5));
        b.play(GoColor.BLACK, GoPoint.get(4, 5));
        b.play(GoColor.BLACK, GoPoint.get(1, 2));
        b.play(GoColor.BLACK, GoPoint.get(1, 3));
        b.play(GoColor.BLACK, GoPoint.get(1, 4));
        b.play(GoColor.BLACK, GoPoint.get(5, 2));
        b.play(GoColor.BLACK, GoPoint.get(5, 3));
        b.play(GoColor.BLACK, GoPoint.get(5, 4));
        RenjuPattern rj = new RenjuPattern(b, Move.get(GoColor.BLACK, GoPoint.get(1, 1)));
        assertFalse(rj.isForbiddenMove());
        
        
        
    }
    
}