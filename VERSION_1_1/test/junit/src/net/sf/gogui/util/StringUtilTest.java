// StringUtilTest.java

package net.sf.gogui.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class StringUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(StringUtilTest.class);
    }

    public void testGetNumberFormatLocale()
    {
        Locale oldDefault = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.FRENCH);
            NumberFormat format = StringUtil.getNumberFormat(1);
            assertEquals("3.1", format.format(3.1));
        }
        finally
        {
            Locale.setDefault(oldDefault);
        }
    }

    public void testIsEmpty()
    {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertTrue(StringUtil.isEmpty(" "));
        assertTrue(StringUtil.isEmpty(" \t"));
        assertFalse(StringUtil.isEmpty("a"));
        assertFalse(StringUtil.isEmpty(" ab"));
    }

    public void testSplit()
    {
        String[] s = StringUtil.split("1//23/ ", '/');
        assertEquals(s.length, 4);
        assertEquals(s[0], "1");
        assertEquals(s[1], "");
        assertEquals(s[2], "23");
        assertEquals(s[3], " ");
    }

    public void testSplitArguments1()
    {
        String[] s
            = StringUtil.splitArguments("one two \"three four\"");
        assertEquals(3, s.length);
        assertEquals("one", s[0]);
        assertEquals("two", s[1]);
        assertEquals("three four", s[2]);
    }

    public void testSplitArguments2()
    {
        String[] s
            = StringUtil.splitArguments("one \"two \\\"three four\\\"\"");
        assertEquals(2, s.length);
        assertEquals("one", s[0]);
        assertEquals("two \\\"three four\\\"", s[1]);
    }
}
