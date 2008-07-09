// GtpCommandTest.java

package net.sf.gogui.gtp;

import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;

public final class GtpCommandTest
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

    public void testCommand() throws GtpError
    {
        GtpCommand cmd = new GtpCommand("play w C1");
        assertEquals(cmd.getLine(), "play w C1");
        assertEquals(cmd.getArgLine(), "w C1");
        assertFalse(cmd.hasId());
        assertEquals(cmd.getCommand(), "play");
        assertEquals(cmd.getNuArg(), 2);
        assertEquals(cmd.getArg(0), "w");
        assertTrue(cmd.getColorArg(0) == WHITE);
        assertEquals(cmd.getArg(1), "C1");
        assertTrue(cmd.getPointArg(1, 19) == GoPoint.get(2, 0));
    }

    public void testCommandWithComment() throws GtpError
    {
        GtpCommand cmd = new GtpCommand("10 boardsize 9  # foo bar");
        assertEquals("boardsize 9", cmd.getLine());
        assertEquals("9", cmd.getArgLine());
        assertTrue(cmd.hasId());
        assertEquals(10, cmd.getId());
        assertEquals("boardsize", cmd.getCommand());
        assertEquals(1, cmd.getNuArg());
        assertEquals("9", cmd.getArg(0));
        assertEquals(9, cmd.getIntArg(0));
    }

    public void testCommandWithId() throws GtpError
    {
        GtpCommand cmd = new GtpCommand("10 boardsize 9");
        assertEquals(cmd.getLine(), "boardsize 9");
        assertEquals(cmd.getArgLine(), "9");
        assertTrue(cmd.hasId());
        assertEquals(cmd.getId(), 10);
        assertEquals(cmd.getCommand(), "boardsize");
        assertEquals(cmd.getNuArg(), 1);
        assertEquals(cmd.getArg(0), "9");
        assertEquals(cmd.getIntArg(0), 9);
    }
}
