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

    public void setUp() throws IOException, GtpError
    {
        m_expect = new GtpExpectEngine(null);
        m_connection = new GtpEngineConnection(m_expect);
        m_gtp = m_connection.getGtpClient();
        m_board = new Board(19);
        m_synchronizer = new GtpSynchronizer(m_gtp, null);
    }

    public void testBasic() throws GtpError
    {
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

    private Board m_board;

    private GtpExpectEngine m_expect;

    private GtpEngineConnection m_connection;

    private GtpClient m_gtp;

    private GtpSynchronizer m_synchronizer;

    private void assertExpectQueueEmpty()
    {
        assertTrue(m_expect.isExpectQueueEmpty());
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

//----------------------------------------------------------------------------
