// XmlUtilTest.java

package net.sf.gogui.util;

public final class XmlUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(XmlUtilTest.class);
    }

    public void testEscape()
    {
        assertEquals("&lt;ok&gt;&amp;", XmlUtil.escapeText("<ok>&"));
    }

    public void testEscapeAttr()
    {
        assertEquals("&lt;ok&gt;&amp;&quot;", XmlUtil.escapeAttr("<ok>&\""));
    }
}
