//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** Const functions of go.Board.
    @see Board
*/
public interface ConstBoard
{
    boolean bothPassed();

    boolean contains(GoPoint point);

    ConstPointList getAdjacentPoints(GoPoint point);

    int getCaptured(GoColor c);

    GoColor getColor(GoPoint p);

    ConstPointList getKilled();

    Move getLastMove();

    Board.Action getAction(int i);

    int getNumberActions();

    int getNumberPoints();

    GoPoint getPoint(int i);

    int getSize();

    void getStones(GoPoint p, GoColor color, PointList stones);

    ConstPointList getSuicide();

    GoColor getToMove();

    boolean isCaptureOrSuicide(GoColor c, GoPoint p);

    boolean isHandicap(GoPoint point);

    boolean isKo(GoPoint point);

    boolean isModified();

    boolean isSuicide(GoColor c, GoPoint p);
}

