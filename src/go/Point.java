//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package go;

import java.util.Vector;

//----------------------------------------------------------------------------

/** Intersection on the Go board.
    This class is immutable.
*/
public final class Point
{
    public Point(int x, int y)
    {
        set(x, y);
    }

    public static boolean equals(Point point1, Point point2)
    {
        if (point1 == null && point2 == null)
            return true;
        if (point1 == null || point2 == null)
            return false;
        return point1.equals(point2);
    }

    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        if (object instanceof Point)
        {
            Point point = (Point)object;
            return (m_x == point.m_x && m_y == point.m_y);
        }
        return false;
    }

    public void down()
    {
        if (m_y > 0)
            --m_y;
    }

    public int getX()
    {
        return m_x;
    }

    public int getY()
    {
        return m_y;
    }

    public int hashCode()
    {
        return (m_x << 16) | m_y;
    }

    public void left()
    {
        if (m_x > 0)
            --m_x;
    }

    public void right(int max)
    {
        if (m_x < max - 1)
            ++m_x;
    }

    public void set(int x, int y)
    {
        assert(x >= 0);
        assert(y >= 0);
        m_x = x;
        m_y = y;
    }

    public String toString()
    {
        return m_xString[m_x] + Integer.toString(m_y + 1);
    }
    
    public static String toString(Point point)
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
            buffer.append(((Point)pointList.get(i)).toString());
            if (i < length - 1)
                buffer.append(' ');
        }
        return buffer.toString();
    }
    
    public void up(int max)
    {
        if (m_y < max - 1)
            ++m_y;
    }

    private int m_x;

    private int m_y;

    private static String m_xString[] = {
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K",
        "L", "M", "N", "O", "P", "Q", "R", "S", "T" };
}

//----------------------------------------------------------------------------
