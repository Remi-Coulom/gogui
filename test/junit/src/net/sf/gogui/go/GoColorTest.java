//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

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
        assertSame(GoColor.WHITE, GoColor.BLACK.getNextBlackWhite());
        assertNull(GoColor.WHITE.getNextBlackWhite());
    }

    public void testGetNextBlackWhiteEmpty()
    {
        assertSame(GoColor.WHITE, GoColor.BLACK.getNextBlackWhiteEmpty());
        assertSame(GoColor.EMPTY, GoColor.WHITE.getNextBlackWhiteEmpty());
        assertNull(GoColor.EMPTY.getNextBlackWhiteEmpty());
    }


    public void testGetPreviousBlackWhiteEmpty()
    {
        assertSame(GoColor.WHITE, GoColor.EMPTY.getPreviousBlackWhiteEmpty());
        assertSame(GoColor.BLACK, GoColor.WHITE.getPreviousBlackWhiteEmpty());
        assertNull(GoColor.BLACK.getPreviousBlackWhiteEmpty());
    }

    public void testGetUppercaseLetter()
    {
        assertEquals("B", GoColor.BLACK.getUppercaseLetter());
        assertEquals("W", GoColor.WHITE.getUppercaseLetter());
        assertEquals("E", GoColor.EMPTY.getUppercaseLetter());
    }

    public void testIsBlackWhite()
    {
        assertTrue(GoColor.BLACK.isBlackWhite());
        assertTrue(GoColor.WHITE.isBlackWhite());
        assertFalse(GoColor.EMPTY.isBlackWhite());
    }

    public void testOtherColor()
    {
        assertSame(GoColor.BLACK.otherColor(), GoColor.WHITE);
        assertSame(GoColor.WHITE.otherColor(), GoColor.BLACK);
        assertSame(GoColor.EMPTY.otherColor(), GoColor.EMPTY);
    }
    public void testToInteger()
    {
        assertEquals(0, GoColor.BLACK.toInteger());
        assertEquals(1, GoColor.WHITE.toInteger());
        assertEquals(2, GoColor.EMPTY.toInteger());
    }

    public void testToString()
    {
        assertEquals(GoColor.BLACK.toString(), "black");
        assertEquals(GoColor.WHITE.toString(), "white");
        assertEquals(GoColor.EMPTY.toString(), "empty");
    }
}

