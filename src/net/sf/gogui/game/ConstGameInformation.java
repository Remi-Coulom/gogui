//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

/** Const functions of game.GameInformation.
    @see GameInformation
*/
public interface ConstGameInformation
{
    String getBlackRank();

    int getBoardSize();

    String getDate();

    int getHandicap();

    double getKomi();

    String getPlayerBlack();

    String getPlayerWhite();

    String getResult();

    String getRules();

    TimeSettings getTimeSettings();

    String getWhiteRank();

    boolean komiEquals(double komi);

    int parseRules();

    String suggestGameName();
}
