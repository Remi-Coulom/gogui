//----------------------------------------------------------------------------
// Clock.java
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.TimerTask;
import java.util.Timer;
import net.sf.gogui.go.BlackWhiteSet;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.util.StringUtil;

/** Time control for a Go game.
    If the clock is not initialized with Clock.setTimeSettings, the clock
    will count upwards, otherwise the time settings with main and/or
    byoyomi time are used. The time unit is milliseconds.
*/
public final class Clock
    implements ConstClock
{
    /** Provides the time for a clock. */
    public interface TimeSource
    {
        long currentTimeMillis();
    }

    /** Time source using the system time. */
    public static final class SystemTimeSource
        implements TimeSource
    {
        public long currentTimeMillis()
        {
            return System.currentTimeMillis();
        }
    }

    /** Listener to clock changes.
        This function will be called from a different thread at regular
        intervals.
    */
    public interface Listener
    {
        void clockChanged();
    }

    public Clock()
    {
        this(new SystemTimeSource());
    }

    public Clock(TimeSource timeSource)
    {
        m_timeSource = timeSource;
        reset();
    }

    /** Get moves left.
        Requires: getUseByoyomi() and isInByoyomi(color)
    */
    public int getMovesLeft(GoColor color)
    {
        assert getUseByoyomi() && isInByoyomi(color);
        return getRecord(color).m_movesLeft;
    }

    /** Get time left.
        Requires: isInitialized()
    */
    public long getTimeLeft(GoColor color)
    {
        assert isInitialized();
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (getUseByoyomi())
            return (getByoyomi() - time);
        else
            return (getPreByoyomi() - time);
    }

    public TimeSettings getTimeSettings()
    {
        return m_timeSettings;
    }

    public String getTimeString(GoColor color)
    {
        assert color.isBlackWhite();
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (color.equals(m_toMove))
            time += currentTimeMillis() - m_startMoveTime;
        if (isInitialized())
        {
            if (record.m_isInByoyomi)
                time = getByoyomi() - time;
            else
                time = getPreByoyomi() - time;
        }
        int movesLeft = -1;
        if (isInitialized() && record.m_isInByoyomi)
        {
            movesLeft = record.m_movesLeft;
        }
        // Round time to seconds
        time = time / 1000L;
        return getTimeString((double)time, movesLeft);
    }

    /** If not in byoyomi movesLeft &lt; 0. */
    public static String getTimeString(double timeLeft, int movesLeft)
    {
        StringBuilder buffer = new StringBuilder(8);
        buffer.append(StringUtil.formatTime((long)timeLeft));
        if (movesLeft >= 0)
        {
            buffer.append('/');
            buffer.append(movesLeft);
        }
        return buffer.toString();
    }

    /** Return color the clock is currently measuring the time for.
        Returns null, if clock is between a #stopMove and #startMove.
    */
    public GoColor getToMove()
    {
        return m_toMove;
    }

    public void halt()
    {
        if (m_toMove == null)
            return;
        TimeRecord record = getRecord(m_toMove);
        long time = currentTimeMillis() - m_startMoveTime;
        record.m_time += time;
        m_toMove = null;
        updateListener();
        stopTimer();
    }

    public boolean isInitialized()
    {
        return (m_timeSettings != null);
    }

    public boolean isInByoyomi(GoColor color)
    {
        return getUseByoyomi() && getRecord(color).m_isInByoyomi;
    }

    public boolean isRunning()
    {
        return (m_toMove != null);
    }

    public boolean lostOnTime(GoColor color)
    {
        if (! isInitialized())
            return false;
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (getUseByoyomi())
            return record.m_byoyomiExceeded;
        else
            return (time > getPreByoyomi());
    }

    public void reset()
    {
        reset(BLACK);
        reset(WHITE);
        m_toMove = null;
        updateListener();
    }

    public void reset(GoColor color)
    {
        TimeRecord timeRecord = getRecord(color);
        timeRecord.m_time = 0;
        timeRecord.m_movesLeft = 0;
        timeRecord.m_isInByoyomi = false;
        timeRecord.m_byoyomiExceeded = false;
        updateListener();
    }

    /** Resume clock, if it was halted during a player's move time. */
    public void resume()
    {
        if (m_toMove == null)
            return;
        startTimer();
    }

    /** Register listener for clock changes.
        Only one listener supported at the moment.
        If the clock has a listener, the clock should be stopped with halt()
        if it is no longer used, otherwise the timer thread can keep an
        application from terminating.
    */
    public void setListener(Listener listener)
    {
        m_listener = listener;
    }

    /** Set time settings.
        Changing the time settings does not change the current state of the
        clock. The time settings are only used when the clock is reset or
        the next byoyomi period is initialized.
    */
    public void setTimeSettings(TimeSettings settings)
    {
        m_timeSettings = settings;
    }

    /** Set time left.
        @param color Color to set the time for.
        @param time New value for time left.
        @param movesLeft -1, if not in byoyomi.
    */
    public void setTimeLeft(GoColor color, long time, int movesLeft)
    {
        halt();
        TimeRecord record = getRecord(color);
        record.m_isInByoyomi = (movesLeft >= 0);
        if (record.m_isInByoyomi)
        {
            record.m_time = getByoyomi() - time;
            record.m_movesLeft = movesLeft;
            record.m_byoyomiExceeded = time > 0;
        }
        else
        {
            record.m_time = getPreByoyomi() - time;
            record.m_movesLeft = -1;
            record.m_byoyomiExceeded = false;
        }
        if (m_toMove != null)
            startMove(m_toMove);
        updateListener();
    }

    /** Start time for a move.
        If the clock was already running, the passed time for the current move
        is discarded.
    */
    public void startMove(GoColor color)
    {
        assert color.isBlackWhite();
        m_toMove = color;
        m_startMoveTime = currentTimeMillis();
        startTimer();
    }

    /** Stop time for a move.
        If the clock was running, the time for the move is added to the
        total time for the color the clock was running for; otherwise
        this function does nothing.
    */
    public void stopMove()
    {
        if (m_toMove == null)
            return;
        TimeRecord record = getRecord(m_toMove);
        long time = currentTimeMillis() - m_startMoveTime;
        record.m_time += time;
        if (isInitialized() && getUseByoyomi())
        {
            if (! record.m_isInByoyomi
                && record.m_time > getPreByoyomi())
            {
                record.m_isInByoyomi = true;
                record.m_time -= getPreByoyomi();
                assert getByoyomiMoves() > 0;
                record.m_movesLeft = getByoyomiMoves();
            }
            if (record.m_isInByoyomi)
            {
                if (record.m_time > getByoyomi())
                    record.m_byoyomiExceeded = true;
                assert record.m_movesLeft > 0;
                --record.m_movesLeft;
                if (record.m_movesLeft == 0)
                {
                    record.m_time = 0;
                    assert getByoyomiMoves() > 0;
                    record.m_movesLeft = getByoyomiMoves();
                }
            }
        }
        m_toMove = null;
        updateListener();
    }

    private static class TimeRecord
    {
        public boolean m_isInByoyomi;

        public boolean m_byoyomiExceeded;

        public int m_movesLeft;

        public long m_time;
    }

    private long m_startMoveTime;

    private GoColor m_toMove;

    private final BlackWhiteSet<TimeRecord> m_timeRecord
        = new BlackWhiteSet<TimeRecord>(new TimeRecord(), new TimeRecord());

    private TimeSettings m_timeSettings;

    private Listener m_listener;

    private Timer m_timer;

    private final TimeSource m_timeSource;

    private long currentTimeMillis()
    {
        return m_timeSource.currentTimeMillis();
    }

    private TimeRecord getRecord(GoColor c)
    {
        return m_timeRecord.get(c);
    }

    private long getByoyomi()
    {
        return m_timeSettings.getByoyomi();
    }

    private int getByoyomiMoves()
    {
        return m_timeSettings.getByoyomiMoves();
    }

    private long getPreByoyomi()
    {
        return m_timeSettings.getPreByoyomi();
    }

    private boolean getUseByoyomi()
    {
        return m_timeSettings.getUseByoyomi();
    }

    private void startTimer()
    {
        if (m_timer == null && m_listener != null)
        {
            m_timer = new Timer();
            TimerTask task = new TimerTask() {
                    public void run() {
                        updateListener();
                    }
                };
            m_timer.scheduleAtFixedRate(task, 1000, 1000);
        }
    }

    private void stopTimer()
    {
        if (m_timer != null)
        {
            m_timer.cancel();
            m_timer = null;
        }
    }

    private void updateListener()
    {
        if (m_listener != null)
            m_listener.clockChanged();
    }
}
