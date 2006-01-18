//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.utils.StringUtils;

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
        @return A game name built from the player names and ranks or null,
        if not at least one player name is known.
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
            playerBlack = StringUtils.capitalize(playerBlack);
        else
            playerBlack = "Unknown";
        if (playerWhiteKnown)
            playerWhite = StringUtils.capitalize(playerWhite);
        else
            playerWhite = "Unknown";
        if (m_blackRank != null && ! m_blackRank.trim().equals(""))
            playerBlack = playerBlack + " [" + m_blackRank + "]";
        if (m_whiteRank != null && ! m_whiteRank.trim().equals(""))
            playerWhite = playerWhite + " [" + m_whiteRank + "]";
        return playerWhite + " vs " + playerBlack + " (B)";
    }
}

//----------------------------------------------------------------------------
