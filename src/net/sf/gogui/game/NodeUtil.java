// NodeUtil.java

package net.sf.gogui.game;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Random;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.StringUtil;

/** Utility functions operating on a tree of nodes. */
public final class NodeUtil
{
    /** Get node reached by going a number of nodes backward.
        @param node The start node.
        @param n The number of moves to go backward.
        @return The node reached by going n moves backward or the root node
        of the tree, if there are less than n nodes in the sequence before
        the start node. */
    public static ConstNode backward(ConstNode node, int n)
    {
        assert n >= 0;
        for (int i = 0; i < n && node.getFatherConst() != null; ++i)
            node = node.getFatherConst();
        return node;
    }

    /** Remove SGF properties for information already contained in game info.
        This can be used for example, if a SGF reader cannot handle an unknown
        format of the TM property, puts it into the SGF properties of the node
        to preserve the information for future saving, but later a
        well-defined time settings was set in the game information of the node.
        Handles the following properties: TM (because FF3 allowed an arbitrary
        text in TM), OT (FF4 allows arbitrary text)
        @return The cleaned properties, empty properties, if
        node.getSgfProperties() was null */
    public static SgfProperties cleanSgfProps(ConstNode node)
    {
        SgfProperties props = new SgfProperties(node.getSgfPropertiesConst());
        ConstGameInfo info = node.getGameInfoConst();
        if (info != null)
        {
            if (info.getTimeSettings() != null)
            {
                props.remove("OT");
                props.remove("TM");
                props.remove("OM"); // FF3
                props.remove("OP"); // FF3
            }
        }
        return props;
    }

    /** Check if the comment of the current node contains a pattern.
        @param node The node to check.
        @param pattern The pattern.
        @return <tt>true</tt>, if the current node has a comment and a match
        for the pattern is found in the comment. */
    public static boolean commentContains(ConstNode node, Pattern pattern)
    {
        String comment = node.getComment();
        return (comment != null && pattern.matcher(comment).find());
    }

    /** Find first node with a certain move number in main variation
        of a given node.
        @param node The given node.
        @param moveNumber The move number of the wanted node.
        @return The node with the given move number or <code>null</code> if
        no such node exists. */
    public static ConstNode findByMoveNumber(ConstNode node, int moveNumber)
    {
        int maxMoveNumber = getMoveNumber(node) + getMovesLeft(node);
        if (moveNumber < 0 || moveNumber >  maxMoveNumber)
            return null;
        if (moveNumber <= getMoveNumber(node))
        {
            while (node.getFatherConst() != null
                   && (getMoveNumber(node) > moveNumber
                       || node.getMove() == null))
                node = node.getFatherConst();
        }
        else
        {
            while (node.getChildConst() != null
                   && getMoveNumber(node) < moveNumber)
                node = node.getChildConst();
        }
        return node;
    }

    /** Get first node of a given variation.
        Searches the node that can be reached from the root node by taking the
        children defined by the integers in the variation string for nodes
        with more than one child.
        @param root The root node of the tree.
        @param variation The variation string (e.g. "1.1.3.1.5").
        @return The first node of the given variation, or the root node,
        if the variation string is empty, or <code>null</code>, if the
        variation string is invalid or does not specify a node in the
        given tree. */
    public static ConstNode findByVariation(ConstNode root, String variation)
    {
        if (variation.trim().equals(""))
            return root;
        String[] tokens = StringUtil.split(variation, '.');
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
        ConstNode node = root;
        for (int i = 0; i < n.length; ++i)
        {
            while (node.getNumberChildren() <= 1)
            {
                node = node.getChildConst();
                if (node == null)
                    return null;
            }
            if (n[i] >= node.getNumberChildren())
                return null;
            node = node.getChildConst(n[i]);
        }
        return node;
    }

    /** Find next node with a comment containing a pattern in the iteration
        through complete tree.
        @param node The current node in the iteration.
        @param pattern The pattern.
        @return The next node in the iteration through the complete tree
        after the current node that contains a match of the pattern. */
    public static ConstNode findInComments(ConstNode node, Pattern pattern)
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

    /** Find next node with a comment in the iteration through complete tree.
        @param node The current node in the iteration.
        @return The next node in the iteration through the complete tree
        after the current node that has a comment. */
    public static ConstNode findNextComment(ConstNode node)
    {
        node = nextNode(node);
        while (node != null)
        {
            if (node.hasComment())
                return node;
            node = nextNode(node);
        }
        return null;
    }

