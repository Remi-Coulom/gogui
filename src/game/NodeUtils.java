//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package game;

import java.util.*;
import java.util.regex.*;
import go.*;

//----------------------------------------------------------------------------

/** Utility functions operating on a tree of nodes. */
public class NodeUtils
{
    /** Find node with a certain move number in main variation containing
        a given node.
        @return null if no such node exists.
    */
    public static Node findByMoveNumber(Node node, int moveNumber)
    {
        int maxMoveNumber = getMoveNumber(node) + getMovesLeft(node);
        if (moveNumber < 0 || moveNumber >  maxMoveNumber)
            return null;
        if (moveNumber < getMoveNumber(node))
        {
            while (node.getFather() != null
                   && getMoveNumber(node) > moveNumber)
                node = node.getFather();
        }
        else
        {
            while (node.getChild() != null
                   && getMoveNumber(node) < moveNumber)
                node = node.getChild();
        }
        return node;
    }

    public static Node findInComments(Node node, Pattern pattern)
    {
        node = nextNode(node);
        while (node != null)
        {
            String comment = node.getComment();
            if (comment != null)
            {
                if (pattern.matcher(comment).find())
                    return node;
            }
            node = nextNode(node);
        }
        return node;
    }

    /** Find the last node that was still in the main variation. */
    public static Node getBackToMainVariation(Node node)
    {
        while (! isInMainVariation(node))
            node = node.getFather();
        return node;
    }

