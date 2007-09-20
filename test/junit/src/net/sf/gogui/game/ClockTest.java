//----------------------------------------------------------------------------
// ClockTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
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

    /** Test that move time is discarded if move is started twice.
        According to the specification of Clock#startMove, the time for
        the current move should be discarded is startMove is called and clock
        is already running.
    */
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
