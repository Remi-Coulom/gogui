//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Move.
    Contains a point (null for pass move) and a color.
    The color is usually black or white, but empty can be used for
    removing a stone on the board.
    This class is immutable, references are unique.
*/
public final class Move
{
    public static Move create(GoPoint point, GoColor color)
    {
        if (point == null)
        {
            if (color == GoColor.BLACK)
                return s_passBlack;
            else if (color == GoColor.WHITE)
                return s_passWhite;
            else if (color == GoColor.EMPTY)
            {
                assert(false);
                return s_passBlack;
            }
        }
        int x = point.getX();
        int y = point.getY();
        int max = Math.max(x, y);
        if (max >= s_size)
            grow(max + 1);
        if (color == GoColor.BLACK)
            return s_movesBlack[x][y];
        else if (color == GoColor.WHITE)
            return s_movesWhite[x][y];
        else
            return s_movesEmpty[x][y];
    }
    
    /** Fill a list of moves with pass moves.
        The resulting list will contain all moves of the original list
        in the same order, but ensure it starts with a move of color toMove
        and have no subsequent moves of the same color.
    */
    public static ArrayList fillPasses(ArrayList moves, GoColor toMove)
    {
        ArrayList result = new ArrayList(moves.size() * 2);
        if (moves.size() == 0)
            return result;
        for (int i = 0; i < moves.size(); ++i)
        {
            Move move = (Move)moves.get(i);
            if (move.getColor() != toMove)
                result.add(new Move(null, toMove));
            result.add(move);
            toMove = move.getColor().otherColor();
        }
        return result;
    }

    public GoColor getColor()
    {
        return m_color;
    }

    public GoPoint getPoint()
    {
        return m_point;
    }

    public String toString()
    {
        return m_string;
    }

    private static int s_size;

    private static Move s_passBlack;

    private static Move s_passWhite;

    private static Move[][] s_movesBlack;

    private static Move[][] s_movesEmpty;

    private static Move[][] s_movesWhite;

    private final GoColor m_color;

    private final GoPoint m_point;

    private final String m_string;

    static
    {
        s_passBlack = new Move(null, GoColor.BLACK);
        s_passWhite = new Move(null, GoColor.WHITE);
        s_size = 0;
        grow(19);
    };

    private static void grow(int size)
    {
        assert(size > s_size);
        s_movesBlack = grow(size, GoColor.BLACK, s_movesBlack);
        s_movesWhite = grow(size, GoColor.WHITE, s_movesWhite);
        s_movesEmpty = grow(size, GoColor.EMPTY, s_movesEmpty);
        s_size = size;
    }

    private static Move[][] grow(int size, GoColor color, Move[][] moves)
    {
        assert(size > s_size);
        Move[][] result = new Move[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
                if (x < s_size && y < s_size)
                    result[x][y] = moves[x][y];
                else
                    result[x][y] = new Move(GoPoint.create(x, y), color);
        return result;
    }

    private Move(GoPoint point, GoColor color)
    {
        m_point = point;
        m_color = color;
        m_string = m_color.toString() + " " + GoPoint.toString(m_point);
    }
}

//----------------------------------------------------------------------------
