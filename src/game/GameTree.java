//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package game;

import java.text.*;
import java.util.*;

//-----------------------------------------------------------------------------

/** Game tree. */
public class GameTree
{
    public GameTree()
    {
        m_gameInformation = new GameInformation();
        setDate();
        m_root = new Node();
    }

    public GameTree(int boardSize)
    {
        m_gameInformation = new GameInformation();
        setDate();
        m_root = new Node();
        m_gameInformation.m_boardSize = boardSize;
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

    private GameInformation m_gameInformation;

    private Node m_root;

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

//-----------------------------------------------------------------------------
