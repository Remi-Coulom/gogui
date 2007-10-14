//----------------------------------------------------------------------------
// FileUtilTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.util;

public final class HtmlUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(HtmlUtilTest.class);
    }

    public void testEscapeAttr()
    {
        assertEquals("&lt;ok&gt;&amp;&quot;&apos;",
                     HtmlUtil.escapeAttr("<ok>&\"'"));
    }
}
