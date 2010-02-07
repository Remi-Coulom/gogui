// SgfUtilTest.java

package net.sf.gogui.sgf;

public final class SgfUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(SgfUtilTest.class);
    }

    public void testParseTime() throws Exception
    {
        assertEquals(13L * 3600L * 1000L, SgfUtil.parseTime("13h"));
        assertEquals(13L * 3600L * 1000L, SgfUtil.parseTime("13 hr"));
        assertEquals(13L * 3600L * 1000L, SgfUtil.parseTime("  13 hours"));
        assertEquals(13L * 3600L * 1000L, SgfUtil.parseTime("13 hours  each"));
        assertEquals(10L * 3600L * 1000L, SgfUtil.parseTime("10 hrs each"));
        assertEquals(70L * 60L * 1000L, SgfUtil.parseTime("70m"));
    }
}
