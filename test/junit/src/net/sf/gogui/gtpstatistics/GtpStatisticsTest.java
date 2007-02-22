//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpEngineClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpExpectEngine;
import net.sf.gogui.util.ErrorMessage;

public final class GtpStatisticsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GtpStatisticsTest.class);
    }

    public void testBasics() throws Exception
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpClientBase gtp = new GtpEngineClient(expect);
        String program = null;
        ArrayList sgfFiles = new ArrayList();
        sgfFiles.add(getClass().getResource("game-1.sgf").getFile());
        File output = File.createTempFile("gogui-gtpstatisticstest", ".dat");
        output.deleteOnExit();
        int size = 9;
        ArrayList commands = new ArrayList();
        commands.add("foo");
        ArrayList beginCommands = null;
        ArrayList finalCommands = null;
        boolean force = true;
        boolean allowSetup = false;
        int min = 0;
        int max = Integer.MAX_VALUE;
        boolean backward = false;
        expect.expect("protocol_version", "2");
        expect.expect("name");
        expect.expect("version");
        expect.expect("boardsize 9");
        expect.expect("clear_board");
        expect.expect("foo");
        expect.expect("play b C3");
        expect.expect("foo");
        expect.expect("play w G7");
        expect.expect("foo");
        expect.expect("play b C7");
        expect.expect("foo");
        expect.expect("quit");
        new GtpStatistics(gtp, program, sgfFiles, output, size, commands,
                          beginCommands, finalCommands, force, allowSetup, min,
                          max, backward);
        assertTrue(expect.isExpectQueueEmpty());
    }
}
