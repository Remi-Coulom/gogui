//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.util.*;
import go.Color;

//----------------------------------------------------------------------------

public class TimeControl
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    /** Time control.
        @param time Specification string for time or null.
        The format of the specification string is "minutes[+minutes/moves]"
    */
    public TimeControl()
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

    public long getPreByoyomi()
    {
        return m_preByoyomi / 60000L;
    }

    public boolean getUseByoyomi()
    {
        return m_useByoyomi;
    }

    public String getTimeString(go.Color c)
    {
        TimeRecord timeRecord = getRecord(c);
        long time = timeRecord.m_time;
        if (m_toMove == c)
            time += new Date().getTime() - m_startMoveTime;
        if (m_initialized)
        {
            if (timeRecord.m_isInByoyomi)
                time = m_byoyomi - time;
            else
                time = m_preByoyomi - time;
        }
        int movesLeft = -1;
        if (m_initialized && timeRecord.m_isInByoyomi)
        {
            movesLeft = timeRecord.m_movesLeft;
        }
        return getTimeString((float)(time / 1000L), movesLeft);
    }

    /** If not in byoyomi movesLeft < 0 */
    public static String getTimeString(float timeLeft, int movesLeft)
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
        m_toMove = Color.EMPTY;
    }

    public boolean isInitialized()
    {
        return m_initialized;
    }

    public boolean isRunning()
    {
        return (m_toMove != go.Color.EMPTY);
    }

    public boolean lostOnTime(go.Color c)
    {
        if (! m_initialized)
            return false;
        TimeRecord timeRecord = getRecord(c);
        long time = timeRecord.m_time;
        if (! m_useByoyomi)
            return (time > m_preByoyomi);
        else
            return (timeRecord.m_byoyomiExceeded);
    }

    public void reset()
    {
        reset(Color.BLACK);
        reset(Color.WHITE);
        m_toMove = Color.EMPTY;
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
                    throw new Error("Invalid time specification.");
                byoyomi = Long.parseLong(s.substring(idx + 1, idx2)) * 60000L;
                byoyomiMoves = Integer.parseInt(s.substring(idx2 + 1));
            }
        }
        catch (NumberFormatException e)
        {
            throw new Error("Invalid time specification.");
        }
        if (preByoyomi < 0)
            throw new Error("Pre byoyomi time must be positive.");
        if (byoyomi < 0)
            throw new Error("Byoyomi time must be positive.");
        if (byoyomiMoves <= 0)
            throw new Error("Moves for byoyomi time must be greater 0.");
        m_useByoyomi = useByoyomi;
        m_preByoyomi = preByoyomi;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
        m_initialized = true;
    }

    public void startMove(go.Color c)
    {
        if  (m_toMove != Color.EMPTY)
            stopMove();
        m_toMove = c;
        m_startMoveTime = new Date().getTime();
    }

    public void stopMove()
    {
        if (m_toMove == Color.EMPTY)
            return;
        TimeRecord timeRecord = getRecord(m_toMove);
        long time = new Date().getTime() - m_startMoveTime;
        timeRecord.m_time += time;
        if (m_useByoyomi)
        {
            if (! timeRecord.m_isInByoyomi)
            {
                if (timeRecord.m_time > m_preByoyomi)
                {
                    timeRecord.m_isInByoyomi = true;
                    timeRecord.m_time -= m_preByoyomi;
                    assert(m_byoyomiMoves > 0);
                    timeRecord.m_movesLeft = m_byoyomiMoves;
                }
            }
            if (timeRecord.m_isInByoyomi)
            {
                if (timeRecord.m_time > m_byoyomi)
                    timeRecord.m_byoyomiExceeded = true;
                assert(timeRecord.m_movesLeft > 0);
                --timeRecord.m_movesLeft;
                if (timeRecord.m_movesLeft == 0)
                {
                    timeRecord.m_time = 0;
                    assert(m_byoyomiMoves > 0);
                    timeRecord.m_movesLeft = m_byoyomiMoves;
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

    private go.Color m_toMove = Color.EMPTY;

    private TimeRecord m_timeRecordBlack = new TimeRecord();

    private TimeRecord m_timeRecordWhite = new TimeRecord();

    private TimeRecord getRecord(go.Color c)
    {
        if (c == Color.BLACK)
            return m_timeRecordBlack;
        else
            return m_timeRecordWhite;
    }

    private void reset(go.Color c)
    {
        TimeRecord timeRecord = getRecord(c);
        timeRecord.m_time = 0;
        timeRecord.m_movesLeft = 0;
        timeRecord.m_isInByoyomi = false;
        timeRecord.m_byoyomiExceeded = false;
    }
}

//----------------------------------------------------------------------------
