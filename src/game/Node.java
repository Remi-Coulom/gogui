//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package game;

import java.util.*;
import go.*;

//----------------------------------------------------------------------------

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

    /** Add other unspecified SGF property.
        Do not add SGF properties that can be set with other member functions.
        This is for preserving unknown SGF properties that are not used
        by this program.
    */
    public void addSgfProperty(String label, String value)
    {
        if (m_sgfProperties == null)
            m_sgfProperties = new TreeMap();
        m_sgfProperties.put(label, value);
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

    /** Get stones added and moves all as moves.
        Also might include a pass move at the end to make sure, that the
        right color is to move after executing all returned moves.
    */
    public Vector getAllAsMoves()
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
        if (moves.size() > 0)
        {
            Color toMove = getToMove();
            if (toMove == Color.EMPTY)
                toMove = Color.BLACK;
            Move lastMove = (Move)moves.get(moves.size() - 1);
            if (toMove != lastMove.getColor().otherColor())
                moves.add(new Move(null, lastMove.getColor().otherColor()));
        }
        return moves;
    }

    /** Go back to last node that was still in the main variation. */
    public Node getBackToMainVariation()
    {
        Node node = this;
        while (! node.isInMainVariation())
            node = node.m_father;
        return node;
    }

    /** Child of main variation or null if no child */
    public Node getChild()
    {
        if (getNumberChildren() == 0)
            return null;
        return getChild(0);
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
        Node node = getChild();
        while (node != null)
        {
            if (node.m_move != null)
                ++movesLeft;
            node = node.getChild();
        }
        return movesLeft;
    }

    /** Moves left in byoyomi for black.
        -1 if not in byyomi or unknown.
    */
    public int getMovesLeftBlack()
    {
        return m_movesLeftBlack;
    }

    /** Moves left in byoyomi for white.
        -1 if not in byyomi or unknown.
    */
    public int getMovesLeftWhite()
    {
        return m_movesLeftWhite;
    }

    /** Return start node of next variation before this node. */
    public Node getNextVariation()
    {
        Node child = this;
        Node node = getFather();
        while (node != null && node.variationAfter(child) == null)
        {
            child = node;
            node = node.getFather();
        }
        if (node == null)
            return null;
        return node.variationAfter(child);
    }

    /** Nodes left in main variation. */
    public int getNodesLeft()
    {
        int nodesLeft = 0;
        Node node = this;
        while (node != null)
        {
            ++nodesLeft;
            node = node.getChild();
        }
        return nodesLeft;
    }

    public int getNumberAddBlack()
    {
        if (m_addBlack == null)
            return 0;
        return m_addBlack.size();
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

    public Vector getPathFromRoot()
    {
        Vector result = new Vector();
        Node node = this;
        while (node != null)
        {
            result.add(0, node);
            node = node.getFather();
        }
        return result;
    }

    /** Return color to play if explicitely set.
        Returns Color.EMPTY if color is not explicitely set.
        @see getToMove() for getting the color to play.
    */
    public Color getPlayer()
    {
        return m_player;
    }

    /** Return start node of previous variation before this node. */
    public Node getPreviousVariation()
    {
        Node child = this;
        Node node = getFather();
        while (node != null && node.variationBefore(child) == null)
        {
            child = node;
            node = node.getFather();
        }
        if (node == null)
            return null;
        return node.variationBefore(child);
    }

    /** Get other unspecified SGF properties.
        @see setSgfProperty.
    */
    public Map getSgfProperties()
    {
        return m_sgfProperties;
    }

    public Vector getShortestPath(Node node)
    {
        Vector rootToThis = getPathFromRoot();
        Vector rootToNode = node.getPathFromRoot();
        while (rootToThis.size() > 0 && rootToNode.size() > 0
               && rootToThis.get(0) == rootToNode.get(0))
        {
            rootToThis.remove(0);
            rootToNode.remove(0);
        }
        Vector result = new Vector();
        for (int i = rootToThis.size() - 1; i >= 0; --i)
            result.add(rootToThis.get(i));
        for (int i = 0; i < rootToNode.size(); ++i)
            result.add(rootToNode.get(i));
        return result;
    }

    /** Time left for black after move was made.
        Returns Float.NaN if unknown.
    */
    public float getTimeLeftBlack()
    {
        return m_timeLeftBlack;
    }

    /** Time left for white after move was made.
        Returns Float.NaN if unknown.
    */
    public float getTimeLeftWhite()
    {
        return m_timeLeftWhite;
    }

    /** Get color to move.
        Determining the color to move takes into consideration an explicitely
        set player color and moves contained in this node.
        If nothing is known about the color to move, it returns Color.EMPTY.
    */
    public Color getToMove()
    {
        if (m_player != Color.EMPTY)
            return m_player;
        if (m_move != null)
            return m_move.getColor().otherColor();
        return Color.EMPTY;
    }

    public boolean hasChildWithMove(Move move)
    {
        for (int i = 0; i < getNumberChildren(); ++i)
        {
            Move m = getChild(i).getMove();
            if (m != null && m.equals(move))
                return true;
        }
        return false;
    }

    public boolean isChildOf(Node node)
    {
        for (int i = 0; i < node.getNumberChildren(); ++i)
            if (node.getChild(i) == this)
                return true;
        return false;
    }

    public boolean isInMainVariation()
    {
        Node node = this;
        while (node.m_father != null)
        {
            if (node.m_father.getChild(0) != node)
                return false;
            node = node.m_father;
        }
        return true;
    }

    public void makeMainVariation()
    {
        Node node = this;
        while (node.m_father != null)
        {
            node.m_father.makeMainVariation(node);
            node = node.m_father;
        }
    }

    public void removeVariations()
    {
        if (m_children == null || m_children.size() <= 1)
            return;
        Node child = getChild(0);
        m_children.removeAllElements();
        m_children.add(child);
    }

    public void setComment(String comment)
    {
        m_comment = comment;
    }

    public void setFather(Node father)
    {
        m_father = father;
    }

    public void setMove(Move move)
    {
        m_move = move;
    }

    public void setMovesLeftBlack(int moves)
    {
        m_movesLeftBlack = moves;
    }

    public void setMovesLeftWhite(int moves)
    {
        m_movesLeftWhite = moves;
    }

    public void setTimeLeftBlack(float timeLeft)
    {
        m_timeLeftBlack = timeLeft;
    }

    public void setTimeLeftWhite(float timeLeft)
    {
        m_timeLeftWhite = timeLeft;
    }

    public void setPlayer(Color color)
    {
        assert(color == Color.BLACK || color == Color.WHITE);
        m_player = color;
    }

    public void removeChild(Node child)
    {
        assert(m_children.contains(child));
        m_children.remove(child);
    }

    private int m_movesLeftBlack = -1;

    private int m_movesLeftWhite = -1;

    private float m_timeLeftBlack = Float.NaN;

    private float m_timeLeftWhite = Float.NaN;

    private Color m_player = Color.EMPTY;

    private Move m_move;

    private Node m_father;

    private String m_comment;

    private TreeMap m_sgfProperties;

    private Vector m_addBlack;

    private Vector m_addWhite;

    private Vector m_children;

    private void makeMainVariation(Node child)
    {
        assert(m_children.contains(child));
        m_children.remove(child);
        m_children.add(0, child);
    }

    private Node variationBefore(Node child)
    {
        int numberChildren = getNumberChildren();
        if (numberChildren == 1)
            return null;
        int i;
        for (i = 0; i < numberChildren; ++i)
            if (getChild(i) == child)
                break;
        if (i == 0)
            return null;
        return getChild(i - 1);
    }

    private Node variationAfter(Node child)
    {
        int numberChildren = getNumberChildren();
        if (numberChildren == 1)
            return null;
        int i;
        for (i = 0; i < numberChildren; ++i)
            if (getChild(i) == child)
                break;
        if (i == numberChildren - 1)
            return null;
        return getChild(i + 1);
    }

}

//----------------------------------------------------------------------------
