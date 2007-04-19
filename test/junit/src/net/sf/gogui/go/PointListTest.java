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

    public void testEquals()
    {
        PointList list1 = new PointList(GoPoint.get(0, 0));
        PointList list2 = new PointList(GoPoint.get(0, 0));
        PointList list3 = new PointList(GoPoint.get(1, 1));
        assertTrue(list1.equals(list1));
        assertTrue(list1.equals(list2));
        assertFalse(list1.equals(list3));
    }

    public void testHashCode()
    {
        PointList list1 = new PointList(GoPoint.get(0, 0));
        PointList list2 = new PointList(GoPoint.get(0, 0));
        PointList list3 = new PointList(GoPoint.get(1, 1));
        assertEquals(list1.hashCode(), list2.hashCode());
        assertTrue(list1.hashCode() != list3.hashCode());
    }

    public void testRemove()
    {
        PointList list = new PointList();
        GoPoint p1 = GoPoint.get(1, 1);
        GoPoint p2 = GoPoint.get(2, 2);
        GoPoint p3 = GoPoint.get(3, 3);
        list.add(p1);
        list.add(p2);
        list.add(p1);
        list.add(p3);
        boolean result = list.remove(p1);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(p2, list.get(0));
        assertEquals(p1, list.get(1));
        assertEquals(p3, list.get(2));
        result = list.remove(p1);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(p2, list.get(0));
        assertEquals(p3, list.get(1));
        result = list.remove(p3);
        assertTrue(result);
        assertEquals(1, list.size());
        assertEquals(p2, list.get(0));
        result = list.remove(p1);
        assertFalse(result);
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
