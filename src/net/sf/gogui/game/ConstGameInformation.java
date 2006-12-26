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
    String getBlackRank();

    int getBoardSize();

    String getDate();

    int getHandicap();

    Komi getKomi();

    String getPlayerBlack();

    String getPlayerWhite();

    String getResult();

    String getRules();

    TimeSettings getTimeSettings();

    String getWhiteRank();

    int parseRules();

    String suggestGameName();
}
