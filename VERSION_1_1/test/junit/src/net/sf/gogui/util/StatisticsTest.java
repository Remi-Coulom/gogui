// StatisticsTest.java

package net.sf.gogui.util;

public final class StatisticsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(StatisticsTest.class);
    }

    public void testBasic() throws ErrorMessage
    {
        Statistics s = new Statistics();
        s.add(6.0);
        s.add(9.0);
        s.add(8.0);
        s.add(5.0);
        double epsilon = 1e-7;
        double sum = 6.0 + 9.0 + 8.0 + 5.0;
        int count = 4;
        double min = 5.0;
        double max = 9.0;
        double mean = sum / count;
        double variance
            = (Math.pow(6.0 - mean, 2) + Math.pow(9.0 - mean, 2)
               + Math.pow(8.0 - mean, 2) + Math.pow(5.0 - mean, 2)) / count;
        double deviation = Math.sqrt(variance);
        double error = Math.sqrt(variance / count);
        assertEquals(count, s.getCount());
        assertEquals(deviation, s.getDeviation(), epsilon);
        assertEquals(error, s.getError(), epsilon);
        assertEquals(mean, s.getMean(), epsilon);
        assertEquals(min, s.getMin(), epsilon);
        assertEquals(max, s.getMax(), epsilon);
        assertEquals(sum, s.getSum(), epsilon);
        assertEquals(Math.sqrt((double)count / 2) * error, s.getMaxError(2),
                     epsilon);
    }
}
