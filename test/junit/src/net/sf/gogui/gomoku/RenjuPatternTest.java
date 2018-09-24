package net.sf.gogui.gomoku;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

public class RenjuPatternTest extends junit.framework.TestCase{

	public static Test suite() {
		TestSuite suite = new TestSuite(RenjuPatternTest.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(AlignedTest.suite());
		//$JUnit-END$
		return new junit.framework.TestSuite(RenjuPatternTest.class);
	}
	
	public void testRenjuPatternSimple() {
		Board b = new Board(15);
		b.play(GoColor.WHITE,GoPoint.get(0,2));
		b.play(GoColor.BLACK,GoPoint.get(2, 0));
		b.play(GoColor.WHITE,GoPoint.get(1, 1));
		b.play(GoColor.WHITE,GoPoint.get(2,2));
		RenjuPattern rp = new RenjuPattern(b, GoPoint.get(1,1), GoColor.BLACK);
		System.out.println(rp.toString(b));
	}

	
	public void testRenjuPattern() {
		Board b = new Board(16);
		b.play(GoColor.BLACK, GoPoint.get('k'-'a'-1, 15-1));
		b.play(GoColor.BLACK, GoPoint.get('k'-'a'-1, 13-1));
		b.play(GoColor.BLACK, GoPoint.get('k'-'a'-1, 11-1));
		b.play(GoColor.BLACK, GoPoint.get('k'-'a'-1, 9-1));
		b.play(GoColor.BLACK, GoPoint.get('k'-'a'-1, 8-1));
		b.play(GoColor.BLACK, GoPoint.get('l'-'a'-1, 11-1));
		b.play(GoColor.BLACK, GoPoint.get('m'-'a'-1, 12-1));
		b.play(GoColor.BLACK, GoPoint.get('p'-'a'-1, 15-1));
		b.play(GoColor.BLACK, GoPoint.get('e'-'a', 15-1));
		b.play(GoColor.BLACK, GoPoint.get('k'-'a'-1, 9-1));
		b.play(GoColor.WHITE, GoPoint.get('d'-'a', 10-1));
		b.play(GoColor.WHITE, GoPoint.get('f'-'a', 10-1));
		b.play(GoColor.WHITE, GoPoint.get('f'-'a', 14-1));
		b.play(GoColor.WHITE, GoPoint.get('g'-'a', 13-1));
		b.play(GoColor.WHITE, GoPoint.get('h'-'a', 10-1));
		b.play(GoColor.WHITE, GoPoint.get('k'-'a'-1, 5-1));
		b.play(GoColor.WHITE, GoPoint.get('p'-'a'-1, 5-1));
		b.play(GoColor.WHITE, GoPoint.get('k'-'a'-1, 14-1));
		b.play(GoColor.WHITE, GoPoint.get('k'-'a'-1, 12-1));
		b.play(GoColor.WHITE, GoPoint.get('c'-'a', 3-1));
		RenjuPattern rp = new RenjuPattern(b, GoPoint.get('k'-'a', 10), GoColor.WHITE);
		System.out.println(rp.toString(b));
	}

}
