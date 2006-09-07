//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.IOException;
import net.sf.gogui.go.Board;
import net.sf.gogui.gtp.GtpExpectEngine;

//----------------------------------------------------------------------------

public class GtpSynchronizerTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GtpSynchronizerTest.class);
    }

    public void testPlay() throws IOException, GtpError
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpEngineConnection connection = new GtpEngineConnection(expect);
        GtpClient gtp = connection.getGtpClient();
        expect.expect("protocol_version", "2");
        expect.expect("list_commands", "");
        gtp.queryProtocolVersion();
        gtp.querySupportedCommands();
        assertTrue(expect.isExpectQueueEmpty());
        GtpSynchronizer synchronizer = new GtpSynchronizer(gtp, null);
        Board board = new Board(19);
        expect.expect("boardsize 19", "");
        expect.expect("clear_board", "");
        synchronizer.synchronize(board);
        assertTrue(expect.isExpectQueueEmpty());
    }
}

//----------------------------------------------------------------------------
