//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

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

    String getPlayerBlack();

    String getPlayerWhite();

    String getRankBlack();

    String getRankWhite();

    String getResult();

    String getRules();

    TimeSettings getTimeSettings();

    int parseRules();

    String suggestGameName();
}
