// MoveTest.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

public final class MoveTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(MoveTest.class);
    }

    public void testPass()
    {
        Move blackPass = Move.getPass(BLACK);
        assertNull(blackPass.getPoint());
        assertSame(blackPass.getColor(), BLACK);
        assertSame(Move.get(BLACK, null), blackPass);
        Move whitePass = Move.getPass(WHITE);
        assertNull(whitePass.getPoint());
        assertSame(whitePass.getColor(), WHITE);
        assertSame(Move.get(WHITE, null), whitePass);
    }

    public void testToString()
    {
        assertEquals("B A1", Move.get(BLACK, 0, 0).toString());
        assertEquals("W PASS", Move.getPass(WHITE).toString());
    }
}
