//=============================================================================
// $Id$
// $Source$
//=============================================================================

package board;

//=============================================================================

public class Point
{
    public Point(int x, int y)
    {
        assert(x >= 0);
        assert(y >= 0);
        m_x = x;
        m_y = y;
    }

    public boolean equals(Point p)
    {
        return (m_x == p.m_x && m_y == p.m_y);
    }

    public int getX()
    {
        return m_x;
    }

    public int getY()
    {
        return m_y;
    }

    public String toString()
    {
        return m_xString[m_x] + Integer.toString(m_y + 1);
    }
    
    private int m_x;
    private int m_y;
    private static String m_xString[] = {
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K",
        "L", "M", "N", "O", "P", "Q", "R", "S", "T" };
}

//=============================================================================
