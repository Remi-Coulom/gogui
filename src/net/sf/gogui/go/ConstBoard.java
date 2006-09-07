//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Const functions of go.Board. */
public interface ConstBoard
{
    boolean bothPassed();

    boolean contains(GoPoint point);

    ArrayList getAdjacentPoints(GoPoint point);

    int getCapturedB();

    int getCapturedW();

    GoColor getColor(GoPoint p);

    ArrayList getHandicapStones(int n);

    Placement getPlacement(int i);

    int getNumberPlacements();

    int getNumberPoints();

    GoPoint getPoint(int i);

    int getSize();

    void getStones(GoPoint p, GoColor color, ArrayList stones);

    GoColor getToMove();

    boolean isHandicap(GoPoint point);

    boolean isKo(GoPoint point);

    boolean isModified();

    boolean isSuicide(GoPoint point, GoColor toMove);
}

//----------------------------------------------------------------------------
