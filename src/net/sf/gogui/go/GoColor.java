//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

//----------------------------------------------------------------------------

/** State of a point on the board (black, white, empty). */
public final class GoColor
{
    public static final GoColor BLACK = new GoColor();

    public static final GoColor WHITE = new GoColor();

    public static final GoColor EMPTY = new GoColor();

    public GoColor otherColor()
    {
        if (this == BLACK)
            return WHITE;
        else if (this == WHITE)
            return BLACK;
        else
            return EMPTY;
    }

    public String toString()
    {
        if (this == BLACK)
            return "black";
        else if (this == WHITE)
            return "white";
        else
            return "empty";
    }

    private GoColor()
    {
    }
}

//----------------------------------------------------------------------------
