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

    ArrayList getAdjacentPoints(GoPoint point);

    int getCapturedBlack();

    int getCapturedWhite();

    GoColor getColor(GoPoint p);

    ArrayList getHandicapStones(int n);

    ArrayList getKilled();

    Move getLastMove();

    Board.Placement getPlacement(int i);

    int getNumberPlacements();

    int getNumberPoints();

    GoPoint getPoint(int i);

    int getSize();

    void getStones(GoPoint p, GoColor color, ArrayList stones);

    ArrayList getSuicide();

    GoColor getToMove();

    boolean isCaptureOrSuicide(GoPoint point, GoColor toMove);

    boolean isHandicap(GoPoint point);

    boolean isKo(GoPoint point);

    boolean isModified();

    boolean isSuicide(GoPoint point, GoColor toMove);
}

