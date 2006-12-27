//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.ObjectUtil;

/** Manages a tree, board and current node in tree. */
public class Game
{
    public Game(int boardSize)
    {
        m_board = new Board(boardSize);
        init(boardSize, null, null, "", null);
    }

    public Game(int boardSize, Komi komi, ArrayList handicap, String rules,
                TimeSettings timeSettings)
    {
        m_board = new Board(boardSize);
        init(boardSize, komi, handicap, rules, timeSettings);
    }

    /** Add a mark property to current node. */
    public void addMarked(GoPoint point, MarkType type)
    {
        m_current.addMarked(point, type);
        m_modified = true;
    }

    public void backward(int n)
    {
        assert(n >=0);
        for (int i = 0; i < n && m_current != getRoot(); ++i)
            m_current = m_current.getFather();
        updateBoard();
    }

    /** Clear modified flag.
        Can be used for instance after game was saved.
        @see #isModified()
    */
    public void clearModified()
    {
        m_modified = false;
    }

    /** Append new empty node and make it current node.
        Can be use for instance to prepare for setup stones if current node
        contains a move.
    */
    public void createNewChild()
    {
        Node node = new Node();
        m_current.append(node);
        m_current = node;
        m_modified = true;
    }

    public void forward(int n)
    {
        assert(n >= 0);
        ConstNode node = m_current;
        for (int i = 0; i < n; ++i)
        {
            ConstNode child = node.getChildConst();
            if (child == null)
                break;
            node = child;
        }
        gotoNode(node);
    }

    public ConstBoard getBoard()
    {
        return m_board;
    }

    public ConstNode getCurrentNode()
    {
        return m_current;
    }

    public ConstGameInformation getGameInformation()
    {
        return m_tree.getGameInformation();
    }

    public int getSize()
    {
        return m_board.getSize();
    }

    public GoColor getToMove()
    {
        return m_board.getToMove();
    }

    public ConstGameTree getTree()
    {
        return m_tree;
    }

    public void gotoNode(ConstNode node)
    {
        assert(NodeUtil.getRoot(node) == getRoot());
        m_current = (Node)node;
        updateBoard();
    }

    public void init(int boardSize, Komi komi, ArrayList handicap,
                     String rules, TimeSettings timeSettings)
    {
        m_tree = new GameTree(boardSize, komi, handicap, rules, timeSettings);
        m_current = m_tree.getRoot();
        updateBoard();
        m_modified = false;
    }

    public void init(GameTree tree)
    {
        m_tree = tree;
        m_current = m_tree.getRoot();
        updateBoard();
        m_modified = false;
    }

    /** Check if game was modified.
        @return true, if game was mofified since constructor or last call to
        one of the init() functions or to clearModified().
    */
    public boolean isModified()
    {
        return m_modified;
    }

    public void keepOnlyMainVariation()
    {
        m_tree.keepOnlyMainVariation();
        m_modified = true;
    }

    public void keepOnlyPosition()
    {
        m_tree = NodeUtil.makeTreeFromPosition(getGameInformation(), m_board);
        m_board.init(m_board.getSize());
        m_current = m_tree.getRoot();
        updateBoard();
        m_modified = true;
    }

    /** Make current node the main variation. */
    public void makeMainVariation()
    {
        NodeUtil.makeMainVariation(m_current);
        m_modified = true;
    }

    public void play(Move move, ConstClock clock)
    {
        m_current = createNode(m_current, move, clock);
        updateBoard();
        m_modified = true;
    }

    /** Remove a mark property from current node. */
    public void removeMarked(GoPoint point, MarkType type)
    {
        m_current.removeMarked(point, type);
        m_modified = true;
    }

    /** Set comment in current node. */
    public void setComment(String comment)
    {
        m_modified = ! ObjectUtil.equals(comment, m_current.getComment());
        m_current.setComment(comment);
    }

    public void setDate(String date)
    {
        GameInformation gameInformation = m_tree.getGameInformation();
        m_modified = ! ObjectUtil.equals(date, gameInformation.getDate());
        gameInformation.setDate(date);
    }

    public void setResult(String result)
    {
        GameInformation gameInformation = m_tree.getGameInformation();
        m_modified = ! ObjectUtil.equals(result, gameInformation.getResult());
        gameInformation.setResult(result);
    }

