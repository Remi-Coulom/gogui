// Node.java

package net.sf.gogui.game;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.sf.gogui.go.BlackWhiteSet;
import net.sf.gogui.go.BlackWhiteEmptySet;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import static net.sf.gogui.go.GoColor.BLACK_WHITE_EMPTY;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.StringUtil;

/** Extended info.
    Contains markups and value, because these are used in the large SGF
    search traces of Explorer. */
final class ExtraInfo
{
    public Map<MarkType,PointList> m_marked;

    /** Node value.
        Float instead of double for space efficiency. */
    public float m_value = Float.NaN;

    public MoreExtraInfo m_moreExtraInfo;

    public boolean isEmpty()
    {
        return ((m_marked == null || m_marked.size() == 0)
                && Float.isNaN(m_value)
                && (m_moreExtraInfo == null || m_moreExtraInfo.isEmpty()));
    }
}

/** More extended info.
    Contains all the information typically not in large SGF traces. */
final class MoreExtraInfo
{
    public SetupInfo m_setupInfo;

    public TimeInfo m_timeInfo;

    public SgfProperties m_sgfProperties;

    public Map<GoPoint,String> m_label;

    public GameInfo m_info;

    public boolean isEmpty()
    {
        return ((m_setupInfo == null || m_setupInfo.isEmpty())
                && (m_timeInfo == null || m_timeInfo.isEmpty())
                && (m_sgfProperties == null || m_sgfProperties.isEmpty())
                && (m_label == null || m_label.size() == 0)
                && (m_info == null || m_info.isEmpty()));
    }
}

final class SetupInfo
{
    public GoColor m_player;

    /** Stones added or removed.
        The array is indexed by Black, White, Empty. */
    public BlackWhiteEmptySet<PointList> m_stones
        = new BlackWhiteEmptySet<PointList>(new PointList(), new PointList(),
                                            new PointList());

    public boolean isEmpty()
    {
        return (m_player == null && m_stones.get(BLACK).size() == 0
                 && m_stones.get(WHITE).size() == 0
                && m_stones.get(EMPTY).size() == 0);
    }
}

final class TimeInfo
{
    public BlackWhiteSet<Integer> m_movesLeft
        = new BlackWhiteSet<Integer>(-1, -1);

    public BlackWhiteSet<Double> m_timeLeft
        = new BlackWhiteSet<Double>(Double.NaN, Double.NaN);

    public boolean isEmpty()
    {
        return (m_movesLeft.get(BLACK) == -1 && m_movesLeft.get(WHITE) == -1
                && Double.isNaN(m_timeLeft.get(BLACK))
                && Double.isNaN(m_timeLeft.get(WHITE)));
    }
}

/** Node in a game tree.
    The memory requirement is optimized for nodes containing only a move and
    comment property (e.g. for GNU Go's large SGF traces).
    The optimization also expects that most nodes have only one child. */
