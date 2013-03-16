// GameInfo.java

package net.sf.gogui.game;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    Contains information about handicap, rules and players. */
public class GameInfo
    implements ConstGameInfo
{
    public GameInfo()
    {
    }

    public GameInfo(ConstGameInfo info)
    {
        copyFrom(info);
    }

    public final void copyFrom(ConstGameInfo info)
    {
        GameInfo infoNonConst = (GameInfo)info;
        m_handicap = infoNonConst.m_handicap;
        m_komi = infoNonConst.m_komi;
        m_timeSettings = infoNonConst.m_timeSettings;
        m_stringInfo.clear();
        m_stringInfo.putAll(infoNonConst.m_stringInfo);
        m_stringInfoColor.clear();
        for (Map.Entry<StringInfoColor,BlackWhiteSet<String>> entry
                 : infoNonConst.m_stringInfoColor.entrySet())
        {
            StringInfoColor type = entry.getKey();
            BlackWhiteSet<String> set = entry.getValue();
            assert set != null;
            BlackWhiteSet<String> newSet = new BlackWhiteSet<String>();
            newSet.set(BLACK, set.get(BLACK));
            newSet.set(WHITE, set.get(WHITE));
            m_stringInfoColor.put(type, newSet);
        }
    }

    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;
        GameInfo info = (GameInfo)object;
        return (m_handicap == info.getHandicap()
                && ObjectUtil.equals(m_komi, info.getKomi())
                && ObjectUtil.equals(m_timeSettings, info.getTimeSettings())
                && m_stringInfo.equals(info.m_stringInfo)
                && m_stringInfoColor.equals(info.m_stringInfoColor));
    }

    public String get(StringInfo type)
    {
        return m_stringInfo.get(type);
    }

    public String get(StringInfoColor type, GoColor c)
    {
        BlackWhiteSet<String> set = m_stringInfoColor.get(type);
        if (set == null)
            return null;
        return set.get(c);
    }

    public int getHandicap()
    {
        return m_handicap;
    }

    /** Get komi.
        @return The komi or null if komi is unknown. */
    public Komi getKomi()
    {
        return m_komi;
    }

    public TimeSettings getTimeSettings()
    {
        return m_timeSettings;
    }

    /** Hash code dummy function (don't use).
        This class is not desgined to be used in a HashMap/HashTable. The
        function will trigger an assertion if assertions are enabled. */
    public int hashCode()
    {
        assert false : "hashCode not designed";
        return 0;
    }

    public boolean isEmpty()
    {
        return (m_handicap == 0 && m_komi == null && m_stringInfo.isEmpty()
                && m_stringInfoColor.isEmpty() && m_timeSettings == null);
    }

    /** Try to parse rules.
        @return Score.ScoringMethod.TERRITORY if rules string (to lowercase)
        is "japanese", Score.ScoringMethod.AREA otherwise. */
    public ScoringMethod parseRules()
    {
        ScoringMethod result = AREA;
        String rules = get(StringInfo.RULES);
        if (rules != null)
        {
            rules = rules.trim().toLowerCase(Locale.ENGLISH);
            if (rules.equals("japanese"))
                result = TERRITORY;
        }
        return result;
    }

    public void set(StringInfo type, String value)
    {
        value = checkEmpty(value);
        if (value == null)
            m_stringInfo.remove(type);
        else
            m_stringInfo.put(type, value);
    }

    public void set(StringInfoColor type, GoColor c, String value)
    {
        value = checkEmpty(value);
        BlackWhiteSet<String> set = m_stringInfoColor.get(type);
        if (set == null)
        {
            set = new BlackWhiteSet<String>();
            m_stringInfoColor.put(type, set);
        }
        set.set(c, value);
        if (set.get(BLACK) == null && set.get(WHITE) == null)
            m_stringInfoColor.remove(type);
    }

    public void setHandicap(int handicap)
    {
        m_handicap = handicap;
    }

    public void setKomi(Komi komi)
    {
        m_komi = komi;
    }

    /** Set time settings. */
    public void setTimeSettings(TimeSettings timeSettings)
    {
        m_timeSettings = timeSettings;
    }

    /** Suggest a game name from the player names.
        @return A game name built from the player names or null, if not at
        least one player name is known. */
    public String suggestGameName()
    {
        String playerBlack = get(StringInfoColor.NAME, BLACK);
        String playerWhite = get(StringInfoColor.NAME, WHITE);
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
        return playerWhite + " vs. " + playerBlack + " (B)";
    }

    private int m_handicap;

    private Komi m_komi;

    private TimeSettings m_timeSettings;

    private Map<StringInfo,String> m_stringInfo =
        new TreeMap<StringInfo,String>();

    private Map<StringInfoColor,BlackWhiteSet<String>> m_stringInfoColor =
        new TreeMap<StringInfoColor,BlackWhiteSet<String>>();

    private String checkEmpty(String s)
    {
        if (s == null || s.trim().equals(""))
            return null;
        return s;
    }
}
