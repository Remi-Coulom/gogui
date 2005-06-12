//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

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
}

//----------------------------------------------------------------------------
