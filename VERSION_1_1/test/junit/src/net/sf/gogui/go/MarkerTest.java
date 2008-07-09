// MarkerTest.java

package net.sf.gogui.go;

public final class MarkerTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(MarkerTest.class);
    }

    public void testBasics()
    {
        Marker marker = new Marker(19);
        assertTrue(marker.isCleared());
        GoPoint p1 = GoPoint.get(0, 0);
        GoPoint p2 = GoPoint.get(5, 5);
        GoPoint p3 = GoPoint.get(0, 5);
        assertFalse(marker.get(p1));
        assertFalse(marker.get(p2));
        assertFalse(marker.get(p3));
        marker.set(p1);
        assertFalse(marker.isCleared());
        assertTrue(marker.get(p1));
        assertFalse(marker.get(p2));
        assertFalse(marker.get(p3));
        marker.set(p2, true);
        assertTrue(marker.get(p1));
        assertTrue(marker.get(p2));
        assertFalse(marker.get(p3));
        marker.clear();
        assertTrue(marker.isCleared());
        assertFalse(marker.get(p1));
        assertFalse(marker.get(p2));
        assertFalse(marker.get(p3));
    }

    public void testSetList()
    {
        Marker marker = new Marker(19);
        GoPoint p1 = GoPoint.get(0, 0);
        GoPoint p2 = GoPoint.get(5, 5);
        GoPoint p3 = GoPoint.get(0, 5);
        PointList list = new PointList();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        marker.set(list);
        assertTrue(marker.get(p1));
        assertTrue(marker.get(p2));
        assertTrue(marker.get(p3));
        list.clear();
        list.add(p1);
        list.add(p2);
        marker.clear(list);
        assertFalse(marker.get(p1));
        assertFalse(marker.get(p2));
        assertTrue(marker.get(p3));
        marker.clear(p3);
        assertTrue(marker.isCleared());
    }
}