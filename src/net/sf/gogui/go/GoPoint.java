//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.Vector;

//----------------------------------------------------------------------------

/** Intersection on the Go board.
    This class is immutable and references to the same point are unique.
*/
public final class GoPoint
{
    public static final int MAXSIZE = 100;

    public static GoPoint create(int x, int y)
    {
        assert(x >= 0);
        assert(y >= 0);
        assert(x < MAXSIZE);
        assert(y < MAXSIZE);
        int max = Math.max(x, y);
        if (max >= s_size)
            grow(max + 1);
        GoPoint point = s_points[x][y];
        assert(point != null);
        return point;
    }
    
    public GoPoint down()
    {
        if (m_y > 0)
            return create(m_x, m_y - 1);
        else
            return this;
    }

    public int getX()
    {
        return m_x;
    }

    public int getY()
    {
        return m_y;
    }

    public GoPoint left()
    {
        if (m_x > 0)
            return create(m_x - 1, m_y);
        else
            return this;
    }

    public GoPoint right(int max)
    {
        if (m_x < max - 1)
            return create(m_x + 1, m_y);
        else
            return this;
    }

    public String toString()
    {
        return m_xString[m_x] + Integer.toString(m_y + 1);
    }
    
    public static String toString(GoPoint point)
    {
        if (point == null)
            return "PASS";
        return point.toString();
    }
    
    public static String toString(Vector pointList)
    {
        if (pointList == null)
            return "(null)";
        int length = pointList.size();
        StringBuffer buffer = new StringBuffer(length * 4);
        for (int i = 0; i < length; ++i)
        {
            buffer.append(((GoPoint)pointList.get(i)).toString());
            if (i < length - 1)
                buffer.append(' ');
        }
        return buffer.toString();
    }
    
    public GoPoint up(int max)
    {
        if (m_y < max - 1)
            return create(m_x, m_y + 1);
        else
            return this;
    }

    private static int s_size;

    private static GoPoint[][] s_points;

    private int m_x;

    private int m_y;

    private static String m_xString[] = {
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K",
        "L", "M", "N", "O", "P", "Q", "R", "S", "T" };

    static
    {
        s_size = 0;
        grow(19);
    };

    private static void grow(int size)
    {
        System.err.println("XXX GoPoint.grow " + size);
        assert(size > s_size);
        GoPoint[][] points = new GoPoint[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
                if (x < s_size && y < s_size)
                    points[x][y] = s_points[x][y];
                else
                    points[x][y] = new GoPoint(x, y);
        s_points = points;
        s_size = size;
    }

    private GoPoint(int x, int y)
    {
        set(x, y);
    }

    private void set(int x, int y)
    {
        assert(x >= 0);
        assert(y >= 0);
        m_x = x;
        m_y = y;
    }
}

//----------------------------------------------------------------------------
