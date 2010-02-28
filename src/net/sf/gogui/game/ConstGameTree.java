// ConstGameTree.java

package net.sf.gogui.game;

/** Const functions of game.GameTree.
    @see GameTree */
public interface ConstGameTree
{
    int getBoardSize();

    ConstGameInfo getGameInfoConst(ConstNode node);

    ConstNode getRootConst();

    boolean hasVariations();
}
