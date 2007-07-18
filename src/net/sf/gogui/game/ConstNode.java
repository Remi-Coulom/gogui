//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.Map;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

/** Const functions of game.Node.
    @see Node
*/
public interface ConstNode
{
    ConstPointList getAddStones(GoColor c);

    String getComment();

    ConstNode getFatherConst();

    ConstNode getChildConst();

    ConstNode getChildConst(int i);

    int getChildIndex(ConstNode child);

    ConstGameInformation getGameInformationConst();

    String getLabel(GoPoint point);

    Map<GoPoint,String> getLabelsUnmodifiable();

    ConstPointList getMarkedConst(MarkType type);

    Move getMove();

    int getMovesLeft(GoColor color);

    int getNumberChildren();

    GoColor getPlayer();

    Map<String,String> getSgfPropertiesUnmodifiable();

    double getTimeLeft(GoColor color);

    GoColor getToMove();

    float getValue();

    boolean hasChildren();

    boolean hasComment();

    boolean hasFather();

    boolean hasSetup();

    ConstNode variationAfter(ConstNode child);

    ConstNode variationBefore(ConstNode child);
}
