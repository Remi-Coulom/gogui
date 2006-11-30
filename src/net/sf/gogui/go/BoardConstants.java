//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

/** Some values that are constant for a given board size. */
public final class BoardConstants
{
    public BoardConstants(int size)
    {
        m_size = size;
        m_handicapLine1 = -1;
        m_handicapLine2 = -1;
        m_handicapLine3 = -1;
        if (size >= 13)
        {
            m_handicapLine1 = 3;
            m_handicapLine3 = size - 4;
        }
        else if (size >= 8)
        {
            m_handicapLine1 = 2;
            m_handicapLine3 = size - 3;
        }
        if (size >= 11 && size % 2 != 0)
            m_handicapLine2 = size / 2;
        initAllPoints();
    }

    public ArrayList getHandicapStones(int n)
    {
        ArrayList result = new ArrayList(9);
        if (n == 0)
            return result;
        int line1 = m_handicapLine1;
        int line2 = m_handicapLine2;
        int line3 = m_handicapLine3;
        if (line1 < 0)
            return null;
        if (n > 4 && line2 < 0)
            return null;
        if (n >= 1)
            result.add(GoPoint.get(line1, line1));
        if (n >= 2)
            result.add(GoPoint.get(line3, line3));
        if (n >= 3)
            result.add(GoPoint.get(line1, line3));
        if (n >= 4)
            result.add(GoPoint.get(line3, line1));
        if (n >= 5 && n % 2 != 0)
        {
            result.add(GoPoint.get(line2, line2));
            --n;
        }
        if (n >= 5)
            result.add(GoPoint.get(line1, line2));
        if (n >= 6)
            result.add(GoPoint.get(line3, line2));
        if (n >= 7)
            result.add(GoPoint.get(line2, line1));
        if (n >= 8)
            result.add(GoPoint.get(line2, line3));
        return result;
    }

    public int getNumberPoints()
    {
        return m_allPoints.length;
    }

    public GoPoint getPoint(int i)
    {
        return m_allPoints[i];
    }

    public int getSize()
    {
        return m_size;
    }

    public boolean isEdgeLine(int i)
    {
        return (i == 0 || i == m_size - 1);
    }

    public boolean isHandicapLine(int i)
    {
        return (i == m_handicapLine1 || i == m_handicapLine2
                || i == m_handicapLine3);
    }

    public boolean isHandicap(GoPoint p)
    {
        int x = p.getX();
        int y = p.getY();
        return (isHandicapLine(x) && isHandicapLine(y));
    }

    private final int m_size;

    private int m_handicapLine1;

    private int m_handicapLine2;

    private int m_handicapLine3;

    private GoPoint m_allPoints[];

    private void initAllPoints()
    {
        m_allPoints = new GoPoint[m_size * m_size];
        int i = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                m_allPoints[i++] = point;
            }
    }
}

