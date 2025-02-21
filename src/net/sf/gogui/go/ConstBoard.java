// ConstBoard.java

package net.sf.gogui.go;

import java.util.Iterator;

import net.sf.gogui.gtp.GtpClientBase;

/** Const functions of go.Board.
    @see Board */
public interface ConstBoard
    extends Iterable<GoPoint>
{
    boolean bothPassed();

    boolean contains(GoPoint point);

    ConstPointList getAdjacent(GoPoint point);

    int getCaptured(GoColor c);

    GoColor getColor(GoPoint p);

    ConstPointList getKilled();

    Move getLastMove();

    Move getMove(int i);

    int getNumberMoves();

    ConstPointList getSetup(GoColor c);

    GoColor getSetupPlayer();

    BoardParameters getParameters();

    void getStones(GoPoint p, GoColor color, PointList stones);

    ConstPointList getSuicide();

    GoColor getToMove();

    boolean isCaptureOrSuicide(GoColor c, GoPoint p);

    boolean isHandicap(GoPoint point);

    boolean isKo(GoPoint point);

    boolean isModified();

    boolean isSetupHandicap();

    boolean isSuicide(GoColor c, GoPoint p);

    Iterator<GoPoint> iterator();

    void attachGameRuler(GtpClientBase gameRuler);

    void detachGameRuler();

    int getSize();
}
