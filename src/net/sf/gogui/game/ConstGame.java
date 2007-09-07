//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;

/** Const functions of game.Game.
    @see Game
*/
public interface ConstGame
{
    ConstBoard getBoard();

    ConstClock getClock();

    ConstNode getCurrentNode();

    ConstGameInformation getGameInformation(ConstNode node);

    ConstNode getGameInformationNode();

    int getMoveNumber();

    ConstNode getRoot();

    int getSize();

    GoColor getToMove();

    ConstGameTree getTree();

    boolean isModified();
}
