// HistogramTest.java

package net.sf.gogui.util;

public final class HistogramTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(HistogramTest.class);
    }

    public void testBasic() throws ErrorMessage
    {
        Histogram h = new Histogram(0.0, 10.0, 2.0);
        h.add(5.5);
        h.add(9.0);
        h.add(8.5);
        h.add(5.0);
        double epsilon = 1e-7;
        assertEquals(4, h.getCount());
        assertEquals(0, h.getCount(0));
        assertEquals(0, h.getCount(1));
        assertEquals(2, h.getCount(2));
        assertEquals(0, h.getCount(3));
        assertEquals(2, h.getCount(4));
        assertEquals(5, h.getSize());
        assertEquals(2.0, h.getStep(), epsilon);
        assertEquals(0.0, h.getValue(0), epsilon);
        assertEquals(2.0, h.getValue(1), epsilon);
        assertEquals(4.0, h.getValue(2), epsilon);
        assertEquals(6.0, h.getValue(3), epsilon);
        assertEquals(8.0, h.getValue(4), epsilon);
    }

    /** Test adding values that are equal to min and max. */
    public void testBorderValues() throws ErrorMessage
    {
        Histogram h = new Histogram(0.0, 1.0, 1.0);
        h.add(0.0);
        h.add(1.0);
        double epsilon = 1e-7;
        assertEquals(2, h.getCount());
        assertEquals(2, h.getCount(0));
        assertEquals(1, h.getSize());
        assertEquals(1.0, h.getStep(), epsilon);
        assertEquals(0.0, h.getValue(0), epsilon);
    }
}
