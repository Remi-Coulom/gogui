//----------------------------------------------------------------------------
// MoveTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.go;

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
        Move blackPass = Move.getPass(GoColor.BLACK);
        assertNull(blackPass.getPoint());
        assertSame(blackPass.getColor(), GoColor.BLACK);
        assertSame(Move.get(GoColor.BLACK, null), blackPass);
        Move whitePass = Move.getPass(GoColor.WHITE);
        assertNull(whitePass.getPoint());
        assertSame(whitePass.getColor(), GoColor.WHITE);
        assertSame(Move.get(GoColor.WHITE, null), whitePass);
    }

    public void testToString()
    {
        assertEquals("black A1", Move.get(GoColor.BLACK, 0, 0).toString());
        assertEquals("white PASS", Move.getPass(GoColor.WHITE).toString());
    }
}
