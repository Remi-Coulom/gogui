//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package game;

import java.util.*;
import go.*;

//-----------------------------------------------------------------------------

public class Node
{
    public Node()
    {
    }

    public Node(Move move)
    {
        m_move = move;
    }

    public void append(Node node)
    {
        assert(node.m_father == null);
        if (m_children == null)
            m_children = new Vector(1);
        node.m_father = this;
        m_children.add(node);
    }

    public void addBlack(Point point)
    {
        assert(point != null);
        if (m_addBlack == null)
            m_addBlack = new Vector(1);
        m_addBlack.add(point);
    }

    public void addWhite(Point point)
    {
        assert(point != null);
        if (m_addWhite == null)
            m_addWhite = new Vector(1);
        m_addWhite.add(point);
    }

    public Point getAddBlack(int i)
    {
        return (Point)m_addBlack.get(i);
    }

    public Point getAddWhite(int i)
    {
        return (Point)m_addWhite.get(i);
    }

    /** Get stones added and moves all as moves. */
    public Vector getAddStonesAndMoves()
    {
        Vector moves = new Vector();
        if (m_addBlack != null)
            for (int i = 0; i < m_addBlack.size(); ++i)
                moves.add(new Move((Point)m_addBlack.get(i), Color.BLACK));
        if (m_addWhite != null)
            for (int i = 0; i < m_addWhite.size(); ++i)
                moves.add(new Move((Point)m_addWhite.get(i), Color.WHITE));
        if (m_move != null)
            moves.add(m_move);
        return moves;
    }

    public Node getChild(int i)
    {
        return (Node)m_children.get(i);
    }

    public String getComment()
    {
        return m_comment;
    }

    public int getDepth()
    {
        int depth = 0;
        Node node = this;
        while (node.m_father != null)
        {
            node = node.m_father;
            ++depth;
        }
        return depth;
    }

    public Node getFather()
    {
        return m_father;
    }

    public Move getMove()
    {
        return m_move;
    }

    public int getMoveNumber()
    {
        int moveNumber = 0;
        Node node = this;
        while (node != null)
        {
            if (node.m_move != null)
                ++moveNumber;
            node = node.m_father;
        }
        return moveNumber;
    }

    /** Moves left in main variation. */
    public int getMovesLeft()
    {
        int movesLeft = 0;
        Node node = this;
        while (node.m_children != null)
        {
            node = node.getChild(0);
            if (node.m_move != null)
                ++movesLeft;
        }
        return movesLeft;
    }

    /** Nodes left in main variation. */
    public int getNodesLeft()
    {
        int nodesLeft = 0;
        Node node = this;
        while (node.m_children != null)
        {
            node = node.getChild(0);
            ++nodesLeft;
        }
        return nodesLeft;
    }

    public int getNumberAddBlack()
    {
        if (m_addBlack == null)
            return 0;
        return m_addBlack.size();
    }

    public int getNumberAddStonesAndMoves()
    {
        int result = 0;
        if (m_addBlack != null)
            result += m_addBlack.size();
        if (m_addWhite != null)
            result += m_addWhite.size();
        if (m_move != null)
            ++result;
        return result;
    }

    public int getNumberAddWhite()
    {
        if (m_addWhite == null)
            return 0;
        return m_addWhite.size();
    }

    public int getNumberChildren()
    {
        if (m_children == null)
            return 0;
        return m_children.size();
    }

    /** @return Color.EMPTY if unknown */
    public Color getToMove()
    {
        return m_toMove;
    }

    public void setToMove(Color color)
    {
        assert(color == Color.BLACK || color == Color.WHITE);
        m_toMove = color;
    }

    private Color m_toMove = Color.EMPTY;

    private Move m_move;

    private Node m_father;

    private String m_comment;

    private Vector m_addBlack;

    private Vector m_addWhite;

    private Vector m_children;
}

//-----------------------------------------------------------------------------
