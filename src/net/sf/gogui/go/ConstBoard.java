//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

/** Const functions of go.Board.
    @see Board
*/
public interface ConstBoard
{
    boolean bothPassed();

    boolean contains(GoPoint point);

    ConstPointList getAdjacentPoints(GoPoint point);

    int getCapturedBlack();

    int getCapturedWhite();

    GoColor getColor(GoPoint p);

    ConstPointList getHandicapStones(int n);

    ConstPointList getKilled();

    Move getLastMove();

    Board.Placement getPlacement(int i);

    int getNumberPlacements();

    int getNumberPoints();

    GoPoint getPoint(int i);

    int getSize();

    void getStones(GoPoint p, GoColor color, PointList stones);

    ConstPointList getSuicide();

    GoColor getToMove();

    boolean isCaptureOrSuicide(GoPoint point, GoColor toMove);

    boolean isHandicap(GoPoint point);

    boolean isKo(GoPoint point);

    boolean isModified();

    boolean isSuicide(GoPoint point, GoColor toMove);
}