    /** Set label in current node. */
    public void setLabel(GoPoint point, String value)
    {
        m_modified = ! ObjectUtil.equals(value, m_current.getLabel(point));
        m_current.setLabel(point, value);
    }

    public void setPlayerBlack(String name)
    {
        GameInformation gameInformation = m_tree.getGameInformation();
        m_modified = ! ObjectUtil.equals(name,
                                         gameInformation.getPlayerBlack());
        gameInformation.setPlayerBlack(name);
    }

    public void setPlayerWhite(String name)
    {
        GameInformation gameInformation = m_tree.getGameInformation();
        m_modified = ! ObjectUtil.equals(name,
                                         gameInformation.getPlayerWhite());
        m_tree.getGameInformation().setPlayerWhite(name);
    }

    public void setRankBlack(String rank)
    {
        GameInformation gameInformation = m_tree.getGameInformation();
        m_modified = ! ObjectUtil.equals(rank, gameInformation.getRankBlack());
        m_tree.getGameInformation().setRankBlack(rank);
    }

    public void setRankWhite(String rank)
    {
        GameInformation gameInformation = m_tree.getGameInformation();
        m_modified = ! ObjectUtil.equals(rank, gameInformation.getRankWhite());
        m_tree.getGameInformation().setRankWhite(rank);
    }

    public void setToMove(GoColor color)
    {
        assert(color != null);
        assert(! color.equals(GoColor.EMPTY));
        m_modified = (! ObjectUtil.equals(color, m_current.getPlayer())
                      || color.equals(m_board.getToMove()));
        m_current.setPlayer(color);
        m_board.setToMove(color);
    }

    public void setup(GoPoint point, GoColor color)
    {
        assert(point != null);
        m_modified = true;
        m_current.removeSetup(point);
        Node father = m_current.getFather();
        if (father != null)
        {
            m_boardUpdater.update(getTree(), father, m_board);
            GoColor oldColor = m_board.getColor(point);
            if (oldColor == color)
            {
                updateBoard();
                return;
            }
        }
        if (color == GoColor.EMPTY)
            m_current.addEmpty(point);
        else if (color == GoColor.BLACK)
            m_current.addBlack(point);
        else if (color == GoColor.WHITE)
            m_current.addWhite(point);
        updateBoard();
    }

    /** Truncate current node and subtree.
        New current node is the father of the old current node.
    */
    public void truncate()
    {
        assert(m_current.getFather() != null);
        Node oldCurrentNode = m_current;
        backward(1);
        m_current.removeChild(oldCurrentNode);
        m_modified = true;
    }

    /** Remove children of currentNode. */
    public void truncateChildren()
    {
        NodeUtil.truncateChildren(m_current);
        m_modified = true;
    }

    /** See #isModified() */
    private boolean m_modified;

    private Board m_board;

    private BoardUpdater m_boardUpdater = new BoardUpdater();

    private GameTree m_tree;

    private Node m_current;

    /** Create a new node with a move and append it to current node.
        Also adds time information from clock, if not null and initialized.
        The clock must not be running.
    */
    private static Node createNode(Node currentNode, Move move,
                                   ConstClock clock)
    {
        Node node = new Node(move);
        if (clock != null && clock.isInitialized())
        {
            assert(! clock.isRunning());
            GoColor color = move.getColor();
            // Round time to seconds
            long timeLeft = clock.getTimeLeft(color) / 1000L;
            if (color == GoColor.BLACK)
            {
                node.setTimeLeftBlack((double)timeLeft);
                if (clock.isInByoyomi(color))
                    node.setMovesLeftBlack(clock.getMovesLeft(color));
            }
            else
            {
                assert(color == GoColor.WHITE);
                node.setTimeLeftWhite((double)timeLeft);
                if (clock.isInByoyomi(color))
                    node.setMovesLeftWhite(clock.getMovesLeft(color));
            }
        }
        currentNode.append(node);
        return node;
    }

    private ConstNode getRoot()
    {
        return m_tree.getRoot();
    }

    private void updateBoard()
    {
        m_boardUpdater.update(m_tree, m_current, m_board);
    }
}
