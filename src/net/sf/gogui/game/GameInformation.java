//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.Locale;
import net.sf.gogui.go.Board;
import net.sf.gogui.utils.StringUtil;

//----------------------------------------------------------------------------

/** Game information.
    Contains information about the board size, handicap, rules and players.
*/
public class GameInformation
{
    public GameInformation(int boardSize)
    {
        m_boardSize = boardSize;
    }

    public int m_boardSize;

    public int m_handicap;

    public double m_komi;

    public String m_blackRank;

    public String m_date;

    public String m_playerBlack;

    public String m_playerWhite;

    public String m_result;

    public String m_rules;

    public String m_whiteRank;

    public TimeSettings m_timeSettings;

    /** Compare komi with a resultion of 0.5 points. */
    public boolean komiEquals(double komi)
    {
        return Math.abs(m_komi - komi) < 0.25;
    }

    /** Try to parse rules.
        @return Board.RULES_JAPANESE if rules string (to lowercase) is
        "japanese", Board.RULES_CHINESE if "chinese", Board.RULES_UNKNOWN
        otherwise.
    */
    public int parseRules()
    {
        int result = Board.RULES_UNKNOWN;
        String rules = m_rules;
        if (rules != null)
        {
            rules = rules.trim().toLowerCase(Locale.ENGLISH);
            if (rules.equals("japanese"))
                result = Board.RULES_JAPANESE;
            else if (rules.equals("chinese"))
                result = Board.RULES_CHINESE;
        }
        return result;
    }

    /** Get komi as string rounded to 0.5 points. */
    public String roundKomi()
    {
        return roundKomi(m_komi);
    }

    /** Get komi as string rounded to 0.5 points. */
    public static String roundKomi(double komi)
    {
        long intKomi = Math.round(komi * 2);
        if (intKomi % 2 == 0)
            return Long.toString(intKomi / 2);
        return Long.toString(intKomi / 2) + ".5";
    }

    /** Suggest a game name from the player names.
        @return A game name built from the player names or null, if not at
        least one player name is known.
    */
    public String suggestGameName()
    {
        String playerBlack = m_playerBlack;
        String playerWhite = m_playerWhite;
        boolean playerBlackKnown =
            (playerBlack != null && ! playerBlack.trim().equals(""));
        boolean playerWhiteKnown =
            (playerWhite != null && ! playerWhite.trim().equals(""));
        if (! playerBlackKnown && ! playerWhiteKnown)
            return null;
        if (playerBlackKnown)
            playerBlack = StringUtil.capitalize(playerBlack);
        else
            playerBlack = "Unknown";
        if (playerWhiteKnown)
            playerWhite = StringUtil.capitalize(playerWhite);
        else
            playerWhite = "Unknown";
        return playerWhite + " vs " + playerBlack + " (B)";
    }
}

//----------------------------------------------------------------------------
