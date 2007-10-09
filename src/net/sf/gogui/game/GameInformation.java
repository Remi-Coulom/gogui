//----------------------------------------------------------------------------
// GameInformation.java
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.Locale;
import net.sf.gogui.go.BlackWhiteSet;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Score.ScoringMethod;
import static net.sf.gogui.go.Score.ScoringMethod.AREA;
import static net.sf.gogui.go.Score.ScoringMethod.TERRITORY;
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

    public final void copyFrom(ConstGameInformation info)
    {
        m_handicap = info.getHandicap();
        m_komi = info.getKomi();
        m_date = info.getDate();
        m_player.set(BLACK, info.getPlayer(BLACK));
        m_player.set(WHITE, info.getPlayer(WHITE));
        m_result = info.getResult();
        m_rules = info.getRules();
        m_rank.set(BLACK, info.getRank(BLACK));
        m_rank.set(WHITE, info.getRank(WHITE));
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
                && ObjectUtil.equals(m_player, info.m_player)
                && ObjectUtil.equals(m_result, info.getResult())
                && ObjectUtil.equals(m_rules, info.getRules())
                && ObjectUtil.equals(m_rank, info.m_rank)
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
        return m_player.get(c);
    }

    /** Get player rank.
        @return The player rank or null if unknown.
    */
    public String getRank(GoColor c)
    {
        return m_rank.get(c);
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

    /** Hash code dummy function (don't use).
        This class is not desgined to be used in a HashMap/HashTable. The
        function will trigger an assertion if assertions are enabled.
    */
    public int hashCode()
    {
        assert false : "hashCode not designed";
        return 0;
    }

    public boolean isEmpty()
    {
        return (m_handicap == 0 && m_komi == null && m_date == null
                && m_player.get(BLACK) == null && m_player.get(WHITE) == null
                && m_rank.get(BLACK) == null && m_rank.get(WHITE) == null
                && m_result == null && m_rules == null
                && m_timeSettings == null);
    }

    /** Try to parse rules.
        @return Score.ScoringMethod.TERRITORY if rules string (to lowercase)
        is "japanese", Score.ScoringMethod.AREA otherwise.
    */
    public ScoringMethod parseRules()
    {
        ScoringMethod result = AREA;
        String rules = m_rules;
        if (rules != null)
        {
            rules = rules.trim().toLowerCase(Locale.ENGLISH);
            if (rules.equals("japanese"))
                result = TERRITORY;
        }
        return result;
    }

    public void setDate(String date)
    {
        m_date = checkEmpty(date);
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
        m_player.set(c, checkEmpty(name));
    }

    public void setRank(GoColor c, String rank)
    {
        m_rank.set(c, checkEmpty(rank));
    }

    public void setResult(String result)
    {
        m_result = checkEmpty(result);
    }

    public void setRules(String rules)
    {
        m_rules = checkEmpty(rules);
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
        String playerBlack = m_player.get(BLACK);
        String playerWhite = m_player.get(WHITE);
        boolean playerBlackKnown = ! StringUtil.isEmpty(playerBlack);
        boolean playerWhiteKnown = ! StringUtil.isEmpty(playerWhite);
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

    private BlackWhiteSet<String> m_player = new BlackWhiteSet<String>();

    private BlackWhiteSet<String> m_rank = new BlackWhiteSet<String>();

    private String m_rankWhite;

    private String m_result;

    private String m_rules;

    private TimeSettings m_timeSettings;

    private String checkEmpty(String s)
    {
        if (s == null || s.trim().equals(""))
            return null;
        return s;
    }
}
