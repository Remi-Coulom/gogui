//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

class ExtraInfo
{
    public SetupInfo m_setupInfo;

    public TimeInfo m_timeInfo;

    public TreeMap m_sgfProperties;

    public Map m_marked;

    public Map m_label;
}

//----------------------------------------------------------------------------

class SetupInfo
{
    public GoColor m_player = GoColor.EMPTY;

    public ArrayList m_black = new ArrayList();

    public ArrayList m_white = new ArrayList();

    public ArrayList m_empty = new ArrayList();
}

//----------------------------------------------------------------------------

class TimeInfo
{
    public int m_movesLeftBlack = -1;

    public int m_movesLeftWhite = -1;

    public double m_timeLeftBlack = Double.NaN;

    public double m_timeLeftWhite = Double.NaN;

}

//----------------------------------------------------------------------------

/** Node in a GameTree.
    The memory requirement is optimized for nodes containing only a move and
    comment property (e.g. for GNU Go's large SGF traces).
    The optimization also expects that most nodes have only one child.
*/
public final class Node
{
    /** Construct empty node. */
    public Node()
    {
    }

    /** Construct node containing a move.
        @param move The move to store in this node.
    */
    public Node(Move move)
    {
        m_move = move;
    }

    /** Append a node as a child to this node.
        @param node The node to append.
    */
    public void append(Node node)
    {
        assert(node.m_father == null);
        if (m_children == null)
        {
            m_children = node;
        }
        else
        {
            if (m_children instanceof Node)
            {
                ArrayList list = new ArrayList(2);
                list.add(m_children);
                list.add(node);
                m_children = list;
            }
            else
                ((ArrayList)m_children).add(node);
        }
        node.m_father = this;
    }

    /** Add a black setup stone.
        @param point The location of the setup stone.
    */
    public void addBlack(GoPoint point)
    {
        assert(point != null);
        createSetupInfo().m_black.add(point);
    }

    /** Add an empty setup point.
        @param point The location that should be set to empty.
    */
    public void addEmpty(GoPoint point)
    {
        assert(point != null);
        createSetupInfo().m_empty.add(point);
    }

    /** Add a markup.
        @param point The location that should be marked.
        @param type The type of the markup from Node.MARK_TYPES.
    */
    public void addMarked(GoPoint point, MarkType type)
    {
        assert(point != null);
        Map marked = createMarked();
        ArrayList pointList = (ArrayList)marked.get(type);
        if (pointList == null)
        {
            pointList = new ArrayList(1);
            pointList.add(point);
            marked.put(type, pointList);
        }
        else if (! pointList.contains(point))
            pointList.add(point);
    }

    /** Add other unspecified SGF property.
        Do not add SGF properties that can be set with other member functions.
        This is for preserving unknown SGF properties that are not used
        by this program.
    */
    public void addSgfProperty(String label, String value)
    {
        createSgfProperties().put(label, value);
    }

    /** Add a white setup stone.
        @param point The location of the setup stone.
    */
    public void addWhite(GoPoint point)
    {
        assert(point != null);
        createSetupInfo().m_white.add(point);
    }

    /** Get black setup stone.
        @param i The index of the setup stone
        in [0...getNumberAddBlack() - 1]
    */
    public GoPoint getAddBlack(int i)
    {
        return (GoPoint)m_extraInfo.m_setupInfo.m_black.get(i);
    }

    /** Get empty setup point.
        @param i The index of the setup point
        in [0...getNumberAddEmpty() - 1]
    */
    public GoPoint getAddEmpty(int i)
    {
        return (GoPoint)m_extraInfo.m_setupInfo.m_empty.get(i);
    }

    /** Get white setup stone.
        @param i The index of the setup stone
        in [0...getNumberAddWhite() - 1]
    */
    public GoPoint getAddWhite(int i)
    {
        return (GoPoint)m_extraInfo.m_setupInfo.m_white.get(i);
    }

    /** Child of main variation or null if no child.
        @return Node with index 0 or null, if no children.
    */
    public Node getChild()
    {
        if (getNumberChildren() == 0)
            return null;
        return getChild(0);
    }

    /** Get child node.
        @param i Index of the child
        in [0...getNumberChildren() - 1]
    */
    public Node getChild(int i)
    {
        if (getNumberChildren() == 1)
            return (Node)m_children;
        return (Node)((ArrayList)m_children).get(i);
    }

    /** Get index of child node.
        @param child The child.
        @return Index of child or -1, if node is not a child of this node.
    */  
    public int getChildIndex(Node child)
    {
        for (int i = 0; i < getNumberChildren(); ++i)
            if (getChild(i) == child)
                return i;
        return -1;
    }

