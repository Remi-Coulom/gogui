//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package go;

import java.util.*;

//-----------------------------------------------------------------------------

public class Move
{
    public Move(Point p, Color c)
    {
        m_point = p;
        m_color = c;
    }

    public boolean equals(Move m)
    {
        if (m_color != m.m_color)
            return false;
        if (m_point == null)
            return (m.m_point == null);
        if (m.m_point == null)
            return false;
        return (m_point.equals(m.m_point));
    }

    /** Fill a list of moves with pass moves.
        The resulting list will contain all moves of the original list
        in the same order, but ensure it starts with a move of color toMove
        and have no subsequent moves of the same color.
    */
    public static Vector fillPasses(Vector moves, Color toMove)
    {
        Vector result = new Vector(moves.size() * 2);
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
        if (m_point == null)
            return (m_color.toString() + " pass");
        else
            return (m_color.toString() + " " + m_point.toString());
    }

    private Color m_color;

    private Point m_point;
}

//-----------------------------------------------------------------------------
