// PointListTest.java

package net.sf.gogui.go;

import java.util.Iterator;

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

    public void testIterate()
    {
        PointList list = new PointList();
        GoPoint p1 = GoPoint.get(1, 1);
        GoPoint p2 = GoPoint.get(2, 2);
        GoPoint p3 = GoPoint.get(2, 3);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        Iterator<GoPoint> it = list.iterator();
        GoPoint p;
        assertTrue(it.hasNext());
        p = it.next();
        assertEquals(p1, p);
        assertTrue(it.hasNext());
        p = it.next();
        assertEquals(p2, p);
        assertTrue(it.hasNext());
        p = it.next();
        assertEquals(p3, p);
        assertFalse(it.hasNext());
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
