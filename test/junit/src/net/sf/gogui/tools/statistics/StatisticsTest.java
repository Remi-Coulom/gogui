// StatisticsTest.java

package net.sf.gogui.tools.statistics;

import java.util.ArrayList;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpEngineClient;
import net.sf.gogui.gtp.GtpExpectEngine;

public final class StatisticsTest
    extends junit.framework.TestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(StatisticsTest.class);
    }

    public void testBasics() throws Exception
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpClientBase gtp = new GtpEngineClient(expect);
        String program = null;
        ArrayList<String> sgfFiles = new ArrayList<String>();
        sgfFiles.add(getClass().getResource("game-1.sgf").getFile());
        int size = 9;
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("foo");
        boolean allowSetup = false;
        boolean backward = false;
        expect.expect("protocol_version", "2");
        expect.expect("name");
        expect.expect("version");
        expect.expect("boardsize 9");
        expect.expect("clear_board");
        expect.expect("foo");
        expect.expect("play B C3");
        expect.expect("foo");
        expect.expect("play W G7");
        expect.expect("foo");
        expect.expect("play B C7");
        expect.expect("foo");
        expect.expect("quit");
        Statistics statistics = new Statistics();
        statistics.setQuiet(true);
        statistics.run(gtp, program, sgfFiles, size, commands, null, null,
                       allowSetup, backward, false);
        assertTrue(expect.isExpectQueueEmpty());
    }
}
