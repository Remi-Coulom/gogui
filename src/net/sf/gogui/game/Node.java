//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

class ExtraInfo
{
    public SetupInfo m_setupInfo;

    public TimeInfo m_timeInfo;

    public TreeMap m_sgfProperties;

    /** Map<String,Vector<GoPoint>> */
    public Map m_marked;

    public Map m_label;
}

//----------------------------------------------------------------------------

class SetupInfo
{
    public GoColor m_player = GoColor.EMPTY;

    public Vector m_black = new Vector();

    public Vector m_white = new Vector();

    public Vector m_empty = new Vector();
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
*/
public final class Node
{
    public static final String MARKED = "mark";

    public static final String MARKED_CIRCLE = "circle";

    public static final String MARKED_SQUARE = "square";

    public static final String MARKED_TRIANGLE = "triangle";

    public static final String[] MARK_TYPES = {
        MARKED,
        MARKED_CIRCLE,
        MARKED_SQUARE,
        MARKED_TRIANGLE
    };

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
        {
            m_children = node;
        }
        else
        {
            if (m_children instanceof Node)
            {
                Vector vector = new Vector(2);
                vector.add(m_children);
                vector.add(node);
                m_children = vector;
            }
            else
                ((Vector)m_children).add(node);
        }
        node.m_father = this;
    }

    public void addBlack(GoPoint point)
    {
        assert(point != null);
        createSetupInfo().m_black.add(point);
    }

    public void addEmpty(GoPoint point)
    {
        assert(point != null);
        createSetupInfo().m_empty.add(point);
    }

    public void addMarked(GoPoint point, String type)
    {
        assert(point != null);
        assert(type == MARKED || type == MARKED_SQUARE
               || type == MARKED_CIRCLE || type == MARKED_TRIANGLE);
        Map marked = createMarked();
        Vector pointList = (Vector)marked.get(type);
        if (pointList == null)
        {
            pointList = new Vector(1);
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

    public void addWhite(GoPoint point)
    {
        assert(point != null);
        createSetupInfo().m_white.add(point);
    }

    public GoPoint getAddBlack(int i)
    {
        return (GoPoint)m_extraInfo.m_setupInfo.m_black.get(i);
    }

    public GoPoint getAddEmpty(int i)
    {
        return (GoPoint)m_extraInfo.m_setupInfo.m_empty.get(i);
    }

    public GoPoint getAddWhite(int i)
    {
        return (GoPoint)m_extraInfo.m_setupInfo.m_white.get(i);
    }

    /** Get stones added and moves all as moves.
        This function is for transmitting setup stones to Go engines
        that support only play commands.
        May include moves with color EMPTY for delete stones.
        Also may include a pass move at the end to make sure, that the
        right color is to move after executing all returned moves.
        No check is performed if the setup stones create a position
        with no-liberty blocks, in which case a play command would
        capture some stones.
    */
    public Vector getAllAsMoves()
    {
        Vector moves = new Vector();
        if (hasSetupInfo())
        {
            for (int i = 0; i < getNumberAddBlack(); ++i)
                moves.add(Move.create(getAddBlack(i), GoColor.BLACK));
            for (int i = 0; i < getNumberAddWhite(); ++i)
                moves.add(Move.create(getAddWhite(i), GoColor.WHITE));
            for (int i = 0; i < getNumberAddEmpty(); ++i)
                moves.add(Move.create(getAddEmpty(i), GoColor.EMPTY));
        }
        if (m_move != null)
            moves.add(m_move);
        if (moves.size() > 0)
        {
            GoColor toMove = getToMove();
            if (toMove == GoColor.EMPTY)
                toMove = GoColor.BLACK;
            Move lastMove = (Move)moves.get(moves.size() - 1);
            GoColor otherColor = lastMove.getColor().otherColor();
            if (toMove != otherColor && otherColor != GoColor.EMPTY)
                moves.add(Move.create(null, otherColor));
        }
        return moves;
    }

    /** Child of main variation or null if no child. */
    public Node getChild()
    {
        if (getNumberChildren() == 0)
            return null;
        return getChild(0);
    }

    public Node getChild(int i)
    {
        if (getNumberChildren() == 1)
            return (Node)m_children;
        return (Node)((Vector)m_children).get(i);
    }

    public int getChildIndex(Node child)
    {
        for (int i = 0; i < getNumberChildren(); ++i)
            if (getChild(i) == child)
                return i;
        return -1;
    }

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

    public Node getFather()
    {
        return m_father;
    }

    public String getLabel(GoPoint point)
    {
        Map map = getLabels();
        if (map == null || ! map.containsKey(point))
            return null;
        return (String)map.get(point);
    }

    public Map getLabels()
    {
        if (m_extraInfo == null)
            return null;
        return m_extraInfo.m_label;
    }

    public Vector getMarked(String type)
    {
        if (m_extraInfo == null || m_extraInfo.m_marked == null)
            return null;
        return (Vector)m_extraInfo.m_marked.get(type);
    }

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

    public int getNumberAddBlack()
    {
        if (! hasSetupInfo())
            return -1;
        return m_extraInfo.m_setupInfo.m_black.size();
    }

    public int getNumberAddEmpty()
    {
        if (! hasSetupInfo())
            return -1;
        return m_extraInfo.m_setupInfo.m_empty.size();
    }

    public int getNumberAddWhite()
    {
        if (! hasSetupInfo())
            return -1;
        return m_extraInfo.m_setupInfo.m_white.size();
    }

    public int getNumberChildren()
    {
        if (m_children == null)
            return 0;
        if (m_children instanceof Node)
            return 1;
        return ((Vector)m_children).size();
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

    public boolean isChildOf(Node node)
    {
        for (int i = 0; i < node.getNumberChildren(); ++i)
            if (node.getChild(i) == this)
                return true;
        return false;
    }

    public void makeMainVariation(Node child)
    {
        if (getNumberChildren() <= 1)
            return;
        Vector vector = (Vector)m_children;
        vector.remove(child);
        vector.add(0, child);
    }

    public void removeMarked(GoPoint point, String type)
    {
        assert(point != null);
        Map marked = createMarked();
        Vector pointList = (Vector)marked.get(type);
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

    public void setFather(Node father)
    {
        m_father = father;
    }

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

    public void setMove(Move move)
    {
        m_move = move;
    }

    public void setMovesLeftBlack(int moves)
    {
        createTimeInfo().m_movesLeftBlack = moves;
    }

    public void setMovesLeftWhite(int moves)
    {
        createTimeInfo().m_movesLeftWhite = moves;
    }

    public void setTimeLeftBlack(double timeLeft)
    {
        createTimeInfo().m_timeLeftBlack = timeLeft;
    }

    public void setTimeLeftWhite(double timeLeft)
    {
        createTimeInfo().m_timeLeftWhite = timeLeft;
    }

    public void setPlayer(GoColor color)
    {
        assert(color == GoColor.BLACK || color == GoColor.WHITE);
        createSetupInfo().m_player = color;
    }

    public void removeChild(Node child)
    {
        int numberChildren = getNumberChildren();
        assert(numberChildren > 0);
        if (numberChildren == 1)
        {
            assert(m_children == child);
            m_children = null;
            return;
        }
        else if (numberChildren == 2)
        {
            Vector vector = (Vector)m_children;
            assert(vector.contains(child));
            m_children = child;
            return;
        }
        else if (numberChildren > 2)
        {
            Vector vector = (Vector)m_children;
            assert(vector.contains(child));
            vector.remove(child);
        }
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

    /** Node if one child only, Vector otherwise. */
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
