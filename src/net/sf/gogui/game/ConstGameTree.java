//----------------------------------------------------------------------------
// ConstGameTree.java
//----------------------------------------------------------------------------

package net.sf.gogui.game;

/** Const functions of game.GameTree.
    @see GameTree
*/
public interface ConstGameTree
{
    int getBoardSize();

    ConstGameInformation getGameInformationConst(ConstNode node);

    ConstNode getRootConst();

    boolean hasVariations();
}
