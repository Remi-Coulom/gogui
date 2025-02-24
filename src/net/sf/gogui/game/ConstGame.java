// ConstGame.java

package net.sf.gogui.game;

import net.sf.gogui.gtp.BoardParameters;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;

/** Const functions of game.Game.
    @see Game */
public interface ConstGame
{
    ConstBoard getBoard();

    ConstClock getClock();

    ConstNode getCurrentNode();

    ConstGameInfo getGameInfo(ConstNode node);

    ConstNode getGameInfoNode();

    int getMoveNumber();

    ConstNode getRoot();

    BoardParameters getParameters();

    GoColor getToMove();

    ConstGameTree getTree();

    boolean isModified();
}
