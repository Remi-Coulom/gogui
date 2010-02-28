// GtpSynchronizerTest.java

package net.sf.gogui.gtp;

import java.io.IOException;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;

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
        play(BLACK, 3, 4);
        expect("play B D5", "");
        synchronize();
        assertExpectQueueEmpty();
        play(BLACK, 4, 4);
        play(WHITE, 5, 5);
        play(BLACK, null);
        expect("play B E5", "");
        expect("play W F6", "");
        expect("play B PASS", "");
        synchronize();
        assertExpectQueueEmpty();
        undo();
        expect("undo", "");
        synchronize();
        assertExpectQueueEmpty();
        undo(2);
        play(WHITE, 5, 5);
        play(BLACK, 4, 4);
        expect("undo", "");
        expect("undo", "");
        expect("play W F6", "");
        expect("play B E5", "");
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
        play(BLACK, 3, 4);
        expect("play B D5", "");
        synchronize();
        assertExpectQueueEmpty();
        play(BLACK, 4, 4);
        play(WHITE, 5, 5);
        play(BLACK, null);
        expect("play W PASS", "");
        expect("play B E5", "");
        expect("play W F6", "");
        expect("play B PASS", "");
        synchronize();
        assertExpectQueueEmpty();
        undo();
        expect("undo", "");
        synchronize();
        assertExpectQueueEmpty();
        undo(2);
        play(WHITE, 5, 5);
        play(BLACK, 4, 4);
        expect("undo", "");
        expect("undo", "");
        expect("undo", "");
        expect("play W F6", "");
        expect("play B E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test that set_free_handicap command is used if supported by the
        engine. */
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
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("set_free_handicap D5 E5", "");
        synchronize();
        assertExpectQueueEmpty();

        // Playing a move should not trigger a re-transmission
        play(WHITE, 5, 5);
        expect("play W F6", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test that handicap stones are transmitted as moves if neither
        set_free_handicap nor gogui-setup is supported. */
    public void testSetupHandicapAsMoves() throws GtpError
    {
        createSynchronizer();

        expect("list_commands", "");
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
        expect("play B D5", "");
        expect("play B E5", "");
        synchronize();
        assertExpectQueueEmpty();

        // Playing a move should not trigger a re-transmission
        play(WHITE, 5, 5);
        expect("play W F6", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    /** Test that handicap stones are transmitted using gogui-setup if
        set_free_handicap is not supported, but gogui-setup is supported. */
    public void testSetupHandicapAsSetup() throws GtpError
    {
        createSynchronizer();

        expect("list_commands", "gogui-setup");
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
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("gogui-setup B D5 B E5", "");
        synchronize();
        assertExpectQueueEmpty();

        // Playing a move should not trigger a re-transmission
        play(WHITE, 5, 5);
        expect("play W F6", "");
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
        setup(black, white, BLACK);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("gogui-setup B D5 B E5 W F6", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    public void testSetupWithMoves1() throws GtpError
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
        setup(black, null, BLACK);
        expect("play B D5", "");
        expect("play B E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    public void testSetupWithMoves2() throws GtpError
    {
        createSynchronizer();
        assertExpectQueueEmpty();
        // synchronizer should send moves of color to move first, such
        // that the right color is to move after the moves (works only
        // if there are setup stones by both colors)
        PointList black = new PointList();
        black.add(GoPoint.get(4, 4));
        PointList white = new PointList();
        white.add(GoPoint.get(5, 5));
        setup(black, white, BLACK);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("play B E5", "");
        expect("play W F6", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    public void testSetupWithMoves3() throws GtpError
    {
        createSynchronizer();
        assertExpectQueueEmpty();
        // synchronizer should send moves of color to move first, such
        // that the right color is to move after the moves (works only
        // if there are setup stones by both colors)
        PointList black = new PointList();
        black.add(GoPoint.get(4, 4));
        PointList white = new PointList();
        white.add(GoPoint.get(5, 5));
        setup(black, white, WHITE);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("play W F6", "");
        expect("play B E5", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    public void testSetupPlayer() throws GtpError
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
        setup(new PointList(GoPoint.get(1, 1)), null, WHITE);
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("gogui-setup B B2", "");
        expect("gogui-setup_player W", "");
        synchronize();
        assertExpectQueueEmpty();
    }

    public void testTimeSettings() throws GtpError
    {
        createSynchronizer();
        expect("list_commands",
               "time_settings\n");
        m_gtp.querySupportedCommands();
        assertExpectQueueEmpty();
        expect("boardsize 19", "");
        expect("clear_board", "");
        synchronize();
        assertExpectQueueEmpty();
        expect("time_settings 1800 0 0", "");
        m_synchronizer.synchronize(m_board, null, new TimeSettings(1800000));
        assertExpectQueueEmpty();
        expect("time_settings 800 0 0", "");
        m_synchronizer.synchronize(m_board, null, new TimeSettings(800000));
        assertExpectQueueEmpty();
        expect("time_settings 0 1 0", "");
        m_synchronizer.synchronize(m_board, null, null);
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

    private void setup(ConstPointList black, ConstPointList white,
                       GoColor player)
    {
        m_board.setup(black, white, player);
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
