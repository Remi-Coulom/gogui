//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.Locale;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.StringUtil;

/** Game information.
    Contains information about handicap, rules and players.
*/
public class GameInformation
    implements ConstGameInformation
{
    public GameInformation()
    {
    }

    public GameInformation(ConstGameInformation info)
    {
        copyFrom(info);
    }

    public void copyFrom(ConstGameInformation info)
    {
        m_handicap = info.getHandicap();
        m_komi = info.getKomi();
        m_date = info.getDate();
        m_playerBlack = info.getPlayer(GoColor.BLACK);
        m_playerWhite = info.getPlayer(GoColor.WHITE);
        m_result = info.getResult();
        m_rules = info.getRules();
        m_rankBlack = info.getRank(GoColor.BLACK);
        m_rankWhite = info.getRank(GoColor.WHITE);
        m_timeSettings = info.getTimeSettings();
    }

    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;        
        GameInformation info = (GameInformation)object;
        return (m_handicap == info.getHandicap()
                && ObjectUtil.equals(m_komi, info.getKomi())
                && ObjectUtil.equals(m_date, info.getDate())
                && ObjectUtil.equals(m_playerBlack,
                                     info.getPlayer(GoColor.BLACK))
                && ObjectUtil.equals(m_playerWhite,
                                     info.getPlayer(GoColor.WHITE))
                && ObjectUtil.equals(m_result, info.getResult())
                && ObjectUtil.equals(m_rules, info.getRules())
                && ObjectUtil.equals(m_rankBlack, info.getRank(GoColor.BLACK))
                && ObjectUtil.equals(m_rankWhite, info.getRank(GoColor.WHITE))
                && ObjectUtil.equals(m_timeSettings, info.getTimeSettings()));
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

    /** Get player name.
        @return The player name or null if unknown.
    */
    public String getPlayer(GoColor c)
    {
        assert(c.isBlackWhite());
        if (c == GoColor.BLACK)
            return m_playerBlack;
        else
            return m_playerWhite;
    }

    /** Get player rank.
        @return The player rank or null if unknown.
    */
    public String getRank(GoColor c)
    {
        assert(c.isBlackWhite());
        if (c == GoColor.BLACK)
            return m_rankBlack;
        else
            return m_rankWhite;
    }

    public String getResult()
    {
        return m_result;
    }

    public String getRules()
    {
        return m_rules;
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

    public void setPlayer(GoColor c, String name)
    {
        assert(c.isBlackWhite());
        if (c == GoColor.BLACK)
            m_playerBlack = name;
        else
            m_playerWhite = name;
    }

    public void setRank(GoColor c, String rank)
    {
        assert(c.isBlackWhite());
        if (c == GoColor.BLACK)
            m_rankBlack = rank;
        else
            m_rankWhite = rank;
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

    private int m_handicap;

    private Komi m_komi;

    private String m_date;

    private String m_playerBlack;

    private String m_playerWhite;

    private String m_rankBlack;

    private String m_rankWhite;

    private String m_result;

    private String m_rules;

    private TimeSettings m_timeSettings;
}

