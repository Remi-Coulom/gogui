// Score.java

package net.sf.gogui.go;

/** Result of a game.
    Includes information about the score under Chinese and Japanese rules,
    the rules and komi used, territory, area (stones and territory) and
    number of captured stones. */
public class Score
{
    public enum ScoringMethod
    {
        /** Constant for area scoring method (Chinese). */
        AREA,

        /** Constant for territory scoring method (Japanese). */
        TERRITORY;
    }

    public int m_areaBlack;

    public int m_areaWhite;

    public int m_capturedBlack;

    public int m_capturedWhite;

    public Komi m_komi;

    public double m_result;

    public double m_resultArea;

    public double m_resultTerritory;

    public ScoringMethod m_rules;

    public int m_territoryBlack;

    public int m_territoryWhite;

    public String formatResult()
    {
        return formatResult(m_result);
    }

    public static String formatResult(double result)
    {
        long intResult = Math.round(result * 2);
        String strResult;
        if (intResult % 2 == 0)
            strResult = Long.toString(intResult / 2);
        else
            strResult = Long.toString(intResult / 2) + ".5";
        if (intResult > 0)
            return "B+" + strResult;
        else if (intResult < 0)
            return "W+" + (-result);
        else
            return "0";
    }

    public void updateRules(ScoringMethod rules)
    {
        m_rules = rules;
        if (rules == ScoringMethod.TERRITORY)
            m_result = m_resultTerritory;
        else
        {
            assert rules == ScoringMethod.AREA;
            m_result = m_resultArea;
        }
    }
}
