//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

/** Count the final score on a Go board.
    Allows to mark stones as dead and count the territory surrounded by
    alive stones of one color.
    The reason why all stones in a dead block have to marked as dead is that
    Go engines return a list of dead stones on the final_status GTP command.
    It could happen that the program returns nonsense (e.g. a contiguous block
    of stones with only some stones dead) and this class should not crash
    if that happens (even if the score will be no longer meaningful).
*/
public class CountScore
{
    /** Begin counting a score.
        @param board The board.
        @param isDeadStone Initial set of stones to be marked as dead.
     */
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

    /** Update score after changing the life-death status of stones. */
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
                    setScore(territory, GoColor.BLACK);
                else
                {
                    mark.set(territory, false);
                    if (isTerritory(mark, p, territory, GoColor.WHITE))
                        setScore(territory, GoColor.WHITE);
                    else
                        mark.set(territory, false);
                }
            }
        }
    }

    /** Get the owner of a point.
        @param p The point (empty or occupied)
        @return GoColor.BLACK, if point belongs to Black; GoColor.WHITE, if
        point belongs to White; GoColor.EMPTY, if point is neutral.
    */
    public GoColor getColor(GoPoint p)
    {
        return m_score[p.getX()][p.getY()];
    }

    /** Get the life-death status of a stone.
        @param p The stone.
        @return true, if stone is dead, false if stone is alive.
    */
    public boolean getDead(GoPoint p)
    {
        return m_dead.get(p);
    }

    /** Get the score.
        @param komi The komi.
        @param rules The rules (Board.RULES_CHINESE or Board.RULES_JAPANESE)
    */
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

    /** Change the life-death status of a stone.
        All stones in a block have to be marked as dead or alive (see comment
        in the description of this class).
        You have to call #update() to update the score after changing the
        life-death status of one or more stones.
        @param p The stone.
        @param value true, if stone is dead, false if stone is alive.
    */
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

    private void setScore(ArrayList points, GoColor c)
    {
        for (int i = 0; i < points.size(); ++i)
            setScore((GoPoint)points.get(i), c);
    }
}

