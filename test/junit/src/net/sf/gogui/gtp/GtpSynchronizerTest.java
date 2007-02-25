//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.IOException;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;
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
        createSynchronizer();
        expect("list_commands", "undo");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoColor.BLACK, 3, 4);
        expect("play b D5", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoColor.BLACK, 4, 4);
        play(GoColor.WHITE, 5, 5);
        play(GoColor.BLACK, null);
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
        play(GoColor.WHITE, 5, 5);
        play(GoColor.BLACK, 4, 4);
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
        expect("list_commands", "undo");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoColor.BLACK, 3, 4);
        expect("play b D5", "");
        synchronize();
        assertExpectQueueEmpty();
        play(GoColor.BLACK, 4, 4);
        play(GoColor.WHITE, 5, 5);
        play(GoColor.BLACK, null);
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
        play(GoColor.WHITE, 5, 5);
        play(GoColor.BLACK, 4, 4);
        expect("undo", "");
        expect("undo", "");
        expect("undo", "");
        expect("play w F6", "");
        expect("play b E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test that set_free_handicap command is used if supported by the
        engine.
    */
    public void testSetupHandicap() throws GtpError
    {
        createSynchronizer();

        expect("list_commands", "set_free_handicap");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();

        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();

        PointList black = new PointList();
        black.add(GoPoint.get(3, 4));
        black.add(GoPoint.get(4, 4));

        setupHandicap(black);
        // Changed handicap setup should trigger a re-transmission from scratch
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("set_free_handicap D5 E5", "");
        synchronize();
        assertExpectQueueEmpty();

        undo();
        // There is no GTP command to undo a handicap placement
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();

        setupHandicap(black);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("set_free_handicap D5 E5", "");
        synchronize();
        assertExpectQueueEmpty();

        // Playing a move should not trigger a re-transmission
        play(GoColor.WHITE, 5, 5);
        expect("play w F6", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test that gogui-setup command is used if supported by the engine. */
    public void testSetup() throws GtpError
    {
        createSynchronizer();
        expect("list_commands",
               "gogui-setup\n");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        PointList black = new PointList();
        black.add(GoPoint.get(3, 4));
        black.add(GoPoint.get(4, 4));
        PointList white = new PointList();
        white.add(GoPoint.get(5, 5));
        setup(black, white);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("gogui-setup b D5 b E5 w F6", "");
        synchronize();
        assertExpectQueueEmpty();
        undo();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test setup with removed stones, if engine doesn't support
        gogui-setup.
    */
    public void testSetupEmptyAsMoves() throws GtpError
    {
        createSynchronizer();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        PointList black = new PointList();
        black.add(GoPoint.get(3, 4));
        black.add(GoPoint.get(4, 4));
        setup(black, null);
        expect("play b D5", "");
        expect("play b E5", "");
        synchronize();
        assertExpectQueueEmpty();
        PointList empty = new PointList();
        empty.add(GoPoint.get(3, 4));
        setup(null, null, empty);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("play b E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test setup that changes only color to play.
        Should only send gogui-setup_player (no gogui-setup with no arguments).
    */
    public void testSetupOnlyPlayer() throws GtpError
    {
        createSynchronizer();
        expect("list_commands",
               "gogui-setup\n" +
               "gogui-setup_player\n");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        setup(null, null, null, GoColor.WHITE);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("gogui-setup_player w", "");
        synchronize();
        assertExpectQueueEmpty();
        undo();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    private Board m_board;

    private GtpExpectEngine m_expect;

    private GtpClientBase m_gtp;

    private GtpSynchronizer m_synchronizer;

    private void assertExpectQueueEmpty()
    {
        assertTrue("Command not sent: " + m_expect.getNextExpectedCommand(),
                   m_expect.isExpectQueueEmpty());
    }

    private void createSynchronizer()
    {
        createSynchronizer(false);
    }

    private void createSynchronizer(boolean fillPasses)
    {
        m_synchronizer = new GtpSynchronizer(m_gtp, null, fillPasses);
    }

    private void expect(String command, String response)
    {
        m_expect.expect(command, response);
    }

    private void play(GoColor c, GoPoint p)
    {
        m_board.play(c, p);
    }

    private void play(GoColor c, int x, int y)
    {
        m_board.play(c, GoPoint.get(x, y));
    }

    private void setup(ConstPointList black, ConstPointList white)
    {
        m_board.setup(black, white);
    }

    private void setup(ConstPointList black, ConstPointList white,
                       ConstPointList empty)
    {
        m_board.setup(black, white, empty);
    }

    private void setup(ConstPointList black, ConstPointList white,
                       ConstPointList empty, GoColor player)
    {
        m_board.setup(black, white, empty, player);
    }

    private void setupHandicap(ConstPointList black)
    {
        m_board.setupHandicap(black);
    }

    private void synchronize() throws GtpError
    {
        m_synchronizer.synchronize(m_board, null, null);
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
