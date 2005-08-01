//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

//----------------------------------------------------------------------------

public class StringUtilsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(StringUtilsTest.class);
    }

    public void testSplit()
    {
        String[] s = StringUtils.split("1//23/ ", '/');
        assertEquals(s.length, 4);
        assertEquals(s[0], "1");
        assertEquals(s[1], "");
        assertEquals(s[2], "23");
        assertEquals(s[3], " ");
    }
}

//----------------------------------------------------------------------------
