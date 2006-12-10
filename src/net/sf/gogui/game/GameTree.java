//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

/** Game tree. */
public class GameTree
    implements ConstGameTree
{
    public GameTree()
    {
        m_gameInformation = new GameInformation(GoPoint.DEFAULT_SIZE);
        setDate();
        m_root = new Node();
    }

    public GameTree(int boardSize, double komi, ArrayList handicap,
                    String rules, TimeSettings timeSettings)
    {
        m_gameInformation = new GameInformation(boardSize);
        setDate();
        m_root = new Node();
        m_gameInformation.setKomi(komi);
        m_gameInformation.setRules(rules);
        if (timeSettings != null)
            m_gameInformation.setTimeSettings(timeSettings);
        if (handicap != null)
        {
            m_gameInformation.setHandicap(handicap.size());
            if (handicap.size() > 0)
            {
                m_root.setPlayer(GoColor.WHITE);
                for (int i = 0; i < handicap.size(); ++i)
                    m_root.addBlack((GoPoint)handicap.get(i));
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

    public ConstGameInformation getGameInformationConst()
    {
        return m_gameInformation;
    }

    /** Get a non-const reference to a const node.
        Requires: node is part of this game tree.
    */
    public Node getNode(ConstNode node)
    {
        assert(NodeUtil.getRoot(node) == getRoot());
        return (Node)node;
    }

    public Node getRoot()
    {
        return m_root;
    }

    public ConstNode getRootConst()
    {
        return m_root;
    }

    public boolean hasVariations()
    {        
        ConstNode node = m_root;
        while (node != null)
        {
            if (node.getNumberChildren() > 1)
                return true;
            node = node.getChildConst();
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
        m_gameInformation.setDate(Integer.toString(year) + "-"
                                  + format.format(month) + "-"
                                  + format.format(day));
    }
}

