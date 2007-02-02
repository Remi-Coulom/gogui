//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;

/** Const functions of game.GameInformation.
    @see GameInformation
*/
public interface ConstGameInformation
{
    int getBoardSize();

    String getDate();

    int getHandicap();

    Komi getKomi();

    String getPlayer(GoColor c);

    String getRank(GoColor c);

    String getResult();

    String getRules();

    TimeSettings getTimeSettings();

    int parseRules();

    String suggestGameName();
}
