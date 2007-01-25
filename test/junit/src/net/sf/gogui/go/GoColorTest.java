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

    public void testToString()
    {
        assertEquals(GoColor.BLACK.toString(), "black");
        assertEquals(GoColor.WHITE.toString(), "white");
        assertEquals(GoColor.EMPTY.toString(), "empty");
    }
}

