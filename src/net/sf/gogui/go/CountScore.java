//----------------------------------------------------------------------------
// CountScore.java
//----------------------------------------------------------------------------

package net.sf.gogui.go;

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
        @param deadStones Initial set of stones to be marked as dead.
     */
    public void begin(ConstBoard board, ConstPointList deadStones)
    {
        m_board = board;
        int size = board.getSize();
        m_dead = new Marker(size);
        m_score = new GoColor[size][size];
        for (int i = 0; i < m_board.getPoints().size(); ++i)
            m_dead.clear(m_board.getPoints().get(i));
        if (deadStones != null)
            for (int i = 0; i < deadStones.size(); ++i)
                m_dead.set(deadStones.get(i));
        compute();
    }

    /** Change the life and death status of a group of stones.
        Will change the life and death status of all stones of the same color
        in the connected region surrounded by opponent stones, if all
        surrounding opponent stones are alive. Otherwise it only changes the
        life death status of all stones in the block the stone belongs to.
        @param p Location of a stone.
        @return List of all points that changed their life and death status.
     */
    public PointList changeStatus(GoPoint p)
    {
        GoColor c = m_board.getColor(p);
        assert c.isBlackWhite();
        PointList stones = new PointList();
        Marker marker = new Marker(m_board.getSize());
        boolean allSurroundingAlive = findRegion(p, c, marker, stones);
        if (! allSurroundingAlive)
        {
            stones.clear();
            m_board.getStones(p, c, stones);
        }
        boolean isDead = ! isDead(p);
        for (int i = 0; i < stones.size(); ++i)
            setDead(stones.get(i), isDead);
        return stones;
    }

    /** Update score after changing the life-death status of stones. */
    public void compute()
    {
        Marker mark = new Marker(m_board.getSize());
        boolean allEmpty = true;
        for (int i = 0; i < m_board.getPoints().size(); ++i)
        {
            GoPoint p = m_board.getPoints().get(i);
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
        PointList territory = new PointList(m_board.getPoints().size());
        for (int i = 0; i < m_board.getPoints().size(); ++i)
        {
            GoPoint p = m_board.getPoints().get(i);
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
    public boolean isDead(GoPoint p)
    {
        return m_dead.get(p);
    }

    /** Get the score.
        @param komi The komi.
        @param rules The rules (Score.AREA or Score.TERRITORY)
    */
    public Score getScore(Komi komi, int rules)
    {
        Score s = new Score();
        s.m_rules = rules;
        s.m_komi = komi;
        s.m_capturedBlack = m_board.getCaptured(GoColor.BLACK);
        s.m_capturedWhite = m_board.getCaptured(GoColor.WHITE);
        int areaDiff = 0;
        int territoryDiff = 0;
        for (int i = 0; i < m_board.getPoints().size(); ++i)
        {
            GoPoint p = m_board.getPoints().get(i);
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
        s.m_resultArea = areaDiff;
        s.m_resultTerritory =
            s.m_capturedWhite - s.m_capturedBlack + territoryDiff;
        if (komi != null)
        {
            s.m_resultArea -= komi.toDouble();
            s.m_resultTerritory -= komi.toDouble();
        }
        if (rules == Score.TERRITORY)
            s.m_result = s.m_resultTerritory;
        else
            s.m_result = s.m_resultArea;
        return s;
    }

    /** Change the life-death status of a stone.
        All stones in a block have to be marked as dead or alive (see comment
        in the description of this class).
        You have to call #compute to update the score after changing the
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

    private boolean findRegion(GoPoint p, GoColor color, Marker marker,
                               PointList stones)
    {
        if (marker.get(p))
            return true;
        GoColor c = m_board.getColor(p);
        if (c == color.otherColor())
            return ! isDead(p);
        marker.set(p);
        if (c == color)
            stones.add(p);
        ConstPointList adj = m_board.getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! findRegion(adj.get(i), color, marker, stones))
                return false;
        return true;
    }

    private boolean isTerritory(Marker mark, GoPoint p,
                                PointList territory, GoColor color)
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
        ConstPointList adj = m_board.getAdjacentPoints(p);
        for (int i = 0; i < adj.size(); ++i)
            if (! isTerritory(mark, adj.get(i), territory, color))
                return false;
        return true;
    }

    private void setScore(GoPoint p, GoColor c)
    {
        assert c != null;
        m_score[p.getX()][p.getY()] = c;
    }

    private void setScore(ConstPointList points, GoColor c)
    {
        for (int i = 0; i < points.size(); ++i)
            setScore(points.get(i), c);
    }
}
