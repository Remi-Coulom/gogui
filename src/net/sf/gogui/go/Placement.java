//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

//----------------------------------------------------------------------------

/** Move or setup stone. */
public final class Placement
{
    public Placement(GoPoint point, GoColor color, boolean isSetup)
    {
        assert(color != GoColor.EMPTY || isSetup);
        assert(point != null || ! isSetup);
        m_point = point;
        m_color = color;
        m_isSetup = isSetup;
    }

    public GoColor getColor()
    {
        return m_color;
    }

    public GoPoint getPoint()
    {
        return m_point;
    }

    public boolean isPassMove()
    {
        return (! m_isSetup && m_point == null);
    }

    public boolean isSetup()
    {
        return m_isSetup;
    }

    private boolean m_isSetup;

    private GoPoint m_point;

    private GoColor m_color;
}

//----------------------------------------------------------------------------
