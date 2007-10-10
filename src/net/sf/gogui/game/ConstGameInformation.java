//----------------------------------------------------------------------------
// ConstGameInformation.java
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Score.ScoringMethod;

/** Const functions of game.GameInformation.
    @see GameInformation
*/
public interface ConstGameInformation
{
    String get(StringInfo type);

    String get(StringInfoColor type, GoColor c);

    int getHandicap();

    Komi getKomi();

    TimeSettings getTimeSettings();

    ScoringMethod parseRules();

    String suggestGameName();
}
