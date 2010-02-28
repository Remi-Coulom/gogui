// MoveUtil.java

package net.sf.gogui.go;

import java.util.ArrayList;

/** Static utility functions related to class Move. */
public final class MoveUtil
{
    /** Fill a list of moves with pass moves.
        The resulting list will contain all moves of the original list
        in the same order, but ensure it starts with a move of color toMove
        and have no subsequent moves of the same color. */
    public static ArrayList<Move> fillPasses(ArrayList<Move> moves,
                                             GoColor toMove)
    {
        ArrayList<Move> result = new ArrayList<Move>(moves.size() * 2);
        if (moves.isEmpty())
            return result;
        for (Move move : moves)
        {
            if (move.getColor() != toMove)
                result.add(Move.getPass(toMove));
            result.add(move);
            toMove = move.getColor().otherColor();
        }
        return result;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private MoveUtil()
    {
    }
}
