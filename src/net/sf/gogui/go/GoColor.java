//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** Player color / state of a point on the board (black, white, empty). */
public enum GoColor
{
    /** Black stone or black player. */
    BLACK
    {
        public String getCapitalizedName()
        {
            return "Black";
        }

        public GoColor getNextBlackWhite()
        {
            return WHITE;
        }

        public GoColor getNextBlackWhiteEmpty()
        {
            return WHITE;
        }

        public GoColor getPreviousBlackWhiteEmpty()
        {
            return null;
        }

        public String getUppercaseLetter()
        {
            return "B";
        }

        public boolean isBlackWhite()
        {
            return true;
        }

        public GoColor otherColor()
        {
            return WHITE;
        }

        public int toInteger()
        {
            return 0;
        }

        public String toString()
        {
            return "black";
        }
    },

    /** White stone or white player. */
    WHITE
    {
        public String getCapitalizedName()
        {
            return "White";
        }

        public GoColor getNextBlackWhite()
        {
            return null;
        }

        public GoColor getNextBlackWhiteEmpty()
        {
            return EMPTY;
        }

        public GoColor getPreviousBlackWhiteEmpty()
        {
            return BLACK;
        }

        public String getUppercaseLetter()
        {
            return "W";
        }

        public boolean isBlackWhite()
        {
            return true;
        }

        public GoColor otherColor()
        {
            return BLACK;
        }

        public int toInteger()
        {
            return 1;
        }

        public String toString()
        {
            return "white";
        }
    },

    /** Empty intersection. */
    EMPTY
    {
        public String getCapitalizedName()
        {
            return "Empty";
        }

        public GoColor getNextBlackWhite()
        {
            assert false;
            return null;
        }

        public GoColor getNextBlackWhiteEmpty()
        {
            return null;
        }

        public GoColor getPreviousBlackWhiteEmpty()
        {
            return WHITE;
        }

        public String getUppercaseLetter()
        {
            return "E";
        }

        public boolean isBlackWhite()
        {
            return false;
        }

        public GoColor otherColor()
        {
            return EMPTY;
        }

        public int toInteger()
        {
            return 2;
        }

        public String toString()
        {
            return "empty";
        }
    };

    /** Get next color in an iteration from Black to White.
        @return Next color or null, if end of iteration.
    */
    public abstract GoColor getNextBlackWhite();

    /** Get next color in an iteration from Black to White to Empty.
        @return Next color or null, if end of iteration.
    */
    public abstract GoColor getNextBlackWhiteEmpty();

    /** Get previous color in an iteration from Black to White to Empty.
        @return Previous color or null, if end of iteration.
    */
    public abstract GoColor getPreviousBlackWhiteEmpty();

    /** Return color name if used for specifying player.
        Returns the capitalized color name (e.g. "Black" for BLACK).
        This name will also potentially be internationalized in the future.
    */
    public abstract String getCapitalizedName();

    /** Return uppercase letter identifying the color.
        Returns "B", "W", or "E". This letter is not internationalized,
        such that it can be used for instance in standard language independent
        game results (e.g. "W+3").
    */
    public abstract String getUppercaseLetter();

    /** Check if color is black or white.
        @return <code>true</code>, if color is <code>BLACK</code> or
        <code>WHITE</code>.
    */
    public abstract boolean isBlackWhite();

    /** Return other color.
        @return <code>BLACK</code> for <code>WHITE</code>, <code>WHITE</code>
        for <code>BLACK</code>, <code>EMPTY</code> for <code>EMPTY</code>.
    */
    public abstract GoColor otherColor();

    /** Convert color to an integer that can be used as an array index.
        @return 0 for BLACK; 1 for WHITE; 2 for EMPTY
    */
    public abstract int toInteger();

    /** Return string representation.
        @return "black", "white" or "empty"
    */
    public abstract String toString();
}
