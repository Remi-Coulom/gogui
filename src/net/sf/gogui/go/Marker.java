// Marker.java

package net.sf.gogui.go;

/** Mark points on a Go board. */
public class Marker
{
    /** Constructor.
        @param size Size of the board. */
    public Marker(int size)
    {
        m_size = size;
        m_mark = new boolean[GoPoint.NUMBER_INDEXES];
    }

    /** Clear all marked points. */
    public void clear()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                m_mark[GoPoint.getIndex(x, y)] = false;
    }

    /** Clear a marked point.
        @param p The point to clear. */
    public void clear(GoPoint p)
    {
        m_mark[p.getIndex()] = false;
    }

    /** Clear all points from a list.
        @param points List of points. */
    public void clear(ConstPointList points)
    {
        int nuPoints = points.size();
        // Don't use an iterator for efficiency
        for (int i = 0; i < nuPoints; ++i)
            m_mark[points.get(i).getIndex()] = false;
    }

    /** Check if a point is marked.
        @param p The point to check.
        @return true, if point is marked, false otherwise. */
    public boolean get(GoPoint p)
    {
        return m_mark[p.getIndex()];
    }

    /** Check if no point is marked.
        @return true, if no point is marked, false otherwise. */
    public boolean isCleared()
    {
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                if (m_mark[GoPoint.getIndex(x, y)])
                    return false;
        return true;
    }

    /** Mark a point.
        @param p The point to mark. */
    public void set(GoPoint p)
    {
        m_mark[p.getIndex()] = true;
    }

    /** Mark or clear a point.
        @param p The point to mark or clear.
        @param value true, if point should be marked; false, if point should
        be cleared. */
    public void set(GoPoint p, boolean value)
    {
        m_mark[p.getIndex()] = value;
    }

    /** Mark all points from a list.
        @param points List of points. */
    public void set(ConstPointList points)
    {
        int nuPoints = points.size();
        // Don't use an iterator for efficiency
        for (int i = 0; i < nuPoints; ++i)
            m_mark[points.get(i).getIndex()] = true;
    }

    private final int m_size;

    private boolean m_mark[];
}
