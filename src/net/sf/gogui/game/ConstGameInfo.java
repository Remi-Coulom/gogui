// ConstGameInfo.java

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Score.ScoringMethod;

/** Const functions of game.GameInfo.
    @see GameInfo */
public interface ConstGameInfo
{
    String get(StringInfo type);

    String get(StringInfoColor type, GoColor c);

    int getHandicap();

    Komi getKomi();

    TimeSettings getTimeSettings();

    ScoringMethod parseRules();

    String suggestGameName();
}
