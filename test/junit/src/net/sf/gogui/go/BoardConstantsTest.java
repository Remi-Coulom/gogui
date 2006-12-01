//----------------------------------------------------------------------------
// $Id: BoardTest.java 3596 2006-11-29 18:34:51Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;

public class BoardConstantsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(BoardConstantsTest.class);
    }

    /** Test BoardConstants.getHandicapStones according to GTP
        specification.
    */
    public void testGetHandicapStones()
    {
        for (int size = 1; size <= 25; ++size)
        {
            BoardConstants constants = BoardConstants.get(size);
            for (int n = 0; n < 20; ++n)
            {
                String message = "size=" + size + " n=" + n;
                ArrayList list = constants.getHandicapStones(n);
                if (n == 0)
                {
                    assertNotNull(message, list);
                    assertTrue(message, list.isEmpty());
                }
                else if (size <= 6 || n == 1 || n > 9)
                    assertNull(message, list);
                else if (size == 7 || size % 2 == 0)
                {
                    if (n > 4)
                        assertNull(message, list);
                    else
                    {
                        assertNotNull(message, list);
                        assertEquals(message, n, list.size());
                    }
                }
                else
                {
                    if (n > 9)
                        assertNull(message, list);
                    else
                    {
                        assertNotNull(message, list);
                        assertEquals(message, n, list.size());
                    }
                }
                if (size == 19 && n >= 2 && n <= 9)
                {
                    assertTrue(message, list.contains(parsePoint("D4")));
                    assertTrue(message, list.contains(parsePoint("Q16")));
                    if (n >= 3)
                        assertTrue(message, list.contains(parsePoint("D16")));
                    if (n >= 4)
                        assertTrue(message, list.contains(parsePoint("Q4")));
                    if (n == 5 || n == 7 || n == 9)
                        assertTrue(message, list.contains(parsePoint("K10")));
                    if (n >= 6)
                    {
                        assertTrue(message, list.contains(parsePoint("D10")));
                        assertTrue(message, list.contains(parsePoint("Q10")));
                    }
                    if (n >= 8)
                    {
                        assertTrue(message, list.contains(parsePoint("K4")));
                        assertTrue(message, list.contains(parsePoint("K16")));
                    }
                }
            }
        }
    }

    private static GoPoint parsePoint(String s)
    {
        int boardSize = GoPoint.MAXSIZE;
        try
        {
            return GoPoint.parsePoint(s, boardSize);
        }
        catch (GoPoint.InvalidPoint e)
        {
            fail("Invalid point " + s);
            return null;
        }
    }
}

