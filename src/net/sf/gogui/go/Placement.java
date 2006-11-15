//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

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
        if (object == null)
            return false;
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

    public int hashCode()
    {
        int hashCode = m_point.hashCode() << 3;
        if (m_isSetup)
            hashCode |= (1 << 2);
        if (m_color == GoColor.BLACK)
            hashCode |= 1;
        else if (m_color == GoColor.WHITE)
            hashCode |= 2;
        else
            hashCode |= 3;
        return hashCode;
    }

    public boolean isPassMove()
    {
        return (! m_isSetup && m_point == null);
    }

    public boolean isSetup()
    {
        return m_isSetup;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer(16);
        if (m_isSetup)
            buffer.append("setup");
        else
            buffer.append("play");
        buffer.append(' ');
        buffer.append(m_color.toString());
        buffer.append(' ');
        buffer.append(GoPoint.toString(m_point));
        return buffer.toString();
    }

    private boolean m_isSetup;

    private GoPoint m_point;

    private GoColor m_color;
}

