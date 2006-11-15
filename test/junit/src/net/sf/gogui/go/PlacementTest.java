//----------------------------------------------------------------------------
// $Id: PlacementTest.java 3542 2006-10-07 22:54:46Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.go;

public class PlacementTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(PlacementTest.class);
    }

    public void testEquals()
    {
        Placement p1 = new Placement(GoPoint.get(1, 1), GoColor.BLACK, false);
        Placement p2 = new Placement(GoPoint.get(1, 1), GoColor.BLACK, false);
        Placement p3 = new Placement(GoPoint.get(1, 1), GoColor.WHITE, false);
        Placement p4 = new Placement(GoPoint.get(1, 1), GoColor.BLACK, true);
        Placement p5 = new Placement(GoPoint.get(2, 2), GoColor.BLACK, false);
        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(p2));
        assertFalse(p1.equals(p3));
        assertFalse(p1.equals(p4));
        assertFalse(p1.equals(p5));
        assertFalse(p1.equals(null));
        assertTrue(p2.equals(p1));
        assertFalse(p3.equals(p1));
        assertFalse(p4.equals(p1));
        assertFalse(p5.equals(p1));
    }
}
