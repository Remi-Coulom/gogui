// TimeSettings.java

package net.sf.gogui.game;

import net.sf.gogui.util.ErrorMessage;

/** Time settings.
    Time settings consist of a base time for the game and an optional
    overtime (Canadian byoyomi) for overtime periods. Overtime periods also
    have a number of moves assigned, which need to be played during an
    overtime period. The base time can be zero. If no overtime periods are
    used, the whole game must be finished in the base time.
    This class is immutable. */
public final class TimeSettings
{
    /** Construct with total time for game.
        @param totalTime Total time for game in milliseconds. */
    public TimeSettings(long totalTime)
    {
        assert totalTime > 0;
        m_preByoyomi = totalTime;
        m_byoyomi = 0;
        m_byoyomiMoves = -1;
    }

    /** Construct with base time and overtime.
        @param preByoyomi Base time for game in milliseconds.
        @param byoyomi Time for overtime period in milliseconds.
        @param byoyomiMoves Number of moves per overtime period. */
    public TimeSettings(long preByoyomi, long byoyomi, int byoyomiMoves)
    {
        assert preByoyomi >= 0;
        assert byoyomi > 0;
        assert byoyomiMoves > 0;
        m_preByoyomi = preByoyomi;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
    }

    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;
        TimeSettings settings = (TimeSettings)object;
        return (settings.m_preByoyomi == m_preByoyomi
                && settings.m_byoyomi == m_byoyomi
                && settings.m_byoyomiMoves == m_byoyomiMoves);

    }

    /** Get time for overtime period.
        @return Time for overtime period in milliseconds; undefined if there
        are no overtime periods in this time settings. */
    public long getByoyomi()
    {
        assert getUseByoyomi();
        return m_byoyomi;
    }

    /** Get number of moves per overtime period.
        @return Number of moves per overtime period; undefined if there are
        no overtime periods in this time settings. */
    public int getByoyomiMoves()
    {
        assert getUseByoyomi();
        return m_byoyomiMoves;
    }

    /** Get base time for game.
        @return Base time for game in milliseconds; this corresponds to
        the total time for the game, if there are no overtime periods. */
    public long getPreByoyomi()
    {
        return m_preByoyomi;
    }

    /** Check if overtime periods are used.
        @return True, if overtime periods are used in this time settings. */
    public boolean getUseByoyomi()
    {
        return (m_byoyomiMoves > 0);
    }

    /** Hash code dummy function (don't use).
        This class is not desgined to be used in a HashMap/HashTable. The
        function will trigger an assertion if assertions are enabled. */
    public int hashCode()
    {
        assert false : "hashCode not designed";
        return 0;
    }

    /** Parse time settings from a string.
        The string is expected to be in the format:
        basetime[+overtime/moves] <br>
        The base time and overtime (byoyomi) can have an optional unit
        specifier (m or min for minutes; s or sec for seconds; default is
        minutes).
        @param s The string.
        @return TimeSettings The time settings corresponding to this string.
        @throws ErrorMessage On syntax error or invalid values. */
    public static TimeSettings parse(String s) throws ErrorMessage
    {
        boolean useByoyomi = false;
        long preByoyomi = 0;
        long byoyomi = 0;
        int byoyomiMoves = 0;
        int idx = s.indexOf('+');
        if (idx < 0)
            preByoyomi = parseTime(s);
        else
        {
            useByoyomi = true;
            preByoyomi = parseTime(s.substring(0, idx));
            int idx2 = s.indexOf('/');
            if (idx2 <= idx)
                throw new ErrorMessage("Invalid time specification");
            byoyomi = parseTime(s.substring(idx + 1, idx2));
            try
            {
                byoyomiMoves = Integer.parseInt(s.substring(idx2 + 1));
            }
            catch (NumberFormatException e)
            {
                throw new ErrorMessage("Invalid specification for byoyomi"
                                       + " moves");
            }
        }
        if (preByoyomi <= 0)
            throw new ErrorMessage("Pre-byoyomi time must be positive");
        if (useByoyomi)
        {
            if (byoyomi <= 0)
                throw new ErrorMessage("Byoyomi time must be positive");
            if (byoyomiMoves <= 0)
                throw new ErrorMessage("Byoyomi moves must be positive");
            return new TimeSettings(preByoyomi, byoyomi, byoyomiMoves);
        }
        else
            return new TimeSettings(preByoyomi);
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append(toString(m_preByoyomi));
        if (getUseByoyomi())
        {
            buffer.append(" + ");
            buffer.append(toString(m_byoyomi));
            buffer.append(" / ");
            buffer.append(m_byoyomiMoves);
            buffer.append(" moves");
        }
        return buffer.toString();
    }

    private static final long MSEC_PER_MIN = 60000L;

    private static final long MSEC_PER_SEC = 1000L;

    private final long m_preByoyomi;

    private final long m_byoyomi;

    private final int m_byoyomiMoves;

    private static long parseTime(String s) throws ErrorMessage
    {
        long factor = MSEC_PER_MIN;
        s = s.trim();
        if (s.endsWith("m"))
            s = s.substring(0, s.length() - "m".length());
        else if (s.endsWith("min"))
            s = s.substring(0, s.length() - "min".length());
        else if (s.endsWith("s"))
        {
            s = s.substring(0, s.length() - "s".length());
            factor = MSEC_PER_SEC;
        }
        else if (s.endsWith("sec"))
        {
            s = s.substring(0, s.length() - "sec".length());
            factor = MSEC_PER_SEC;
        }
        try
        {
            return (Long.parseLong(s.trim()) * factor);
        }
        catch (NumberFormatException e)
        {
            throw new ErrorMessage("Invalid time specification: '" + s + "'");
        }
    }

    private static String toString(long millisec)
    {
        StringBuilder buffer = new StringBuilder(64);
        if (millisec % MSEC_PER_MIN == 0)
        {
            buffer.append(millisec / MSEC_PER_MIN);
            buffer.append(" min");
        }
        else
        {
            buffer.append(millisec / MSEC_PER_SEC);
            buffer.append(" sec");
        }
        return buffer.toString();
    }
}
