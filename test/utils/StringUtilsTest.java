//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import junit.framework.TestCase;

//----------------------------------------------------------------------------

public class StringUtilsTest
    extends TestCase
{
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
