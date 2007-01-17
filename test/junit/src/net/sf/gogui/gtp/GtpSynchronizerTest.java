//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.IOException;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpExpectEngine;

public final class GtpSynchronizerTest
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

    public void setUp() throws IOException, GtpError
    {
        m_expect = new GtpExpectEngine(null);
        m_gtp = new GtpEngineClient(m_expect);
        m_board = new Board(19);
    }

    public void testBasic() throws GtpError
    {
        createSynchronizer(false);
        expect("list_commands", "undo");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoPoint.get(3, 4), GoColor.BLACK);
        expect("play b D5", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoPoint.get(4, 4), GoColor.BLACK);
        play(GoPoint.get(5, 5), GoColor.WHITE);
        play(null, GoColor.BLACK);
        expect("play b E5", "");
        expect("play w F6", "");
        expect("play b PASS", "");
        synchronize();
        assertExpectQueueEmpty();
        undo();
        expect("undo", "");
        synchronize();
        assertExpectQueueEmpty();
        undo(2);
        play(GoPoint.get(5, 5), GoColor.WHITE);
        play(GoPoint.get(4, 4), GoColor.BLACK);
        expect("undo", "");
        expect("undo", "");
        expect("play w F6", "");
        expect("play b E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    public void testBasicFillPasses() throws GtpError
    {
        createSynchronizer(true);
        m_synchronizer = new GtpSynchronizer(m_gtp, null, true);
        expect("list_commands", "undo");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoPoint.get(3, 4), GoColor.BLACK);
        expect("play b D5", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoPoint.get(4, 4), GoColor.BLACK);
        play(GoPoint.get(5, 5), GoColor.WHITE);
        play(null, GoColor.BLACK);
        expect("play w PASS", "");
        expect("play b E5", "");
        expect("play w F6", "");
        expect("play b PASS", "");
        synchronize();
        assertExpectQueueEmpty();
        undo();
        expect("undo", "");
        synchronize();
        assertExpectQueueEmpty();
        undo(2);
        play(GoPoint.get(5, 5), GoColor.WHITE);
        play(GoPoint.get(4, 4), GoColor.BLACK);
        expect("undo", "");
        expect("undo", "");
        expect("undo", "");
        expect("play w F6", "");
        expect("play b E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    private Board m_board;

    private GtpExpectEngine m_expect;

    private GtpClientBase m_gtp;

    private GtpSynchronizer m_synchronizer;

    private void assertExpectQueueEmpty()
    {
        assertTrue(m_expect.isExpectQueueEmpty());
    }

    private void createSynchronizer(boolean fillPasses)
    {
        m_synchronizer = new GtpSynchronizer(m_gtp, null, fillPasses);
    }

    private void expect(String command, String response)
    {
        m_expect.expect(command, response);
    }

    private void play(GoPoint point, GoColor color)
    {
        m_board.play(point, color);
    }

    private void synchronize() throws GtpError
    {
        m_synchronizer.synchronize(m_board);
    }

    private void undo()
    {
        m_board.undo();
    }

    private void undo(int n)
    {
        m_board.undo(n);
    }
}

