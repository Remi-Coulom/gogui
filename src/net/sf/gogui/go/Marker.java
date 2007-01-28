//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

/** Mark points on a Go board. */
public class Marker
{
    /** Constructor.
        @param size Size of the board.
    */
    public Marker(int size)
    {
        m_mark = new boolean[size][size];
    }

    /** Clear all marked points. */
    public void clear()
    {
        for (int x = 0; x < m_mark.length; ++x)
            for (int y = 0; y < m_mark[x].length; ++y)
                m_mark[x][y] = false;
    }

    /** Clear a marked point.
        @param p The point to clear.
    */
    public void clear(GoPoint p)
    {
        set(p, false);
    }

    /** Check if a point is marked.
        @param p The point to check.
        @return true, if point is marked, false otherwise.
    */
    public boolean get(GoPoint p)
    {
        return m_mark[p.getX()][p.getY()];
    }

    /** Check if no point is marked.
        @return true, if no point is marked, false otherwise.
    */
    public boolean isCleared()
    {
        for (int x = 0; x < m_mark.length; ++x)
            for (int y = 0; y < m_mark[x].length; ++y)
                if (m_mark[x][y])
                    return false;
        return true;
    }    

    /** Mark a point.
        @param p The point to mark.
    */
    public void set(GoPoint p)
    {
        set(p, true);
    }

    /** Mark or clear a point.
        @param p The point to mark or clear.
        @param value true, if point should be marked; false, if point should
        be cleared.
    */
    public void set(GoPoint p, boolean value)
    {
        m_mark[p.getX()][p.getY()] = value;
    }

    /** Mark or clear a list of points.
        @param points List of go.Point to mark or clear.
        @param value true, if points should be marked; false, if points should
        be cleared.
    */
    public void set(ConstPointList points, boolean value)
    {
        int size = points.size();
        for (int i = 0; i < size; ++i)
            set(points.get(i), value);
    }

    private boolean m_mark[][];
}

