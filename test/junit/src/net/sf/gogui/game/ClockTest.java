// ClockTest.java

package net.sf.gogui.game;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

public final class ClockTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(ClockTest.class);
    }

    public void setUp()
    {
        m_timeSource = new TestTimeSource();
        m_clock = new Clock(m_timeSource);
        setTime(0);
    }

    public void testInit()
    {
        m_clock.setTimeSettings(new TimeSettings(30000, 10000, 5));
        m_clock.reset();
        assertTrue(! m_clock.isInByoyomi(BLACK));
        assertTrue(! m_clock.isInByoyomi(WHITE));
        assertEquals(30000, m_clock.getTimeLeft(BLACK));
        assertEquals(30000, m_clock.getTimeLeft(WHITE));
    }

    public void testInitOnlyPreByoyomi()
    {
        m_clock.setTimeSettings(new TimeSettings(30000));
        m_clock.reset();
        assertEquals(30000, m_clock.getTimeLeft(BLACK));
        assertEquals(30000, m_clock.getTimeLeft(WHITE));
    }

    public void testInitOnlyByoyomi()
    {
        m_clock.setTimeSettings(new TimeSettings(0, 10000, 5));
        m_clock.reset();
        assertTrue(m_clock.isInByoyomi(BLACK));
        assertTrue(m_clock.isInByoyomi(WHITE));
        assertEquals(10000, m_clock.getTimeLeft(BLACK));
        assertEquals(10000, m_clock.getTimeLeft(WHITE));
        assertEquals(5, m_clock.getMovesLeft(BLACK));
        assertEquals(5, m_clock.getMovesLeft(WHITE));
    }

    public void testParseTimeString()
    {
        assertEquals(24000L, Clock.parseTimeString("24"));
        assertEquals(144000L, Clock.parseTimeString("02:24"));
        assertEquals(3744000L, Clock.parseTimeString("1:02:24"));
        assertEquals(3603744000L, Clock.parseTimeString("1001:02:24"));
        assertEquals(-1L, Clock.parseTimeString(""));
        assertEquals(-1L, Clock.parseTimeString("foo"));
        assertEquals(-1L, Clock.parseTimeString("-1"));
        assertEquals(-1L, Clock.parseTimeString("02.24"));
    }

    public void testResume()
    {
        m_clock.setTimeSettings(new TimeSettings(10000));
        m_clock.startMove(BLACK);
        setTime(1000);
        m_clock.halt();
        setTime(2000);
        m_clock.resume();
        setTime(2500);
        m_clock.stopMove();
        assertEquals(8500, m_clock.getTimeLeft(BLACK));
    }

    /** Test that move time is discarded if move is started twice.
        According to the specification of Clock#startMove, the time for
        the current move should be discarded is startMove is called and clock
        is already running. */
    public void testStartMoveIfRunning()
    {
        m_clock.setTimeSettings(new TimeSettings(10000));
        m_clock.startMove(BLACK);
        setTime(1000);
        m_clock.startMove(BLACK);
        assertEquals(10000, m_clock.getTimeLeft(BLACK));
    }

    private static final class TestTimeSource
        implements Clock.TimeSource
    {
        public long currentTimeMillis()
        {
            return m_currentTime;
        }

        public void setTime(long millis)
        {
            m_currentTime = millis;
        }

        private long m_currentTime;
    }

    private TestTimeSource m_timeSource;

    private Clock m_clock;

    private void setTime(long millis)
    {
        m_timeSource.setTime(millis);
    }
}
