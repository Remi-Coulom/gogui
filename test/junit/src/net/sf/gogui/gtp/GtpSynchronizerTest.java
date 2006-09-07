//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.IOException;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
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

    public void testBasic() throws IOException, GtpError
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpEngineConnection connection = new GtpEngineConnection(expect);
        GtpClient gtp = connection.getGtpClient();
        expect.expect("list_commands", "undo");
        gtp.querySupportedCommands();
        assertTrue(expect.isExpectQueueEmpty());
        GtpSynchronizer synchronizer = new GtpSynchronizer(gtp, null);
        Board board = new Board(19);
        expect.expect("boardsize 19", "");
        expect.expect("clear_board", "");
        synchronizer.synchronize(board);
        assertTrue(expect.isExpectQueueEmpty());
        board.play(GoPoint.get(3, 4), GoColor.BLACK);
        expect.expect("play b D5", "");
        synchronizer.synchronize(board);
        assertTrue(expect.isExpectQueueEmpty());
        board.play(GoPoint.get(4, 4), GoColor.BLACK);
        board.play(GoPoint.get(5, 5), GoColor.WHITE);
        board.play(null, GoColor.BLACK);
        expect.expect("play b E5", "");
        expect.expect("play w F6", "");
        expect.expect("play b PASS", "");
        synchronizer.synchronize(board);
        assertTrue(expect.isExpectQueueEmpty());
        board.undo();
        expect.expect("undo", "");
        synchronizer.synchronize(board);
        assertTrue(expect.isExpectQueueEmpty());
        board.undo(2);
        board.play(GoPoint.get(5, 5), GoColor.WHITE);
        board.play(GoPoint.get(4, 4), GoColor.BLACK);
        expect.expect("undo", "");
        expect.expect("undo", "");
        expect.expect("play w F6", "");
        expect.expect("play b E5", "");
        synchronizer.synchronize(board);
        assertTrue(expect.isExpectQueueEmpty());
    }
}

//----------------------------------------------------------------------------
