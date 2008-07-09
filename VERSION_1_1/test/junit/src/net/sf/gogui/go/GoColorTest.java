// GoColorTest.java

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
}