public final class Node
    implements ConstNode
{
    /** Construct empty node. */
    public Node()
    {
    }

    /** Construct node containing a move.
        @param move The move to store in this node. */
    public Node(Move move)
    {
        m_move = move;
    }

    /** Append a node as a child to this node.
        @param node The node to append. */
    @SuppressWarnings("unchecked")
    public void append(Node node)
    {
        assert node.m_father == null;
        if (m_children == null)
        {
            m_children = node;
        }
        else
        {
            if (m_children instanceof Node)
            {
                ArrayList<Node> list = new ArrayList<Node>(2);
                list.add((Node)m_children);
                list.add(node);
                m_children = list;
            }
            else
            {
                ((ArrayList<Node>)m_children).add(node);
            }
        }
        node.m_father = this;
    }

    /** Add a markup.
        @param point The location that should be marked.
        @param type The type of the markup from Node.MARK_TYPES. */
    public void addMarked(GoPoint point, MarkType type)
    {
        assert point != null;
        Map<MarkType,PointList> marked = createMarked();
        PointList pointList = (PointList)marked.get(type);
        if (pointList == null)
        {
            pointList = new PointList(1);
            pointList.add(point);
            marked.put(type, pointList);
        }
        else if (! pointList.contains(point))
            pointList.add(point);
    }

    /** Add other unspecified SGF property.
        Do not add SGF properties that can be set with other member functions.
        This is for preserving unknown SGF properties that are unknown to
        this program, or cannot be handled in all cases.
        Example: the OT property is handled only if the value string, whose
        format is not specified by the SGF standard, is in a known format used
        by some other programs. Otherwise it is should be put to the unknown
        SGF properties, so that the old value is preserved if no new value
        is set in GameInfo.
        @param label The name of the property
        @param values The values of the property */
    public void addSgfProperty(String label, ArrayList<String> values)
    {
        createSgfProperties().add(label, values);
    }

    public void addSgfProperty(String label, String value)
    {
        createSgfProperties().add(label, value);
    }

    /** Add or remove a setup stone.
        It is not checked, if this stone is already in the list of added
        or removed stones.
        @param c The color of the stone (Black or White; Empty for removal).
        @param p The location of the setup stone. */
    public void addStone(GoColor c, GoPoint p)
    {
        assert p != null;
        createSetupInfo().m_stones.get(c).add(p);
    }

    /** Add or remove a list of setup stones.
        It is not checked, if this stone is already in the list of added
        or removed stones.
        @param c The color of the stone (Black or White; Empty for removal).
        @param list The locations of the setup stones. */
    public void addStones(GoColor c, ConstPointList list)
    {
        assert list != null;
        createSetupInfo().m_stones.get(c).addAllFromConst(list);
    }

    /** Create game information or return it if already existing. */
    public GameInfo createGameInfo()
    {
        MoreExtraInfo moreExtraInfo = createMoreExtraInfo();
        if (moreExtraInfo.m_info == null)
            moreExtraInfo.m_info = new GameInfo();
        return moreExtraInfo.m_info;
    }

    /** Child of main variation or null if no child.
        @return Node with index 0 or null, if no children. */
    public Node getChild()
    {
        if (! hasChildren())
            return null;
        return getChild(0);
    }

    /** Child of main variation or null if no child (const).
        @return Node with index 0 or null, if no children. */
    public ConstNode getChildConst()
    {
        return getChild();
    }

    /** Get child node.
        @param i Index of the child in [0...getNumberChildren() - 1]
        @return The child node */
    public Node getChild(int i)
    {
        if (getNumberChildren() == 1)
            return (Node)m_children;
        return (Node)((ArrayList)m_children).get(i);
    }

    /** Get child node (const).
        @param i Index of the child in [0...getNumberChildren() - 1]
        @return The child node */
    public ConstNode getChildConst(int i)
    {
        return getChild(i);
    }

    /** Get index of child node.
        @param child The child.
        @return Index of child or -1, if node is not a child of this node. */
    public int getChildIndex(ConstNode child)
    {
        for (int i = 0; i < getNumberChildren(); ++i)
            if (getChildConst(i) == child)
                return i;
        return -1;
    }

    /** Get comment.
        @return Comment stored in this node or null, if node contains no
        comment. */
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
        @return Father node of this node or null, if no father. */
    public Node getFather()
    {
        return m_father;
    }

    /** Get father node (const).
        @return Father node of this node or null, if no father. */
    public ConstNode getFatherConst()
    {
        return m_father;
    }

    public GameInfo getGameInfo()
    {
        if (m_extraInfo == null || m_extraInfo.m_moreExtraInfo == null)
            return null;
        return m_extraInfo.m_moreExtraInfo.m_info;
    }

    public ConstGameInfo getGameInfoConst()
    {
        return getGameInfo();
    }

    /** Get label for a location on the board.
        @param point The location.
        @return Label at location or null, if no label. */
    public String getLabel(GoPoint point)
    {
        Map<GoPoint,String> map = getLabels();
        if (map == null || ! map.containsKey(point))
            return null;
        return map.get(point);
    }

    /** Get all labels on the board.
        @return Map containing (Point,String) pairs. */
    public Map<GoPoint,String> getLabels()
    {
        if (m_extraInfo == null || m_extraInfo.m_moreExtraInfo == null)
            return null;
        return m_extraInfo.m_moreExtraInfo.m_label;
    }

    /** Get all labels on the board (unmodifiable). */
    public Map<GoPoint,String> getLabelsUnmodifiable()
    {
        Map<GoPoint,String> labels = getLabels();
        if (labels == null)
            return null;
        return Collections.unmodifiableMap(labels);
    }

    /** Get all markups of a type.
        @param type Markup type from Node.MARK_TYPES.
        @return Map containing (Point,String) pairs. */
    public PointList getMarked(MarkType type)
    {
        if (m_extraInfo == null || m_extraInfo.m_marked == null)
            return null;
        return m_extraInfo.m_marked.get(type);
    }

    /** Get all markups of a type (const).
        @param type Markup type from Node.MARK_TYPES.
        @return Map containing (Point,String) pairs. */
    public ConstPointList getMarkedConst(MarkType type)
    {
        return getMarked(type);
    }

    /** Get move contained in this node.
        @return Move or null, if no move. */
    public Move getMove()
    {
        return m_move;
    }

    /** Moves left in byoyomi.
        @param c The color.
        @return Moves left in byoyomi for that color or -1 if not in byoyomi or
        unknown. */
    public int getMovesLeft(GoColor c)
    {
        assert c.isBlackWhite();
        TimeInfo timeInfo = getTimeInfo();
        if (timeInfo == null)
            return -1;
        return timeInfo.m_movesLeft.get(c);
    }

    /** Get number of children.
        @return Number of children. */
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
        @return Color to play or null if color is not explicitely set. */
    public GoColor getPlayer()
    {
        SetupInfo setupInfo = getSetupInfo();
        if (setupInfo == null)
            return null;
        return setupInfo.m_player;
    }

    /** Get setup stones.
        @param c Color of the stones; EMPTY for removed stones.
        @return The added or removed stones. */
    public ConstPointList getSetup(GoColor c)
    {
        SetupInfo setupInfo = getSetupInfo();
        if (setupInfo == null)
            return PointList.getEmptyList();
        return setupInfo.m_stones.get(c);
    }

    /** Get other unspecified SGF properties.
        @return The map with other SGF properties mapping String label
        to String value
        @see #addSgfProperty */
    public SgfProperties getSgfProperties()
    {
        if (m_extraInfo == null || m_extraInfo.m_moreExtraInfo == null)
            return null;
        return m_extraInfo.m_moreExtraInfo.m_sgfProperties;
    }

    /** Get other unspecified SGF properties (const).
        @return The map with other SGF properties mapping String label
        to String value
        @see #addSgfProperty */
    public ConstSgfProperties getSgfPropertiesConst()
    {
        return getSgfProperties();
    }

    /** Time left for color after move was made.
        @param c The color
        @return Time left in seconds for this color or Double.NaN if unknown */
    public double getTimeLeft(GoColor c)
    {
        assert c.isBlackWhite();
        TimeInfo timeInfo = getTimeInfo();
        if (timeInfo == null)
            return Double.NaN;
        return timeInfo.m_timeLeft.get(c);
    }

    /** Get color to move.
        If a player is explicitely set, it is returned, otherwise if the
        node contains a move, the color of the move is returned.
        @return The color to move or null if nothing is known about
        the color to move */
    public GoColor getToMove()
    {
        GoColor player = getPlayer();
        if (player != null)
            return player;
        if (m_move != null)
            return m_move.getColor().otherColor();
        return null;
    }

    /** Return a value for the node.
        The meaning of a value is according to the SGF property V[]
        @return The value, or Float.NaN, if node contains no value */
    public float getValue()
    {
        if (m_extraInfo == null)
            return Float.NaN;
        return m_extraInfo.m_value;
    }

    public boolean hasChildren()
    {
        return (getNumberChildren() > 0);
    }

    /** Check if node contains a comment.
        More efficient than #getComment(), because getComment decodes the
        comment into a String, if it exists.
        @return true if node contains a comment */
    public boolean hasComment()
    {
        return (m_comment != null);
    }

    public boolean hasFather()
    {
        return (getFatherConst() != null);
    }

    /** Check if node has setup or delete stones.
        @return true, if node has setup or delete stones. */
    public boolean hasSetup()
    {
        for (GoColor c : BLACK_WHITE_EMPTY)
            if (getSetup(c).size() > 0)
                return true;
        return false;
    }

    /** Check if node is child of this node.
        @param node The node to check.
        @return true, if node is child node. */
    public boolean isChildOf(Node node)
    {
        return (node.getChildIndex(this) != -1);
    }

    /** Return true, if node stores no information. */
    public boolean isEmpty()
    {
        return (m_comment == null && m_move == null
                && (m_extraInfo == null || m_extraInfo.isEmpty()));
    }

    /** Make child the first child of this node.
        @param child One of the child nodes of this node. */
    @SuppressWarnings("unchecked")
    public void makeFirstChild(Node child)
    {
        assert child.isChildOf(this);
        if (getNumberChildren() <= 1)
            return;
        ArrayList<Node> list = (ArrayList<Node>)m_children;
        list.remove(child);
        list.add(0, child);
    }

    /** Remove child of this node.
        @param child Child to remove. */
    public void removeChild(Node child)
    {
        assert child.isChildOf(this);
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
            assert false;
        child.m_father = null;
    }

    /** Remove markup.
        @param point Location of the markup.
        @param type Type of the markup from Node.MARK_TYPES. */
    public void removeMarked(GoPoint point, MarkType type)
    {
        assert point != null;
        Map<MarkType,PointList> marked = createMarked();
        PointList pointList = (PointList)marked.get(type);
        if (pointList != null)
            pointList.remove(point);
    }

    /** Remove setup at point.
        Remove any setup that was added with #addStone at a point.
        @param p Location of the setup. */
    public void removeSetup(GoPoint p)
    {
        assert p != null;
        SetupInfo setupInfo = getSetupInfo();
        if (setupInfo == null)
            return;
        for (GoColor c : BLACK_WHITE_EMPTY)
            while (setupInfo.m_stones.get(c).remove(p));
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
        @param comment The comment. If the parameter is null, empty or
        contains only whitespaces, then the comment will be deleted from this
        node. */
    public void setComment(String comment)
    {
        if (StringUtil.isEmpty(comment))
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
        @param father The new father. */
    public void setFather(Node father)
    {
        m_father = father;
    }

    /** Add label at a location on the board.
        Whitespaces will be trimmed.
        @param point The location.
        @param label The text of the label; empty string or null to delete
        the label. */
    public void setLabel(GoPoint point, String label)
    {
        assert point != null;
        Map<GoPoint,String> map = createLabel();
        map.remove(point);
        if (label == null)
            return;
        label = label.trim();
        if (label.equals(""))
            return;
        map.put(point, label);
    }

    /** Set move stored in this node.
        @param move The move or null, if no move. */
    public void setMove(Move move)
    {
        m_move = move;
    }

    /** Set byoyomi moves left.
        @param c The player.
        @param n Number of moves left. */
    public void setMovesLeft(GoColor c, int n)
    {
        assert c.isBlackWhite();
        createTimeInfo().m_movesLeft.set(c, n);
    }

    /** Set byoyomi time left.
        @param c The player.
        @param seconds Time left in seconds. */
    public void setTimeLeft(GoColor c, double seconds)
    {
        assert c.isBlackWhite();
        createTimeInfo().m_timeLeft.set(c, seconds);
    }

    /** Explicitely set color to play.
        @param color Color to play. */
    public void setPlayer(GoColor color)
    {
        assert color.isBlackWhite();
        createSetupInfo().m_player = color;
    }

    /** Set value for this node.
        @see #getValue()
        @param value The value */
    public void setValue(float value)
    {
        createExtraInfo();
        m_extraInfo.m_value = value;
    }

    /** Sort the lists of setup stones (add stones and remove stones.
        Sorted lists for setup stones make it easier to compare, if
        two nodes have the same lists. */
    public void sortSetup()
    {
        for (GoColor c : BLACK_WHITE_EMPTY)
            if (getSetup(c).size() > 0)
                Collections.sort(getSetupInfo().m_stones.get(c));
    }

    /** Return next child after a given child.
        @param child The child
        @return The next child or null, if there is no next child */
    public ConstNode variationAfter(ConstNode child)
    {
        int numberChildren = getNumberChildren();
        if (numberChildren == 1)
            return null;
        int i;
        for (i = 0; i < numberChildren; ++i)
            if (getChildConst(i) == child)
                break;
        if (i == numberChildren - 1)
            return null;
        return getChildConst(i + 1);
    }

    /** Return previous child before a given child.
        @param child The child
        @return The previous child or null, if there is no previous child */
    public ConstNode variationBefore(ConstNode child)
    {
        int numberChildren = getNumberChildren();
        if (numberChildren == 1)
            return null;
        int i;
        for (i = 0; i < numberChildren; ++i)
            if (getChildConst(i) == child)
                break;
        if (i == 0)
            return null;
        return getChildConst(i - 1);
    }

    /** Comment stored as bytes.
        Store comments in UTF-8, because that saves up to a factor of 2 in
        size compared to a string. */
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

    private MoreExtraInfo createMoreExtraInfo()
    {
        createExtraInfo();
        if (m_extraInfo.m_moreExtraInfo == null)
            m_extraInfo.m_moreExtraInfo = new MoreExtraInfo();
        return m_extraInfo.m_moreExtraInfo;
    }

    private Map<GoPoint,String> createLabel()
    {
        MoreExtraInfo moreExtraInfo = createMoreExtraInfo();
        if (moreExtraInfo.m_label == null)
            moreExtraInfo.m_label = new TreeMap<GoPoint,String>();
        return moreExtraInfo.m_label;
    }

    private Map<MarkType,PointList> createMarked()
    {
        createExtraInfo();
        if (m_extraInfo.m_marked == null)
            m_extraInfo.m_marked = new TreeMap<MarkType,PointList>();
        return m_extraInfo.m_marked;
    }

    private SetupInfo createSetupInfo()
    {
        MoreExtraInfo moreExtraInfo = createMoreExtraInfo();
        if (moreExtraInfo.m_setupInfo == null)
            moreExtraInfo.m_setupInfo = new SetupInfo();
        return moreExtraInfo.m_setupInfo;
    }

    private SgfProperties createSgfProperties()
    {
        MoreExtraInfo moreExtraInfo = createMoreExtraInfo();
        if (moreExtraInfo.m_sgfProperties == null)
            moreExtraInfo.m_sgfProperties = new SgfProperties();
        return moreExtraInfo.m_sgfProperties;
    }

    private TimeInfo createTimeInfo()
    {
        MoreExtraInfo moreExtraInfo = createMoreExtraInfo();
        if (moreExtraInfo.m_timeInfo == null)
            moreExtraInfo.m_timeInfo = new TimeInfo();
        return moreExtraInfo.m_timeInfo;
    }

    private SetupInfo getSetupInfo()
    {
        if (m_extraInfo == null || m_extraInfo.m_moreExtraInfo == null)
            return null;
        return m_extraInfo.m_moreExtraInfo.m_setupInfo;
    }

    private TimeInfo getTimeInfo()
    {
        if (m_extraInfo == null || m_extraInfo.m_moreExtraInfo == null)
            return null;
        return m_extraInfo.m_moreExtraInfo.m_timeInfo;
    }
}
