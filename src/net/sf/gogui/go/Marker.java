//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

public class Marker
{
    public Marker(int size)
    {
        m_mark = new boolean[size][size];
    }

    public void clear()
    {
        for (int x = 0; x < m_mark.length; ++x)
            for (int y = 0; y < m_mark[x].length; ++y)
                m_mark[x][y] = false;
    }

    public void clear(GoPoint p)
    {
        set(p, false);
    }

    public boolean get(GoPoint p)
    {
        return m_mark[p.getX()][p.getY()];
    }

    public boolean isCleared()
    {
        for (int x = 0; x < m_mark.length; ++x)
            for (int y = 0; y < m_mark[x].length; ++y)
                if (m_mark[x][y])
                    return false;
        return true;
    }    

    public void set(GoPoint p)
    {
        set(p, true);
    }

    public void set(GoPoint p, boolean value)
    {
        m_mark[p.getX()][p.getY()] = value;
    }

    public void set(ArrayList points, boolean value)
    {
        int size = points.size();
        for (int i = 0; i < size; ++i)
            set((GoPoint)points.get(i), value);
    }

    private boolean m_mark[][];
}

//----------------------------------------------------------------------------
