// ObjectUtilTest.java

package net.sf.gogui.util;

public final class ObjectUtilTest
    extends junit.framework.TestCase
{
    public static void main(Object args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(ObjectUtilTest.class);
    }

    public void testEquals()
    {
        assertTrue(ObjectUtil.equals(null, null));
        assertTrue(ObjectUtil.equals("foo", "foo"));
        assertFalse(ObjectUtil.equals("foo", "bar"));
        assertFalse(ObjectUtil.equals("foo", null));
        assertFalse(ObjectUtil.equals(null, "foo"));
    }
}