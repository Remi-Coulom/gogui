//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.utils.ErrorMessage;

//----------------------------------------------------------------------------

/** Time settings.
    Unit is milliseconds.
*/
public final class TimeSettings
{
    public TimeSettings(long totalTime)
    {
        assert(totalTime > 0);
        m_preByoyomi = totalTime;
        m_byoyomi = 0;
        m_byoyomiMoves = -1;
    }

    public TimeSettings(long preByoyomi, long byoyomi, int byoyomiMoves)
    {
        assert(preByoyomi > 0);
        assert(byoyomi > 0);
        assert(byoyomiMoves > 0);
        m_preByoyomi = preByoyomi;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
    }

    public TimeSettings(TimeSettings timeSettings)
    {
        m_preByoyomi = timeSettings.m_preByoyomi;
        m_byoyomi = timeSettings.m_byoyomi;
        m_byoyomiMoves = timeSettings.m_byoyomiMoves;
    }

    public long getByoyomi()
    {
        assert(getUseByoyomi());
        return m_byoyomi;
    }

    public int getByoyomiMoves()
    {
        assert(getUseByoyomi());
        return m_byoyomiMoves;
    }

    public long getPreByoyomi()
    {
        return m_preByoyomi;
    }

    public boolean getUseByoyomi()
    {
        return (m_byoyomiMoves > 0);
    }

    public static TimeSettings parse(String s) throws ErrorMessage
    {
        boolean useByoyomi = false;
        long preByoyomi = 0;
        long byoyomi = 0;
        int byoyomiMoves = 0;
        try
        {
            int idx = s.indexOf('+');
            if (idx < 0)
            {
                preByoyomi = Long.parseLong(s) * MSEC_PER_MIN;
            }
            else
            {
                useByoyomi = true;
                preByoyomi
                    = Long.parseLong(s.substring(0, idx)) * MSEC_PER_MIN;
                int idx2 = s.indexOf('/');
                if (idx2 <= idx)
                    throw new ErrorMessage("Invalid time specification");
                byoyomi
                    = Long.parseLong(s.substring(idx + 1, idx2))
                    * MSEC_PER_MIN;
                byoyomiMoves = Integer.parseInt(s.substring(idx2 + 1));
            }
        }
        catch (NumberFormatException e)
        {
            throw new ErrorMessage("Invalid time specification");
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

    private static final long MSEC_PER_MIN = 60000L;

    private final long m_preByoyomi;

    private final long m_byoyomi;

    private final int m_byoyomiMoves;
}

//----------------------------------------------------------------------------
