//----------------------------------------------------------------------------
// GoColor.java
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** Player color / state of a point on the board (black, white, empty). */
public final class GoColor
{
    /** Black stone or black player. */
    public static final GoColor BLACK;

    /** White stone or white player. */
    public static final GoColor WHITE;

    /** Empty intersection. */
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
        assert this != EMPTY;
        return m_nextBlackWhite;
    }

    /** Get next color in an iteration from Black to White to Empty.
        @return Next color or null, if end of iteration.
    */
    public GoColor getNextBlackWhiteEmpty()
    {
        return m_nextBlackWhiteEmpty;
    }

    /** Get previous color in an iteration from Black to White to Empty.
        @return Previous color or null, if end of iteration.
    */
    public GoColor getPreviousBlackWhiteEmpty()
    {
        return m_previousBlackWhiteEmpty;
    }

    /** Return color name if used for specifying player.
        Returns the capitalized color name (e.g. "Black" for GoColor.BLACK).
        This name will also potentially be internationalized in the future.
    */
    public String getCapitalizedName()
    {
        return m_capitalizedName;
    }

    /** Return uppercase letter identifying the color.
        Returns "B", "W", or "E". This letter is not internationalized,
        such that it can be used for instance in standard language independent
        game results (e.g. "W+3").
    */
    public String getUppercaseLetter()
    {
        return m_uppercaseLetter;
    }

    public int hashCode()
    {
        return super.hashCode();
    }

    /** Check if color is black or white.
        @return <code>true</code>, if color is <code>BLACK</code> or
        <code>WHITE</code>.
    */
    public boolean isBlackWhite()
    {
        return (this == BLACK || this == WHITE);
    }

    /** Return other color.
        @return <code>BLACK</code> for <code>WHITE</code>, <code>WHITE</code>
        for <code>BLACK</code>, <code>EMPTY</code> for <code>EMPTY</code>.
    */
    public GoColor otherColor()
    {
        return m_otherColor;
    }

    /** Convert color to an integer that can be used as an array index.
        @return 0 for GoColor.BLACK; 1 for GoColor.WHITE; 2 for GoColor.EMPTY
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

    private final int m_index;

    private GoColor m_otherColor;

    private GoColor m_nextBlackWhite;

    private GoColor m_nextBlackWhiteEmpty;

    private GoColor m_previousBlackWhiteEmpty;

    private final String m_string;

    private final String m_capitalizedName;

    private final String m_uppercaseLetter;

    static
    {
        BLACK = new GoColor("black", 0, "Black", "B");
        WHITE = new GoColor("white", 1, "White", "W");
        EMPTY = new GoColor("empty", 2, "Empty", "E");
        BLACK.setOtherColor(WHITE);
        WHITE.setOtherColor(BLACK);
        EMPTY.setOtherColor(EMPTY);
        BLACK.setNext(WHITE, WHITE, null);
        WHITE.setNext(null, EMPTY, BLACK);
        EMPTY.setNext(null, null, WHITE);
    }

    private GoColor(String string, int index, String capitalizedName,
                    String uppercaseLetter)
    {
        m_index = index;
        m_string = string;
        m_capitalizedName = capitalizedName;
        m_uppercaseLetter = uppercaseLetter;
    }

    private void setNext(GoColor nextBlackWhite, GoColor nextBlackWhiteEmpty,
                         GoColor previousBlackWhiteEmpty)
    {
        m_nextBlackWhite = nextBlackWhite;
        m_nextBlackWhiteEmpty = nextBlackWhiteEmpty;
        m_previousBlackWhiteEmpty = previousBlackWhiteEmpty;
    }

    private void setOtherColor(GoColor color)
    {
        m_otherColor = color;
    }
}