    /** Get all children moves.
        @return Vector contaning the move points, not including passes
        and independent of color.
    */
    public static Vector getChildrenMoves(Node node)
    {
        Vector moves = new Vector();
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
            Node child = node.getChild(i);
            Move childMove = node.getChild(i).getMove();
            if (childMove != null && childMove.getPoint() != null)
                moves.add(childMove.getPoint());
        }
        return moves;
    }

    /** Get child node containg a certain move.
        @return null if no such child existst.
    */
    public static Node getChildWithMove(Node node, Move move)
    {
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
            Node child = node.getChild(i);
            Move childMove = node.getChild(i).getMove();
            if (childMove != null && childMove.equals(move))
                return child;
        }
        return null;
    }

    public static int getDepth(Node node)
    {
        int depth = 0;
        while (node.getFather() != null)
        {
            node = node.getFather();
            ++depth;
        }
        return depth;
    }

    /** Get last node in main variation. */
    public static Node getLast(Node node)
    {
        while (node.getNumberChildren() > 0)
            node = node.getChild();
        return node;
    }

    public static int getMoveNumber(Node node)
    {
        int moveNumber = 0;
        while (node != null)
        {
            if (node.getMove() != null)
                ++moveNumber;
            node = node.getFather();
        }
        return moveNumber;
    }

    /** Moves left in main variation. */
    public static int getMovesLeft(Node node)
    {
        int movesLeft = 0;
        node = node.getChild();
        while (node != null)
        {
            if (node.getMove() != null)
                ++movesLeft;
            node = node.getChild();
        }
        return movesLeft;
    }

    /** Return start node of next variation before this node. */
    public static Node getNextVariation(Node node)
    {
        Node child = node;
        node = node.getFather();
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
    public static int getNodesLeft(Node node)
    {
        int nodesLeft = 0;
        while (node != null)
        {
            ++nodesLeft;
            node = node.getChild();
        }
        return nodesLeft;
    }

    public static Vector getPathFromRoot(Node node)
    {
        Vector result = new Vector();
        while (node != null)
        {
            result.add(0, node);
            node = node.getFather();
        }
        return result;
    }

    /** Return start node of previous variation before this node. */
    public static Node getPreviousVariation(Node node)
    {
        Node child = node;
        node = node.getFather();
        while (node != null && node.variationBefore(child) == null)
        {
            child = node;
            node = node.getFather();
        }
        if (node == null)
            return null;
        return node.variationBefore(child);
    }

    public static Vector getShortestPath(Node start, Node target)
    {
        Vector rootToStart = getPathFromRoot(start);
        Vector rootToTarget = getPathFromRoot(target);
        while (rootToStart.size() > 0 && rootToTarget.size() > 0
               && rootToStart.get(0) == rootToTarget.get(0))
        {
            rootToStart.remove(0);
            rootToTarget.remove(0);
        }
        Vector result = new Vector();
        for (int i = rootToStart.size() - 1; i >= 0; --i)
            result.add(rootToStart.get(i));
        for (int i = 0; i < rootToTarget.size(); ++i)
            result.add(rootToTarget.get(i));
        return result;
    }

    public static boolean isInMainVariation(Node node)
    {
        while (node.getFather() != null)
        {
            if (node.getFather().getChild(0) != node)
                return false;
            node = node.getFather();
        }
        return true;
    }

    public static void makeMainVariation(Node node)
    {
        while (node.getFather() != null)
        {
            node.getFather().makeMainVariation(node);
            node = node.getFather();
        }
    }

    public static Node nextNode(Node node)
    {
        Node child = node.getChild();
        if (child != null)
            return child;
        return getNextVariation(node);
    }

    public static String nodeInfo(Node node)
    {
        StringBuffer buffer = new StringBuffer(512);
        buffer.append("NodeProperties:\n");
        appendInfo(buffer, "Depth", getDepth(node));
        if (node.getMove() != null)
        {
            appendInfo(buffer, "Move", node.getMove().toString());
            appendInfo(buffer, "MoveNumber", getMoveNumber(node));
        }
        Vector addBlack = new Vector();
        for (int i = 0; i < node.getNumberAddBlack(); ++i)
            addBlack.add(node.getAddBlack(i));
        if (node.getNumberAddBlack() > 0)
            appendInfo(buffer, "AddBlack", addBlack);
        Vector addWhite = new Vector();
        for (int i = 0; i < node.getNumberAddWhite(); ++i)
            addWhite.add(node.getAddWhite(i));
        if (node.getNumberAddWhite() > 0)
            appendInfo(buffer, "AddWhite", addWhite);
        if (node.getPlayer() != Color.EMPTY)
            appendInfo(buffer, "Player", node.getPlayer().toString());
        if (! Double.isNaN(node.getTimeLeftBlack()))
            appendInfo(buffer, "TimeLeftBlack", node.getTimeLeftBlack());
        if (node.getMovesLeftBlack() >= 0)
            appendInfo(buffer, "MovesLeftBlack", node.getMovesLeftBlack());
        if (! Double.isNaN(node.getTimeLeftWhite()))
            appendInfo(buffer, "TimeLeftWhite", node.getTimeLeftWhite());
        if (node.getMovesLeftWhite() >= 0)
            appendInfo(buffer, "MovesLeftWhite", node.getMovesLeftWhite());
        Map sgfProperties = node.getSgfProperties();
        if (sgfProperties != null)
        {
            buffer.append("SgfProperties:\n");
            Iterator it = sgfProperties.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry)it.next();
                String label = (String)entry.getKey();
                String value = (String)entry.getValue();
                appendInfo(buffer, label, value);
            }
        }
        return buffer.toString();
    }
        
    private static void appendInfo(StringBuffer buffer, String label,
                                   int value)
    {
        appendInfo(buffer, label, Integer.toString(value));
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   double value)
    {
        appendInfo(buffer, label, Double.toString(value));
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   Vector points)
    {
        appendInfoLabel(buffer, label);
        for (int i = 0; i < points.size(); ++i)
        {
            if (i % 10 == 9 && i < points.size() - 1)
            {
                buffer.append('\n');
                appendInfoLabel(buffer, "");
            }
            buffer.append((Point)points.get(i));
            buffer.append(' ');
        }
        buffer.append('\n');
    }

    private static void appendInfo(StringBuffer buffer, String label,
                                   String value)
    {
        appendInfoLabel(buffer, label);
        buffer.append(value);
        buffer.append('\n');
    }

    private static void appendInfoLabel(StringBuffer buffer, String label)
    {
        buffer.append(label);
        int numberEmpty = Math.max(0, 20 - label.length());
        for (int i = 0; i < numberEmpty; ++i)
            buffer.append(' ');
        buffer.append(' ');
    }
}

//----------------------------------------------------------------------------
