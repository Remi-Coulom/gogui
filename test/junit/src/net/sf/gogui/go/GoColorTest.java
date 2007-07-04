//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;

public final class GoColorTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GoColorTest.class);
    }

    public void testGetNextBlackWhite()
    {
        assertSame(WHITE, BLACK.getNextBlackWhite());
        assertNull(WHITE.getNextBlackWhite());
    }

    public void testGetNextBlackWhiteEmpty()
    {
        assertSame(WHITE, BLACK.getNextBlackWhiteEmpty());
        assertSame(EMPTY, WHITE.getNextBlackWhiteEmpty());
        assertNull(EMPTY.getNextBlackWhiteEmpty());
    }


    public void testGetPreviousBlackWhiteEmpty()
    {
        assertSame(WHITE, EMPTY.getPreviousBlackWhiteEmpty());
        assertSame(BLACK, WHITE.getPreviousBlackWhiteEmpty());
        assertNull(BLACK.getPreviousBlackWhiteEmpty());
    }

    public void testGetUppercaseLetter()
    {
        assertEquals("B", BLACK.getUppercaseLetter());
        assertEquals("W", WHITE.getUppercaseLetter());
        assertEquals("E", EMPTY.getUppercaseLetter());
    }

    public void testIsBlackWhite()
    {
        assertTrue(BLACK.isBlackWhite());
        assertTrue(WHITE.isBlackWhite());
        assertFalse(EMPTY.isBlackWhite());
    }

    public void testOtherColor()
    {
        assertSame(BLACK.otherColor(), WHITE);
        assertSame(WHITE.otherColor(), BLACK);
        assertSame(EMPTY.otherColor(), EMPTY);
    }
    public void testToInteger()
    {
        assertEquals(0, BLACK.toInteger());
        assertEquals(1, WHITE.toInteger());
        assertEquals(2, EMPTY.toInteger());
    }

    public void testToString()
    {
        assertEquals(BLACK.toString(), "black");
        assertEquals(WHITE.toString(), "white");
        assertEquals(EMPTY.toString(), "empty");
    }
}
