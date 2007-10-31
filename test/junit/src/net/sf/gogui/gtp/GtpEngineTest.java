// GtpEngineTest.java

package net.sf.gogui.gtp;

public final class GtpEngineTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GtpEngineTest.class);
    }

    public void testCmdKnownCommand() throws GtpError
    {
        GtpEngine engine = new GtpEngine(null);
        GtpClientBase gtp = new GtpEngineClient(engine);
        assertEquals("true", gtp.send("known_command name"));
        assertEquals("false", gtp.send("known_command foobar"));
    }
}
