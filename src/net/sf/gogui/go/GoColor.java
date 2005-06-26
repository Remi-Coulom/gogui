//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

//----------------------------------------------------------------------------

/** State of a point on the board (black, white, empty). */
public final class GoColor
{
    public static final GoColor BLACK;

    public static final GoColor WHITE;

    public static final GoColor EMPTY;

    public GoColor otherColor()
    {
        return m_otherColor;
    }

    public String toString()
    {
        return m_string;
    }

    private GoColor m_otherColor;

    private final String m_string;

    static
    {
        BLACK = new GoColor("black");
        WHITE = new GoColor("white");
        EMPTY = new GoColor("empty");
        BLACK.setOtherColor(WHITE);
        WHITE.setOtherColor(BLACK);
        EMPTY.setOtherColor(EMPTY);
    }

    private GoColor(String string)
    {
        m_string = string;
    }

    private void setOtherColor(GoColor color)
    {
        m_otherColor = color;
    }
}

//----------------------------------------------------------------------------