    /** Get comment.
        @return Comment stored in this node or null, if node contains no
        comment.
    */
    public String getComment()
    {
        if (m_comment == null)
            return null;
        try
        {
            return new String(m_comment, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return new String(m_comment);
        }
    }

    /** Get father node.
        @return Father node of this node or null, if no father.
    */
    public Node getFather()
    {
        return m_father;
    }

    /** Get label for a location on the board.
        @param point The location.
        @return Label at location or null, if no label.
    */
    public String getLabel(GoPoint point)
    {
        Map map = getLabels();
        if (map == null || ! map.containsKey(point))
            return null;
        return (String)map.get(point);
    }

    /** Get all labels on the board.
        @return Map containing (Point,String) pairs.
    */
    public Map getLabels()
    {
        if (m_extraInfo == null)
            return null;
        return m_extraInfo.m_label;
    }

    /** Get all markups of a type.
        @param type Markup type from Node.MARK_TYPES.
        @return Map containing (Point,String) pairs.
    */
    public ArrayList getMarked(MarkType type)
    {
        if (m_extraInfo == null || m_extraInfo.m_marked == null)
            return null;
        return (ArrayList)m_extraInfo.m_marked.get(type);
    }

    /** Get move contained in this node.
        @return Move or null, if no move.
    */
    public Move getMove()
    {
        return m_move;
    }

    /** Moves left in byoyomi for color.
        -1 if not in byyomi or unknown.
    */
    public int getMovesLeft(GoColor color)
    {
        if (! hasTimeInfo())
            return -1;
        if (color == GoColor.BLACK)
            return m_extraInfo.m_timeInfo.m_movesLeftBlack;
        assert(color == GoColor.WHITE);
        return m_extraInfo.m_timeInfo.m_movesLeftWhite;
    }

    /** Moves left in byoyomi for white.
        -1 if not in byyomi or unknown.
    */
    public int getMovesLeftWhite()
    {
        if (! hasTimeInfo())
            return -1;
        return m_extraInfo.m_timeInfo.m_movesLeftWhite;
    }

    /** Get number of black setup stones.
        @return Number of setup stones.
    */
    public int getNumberAddBlack()
    {
        if (! hasSetupInfo())
            return 0;
        return m_extraInfo.m_setupInfo.m_black.size();
    }

    /** Get number of empty setup points.
        @return Number of setup points.
    */
    public int getNumberAddEmpty()
    {
        if (! hasSetupInfo())
            return 0;
        return m_extraInfo.m_setupInfo.m_empty.size();
    }

    /** Get number of white setup stones.
        @return Number of setup stones.
    */
    public int getNumberAddWhite()
    {
        if (! hasSetupInfo())
            return 0;
        return m_extraInfo.m_setupInfo.m_white.size();
    }

    /** Get number of children.
        @return Number of children.
    */
    public int getNumberChildren()
    {
        if (m_children == null)
            return 0;
        if (m_children instanceof Node)
            return 1;
        return ((ArrayList)m_children).size();
    }

    /** Color to play if explicitely set.
        @see #getToMove for getting the color to play.
        @return Color to play or GoColor.EMPTY if color is not explicitely
        set.
    */
    public GoColor getPlayer()
    {
        if (! hasSetupInfo())
            return GoColor.EMPTY;
        return m_extraInfo.m_setupInfo.m_player;
    }

    /** Get other unspecified SGF properties.
        @see #addSgfProperty
    */
    public Map getSgfProperties()
    {
        if (! hasSgfProperties())
            return null;
        return m_extraInfo.m_sgfProperties;
    }

    /** Time left for color after move was made.
        Returns Double.NaN if unknown.
    */
    public double getTimeLeft(GoColor color)
    {
        if (! hasTimeInfo())
            return Double.NaN;
        if (color == GoColor.BLACK)
            return m_extraInfo.m_timeInfo.m_timeLeftBlack;
        assert(color == GoColor.WHITE);
        return m_extraInfo.m_timeInfo.m_timeLeftWhite;
    }

    /** Get color to move.
        Determining the color to move takes into consideration an explicitely
        set player color and moves contained in this node.
        If nothing is known about the color to move, it returns GoColor.EMPTY.
    */
    public GoColor getToMove()
    {
        GoColor player = getPlayer();
        if (player != GoColor.EMPTY)
            return player;
        if (m_move != null)
            return m_move.getColor().otherColor();
        return GoColor.EMPTY;
    }

    /** Check if node has setup or delete stones.
        @return true, if node has setup or delete stones.
    */
    public boolean hasSetup()
    {
        return getNumberAddBlack() > 0 || getNumberAddWhite() > 0
            || getNumberAddEmpty() > 0;
    }

    /** Check if node is child of this node.
        @param node The node to check.
        @return true, if node is child node.
    */
    public boolean isChildOf(Node node)
    {
        return (node.getChildIndex(this) != -1);
    }

    /** Make child the first child of this node.
        @param child One of the child nodes of this node.
    */
    public void makeMainVariation(Node child)
    {
        assert(child.isChildOf(this));
        if (getNumberChildren() <= 1)
            return;
        ArrayList list = (ArrayList)m_children;
        list.remove(child);
        list.add(0, child);
    }

    /** Remove child of this node.
        @param child Child to remove.
    */
    public void removeChild(Node child)
    {
        assert(child.isChildOf(this));
        int numberChildren = getNumberChildren();
        if (numberChildren == 1)
            m_children = null;
        else if (numberChildren >= 2)
        {
            ArrayList list = (ArrayList)m_children;
            list.remove(child);
            if (numberChildren == 2)
                m_children = list.get(0);
        }
        else
            assert(false);
        child.m_father = null;
    }

    /** Remove markup.
        @param point Location of the markup.
        @param type Type of the markup from Node.MARK_TYPES.
    */
    public void removeMarked(GoPoint point, MarkType type)
    {
        assert(point != null);
        Map marked = createMarked();
        ArrayList pointList = (ArrayList)marked.get(type);
        if (pointList != null)
            pointList.remove(point);
    }

    /** Remove all children but the first. */
    public void removeVariations()
    {
        if (getNumberChildren() <= 1)
            return;
        Node child = getChild(0);
        m_children = child;
    }

    /** Store comment in this node.
        @param comment The comment or null to delete comment.
    */
    public void setComment(String comment)
    {
        if (comment == null)
        {
            m_comment = null;
            return;
        }
        try
        {
            m_comment = comment.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            m_comment = comment.getBytes();
        }
    }

    /** Set father of this node.
        Usually you don't need this function and it may become deprecated
        in the future. Use Node.append() to construct trees.
        @param father The new father.
    */
    public void setFather(Node father)
    {
        m_father = father;
    }

    /** Add label at a location on the board.
        Whitespaces will be trimmed.
        @param point The location.
        @param label The text of the label; empty string or null to delete
        the label.
    */
    public void setLabel(GoPoint point, String label)
    {
        assert(point != null);
        Map tree = createLabel();
        tree.remove(point);
        if (label == null)
            return;
        label = label.trim();
        if (label.equals(""))
            return;
        tree.put(point, label);
    }

    /** Set move stored in this node.
        @param move The move or null, if no move.
    */
    public void setMove(Move move)
    {
        m_move = move;
    }

    /** Set byoyomi moves left for black.
        @param moves Number of moves left.
    */
    public void setMovesLeftBlack(int moves)
    {
        createTimeInfo().m_movesLeftBlack = moves;
    }

    /** Set byoyomi moves left for white.
        @param moves Number of moves left.
    */
    public void setMovesLeftWhite(int moves)
    {
        createTimeInfo().m_movesLeftWhite = moves;
    }

    /** Set byoyomi time left for black.
        @param timeLeft Time left in seconds.
    */
    public void setTimeLeftBlack(double timeLeft)
    {
        createTimeInfo().m_timeLeftBlack = timeLeft;
    }

    /** Set byoyomi time left for white.
        @param timeLeft Time left in seconds.
    */
    public void setTimeLeftWhite(double timeLeft)
    {
        createTimeInfo().m_timeLeftWhite = timeLeft;
    }

    /** Explicitely set color to play.
        @param color Color to play.
    */
    public void setPlayer(GoColor color)
    {
        assert(color == GoColor.BLACK || color == GoColor.WHITE);
        createSetupInfo().m_player = color;
    }

    public Node variationAfter(Node child)
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

    public Node variationBefore(Node child)
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

    /** Comment stored as bytes.
        Store comments in UTF-8, because that saves up to a factor of 2 in
        size compared to a string.
    */
    private byte[] m_comment;

    private ExtraInfo m_extraInfo;

    private Move m_move;

    private Node m_father;

    /** Node if one child only, ArrayList otherwise. */
    private Object m_children;

    private void createExtraInfo()
    {
        if (m_extraInfo == null)
            m_extraInfo = new ExtraInfo();
    }

    private Map createLabel()
    {
        createExtraInfo();
        if (m_extraInfo.m_label == null)
            m_extraInfo.m_label = new TreeMap();
        return m_extraInfo.m_label;
    }

    private Map createMarked()
    {
        createExtraInfo();
        if (m_extraInfo.m_marked == null)
            m_extraInfo.m_marked = new TreeMap();
        return m_extraInfo.m_marked;
    }

    private SetupInfo createSetupInfo()
    {
        createExtraInfo();
        if (m_extraInfo.m_setupInfo == null)
            m_extraInfo.m_setupInfo = new SetupInfo();
        return m_extraInfo.m_setupInfo;
    }

    private Map createSgfProperties()
    {
        createExtraInfo();
        if (m_extraInfo.m_sgfProperties == null)
            m_extraInfo.m_sgfProperties = new TreeMap();
        return m_extraInfo.m_sgfProperties;
    }

    private TimeInfo createTimeInfo()
    {
        createExtraInfo();
        if (m_extraInfo.m_timeInfo == null)
            m_extraInfo.m_timeInfo = new TimeInfo();
        return m_extraInfo.m_timeInfo;
    }

    private boolean hasSetupInfo()
    {
        return (m_extraInfo != null && m_extraInfo.m_setupInfo != null);
    }

    private boolean hasSgfProperties()
    {
        return (m_extraInfo != null && m_extraInfo.m_sgfProperties != null);
    }

    private boolean hasTimeInfo()
    {
        return (m_extraInfo != null && m_extraInfo.m_timeInfo != null);
    }
}

//----------------------------------------------------------------------------
