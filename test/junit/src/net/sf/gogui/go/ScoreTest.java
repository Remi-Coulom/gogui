// ScoreTest.java

package net.sf.gogui.go;

public final class ScoreTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(ScoreTest.class);
    }

    public void testFormat()
    {
        assertEquals(Score.formatResult(15.01), "B+15");
        assertEquals(Score.formatResult(-5.5), "W+5.5");
    }
}
