//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.Locale;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.Komi;
import net.sf.gogui.util.StringUtil;

/** Game information.
    Contains information about the board size, handicap, rules and players.
*/
public class GameInformation
    implements ConstGameInformation
{
    public GameInformation(int boardSize)
    {
        m_boardSize = boardSize;
    }

    public int getBoardSize()
    {
        return m_boardSize;
    }

    public String getBlackRank()
    {
        return m_blackRank;
    }

    public String getDate()
    {
        return m_date;
    }

    public int getHandicap()
    {
        return m_handicap;
    }

    /** Get komi.
        @return The komi or null if komi is unknown.
    */
    public Komi getKomi()
    {
        return m_komi;
    }

    public String getPlayerBlack()
    {
        return m_playerBlack;
    }

    public String getPlayerWhite()
    {
        return m_playerWhite;
    }

    public String getResult()
    {
        return m_result;
    }

    public String getRules()
    {
        return m_rules;
    }

    public String getWhiteRank()
    {
        return m_whiteRank;
    }

    /** Get a copy of the time settings. */
    public TimeSettings getTimeSettings()        
    {
        return m_timeSettings;
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

    public void setBlackRank(String rank)
    {
        m_blackRank = rank;
    }

    public void setBoardSize(int boardSize)
    {
        m_boardSize = boardSize;
    }

    public void setDate(String date)
    {
        m_date = date;
    }

    public void setHandicap(int handicap)
    {
        m_handicap = handicap;
    }

    public void setKomi(Komi komi)
    {
        m_komi = komi;
    }

    public void setPlayerBlack(String name)
    {
        m_playerBlack = name;
    }

    public void setPlayerWhite(String name)
    {
        m_playerWhite = name;
    }

    public void setResult(String result)
    {
        m_result = result;
    }

    public void setRules(String rules)
    {
        m_rules = rules;
    }

    /** Set time settings.
        Keeps a copy of the arguments.
    */
    public void setTimeSettings(TimeSettings timeSettings)
    {
        m_timeSettings = timeSettings;
    }

    public void setWhiteRank(String rank)
    {
        m_whiteRank = rank;
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

    private int m_boardSize;

    private int m_handicap;

    private Komi m_komi;

    private String m_blackRank;

    private String m_date;

    private String m_playerBlack;

    private String m_playerWhite;

    private String m_result;

    private String m_rules;

    private String m_whiteRank;

    private TimeSettings m_timeSettings;
}

