//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.util.Date;
import go.Color;

//----------------------------------------------------------------------------

/** Time control for a Go game.
    The time unit is milliseconds.
*/
public class Clock
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    public Clock()
    {
        reset();
        m_initialized = false;
    }

    /** Get byoyomi time.
        Requires: getUseByoyomi()
    */
    public long getByoyomi()
    {
        return m_byoyomi / 60000L;
    }

    /** Get byoyomi moves.
        Requires: getUseByoyomi()
    */
    public long getByoyomiMoves()
    {
        return m_byoyomiMoves;
    }

    /** Get moves left.
        Requires: getUseByoyomi() && isInByoyomi(color)
    */
    public int getMovesLeft(Color color)
    {
        assert(getUseByoyomi() && isInByoyomi(color));
        return getRecord(color).m_movesLeft;
    }

    public long getPreByoyomi()
    {
        return m_preByoyomi / 60000L;
    }

    /** Get time left.
        Requires: isInitialized()
    */
    public long getTimeLeft(Color color)
    {
        assert(m_initialized);
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (! m_useByoyomi)
            return (m_preByoyomi - time);
        else
            return (m_byoyomi - time);
    }

    public String getTimeString(Color c)
    {
        TimeRecord record = getRecord(c);
        long time = record.m_time;
        if (m_toMove == c)
            time += new Date().getTime() - m_startMoveTime;
        if (m_initialized)
        {
            if (record.m_isInByoyomi)
                time = m_byoyomi - time;
            else
                time = m_preByoyomi - time;
        }
        int movesLeft = -1;
        if (m_initialized && record.m_isInByoyomi)
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

    public boolean getUseByoyomi()
    {
        return m_useByoyomi;
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
        return m_initialized;
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
        if (! m_initialized)
            return false;
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (! m_useByoyomi)
            return (time > m_preByoyomi);
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

    /** Set time.
        @param s Time specification in format minutes[+minutes/moves]
    */
    public void setTime(String s) throws Error
    {
        reset();
        boolean useByoyomi = false;
        long preByoyomi = 0;
        long byoyomi = 0;
        int byoyomiMoves = 1;
        try
        {
            int idx = s.indexOf('+');
            if (idx < 0)
            {
                preByoyomi = Long.parseLong(s) * 60000L;
            }
            else
            {
                useByoyomi = true;
                preByoyomi = Long.parseLong(s.substring(0, idx)) * 60000L;
                int idx2 = s.indexOf('/');
                if (idx2 <= idx)
                    throw new Error("Invalid time specification");
                byoyomi = Long.parseLong(s.substring(idx + 1, idx2)) * 60000L;
                byoyomiMoves = Integer.parseInt(s.substring(idx2 + 1));
            }
        }
        catch (NumberFormatException e)
        {
            throw new Error("Invalid time specification");
        }
        if (preByoyomi < 0)
            throw new Error("Pre byoyomi time must be positive");
        if (byoyomi < 0)
            throw new Error("Byoyomi time must be positive");
        if (byoyomiMoves <= 0)
            throw new Error("Moves for byoyomi time must be greater 0");
        m_useByoyomi = useByoyomi;
        m_preByoyomi = preByoyomi;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
        m_initialized = true;
    }

    /** Set time left.
        @param movesLeft -1, if not in byoyomi.
    */
    public void setTimeLeft(Color color, long time, int movesLeft)
    {
        halt();
        TimeRecord record = getRecord(color);
        record.m_isInByoyomi = (movesLeft >= 0);
        if (record.m_isInByoyomi)
        {
            record.m_time = m_byoyomi - time;
            record.m_movesLeft = movesLeft;
            record.m_byoyomiExceeded = time > 0;
        }
        else
        {
            record.m_time = m_preByoyomi - time;
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
        if (m_useByoyomi)
        {
            if (! record.m_isInByoyomi)
            {
                if (record.m_time > m_preByoyomi)
                {
                    record.m_isInByoyomi = true;
                    record.m_time -= m_preByoyomi;
                    assert(m_byoyomiMoves > 0);
                    record.m_movesLeft = m_byoyomiMoves;
                }
            }
            if (record.m_isInByoyomi)
            {
                if (record.m_time > m_byoyomi)
                    record.m_byoyomiExceeded = true;
                assert(record.m_movesLeft > 0);
                --record.m_movesLeft;
                if (record.m_movesLeft == 0)
                {
                    record.m_time = 0;
                    assert(m_byoyomiMoves > 0);
                    record.m_movesLeft = m_byoyomiMoves;
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

    private boolean m_initialized;

    private boolean m_useByoyomi;

    private int m_byoyomiMoves;

    private long m_startMoveTime;

    private long m_preByoyomi;

    private long m_byoyomi;

    private Color m_toMove = Color.EMPTY;

    private TimeRecord m_timeRecordBlack = new TimeRecord();

    private TimeRecord m_timeRecordWhite = new TimeRecord();

    private TimeRecord getRecord(Color c)
    {
        if (c == Color.BLACK)
            return m_timeRecordBlack;
        else
            return m_timeRecordWhite;
    }
}

//----------------------------------------------------------------------------
