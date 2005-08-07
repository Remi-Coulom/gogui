//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

public class GtpCommandTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GtpCommandTest.class);
    }

    public void testBasic() throws GtpError
    {
        GtpCommand cmd = new GtpCommand("10 boardsize 9");
        assertTrue(cmd.hasId());
        assertEquals(cmd.getId(), 10);
        assertEquals(cmd.getCommand(), "boardsize");
        assertEquals(cmd.getNuArg(), 1);
        assertEquals(cmd.getArg(0), "9");
        assertEquals(cmd.getIntArg(0), 9);
        cmd = new GtpCommand("play w C1");
        assertFalse(cmd.hasId());
        assertEquals(cmd.getCommand(), "play");
        assertEquals(cmd.getNuArg(), 2);
        assertEquals(cmd.getArg(0), "w");
        assertTrue(cmd.getColorArg(0) == GoColor.WHITE);
        assertEquals(cmd.getArg(1), "C1");
        assertEquals(cmd.getArgToLower(1), "c1");
        assertTrue(cmd.getPointArg(1, 19) == GoPoint.create(2, 0));
    }
}

//----------------------------------------------------------------------------
