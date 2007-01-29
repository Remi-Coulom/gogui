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
}

