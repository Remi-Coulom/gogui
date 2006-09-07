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

    public boolean equals(Object object)
    {
        Placement placement = (Placement)object;
        return (m_point == placement.m_point
                && m_color == placement.m_color
                && m_isSetup == placement.m_isSetup);
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
