//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;

/** Move containing a point and a color.
    The point can be <code>null</code> (for pass move).
    The color is black or white.
    This class is immutable, references are unique.
*/
public final class Move
{
    /** Factory method for constructing a move.
        @param color The color of the move
        @param x Column in <code>[0..GoPoint.MAX_SIZE - 1]</code>
        @param y Row in <code>[0..GoPoint.MAX_SIZE - 1]</code>
        @return Reference to this move
    */
    public static Move get(GoColor color, int x, int y)
    {
        return get(color, GoPoint.get(x, y));
    }

    /** Factory method for constructing a move.
        @param color The color of the move (empty can still be used for
        removing a stone on the board, but this will be deprecated in the
        future)
        @param point Location of the move (null for pass move)
        @return Reference to this move
    */
    public static Move get(GoColor color, GoPoint point)
    {
        assert color.isBlackWhite();
        if (point == null)
        {
            if (color == BLACK)
                return s_passBlack;
            else
                return s_passWhite;
        }
        int x = point.getX();
        int y = point.getY();
        if (color == BLACK)
            return s_movesBlack[x][y];
        else
            return s_movesWhite[x][y];
    }

    /** Factory method for constructing a pass move.
        @param c The color of the move.
        @return Reference to this move
    */
    public static Move getPass(GoColor c)
    {
        assert c.isBlackWhite();
        return get(c, null);
    }

    /** Get color of move.
        @return Color of move
    */
    public GoColor getColor()
    {
        return m_color;
    }

    /** Get stone location of move.
        @return Location of move; null for pass move
    */
    public GoPoint getPoint()
    {
        return m_point;
    }

    /** Get string representation of move.
        @return String representation, e.g. B C3, W PASS
    */
    public String toString()
    {
        return m_string;
    }

    private static Move s_passBlack;

    private static Move s_passWhite;

    private static Move[][] s_movesBlack;

    private static Move[][] s_movesWhite;

    private final GoColor m_color;

    private final GoPoint m_point;

    private final String m_string;

    static
    {
        s_passBlack = new Move(BLACK, null);
        s_passWhite = new Move(WHITE, null);
        s_movesBlack = init(BLACK);
        s_movesWhite = init(WHITE);
    }

    private static Move[][] init(GoColor color)
    {
        Move[][] result = new Move[GoPoint.MAX_SIZE][GoPoint.MAX_SIZE];
        for (int x = 0; x < GoPoint.MAX_SIZE; ++x)
            for (int y = 0; y < GoPoint.MAX_SIZE; ++y)
                result[x][y] = new Move(color, GoPoint.get(x, y));
        return result;
    }

    private Move(GoColor color, GoPoint point)
    {
        m_point = point;
        m_color = color;
        m_string =
            m_color.getUppercaseLetter() + " " + GoPoint.toString(m_point);
    }
}
