//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package go;

//----------------------------------------------------------------------------

public class Score
{
    public int m_areaBlack;

    public int m_areaWhite;

    public int m_capturedBlack;

    public int m_capturedWhite;

    public double m_komi;

    public double m_result;

    public double m_resultChinese;

    public double m_resultJapanese;

    public int m_rules;

    public int m_territoryBlack;

    public int m_territoryWhite;

    public String formatResult()
    {
        return formatResult(m_result);
    }

    public static String formatResult(double result)
    {
        if (result > 0)
            return "B+" + result;
        else if (result < 0)
            return "W+" + (-result);
        else
            return "0";
    }
}

//----------------------------------------------------------------------------
