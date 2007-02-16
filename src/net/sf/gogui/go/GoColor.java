//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** State of a point on the board (black, white, empty). */
public final class GoColor
{
    public static final GoColor BLACK;

    public static final GoColor WHITE;

    public static final GoColor EMPTY;

    public boolean equals(Object object)
    {
        return super.equals(object);
    }

    /** Get next color in an iteration from Black to White.
        @return Next color or null, if end of iteration.
    */
    public GoColor getNextBlackWhite()
    {
        assert(this != EMPTY);
        return m_nextBlackWhite;
    }

    /** Get next color in an iteration from Black to White to Empty.
        @return Next color or null, if end of iteration.
    */
    public GoColor getNextBlackWhiteEmpty()
    {
        return m_nextBlackWhiteEmpty;
    }

    /** Return color name if used for specifying player.
        Returns the capitalized color name (e.g. "Black" for GoColor.BLACK).
        This name will also potentially be internationalized in the future.
    */
    public String getCapitalizedName()
    {
        return m_capitalizedName;
    }

    public int hashCode()
    {
        return super.hashCode();
    }

    public boolean isBlackWhite()
    {
        return (this == BLACK || this == WHITE);
    }

    /** Return other color.
        @return BLACK for WHITE, WHITE for BLACK, EMPTY for EMPTY.
    */
    public GoColor otherColor()
    {
        return m_otherColor;
    }

    /** Convert color to an integer that can be used as an array index.
        Black is 0; White 1; Empty 2
    */
    public int toInteger()
    {
        return m_index;
    }

    /** Return string representation.
        @return "black", "white" or "empty"
    */
    public String toString()
    {
        return m_string;
    }

    private int m_index;

    private GoColor m_otherColor;

    private GoColor m_nextBlackWhite;

    private GoColor m_nextBlackWhiteEmpty;

    private final String m_string;

    private final String m_capitalizedName;

    static
    {
        BLACK = new GoColor("black", 0, "Black");
        WHITE = new GoColor("white", 1, "White");
        EMPTY = new GoColor("empty", 2, "Empty");
        BLACK.setOtherColor(WHITE);
        WHITE.setOtherColor(BLACK);
        EMPTY.setOtherColor(EMPTY);
        BLACK.setNext(WHITE, WHITE);
        WHITE.setNext(null, EMPTY);
        EMPTY.setNext(null, null);
    }

    private GoColor(String string, int index, String capitalizedName)
    {
        m_index = index;
        m_string = string;
        m_capitalizedName = capitalizedName;
    }

    private void setNext(GoColor nextBlackWhite, GoColor nextBlackWhiteEmpty)
    {
        m_nextBlackWhite = nextBlackWhite;
        m_nextBlackWhiteEmpty = nextBlackWhiteEmpty;
    }

    private void setOtherColor(GoColor color)
    {
        m_otherColor = color;
    }
}

