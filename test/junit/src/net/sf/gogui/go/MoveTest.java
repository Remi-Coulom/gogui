//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

//----------------------------------------------------------------------------

public class MoveTest
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
        assertSame(Move.get(null, GoColor.BLACK), blackPass);
        Move whitePass = Move.getPass(GoColor.WHITE);
        assertNull(whitePass.getPoint());
        assertSame(whitePass.getColor(), GoColor.WHITE);
        assertSame(Move.get(null, GoColor.WHITE), whitePass);
    }

    public void testToString()
    {
        assertEquals("black A1", Move.get(0, 0, GoColor.BLACK).toString());
        assertEquals("white PASS", Move.getPass(GoColor.WHITE).toString());
    }
}

//----------------------------------------------------------------------------
