//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package go;

//-----------------------------------------------------------------------------

public class Move
{
    public Move( Point p, Color c )
    {
        m_point = p;
        m_color = c;
    }

    public boolean equals( Move m )
    {
        if ( m_color != m.m_color )
            return false;
        if ( m_point == null )
            return ( m.m_point == null );
        if ( m.m_point == null )
            return false;
        return ( m_point.equals( m.m_point ) );
    }

    public Color getColor()
    {
        return m_color;
    }

    public Point getPoint()
    {
        return m_point;
    }

    public String toString()
    {
        if ( m_point == null )
            return ( m_color.toString() + " pass" );
        else
            return ( m_color.toString() + " " + m_point.toString() );
    }

    private Color m_color;
    private Point m_point;
}

//-----------------------------------------------------------------------------
