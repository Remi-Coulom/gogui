//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package game;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import go.Color;
import go.Move;
import go.Point;

//----------------------------------------------------------------------------

class ExtraInfo
{
    public SetupInfo m_setupInfo;

    public TimeInfo m_timeInfo;

    public TreeMap m_sgfProperties;
}

//----------------------------------------------------------------------------

class SetupInfo
{
    public Color m_player = Color.EMPTY;

    public Vector m_black = new Vector();

    public Vector m_white = new Vector();
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

    public void addBlack(Point point)
    {
        assert(point != null);
        createSetupInfo().m_black.add(point);
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

    public void addWhite(Point point)
    {
        assert(point != null);
        createSetupInfo().m_white.add(point);
    }

    public Point getAddBlack(int i)
    {
        return (Point)m_extraInfo.m_setupInfo.m_black.get(i);
    }

    public Point getAddWhite(int i)
    {
        return (Point)m_extraInfo.m_setupInfo.m_white.get(i);
    }

    /** Get stones added and moves all as moves.
        Also might include a pass move at the end to make sure, that the
        right color is to move after executing all returned moves.
    */
    public Vector getAllAsMoves()
    {
        Vector moves = new Vector();
        if (hasSetupInfo())
        {
            for (int i = 0; i < getNumberAddBlack(); ++i)
                moves.add(new Move(getAddBlack(i), Color.BLACK));
            for (int i = 0; i < getNumberAddWhite(); ++i)
                moves.add(new Move(getAddWhite(i), Color.WHITE));
        }
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

    public Move getMove()
    {
        return m_move;
    }

    /** Moves left in byoyomi for black.
        -1 if not in byyomi or unknown.
    */
    public int getMovesLeftBlack()
    {
        if (! hasTimeInfo())
            return -1;
        return m_extraInfo.m_timeInfo.m_movesLeftBlack;
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
        @return Color to play or Color.EMPTY if color is not explicitely set.
    */
    public Color getPlayer()
    {
        if (! hasSetupInfo())
            return Color.EMPTY;
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

    /** Time left for black after move was made.
        Returns Double.NaN if unknown.
    */
    public double getTimeLeftBlack()
    {
        if (! hasTimeInfo())
            return Double.NaN;
        return m_extraInfo.m_timeInfo.m_timeLeftBlack;
    }

    /** Time left for white after move was made.
        Returns Double.NaN if unknown.
    */
    public double getTimeLeftWhite()
    {
        if (! hasTimeInfo())
            return Double.NaN;
        return m_extraInfo.m_timeInfo.m_timeLeftWhite;
    }

    /** Get color to move.
        Determining the color to move takes into consideration an explicitely
        set player color and moves contained in this node.
        If nothing is known about the color to move, it returns Color.EMPTY.
    */
    public Color getToMove()
    {
        Color player = getPlayer();
        if (player != Color.EMPTY)
            return player;
        if (m_move != null)
            return m_move.getColor().otherColor();
        return Color.EMPTY;
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

    public void setPlayer(Color color)
    {
        assert(color == Color.BLACK || color == Color.WHITE);
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

    private SetupInfo createSetupInfo()
    {
        if (m_extraInfo == null)
            m_extraInfo = new ExtraInfo();
        if (m_extraInfo.m_setupInfo == null)
            m_extraInfo.m_setupInfo = new SetupInfo();
        return m_extraInfo.m_setupInfo;
    }

    private Map createSgfProperties()
    {
        if (m_extraInfo == null)
            m_extraInfo = new ExtraInfo();
        if (m_extraInfo.m_sgfProperties == null)
            m_extraInfo.m_sgfProperties = new TreeMap();
        return m_extraInfo.m_sgfProperties;
    }

    private TimeInfo createTimeInfo()
    {
        if (m_extraInfo == null)
            m_extraInfo = new ExtraInfo();
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
