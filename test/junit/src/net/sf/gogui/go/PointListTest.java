//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

public final class PointListTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(PointListTest.class);
    }

    public void testPop()
    {
        PointList list = new PointList();
        GoPoint p1 = GoPoint.get(1, 1);
        GoPoint p2 = GoPoint.get(2, 2);
        list.add(p1);
        list.add(p2);
        GoPoint p;
        p = list.pop();
        assertEquals(p2, p);
        assertEquals(1, list.size());
        p = list.pop();
        assertEquals(p1, p);
        assertEquals(0, list.size());
    }

    public void testToString()
    {
        assertEquals("", PointList.toString(null));
        PointList list = new PointList();
        assertEquals("", list.toString());
        list.add(GoPoint.get(0, 0));
        assertEquals("A1", list.toString());
        list.add(GoPoint.get(1, 1));
        assertEquals("A1 B2", list.toString());
    }
}
