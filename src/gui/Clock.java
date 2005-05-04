//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.util.Date;
import game.TimeSettings;
import go.Color;

//----------------------------------------------------------------------------

/** Time control for a Go game.
    The time unit is milliseconds.
*/
public class Clock
{
    public Clock()
    {
        reset();
    }

    /** Get moves left.
        Requires: getUseByoyomi() && isInByoyomi(color)
    */
    public int getMovesLeft(Color color)
    {
        assert(getUseByoyomi() && isInByoyomi(color));
        return getRecord(color).m_movesLeft;
    }

    /** Get time left.
        Requires: isInitialized()
    */
    public long getTimeLeft(Color color)
    {
        assert(isInitialized());
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (! getUseByoyomi())
            return (getPreByoyomi() - time);
        else
            return (getByoyomi() - time);
    }

    public String getTimeString(Color color)
    {
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (m_toMove == color)
            time += new Date().getTime() - m_startMoveTime;
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
        return getTimeString((double)(time / 1000L), movesLeft);
    }

    /** If not in byoyomi movesLeft < 0. */
    public static String getTimeString(double timeLeft, int movesLeft)
    {
        StringBuffer buffer = new StringBuffer(8);
        long time = (long)timeLeft;
        if (time < 0)
        {
            buffer.append('-');
            time *= -1;
        }
        long hours = time / 3600;
        time %= 3600;
        long minutes = time / 60;
        time %= 60;
        long seconds = time;
        if (hours > 0)
        {
            buffer.append(hours);
            buffer.append(":");
        }
        if (minutes >= 10)
            buffer.append(minutes);
        else
        {
            buffer.append('0');
            buffer.append(minutes);
        }
        buffer.append(":");
        if (seconds >= 10)
            buffer.append(seconds);
        else
        {
            buffer.append('0');
            buffer.append(seconds);
        }
        if (movesLeft >= 0)
        {
            buffer.append('/');
            buffer.append(movesLeft);
        }
        return buffer.toString();
    }

    public void halt()
    {
        if (m_toMove == Color.EMPTY)
            return;
        TimeRecord record = getRecord(m_toMove);
        long time = new Date().getTime() - m_startMoveTime;
        record.m_time += time;
        m_toMove = Color.EMPTY;
    }

    public boolean isInitialized()
    {
        return (m_timeSettings != null);
    }

    public boolean isInByoyomi(Color color)
    {
        return getUseByoyomi() && getRecord(color).m_isInByoyomi;
    }

    public boolean isRunning()
    {
        return (m_toMove != Color.EMPTY);
    }

    public boolean lostOnTime(Color color)
    {
        if (! isInitialized())
            return false;
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (! getUseByoyomi())
            return (time > getPreByoyomi());
        else
            return (record.m_byoyomiExceeded);
    }

    public void reset()
    {
        reset(Color.BLACK);
        reset(Color.WHITE);
        m_toMove = Color.EMPTY;
    }

    public void reset(Color color)
    {
        TimeRecord timeRecord = getRecord(color);
        timeRecord.m_time = 0;
        timeRecord.m_movesLeft = 0;
        timeRecord.m_isInByoyomi = false;
        timeRecord.m_byoyomiExceeded = false;
    }

    public void setTimeSettings(TimeSettings settings)
    {
        m_timeSettings = settings;
    }

    /** Set time left.
        @param color Color to set the time for.
        @param time New value for time left.
        @param movesLeft -1, if not in byoyomi.
    */
    public void setTimeLeft(Color color, long time, int movesLeft)
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
        if (m_toMove != Color.EMPTY)
            startMove(m_toMove);
    }

    public void startMove(Color color)
    {
        if  (m_toMove != Color.EMPTY)
            stopMove();
        m_toMove = color;
        m_startMoveTime = new Date().getTime();
    }

    public void stopMove()
    {
        if (m_toMove == Color.EMPTY)
            return;
        TimeRecord record = getRecord(m_toMove);
        long time = new Date().getTime() - m_startMoveTime;
        record.m_time += time;
        if (isInitialized() && getUseByoyomi())
        {
            if (! record.m_isInByoyomi)
            {
                if (record.m_time > getPreByoyomi())
                {
                    record.m_isInByoyomi = true;
                    record.m_time -= getPreByoyomi();
                    assert(getByoyomiMoves() > 0);
                    record.m_movesLeft = getByoyomiMoves();
                }
            }
            if (record.m_isInByoyomi)
            {
                if (record.m_time > getByoyomi())
                    record.m_byoyomiExceeded = true;
                assert(record.m_movesLeft > 0);
                --record.m_movesLeft;
                if (record.m_movesLeft == 0)
                {
                    record.m_time = 0;
                    assert(getByoyomiMoves() > 0);
                    record.m_movesLeft = getByoyomiMoves();
                }
            }
        }
        m_toMove = Color.EMPTY;
    }    

    private static class TimeRecord
    {
        public boolean m_isInByoyomi;

        public boolean m_byoyomiExceeded;

        public int m_movesLeft;

        public long m_time;
    }

    private long m_startMoveTime;

    private Color m_toMove = Color.EMPTY;

    private TimeRecord m_timeRecordBlack = new TimeRecord();

    private TimeRecord m_timeRecordWhite = new TimeRecord();

    private TimeSettings m_timeSettings;

    private TimeRecord getRecord(Color c)
    {
        if (c == Color.BLACK)
            return m_timeRecordBlack;
        else
            return m_timeRecordWhite;
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
}

//----------------------------------------------------------------------------