    /** Get node reached by going a number of nodes forward.
        @param node The start node.
        @param n The number of moves to go forward.
        @return The node reached by going n moves forward following the main
        variaton (always the first child) or the last node in this variation,
        if it has less than n nodes. */
    public static ConstNode forward(ConstNode node, int n)
    {
        assert n >= 0;
        for (int i = 0; i < n; ++i)
        {
            ConstNode child = node.getChildConst();
            if (child == null)
                break;
            node = child;
        }
        return node;
    }

    /** Find the last node that was still in the main variation. */
    public static ConstNode getBackToMainVariation(ConstNode node)
    {
        if (isInMainVariation(node))
            return node;
        while (! isInMainVariation(node))
            node = node.getFatherConst();
        return node.getChildConst(0);
    }

    /** Get all children moves.
        @return Point list containing the move points, not including passes
        and independent of color. */
    public static PointList getChildrenMoves(ConstNode node)
    {
        PointList moves = new PointList();
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
            Move childMove = node.getChildConst(i).getMove();
            if (childMove != null && childMove.getPoint() != null)
                moves.add(childMove.getPoint());
        }
        return moves;
    }

    /** Get child node containing a certain move.
        @return null if no such child exists. */
    public static ConstNode getChildWithMove(ConstNode node, Move move)
    {
        for (int i = 0; i < node.getNumberChildren(); ++i)
        {
            ConstNode child = node.getChildConst(i);
            Move childMove = child.getMove();
            if (childMove != null && childMove.equals(move))
                return child;
        }
        return null;
    }

    /** Get comment, but no more than a maximum number of characters.
        @return Start of comment, with ellipses appended if trunceted;
        null, if node has no comment. */
    public static String getCommentStart(ConstNode node,
                                         boolean firstLineOnly,
                                         int maxChar)
    {
        String comment = node.getComment();
        if (comment == null)
            return null;
        boolean trimmed = false;
        if (firstLineOnly)
        {
            int pos = comment.indexOf("\n");
            if (pos >= 0)
            {
                comment = comment.substring(0, pos);
                trimmed = true;
            }
        }
        if (comment.length() > maxChar)
        {
            comment = comment.substring(0, maxChar);
            trimmed = true;
        }
        if (trimmed)
        {
            if (maxChar > 30)
            {
                // Try to find a cut-off at a space
                int pos = comment.lastIndexOf(' ');
                if (pos > 20)
                {
                    if (comment.charAt(pos) == '.')
                        --pos;
                    comment = comment.substring(0, pos);
                }
            }
            comment = comment + "...";
        }
        return comment;
    }

    /** Get depth of a node.
        @param node The node.
        @return The number of nodes in the sequence from the root node
        to the given node, excluding the given node (which means that the
        root node has depth 0). */
    public static int getDepth(ConstNode node)
    {
        int depth = 0;
        while (node.getFatherConst() != null)
        {
            node = node.getFatherConst();
            ++depth;
        }
        return depth;
    }

    /** Get last node in main variation. */
    public static ConstNode getLast(ConstNode node)
    {
        while (node.hasChildren())
            node = node.getChildConst();
        return node;
    }

    /** Get the move number of a node.
        @param node The node.
        @return The total number of moves in the sequence of nodes from
        the root node to the given node, including the given node. */
    public static int getMoveNumber(ConstNode node)
    {
        int moveNumber = 0;
        while (node != null)
        {
            if (node.getMove() != null)
                ++moveNumber;
            node = node.getFatherConst();
        }
        return moveNumber;
    }

    /** Moves left in main variation. */
    public static int getMovesLeft(ConstNode node)
    {
        int movesLeft = 0;
        node = node.getChildConst();
        while (node != null)
        {
            if (node.getMove() != null)
                ++movesLeft;
            node = node.getChildConst();
        }
        return movesLeft;
    }

    /** Return next variation of this node. */
    public static ConstNode getNextVariation(ConstNode node)
    {
        ConstNode father = node.getFatherConst();
        if (father == null)
            return null;
        return father.variationAfter(node);
    }

    /** Return next variation before this node. */
    public static ConstNode getNextEarlierVariation(ConstNode node)
    {
        ConstNode child = node;
        node = node.getFatherConst();
        while (node != null && node.variationAfter(child) == null)
        {
            child = node;
            node = node.getFatherConst();
        }
        if (node == null)
            return null;
        return node.variationAfter(child);
    }

    /** Nodes left in main variation. */
    public static int getNodesLeft(ConstNode node)
    {
        int nodesLeft = 0;
        while (node != null)
        {
            ++nodesLeft;
            node = node.getChildConst();
        }
        return nodesLeft;
    }

    /** Get nodes in path from a given node to the root node.
        @param node The node
        @param result The resulting path. Passed as an argument to allow
        reusing an array list. It will be cleared before it is used. */
    public static void getPathToRoot(ConstNode node,
                                     ArrayList<ConstNode> result)
    {
        result.clear();
        while (node != null)
        {
            result.add(node);
            node = node.getFatherConst();
        }
    }

    /** Return previous variation of this node. */
    public static ConstNode getPreviousVariation(ConstNode node)
    {
        ConstNode father = node.getFatherConst();
        if (father == null)
            return null;
        return father.variationBefore(node);
    }

    /** Return previous variation before this node. */
    public static ConstNode getPreviousEarlierVariation(ConstNode node)
    {
        ConstNode child = node;
        node = node.getFatherConst();
        while (node != null && node.variationBefore(child) == null)
        {
            child = node;
            node = node.getFatherConst();
        }
        if (node == null)
            return null;
        node = node.variationBefore(child);
        if (node == null)
            return null;
        while (hasSubtree(node))
            node = node.getChildConst(node.getNumberChildren() - 1);
        return node;
    }

    /** Get the root node.
        @param node The node.
        @return The root node of the tree that the given node belongs to. */
    public static ConstNode getRoot(ConstNode node)
    {
        while (node.getFatherConst() != null)
            node = node.getFatherConst();
        return node;
    }

    /** Get a text representation of the variation to a certain node.
        The string contains the number of the child for each node with more
        than one child in the path from the root node to this node.
        The childs are counted starting with 1 and the numbers are separated
        by colons. */
    public static String getVariationString(ConstNode node)
    {
        ArrayList<String> list = new ArrayList<String>();
        while (node != null)
        {
            ConstNode father = node.getFatherConst();
            if (father != null && father.getNumberChildren() > 1)
            {
                int index = father.getChildIndex(node) + 1;
                list.add(0, Integer.toString(index));
            }
            node = father;
        }
        StringBuilder result = new StringBuilder(list.size() * 3);
        for (int i = 0; i < list.size(); ++i)
        {
            result.append(list.get(i));
            if (i < list.size() - 1)
                result.append('.');
        }
        return result.toString();
    }

    /** Check if a node contains a move and has sibling nodes containing other
        moves. */
    public static boolean hasSiblingMoves(ConstNode node)
    {
        ConstNode father = node.getFatherConst();
        if (father == null)
            return false;
        Move move = node.getMove();
        if (move == null)
            return false;
        for (int i = 0; i < father.getNumberChildren(); ++i)
        {
            Move childMove = father.getChildConst(i).getMove();
            if (childMove != null && childMove != move)
                return true;
        }
        return false;
    }

    /** Subtree of node contains at least one node with 2 or more children. */
    public static boolean hasSubtree(ConstNode node)
    {
        while (node != null && node.getNumberChildren() < 2)
            node = node.getChildConst();
        return (node != null);
    }

    /** Check if game is in cleanup stage.
        Cleanup stage is after two consecutive pass moves have been played. */
    public static boolean isInCleanup(ConstNode node)
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
            node = node.getFatherConst();
        }
        return false;
    }

    /** Check if a node is in the main variation of the tree.
        @param node The node.
        @return <tt>true</tt>, if the given node is in the main variation,
        which is the sequence of nodes starting from the root of the tree
        and always following the first child. */
    public static boolean isInMainVariation(ConstNode node)
    {
        while (node.getFatherConst() != null)
        {
            if (node.getFatherConst().getChildConst(0) != node)
                return false;
            node = node.getFatherConst();
        }
        return true;
    }

    /** Check if node is root node and has no children.
        @param node The node to check.
        @return <tt>true</tt>, if the node has no father node and no
        children. */
    public static boolean isRootWithoutChildren(ConstNode node)
    {
        return (! node.hasFather() && ! node.hasChildren());
    }

    /** Check that the time left for a color at a node is known.
        Returns true, if the last node (including the given one) containing a
        move of the given color also contains information about the time left
        after the move for the color. If a previous node with a game info
        containing time settings exists and no move of the given color was
        played since then, the function also returns true. */
    public static boolean isTimeLeftKnown(ConstNode node, GoColor color)
    {
        while (node != null)
        {
            Move move = node.getMove();
            if (move != null && move.getColor() == color)
                return ! Double.isNaN(node.getTimeLeft(color));
            ConstGameInfo info = node.getGameInfoConst();
            if (info != null && info.getTimeSettings() != null)
                return true;
            node = node.getFatherConst();
        }
        return false;
    }

    /** Make the variation of the current node to be the main variation
        of the tree.
        Changes the children order of all nodes in the sequence from the root
        node to the current node (exclusive the current node), such that all
        nodes in the sequence (inclusive the current node) are the first
        child of their parents.
        @param node The current node. */
    public static void makeMainVariation(Node node)
    {
        while (node.getFatherConst() != null)
        {
            node.getFather().makeFirstChild(node);
            node = node.getFather();
        }
    }

    /** Create a game tree with the current board position as setup stones. */
    public static GameTree makeTreeFromPosition(ConstGameInfo info,
                                                ConstBoard board)
    {
        if (info == null)
            info = new GameInfo();
        GameTree tree = new GameTree(board.getSize(), info.getKomi(), null,
                                     info.get(StringInfo.RULES),
                                     info.getTimeSettings());
        Node root = tree.getRoot();
        for (GoPoint p : board)
        {
            GoColor c = board.getColor(p);
            if (c.isBlackWhite())
                root.addStone(c, p);
        }
        root.setPlayer(board.getToMove());
        return tree;
    }

    /** Get next node for iteration through complete tree. */
    public static ConstNode nextNode(ConstNode node)
    {
        ConstNode child = node.getChildConst();
        if (child != null)
            return child;
        return getNextEarlierVariation(node);
    }

    /** Get next node for iteration through subtree. */
    public static ConstNode nextNode(ConstNode node, int depth)
    {
        node = nextNode(node);
        if (node == null || NodeUtil.getDepth(node) <= depth)
            return null;
        return node;
    }

    /** Return a string containing information about a node.
        The string contains a listing of the data stored in the node
        (like moves or setup stones) and properties of the node in the
        tree (like depth or variation). */
    public static String nodeInfo(ConstNode node)
    {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append("NodeProperties:\n");
        appendInfo(buffer, "Depth", getDepth(node));
        appendInfo(buffer, "Children", node.getNumberChildren());
        if (node.getMove() != null)
        {
            appendInfo(buffer, "Move", node.getMove().toString());
            appendInfo(buffer, "MoveNumber", getMoveNumber(node));
        }
        appendInfo(buffer, "Variation", getVariationString(node));
        ConstPointList black = node.getSetup(BLACK);
        if (black.size() > 0)
            appendInfo(buffer, "AddBlack", black);
        ConstPointList white = node.getSetup(WHITE);
        if (white.size() > 0)
            appendInfo(buffer, "AddWhite", white);
        ConstPointList empty = node.getSetup(EMPTY);
        if (empty.size() > 0)
            appendInfo(buffer, "AddEmpty", empty);
        if (node.getPlayer() != null)
            appendInfo(buffer, "Player", node.getPlayer().toString());
        if (! Double.isNaN(node.getTimeLeft(BLACK)))
            appendInfo(buffer, "TimeLeftBlack", node.getTimeLeft(BLACK));
        if (node.getMovesLeft(BLACK) >= 0)
            appendInfo(buffer, "MovesLeftBlack", node.getMovesLeft(BLACK));
        if (! Double.isNaN(node.getTimeLeft(WHITE)))
            appendInfo(buffer, "TimeLeftWhite", node.getTimeLeft(WHITE));
        if (node.getMovesLeft(WHITE) >= 0)
            appendInfo(buffer, "MovesLeftWhite", node.getMovesLeft(WHITE));
        appendInfoComment(buffer, node);
        for (MarkType type : EnumSet.allOf(MarkType.class))
        {
            ConstPointList marked = node.getMarkedConst(type);
            if (marked != null && marked.size() > 0)
                appendInfo(buffer, "Marked " + type, marked);
        }
        Map<GoPoint,String> labels = node.getLabelsUnmodifiable();
        if (labels != null && ! labels.isEmpty())
        {
            StringBuilder labelsBuffer = new StringBuilder();
            boolean isFirst = true;
            for (Map.Entry<GoPoint,String> entry : labels.entrySet())
            {
                if (! isFirst)
                    labelsBuffer.append(' ');
                isFirst = false;
                GoPoint point = entry.getKey();
                String value = entry.getValue();
                labelsBuffer.append(point);
                labelsBuffer.append(':');
                labelsBuffer.append(value);
            }
            appendInfo(buffer, "Labels", labelsBuffer.toString());
        }
        if (! Float.isNaN(node.getValue()))
            appendInfo(buffer, "Value", Float.toString(node.getValue()));
        ConstGameInfo info = node.getGameInfoConst();
        if (info != null)
        {
            buffer.append("GameInfo:\n");
            for (StringInfo type : EnumSet.allOf(StringInfo.class))
            {
                String s = info.get(type);
                if (s != null)
                    appendInfo(buffer, type.toString(), s);
            }
            for (StringInfoColor type : EnumSet.allOf(StringInfoColor.class))
            {
                String s = info.get(type, BLACK);
                if (s != null)
                    appendInfo(buffer, type.toString() + "[B]", s);
                s = info.get(type, WHITE);
                if (s != null)
                    appendInfo(buffer, type.toString() + "[W]", s);
            }
            if (info.getHandicap() != 0)
                appendInfo(buffer, "HANDICAP", info.getHandicap());
            if (info.getKomi() != null)
                appendInfo(buffer, "KOMI", info.getKomi().toString());
            if (info.getTimeSettings() != null)
                appendInfo(buffer, "TIMESETTINGS",
                           info.getTimeSettings().toString());
        }
        ConstSgfProperties sgfProperties = node.getSgfPropertiesConst();
        if (sgfProperties != null)
        {
            buffer.append("SgfProperties:\n");
            for (String key : sgfProperties.getKeys())
            {
                StringBuilder values = new StringBuilder();
                for (int i = 0; i < sgfProperties.getNumberValues(key); ++i)
                {
                    values.append('[');
                    values.append(sgfProperties.getValue(key, i));
                    values.append(']');
                }
                appendInfo(buffer, key, values.toString());
            }
        }
        return buffer.toString();
    }

    /** Restore the clock to the state corresponding to a node.
        Updates the clock from the time left information stored in the nodes
        of the sequence from the root node to the current node.
        @param node The current node.
        @param clock The clock to update. */
    public static void restoreClock(ConstNode node, Clock clock)
    {
        clock.reset();
        ArrayList<ConstNode> path = new ArrayList<ConstNode>();
        getPathToRoot(node, path);
        for (int i = path.size() - 1; i >= 0; --i)
        {
            ConstNode pathNode = path.get(i);
            restoreTimeLeft(pathNode, clock, BLACK);
            restoreTimeLeft(pathNode, clock, WHITE);
        }
    }

    /** Select a random node in the main variation within a certain depth
        interval.
        @param root The root node
        @param minDepth The minimum depth of the interval (inclusive)
        @param maxDepth The maximum depth of the interval (inclusive)
        @return A random node in the given depth interval, null, if there is
        none. */
    public static ConstNode selectRandom(ConstNode root, int minDepth,
                                         int maxDepth)
    {
        ConstNode last = getLast(root);
        int depth = getDepth(last);
        if (depth < minDepth)
            return null;
        if (depth < maxDepth)
            maxDepth = depth;
        int idx = minDepth + s_random.nextInt(maxDepth - minDepth + 1);
        ConstNode node = last;
        for (int i = depth; i != idx; --i)
            node = node.getFatherConst();
        return node;
    }

    /** Check if the number of nodes in the subtree of a node is greater
        than a given limit. */
    public static boolean subtreeGreaterThan(ConstNode node, int size)
    {
        int n = 0;
        int depth = NodeUtil.getDepth(node);
        while (node != null)
        {
            ++n;
            if (n > size)
                return true;
            node = nextNode(node, depth);
        }
        return false;
    }

    /** Count number of nodes in subtree.
        @param node The root node of the subtree.
        @return The number of nodes in the subtree (including the root
        node). */
    public static int subtreeSize(ConstNode node)
    {
        int n = 0;
        int depth = NodeUtil.getDepth(node);
        while (node != null)
        {
            ++n;
            node = nextNode(node, depth);
        }
        return n;
    }

    /** Return a string containing information and statistics of the subtree
        of a node. */
    public static String treeInfo(ConstNode node)
    {
        int numberNodes = 0;
        int numberTerminal = 0;
        int moreThanOneChild = 0;
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
            assert depth >= 0;
            if (depth > maxDepth)
                maxDepth = depth;
            if (numberChildren > maxChildren)
                maxChildren = numberChildren;
            if (numberChildren == 0)
                ++numberTerminal;
            else
                averageChildrenInner += numberChildren;
            if (numberChildren > 1)
                ++moreThanOneChild;
            averageDepth += depth;
            averageChildren += numberChildren;
            node = nextNode(node, rootDepth);
        }
        int numberInner = numberNodes - numberTerminal;
        averageDepth /= numberNodes;
        averageChildren /= numberNodes;
        averageChildrenInner /= Math.max(numberInner, 1);
        NumberFormat format = StringUtil.getNumberFormat(3);
        format.setMinimumFractionDigits(3);
        StringBuilder buffer = new StringBuilder();
        appendInfo(buffer, "Nodes", numberNodes);
        appendInfo(buffer, "Terminal", numberTerminal);
        appendInfo(buffer, "Inner", numberInner);
        appendInfo(buffer, "AvgDepth", format.format(averageDepth));
        appendInfo(buffer, "MaxDepth", maxDepth);
        appendInfo(buffer, "AvgChildren", format.format(averageChildren));
        appendInfo(buffer, "AvgChildrenInner",
                   format.format(averageChildrenInner));
        appendInfo(buffer, "MaxChildren", maxChildren);
        appendInfo(buffer, "MoreThanOneChild", moreThanOneChild);
        return buffer.toString();
    }

    /** Remove all children. */
    public static void truncateChildren(Node node)
    {
        while (true)
        {
            Node child = node.getChild();
            if (child == null)
                break;
            node.removeChild(child);
        }
    }

    private static Random s_random = new Random();

    /** Make constructor unavailable; class is for namespace only. */
    private NodeUtil()
    {
    }

    private static void appendInfo(StringBuilder buffer, String label,
                                   int value)
    {
        appendInfo(buffer, label, Integer.toString(value));
    }

    private static void appendInfo(StringBuilder buffer, String label,
                                   double value)
    {
        appendInfo(buffer, label, Double.toString(value));
    }

    private static void appendInfo(StringBuilder buffer, String label,
                                   ConstPointList points)
    {
        appendInfoLabel(buffer, label);
        for (int i = 0; i < points.size(); ++i)
        {
            if (i % 10 == 9 && i < points.size() - 1)
            {
                buffer.append('\n');
                appendInfoLabel(buffer, "");
            }
            buffer.append(points.get(i));
            buffer.append(' ');
        }
        buffer.append('\n');
    }

    private static void appendInfo(StringBuilder buffer, String label,
                                   String value)
    {
        appendInfoLabel(buffer, label);
        buffer.append(value);
        buffer.append('\n');
    }

    private static void appendInfoComment(StringBuilder buffer, ConstNode node)
    {
        String comment = getCommentStart(node, true, 30);
        if (comment != null)
            appendInfo(buffer, "Comment", comment);
    }

    private static void appendInfoLabel(StringBuilder buffer, String label)
    {
        buffer.append(label);
        int numberEmpty = Math.max(0, 20 - label.length());
        for (int i = 0; i < numberEmpty; ++i)
            buffer.append(' ');
        buffer.append(' ');
    }

    private static void restoreTimeLeft(ConstNode node, Clock clock,
                                        GoColor color)
    {
        double timeLeft = node.getTimeLeft(color);
        int movesLeft = node.getMovesLeft(color);
        if (! Double.isNaN(timeLeft))
            clock.setTimeLeft(color, (long)(timeLeft * 1000), movesLeft);
    }
}
