//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package go;

//-----------------------------------------------------------------------------

public class Color
{
    public static final Color BLACK = new Color();

    public static final Color WHITE = new Color();

    public static final Color EMPTY = new Color();

    public Color otherColor()
    {
        if ( this == BLACK )
            return WHITE;
        else if ( this == WHITE )
            return BLACK;
        else
            return EMPTY;
    }

    public String toString()
    {
        if ( this == BLACK )
            return "black";
        else if ( this == WHITE )
            return "white";
        else
            return "empty";
    }

    private Color()
    {
    }
}

//-----------------------------------------------------------------------------
