//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.text.*;
import java.util.*;
import go.Color;

//-----------------------------------------------------------------------------

class TimeControl
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
    }

    public String getTimeString(go.Color c)
    {
        TimeRecord timeRecord = getRecord(c);
        long time = timeRecord.m_time;
        if (m_toMove == c)
            time += new Date().getTime() - m_startMoveTime;
        if (m_loseOnTime)
        {
            if (timeRecord.m_isInByoyomi)
                time = m_byoyomi - time;
            else
                time = m_preByoyomi - time;
        }
        m_buffer.setLength(0);
        if (time < 0)
        {
            m_buffer.append('-');
            time *= -1;
        }
        time /= 1000;
        long hours = time / 3600;
        time %= 3600;
        long minutes = time / 60;
        time %= 60;
        long seconds = time;
        if (hours > 0)
        {
            m_buffer.append(hours);
            m_buffer.append(":");
        }
        if (minutes >= 10)
            m_buffer.append(minutes);
        else
        {
            m_buffer.append('0');
            m_buffer.append(minutes);
        }
        m_buffer.append(":");
        if (seconds >= 10)
            m_buffer.append(seconds);
        else
        {
            m_buffer.append('0');
            m_buffer.append(seconds);
        }
        if (m_loseOnTime && timeRecord.m_isInByoyomi)
        {
            m_buffer.append('/');
            m_buffer.append(timeRecord.m_movesLeft);
        }
        return m_buffer.toString();
    }

    public boolean lostOnTime(go.Color c)
    {
        if (! m_loseOnTime)
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
        m_loseOnTime = true;
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

    private class TimeRecord
    {
        public boolean m_isInByoyomi;
        public boolean m_byoyomiExceeded;
        public int m_movesLeft;
        public long m_time;
    }

    private boolean m_loseOnTime;

    private boolean m_useByoyomi;

    private int m_byoyomiMoves;

    private long m_startMoveTime;

    private long m_preByoyomi;

    private long m_byoyomi;

    private go.Color m_toMove = Color.EMPTY;

    private StringBuffer m_buffer = new StringBuffer(8);

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

//-----------------------------------------------------------------------------
