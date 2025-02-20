// GtpCommandTest.java

package net.sf.gogui.gtp;

import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;

public final class GtpCommandTest
    extends junit.framework.TestCase
{
    public static void main(String[] args)
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
        assertEquals("play w C1", cmd.getLine());
        assertEquals("w C1", cmd.getArgLine());
        assertFalse(cmd.hasId());
        assertEquals("play", cmd.getCommand());
        assertEquals(2, cmd.getNuArg());
        assertEquals("w", cmd.getArg(0));
        assertSame(WHITE, cmd.getColorArg(0));
        assertEquals("C1", cmd.getArg(1));
        assertSame(cmd.getPointArg(1, 19), GoPoint.get(2, 0));
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
        assertEquals("boardsize 9", cmd.getLine());
        assertEquals("9", cmd.getArgLine());
        assertTrue(cmd.hasId());
        assertEquals(10, cmd.getId());
        assertEquals("boardsize", cmd.getCommand());
        assertEquals(1, cmd.getNuArg());
        assertEquals("9", cmd.getArg(0));
        assertEquals(9, cmd.getIntArg(0));
    }
}
