//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package go;

//-----------------------------------------------------------------------------

public class Score
{
    public int m_areaBlack;

    public int m_areaWhite;

    public int m_capturedBlack;

    public int m_capturedWhite;

    public float m_komi;

    public float m_result;

    public float m_resultChinese;

    public float m_resultJapanese;

    public int m_rules;

    public int m_territoryBlack;

    public int m_territoryWhite;

    public String formatResult()
    {
        if (m_result > 0)
            return "Black wins by " + m_result;
        else if (m_result < 0)
            return "White wins by " + (-m_result);
        else
            return "Even";
    }

    public String formatDetailedResult()
    {
        String rules =
            (m_rules == go.Board.RULES_JAPANESE ? "Japanese" : "Chinese");
        return
            "Territory Black: " + m_territoryBlack + "\n" +
            "Territory White: " + m_territoryWhite + "\n" +
            "Area Black: " + m_areaBlack + "\n" +
            "Area White: " + m_areaWhite + "\n" +
            "Captured Black: " + m_capturedBlack + "\n" +
            "Captured White: " + m_capturedWhite + "\n" +
            "Komi: " + m_komi + "\n" +
            "Result Chinese: " + m_resultChinese + "\n" +
            "Result Japanese: " + m_resultJapanese + "\n" +
            "Rules: " + rules + "\n" +
            "\n" +
            "Game result:\n" + formatResult();
    }
}

//-----------------------------------------------------------------------------
