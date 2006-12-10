//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

/** Const functions of game.GameTree.
    @see GameTree
*/
public interface ConstGameTree
{
    ConstGameInformation getGameInformationConst();

    ConstNode getRootConst();

    boolean hasVariations();
}
