//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

//----------------------------------------------------------------------------

public class CountScore
{
    public void begin(ConstBoard board, GoPoint[] isDeadStone)
    {
        m_board = board;
        int size = board.getSize();
        m_dead = new Marker(size);
        m_score = new GoColor[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            m_dead.clear(m_board.getPoint(i));
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                m_dead.set(isDeadStone[i]);
        compute();
    }

    public void compute()
    {
        Marker mark = new Marker(m_board.getSize());
        boolean allEmpty = true;
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            GoColor c = m_board.getColor(p);
            setScore(p, GoColor.EMPTY);
            if (c != GoColor.EMPTY)
            {
                allEmpty = false;
                if (! m_dead.get(p))
                    setScore(p, c);
            }
        }
        if (allEmpty)
            return;
        ArrayList territory = new ArrayList(m_board.getNumberPoints());
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            if (! mark.get(p))
            {
                territory.clear();
                if (isTerritory(mark, p, territory, GoColor.BLACK))
                {
                    for (int j = 0; j < territory.size(); ++j)
                        setScore((GoPoint)territory.get(j), GoColor.BLACK);
                }
                else
                {
                    mark.set(territory, false);
                    if (isTerritory(mark, p, territory, GoColor.WHITE))
                    {
                        for (int j = 0; j < territory.size(); ++j)
                            setScore((GoPoint)territory.get(j),
                                     GoColor.WHITE);
                    }
                    else
                        mark.set(territory, false);
                }
            }
        }
    }

    public GoColor getColor(GoPoint p)
    {
        return m_score[p.getX()][p.getY()];
    }

    public boolean getDead(GoPoint p)
    {
        return m_dead.get(p);
    }

    public Score getScore(double komi, int rules)
    {
        Score s = new Score();
        s.m_rules = rules;
        s.m_komi = komi;
        s.m_capturedBlack = m_board.getCapturedB();
        s.m_capturedWhite = m_board.getCapturedW();
        int areaDiff = 0;
        int territoryDiff = 0;
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            GoColor c = m_board.getColor(p);
            GoColor sc = getColor(p);
            if (sc == GoColor.BLACK)
            {
                ++s.m_areaBlack;
                ++areaDiff;
            }
            else if (sc == GoColor.WHITE)
            {
                ++s.m_areaWhite;
                --areaDiff;
            }
            if (c == GoColor.EMPTY)
            {
                if (sc == GoColor.BLACK)
                {
                    ++s.m_territoryBlack;
                    ++territoryDiff;
                }
                else if (sc == GoColor.WHITE)
                {
                    ++s.m_territoryWhite;
                    --territoryDiff;
                }
            }
            if (c == GoColor.BLACK && sc == GoColor.WHITE)
            {
                ++s.m_capturedBlack;
                ++s.m_territoryWhite;
                --territoryDiff;
            }
            if (c == GoColor.WHITE && sc == GoColor.BLACK)
            {
                ++s.m_capturedWhite;
                ++s.m_territoryBlack;
                ++territoryDiff;
            }
        }
        s.m_resultChinese = areaDiff - komi;
        s.m_resultJapanese =
            s.m_capturedWhite - s.m_capturedBlack + territoryDiff - komi;
        if (rules == Board.RULES_JAPANESE)
            s.m_result = s.m_resultJapanese;
        else
            s.m_result = s.m_resultChinese;
        return s;
    }

    public void setDead(GoPoint p, boolean value)
    {
        m_dead.set(p, value);
    }

    private Marker m_dead;

    private GoColor m_score[][];

    private ConstBoard m_board;

    private boolean isTerritory(Marker mark, GoPoint p,
                                ArrayList territory, GoColor color)
    {
        GoColor c = getColor(p);
        if (c == color.otherColor() && ! m_dead.get(p))
            return false;
        if (c.equals(color))
            return (! m_dead.get(p));
        if (mark.get(p))
            return true;
        mark.set(p, true);
        territory.add(p);
        ArrayList adj = m_board.getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isTerritory(mark, (GoPoint)(adj.get(i)), territory, color))
                return false;
        return true;
    }

    private void setScore(GoPoint p, GoColor c)
    {
        assert(c != null);
        m_score[p.getX()][p.getY()] = c;
    }
}

//----------------------------------------------------------------------------
