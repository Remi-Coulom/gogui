//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Utility functions operating on a tree of nodes. */
public final class NodeUtils
{
    /** Find first node with a certain move number in main variation
        containing a given node.
        @return null if no such node exists.
    */
    public static Node findByMoveNumber(Node node, int moveNumber)
    {
        int maxMoveNumber = getMoveNumber(node) + getMovesLeft(node);
        if (moveNumber < 0 || moveNumber >  maxMoveNumber)
            return null;
        if (moveNumber <= getMoveNumber(node))
        {
            while (node.getFather() != null
                   && (getMoveNumber(node) > moveNumber
                       || node.getMove() == null))
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

    /** Return first node of a given variation.
        Returns the root node node if variation string is empty,
        otherwise it returns the node that can be reached from the root node
        by taking the children defined by the integers in the variation string
        for nodes with more than one child.
        Returns null, if the variation string is invalid or does not specify
        a node in the given tree.
    */
    public static Node findByVariation(Node root, String variation)
    {
        if (variation.trim().equals(""))
            return root;
        String[] tokens = StringUtils.split(variation, '.');
        int[] n = new int[tokens.length];
        for (int i = 0; i < tokens.length; ++i)
        {
            try
            {
                n[i] = Integer.parseInt(tokens[i]) - 1;
                if (n[i] < 0)
                    return null;
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        Node node = root;
        for (int i = 0; i < n.length; ++i)
        {
            while (node.getNumberChildren() <= 1)
            {
                node = node.getChild();
                if (node == null)
                    return null;
            }
            if (n[i] >= node.getNumberChildren())
                return null;
            node = node.getChild(n[i]);
        }
        return node;
    }

    public static boolean commentContains(Node node, Pattern pattern)
    {
        String comment = node.getComment();
        return (comment != null && pattern.matcher(comment).find());
    }

    public static Node findInComments(Node node, Pattern pattern)
    {
        node = nextNode(node);
        while (node != null)
        {
            if (commentContains(node, pattern))
                return node;
            node = nextNode(node);
        }
        return null;
    }

    /** Find the last node that was still in the main variation. */
    public static Node getBackToMainVariation(Node node)
    {
        while (! isInMainVariation(node))
            node = node.getFather();
        return node;
    }

    /** Get all children moves.
        @return ArrayList contaning the move points, not including passes
        and independent of color.
    */
    public static ArrayList getChildrenMoves(Node node)
    {
        ArrayList moves = new ArrayList();
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
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

    /** Return next variation of this node. */
    public static Node getNextVariation(Node node)
    {
        Node father = node.getFather();
        if (father == null)
            return null;
        return father.variationAfter(node);
    }

    /** Return next variation before this node. */
    public static Node getNextEarlierVariation(Node node)
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

    /** Get nodes in path from root to a given node. */
    public static ArrayList getPathFromRoot(Node node)
    {
        ArrayList result = new ArrayList();
        while (node != null)
        {
            result.add(0, node);
            node = node.getFather();
        }
        return result;
    }

    /** Return previous variation of this node. */
    public static Node getPreviousVariation(Node node)
    {
        Node father = node.getFather();
        if (father == null)
            return null;
        return father.variationBefore(node);
    }

    /** Return previous variation before this node. */
    public static Node getPreviousEarlierVariation(Node node)
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
        node = node.variationBefore(child);
        if (node == null)
            return null;
        while (hasSubtree(node))
            node = node.getChild(node.getNumberChildren() - 1);
        return node;
    }

    /** Get shortest path between start and target node.
        The shortest path goes backward from the start node (inclusive) until
        the last common moves of both variations (exclusive) and then forward
        to the target node (inclusive).
        @param start Start node
        @param target Target node
        @param nodes Nodes to execute after undoing the number of nodes
        returned
        @return Number if moves to undo
    */
    public static int getShortestPath(Node start, Node target,
                                      ArrayList nodes)
    {
        ArrayList rootToStart = getPathFromRoot(start);
        ArrayList rootToTarget = getPathFromRoot(target);
        while (rootToStart.size() > 0 && rootToTarget.size() > 0
               && rootToStart.get(0) == rootToTarget.get(0))
        {
            rootToStart.remove(0);
            rootToTarget.remove(0);
        }
        nodes.clear();
        for (int i = 0; i < rootToTarget.size(); ++i)
            nodes.add(rootToTarget.get(i));
        return rootToStart.size();
    }

    /** Get a text representation of the variation to a certain node.
        The string contains the number of the child for each node with more
        than one child in the path from the root node to this node.
        The childs are counted starting with 1 and the numbers are separated
        by colons.
    */
    public static String getVariationString(Node node)
    {
        ArrayList list = new ArrayList();
        while (node != null)
        {
            Node father = node.getFather();
            if (father != null && father.getNumberChildren() > 1)
            {
                int index = father.getChildIndex(node) + 1;
                list.add(0, Integer.toString(index));
            }
            node = father;
        }
        StringBuffer result = new StringBuffer(list.size() * 3);
        for (int i = 0; i < list.size(); ++i)
        {
            result.append((String)list.get(i));
            if (i < list.size() - 1)
                result.append('.');
        }
        return result.toString();
    }

    /** Subtree of node contains at least one node with 2 or more children. */
    public static boolean hasSubtree(Node node)
    {
        while (node != null && node.getNumberChildren() < 2)
            node = node.getChild();
        return (node != null);
    }

    /** Check if game is in cleanup stage.
        Cleanup stage is after two consecutive pass moves have been played.
    */
    public static boolean isInCleanup(Node node)
    {
        boolean lastPass = false;
        while (node != null)
        {
            Move move = node.getMove();
            if (move != null)
            {
                if (move.getPoint() == null)
                {
                    if (lastPass)
                        return true;
                    lastPass = true;
                }
                else
                    lastPass = false;
            }
            node = node.getFather();
        }
        return false;
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

    public static boolean isRootWithoutChildren(Node node)
    {
        return (node.getFather() == null && node.getChild() == null);
    }

    public static void makeMainVariation(Node node)
    {
        while (node.getFather() != null)
        {
            node.getFather().makeMainVariation(node);
            node = node.getFather();
        }
    }

    /** Create a game tree with the current board position as setup stones. */
    public static GameTree makeTreeFromPosition(GameInformation info,
                                                Board board)
    {
        GameTree tree = new GameTree(board.getSize(), info.m_komi, null,
                                     info.m_rules, info.m_timeSettings);
        Node root = tree.getRoot();
        for (int i = 0; i < board.getNumberPoints(); ++i)
        {
            GoPoint point = board.getPoint(i);
            GoColor color = board.getColor(point);
            if (color == GoColor.BLACK)
                root.addBlack(point);
            else if (color == GoColor.WHITE)
                root.addWhite(point);
        }
        root.setPlayer(board.getToMove());
        return tree;
    }

    /** Get next node for iteration in complete tree. */
    public static Node nextNode(Node node)
    {
        Node child = node.getChild();
        if (child != null)
            return child;
        return getNextEarlierVariation(node);
    }

    /** Get next node for iteration in subtree. */
    public static Node nextNode(Node node, int depth)
    {
        node = nextNode(node);
        if (node == null || NodeUtils.getDepth(node) <= depth)
            return null;
        return node;
    }

    public static String nodeInfo(Node node)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("NodeProperties:\n");
        appendInfo(buffer, "Depth", getDepth(node));
        appendInfo(buffer, "Children", node.getNumberChildren());
        if (node.getMove() != null)
        {
            appendInfo(buffer, "Move", node.getMove().toString());
            appendInfo(buffer, "MoveNumber", getMoveNumber(node));
        }
        appendInfo(buffer, "Variation", getVariationString(node));
        ArrayList addBlack = new ArrayList();
        for (int i = 0; i < node.getNumberAddBlack(); ++i)
            addBlack.add(node.getAddBlack(i));
        if (node.getNumberAddBlack() > 0)
            appendInfo(buffer, "AddBlack", addBlack);
        ArrayList addWhite = new ArrayList();
        for (int i = 0; i < node.getNumberAddWhite(); ++i)
            addWhite.add(node.getAddWhite(i));
        if (node.getNumberAddWhite() > 0)
            appendInfo(buffer, "AddWhite", addWhite);
        ArrayList addEmpty = new ArrayList();
        for (int i = 0; i < node.getNumberAddEmpty(); ++i)
            addEmpty.add(node.getAddEmpty(i));
        if (node.getNumberAddEmpty() > 0)
            appendInfo(buffer, "AddEmpty", addEmpty);
        if (node.getPlayer() != GoColor.EMPTY)
            appendInfo(buffer, "Player", node.getPlayer().toString());
        if (! Double.isNaN(node.getTimeLeft(GoColor.BLACK)))
            appendInfo(buffer, "TimeLeftBlack",
                       node.getTimeLeft(GoColor.BLACK));
        if (node.getMovesLeft(GoColor.BLACK) >= 0)
            appendInfo(buffer, "MovesLeftBlack",
                       node.getMovesLeft(GoColor.BLACK));
        if (! Double.isNaN(node.getTimeLeft(GoColor.WHITE)))
            appendInfo(buffer, "TimeLeftWhite",
                       node.getTimeLeft(GoColor.WHITE));
        if (node.getMovesLeft(GoColor.WHITE) >= 0)
            appendInfo(buffer, "MovesLeftWhite",
                       node.getMovesLeft(GoColor.WHITE));
        appendInfoComment(buffer, node);
        for (int i = 0; i < Node.MARK_TYPES.length; ++i)
        {
            String type = Node.MARK_TYPES[i];
            ArrayList marked = node.getMarked(type);
            if (marked != null && marked.size() > 0)
                appendInfo(buffer, "Marked" + StringUtils.capitalize(type),
                           marked);
        }
        Map labels = node.getLabels();
        if (labels != null && labels.size() > 0)
        {
            StringBuffer labelsBuffer = new StringBuffer();
            Iterator iter = labels.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                GoPoint point = (GoPoint)entry.getKey();
                String value = (String)entry.getValue();
                labelsBuffer.append(point);
                labelsBuffer.append(':');
                labelsBuffer.append(value);
                if (iter.hasNext())
                    labelsBuffer.append(' ');
            }
            appendInfo(buffer, "Labels", labelsBuffer.toString());
        }
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
        
    public static boolean subtreeGreaterThan(Node node, int size)
    {
        int n = 0;
        int depth = NodeUtils.getDepth(node);
        while (node != null)
        {
            ++n;
            if (n > size)
                return true;
            node = nextNode(node, depth);
        }
        return false;
    }

    /** Number of nodes in subtree.
        Does not include this node.
    */
    public static int subtreeSize(Node node)
    {
        int n = 0;
        int depth = NodeUtils.getDepth(node);
        while (node != null)
        {
            ++n;
            node = nextNode(node, depth);
        }
        return n;
    }

    public static String treeInfo(Node node)
    {
        int numberNodes = 0;
        int numberTerminal = 0;
        int maxDepth = 0;
        int maxChildren = 0;
        double averageDepth = 0;
        double averageChildren = 0;
        double averageChildrenInner = 0;
        int rootDepth = getDepth(node);
        while (node != null)
        {
            ++numberNodes;
            int numberChildren = node.getNumberChildren();
            int depth = getDepth(node) - rootDepth;
            assert(depth >= 0);
            if (depth > maxDepth)
                maxDepth = depth;
            if (numberChildren > maxChildren)
                maxChildren = numberChildren;
            if (numberChildren == 0)
                ++numberTerminal;
            else
                averageChildrenInner += numberChildren;
            averageDepth += depth;
            averageChildren += numberChildren;
            node = nextNode(node, rootDepth);
        }
        int numberInner = numberNodes - numberTerminal;
        averageDepth /= numberNodes;
        averageChildren /= numberNodes;
        averageChildrenInner /= Math.max(numberInner, 1);
        NumberFormat format = StringUtils.getNumberFormat(3);
        StringBuffer buffer = new StringBuffer();
        appendInfo(buffer, "Nodes", numberNodes);
        appendInfo(buffer, "Terminal", numberTerminal);
        appendInfo(buffer, "Inner", numberInner);
        appendInfo(buffer, "AvgDepth", format.format(averageDepth));
        appendInfo(buffer, "MaxDepth", maxDepth);
        appendInfo(buffer, "AvgChildren", format.format(averageChildren));
        appendInfo(buffer, "AvgChildrenInner",
                   format.format(averageChildrenInner));
        appendInfo(buffer, "MaxChildren", maxChildren);
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
                                   ArrayList points)
    {
        appendInfoLabel(buffer, label);
        for (int i = 0; i < points.size(); ++i)
        {
            if (i % 10 == 9 && i < points.size() - 1)
            {
                buffer.append('\n');
                appendInfoLabel(buffer, "");
            }
            buffer.append((GoPoint)points.get(i));
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

    private static void appendInfoComment(StringBuffer buffer, Node node)
    {
        String comment = node.getComment();
        if (comment == null)
            return;
        boolean trimmed = false;
        int pos = comment.indexOf("\n");
        if (pos >= 0)
        {
            comment = comment.substring(0, pos);
            trimmed = true;
        }
        final int maxCharDisplayed = 30;
        if (comment.length() > maxCharDisplayed)
        {
            comment = comment.substring(0, maxCharDisplayed);
            trimmed = true;
        }
        if (trimmed)
            comment = comment + "...";
        appendInfo(buffer, "Comment", comment);
    }

    private static void appendInfoLabel(StringBuffer buffer, String label)
    {
        buffer.append(label);
        int numberEmpty = Math.max(0, 20 - label.length());
        for (int i = 0; i < numberEmpty; ++i)
            buffer.append(' ');
        buffer.append(' ');
    }

    /** Make constructor unavailable; class is for namespace only. */
    private NodeUtils()
    {
    }
}

//----------------------------------------------------------------------------
