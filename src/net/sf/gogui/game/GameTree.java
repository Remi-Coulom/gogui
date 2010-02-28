// GameTree.java

package net.sf.gogui.game;

import java.text.DecimalFormat;
import java.util.Calendar;
import net.sf.gogui.go.ConstPointList;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Komi;

/** Game tree.
    Note that the tree can have different GameInfo objects for different
    subtrees to allow the representation of trees loaded from SGF. Creating
    trees with multiple GameInfo objects is not not actively supported
    in GoGui, because they cannot be saved in (Jago's) XML format. */
public class GameTree
    implements ConstGameTree
{
    public GameTree()
    {
        m_boardSize = GoPoint.DEFAULT_SIZE;
        m_root = new Node();
        m_root.createGameInfo();
        setDate();
    }

    public GameTree(int boardSize, Komi komi, ConstPointList handicap,
                    String rules, TimeSettings timeSettings)
    {
        m_boardSize = boardSize;
        m_root = new Node();
        GameInfo info = m_root.createGameInfo();
        setDate();
        info.setKomi(komi);
        info.set(StringInfo.RULES, rules);
        if (timeSettings != null)
            info.setTimeSettings(timeSettings);
        if (handicap != null)
        {
            info.setHandicap(handicap.size());
            if (handicap.size() > 0)
            {
                m_root.addStones(BLACK, handicap);
                m_root.setPlayer(WHITE);
            }
        }
    }

    /** Probably only needed by SgfReader. */
    public GameTree(int boardSize, Node root)
    {
        m_boardSize = boardSize;
        root.createGameInfo();
        m_root = root;
    }

    public int getBoardSize()
    {
        return m_boardSize;
    }

    /** Find the game information valid for this node.
        @return The game information from the nearest ancestor node,
        which has a game information (the root node is always guaranteed
        to have one). */
    public GameInfo getGameInfo(ConstNode node)
    {
        assert NodeUtil.getRoot(node) == getRoot();
        return getGameInfoNode(node).getGameInfo();
    }

    /** Find the node with game information valid for this node.
        @return The nearest ancestor node which has a game information
        (the root node is always guaranteed to have one). */
    public Node getGameInfoNode(ConstNode node)
    {
        assert NodeUtil.getRoot(node) == getRoot();
        while (node.getGameInfoConst() == null)
            node = node.getFatherConst();
        return (Node)node;
    }

    /** @see #getGameInfo */
    public ConstGameInfo getGameInfoConst(ConstNode node)
    {
        return getGameInfo((Node)node);
    }

    /** Get a non-const reference to a const node.
        Requires: node is part of this game tree. */
    public Node getNode(ConstNode node)
    {
        assert NodeUtil.getRoot(node) == getRoot();
        return (Node)node;
    }

    public Node getRoot()
    {
        return m_root;
    }

    public ConstNode getRootConst()
    {
        return m_root;
    }

    public boolean hasVariations()
    {
        ConstNode node = m_root;
        while (node != null)
        {
            if (node.getNumberChildren() > 1)
                return true;
            node = node.getChildConst();
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

    private final int m_boardSize;

    private final Node m_root;

    private void setDate()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        DecimalFormat format = new DecimalFormat("00");
        GameInfo info = m_root.getGameInfo();
        info.set(StringInfo.DATE,
                 Integer.toString(year) + "-" + format.format(month) + "-"
                 + format.format(day));
    }
}
