//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package game;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Vector;
import go.Color;
import go.Point;

//----------------------------------------------------------------------------

/** Game tree. */
public class GameTree
{
    public GameTree()
    {
        m_gameInformation = new GameInformation(19);
        setDate();
        m_root = new Node();
    }

    public GameTree(int boardSize, double komi, Vector handicap, String rules,
                    TimeSettings timeSettings)
    {
        m_gameInformation = new GameInformation(boardSize);
        setDate();
        m_root = new Node();
        m_gameInformation.m_komi = komi;
        m_gameInformation.m_rules = rules;
        if (timeSettings != null)
            m_gameInformation.m_timeSettings = new TimeSettings(timeSettings);
        if (handicap != null)
        {
            m_gameInformation.m_handicap = handicap.size();
            if (handicap.size() > 0)
            {
                m_root.setPlayer(Color.WHITE);
                for (int i = 0; i < handicap.size(); ++i)
                    m_root.addBlack((Point)handicap.get(i));
            }
        }
    }

    public GameTree(GameInformation gameInformation, Node root)
    {
        m_gameInformation = gameInformation;
        m_root = root;
    }

    public GameInformation getGameInformation()
    {
        return m_gameInformation;
    }

    public Node getRoot()
    {
        return m_root;
    }

    public boolean hasVariations()
    {        
        Node node = m_root;
        while (node != null)
        {
            if (node.getNumberChildren() > 1)
                return true;
            node = node.getChild();
        }
        return false;
    }

    public void keepOnlyMainVariation()
    {
        Node node = m_root;
        while (node != null)
        {
            node.removeVariations();
            node = node.getChild();
        }
    }

    private final GameInformation m_gameInformation;

    private final Node m_root;

    private void setDate()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        DecimalFormat format = new DecimalFormat("00");
        m_gameInformation.m_date =
            Integer.toString(year) + "-" + format.format(month)
            + "-" + format.format(day);
    }
}

//----------------------------------------------------------------------------
