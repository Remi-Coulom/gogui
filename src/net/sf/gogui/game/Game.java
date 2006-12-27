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

/** Manages a tree, board, current node and clock. */
public class Game
{
    public Game(int boardSize)
    {
        m_board = new Board(boardSize);
        m_clock = new Clock();
        init(boardSize, null, null, "", null);
    }

    public Game(int boardSize, Komi komi, ArrayList handicap, String rules,
                TimeSettings timeSettings)
    {
        m_board = new Board(boardSize);
        m_clock = new Clock();
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

    public ConstClock getClock()
    {
        return m_clock;
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

    public void haltClock()
    {
        m_clock.halt();
    }

    public void init(int boardSize, Komi komi, ArrayList handicap,
                     String rules, TimeSettings timeSettings)
    {
        m_tree = new GameTree(boardSize, komi, handicap, rules, timeSettings);
        m_current = m_tree.getRoot();
        updateBoard();
        m_clock.reset();
        m_clock.halt();
        m_modified = false;
    }

    public void init(GameTree tree)
    {
        m_tree = tree;
        m_current = m_tree.getRoot();
        updateBoard();
        m_clock.reset();
        m_clock.halt();
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

    public void play(Move move)
    {
        m_clock.stopMove();
        Node node = new Node(move);
        if (m_clock.isInitialized())
        {
            assert(! m_clock.isRunning());
            GoColor color = move.getColor();
            // Round time to seconds
            long timeLeft = m_clock.getTimeLeft(color) / 1000L;
            if (color == GoColor.BLACK)
            {
                node.setTimeLeftBlack((double)timeLeft);
                if (m_clock.isInByoyomi(color))
                    node.setMovesLeftBlack(m_clock.getMovesLeft(color));
            }
            else
            {
                assert(color == GoColor.WHITE);
                node.setTimeLeftWhite((double)timeLeft);
                if (m_clock.isInByoyomi(color))
                    node.setMovesLeftWhite(m_clock.getMovesLeft(color));
            }
        }
        m_current.append(node);
        m_current = node;
        updateBoard();
        m_modified = true;
        m_clock.startMove(getToMove());
    }

    /** Remove a mark property from current node. */
    public void removeMarked(GoPoint point, MarkType type)
    {
        m_current.removeMarked(point, type);
        m_modified = true;
    }

    public void resetClock()
    {
        m_clock.reset();
    }

    public void restoreClock()
    {        
        GoColor color = getToMove();
        ConstNode currentNode = getCurrentNode();
        restoreClock(currentNode, color.otherColor());
        ConstNode father = currentNode.getFatherConst();
        if (father != null)
            restoreClock(father, color);
    }

    public void setClockListener(Clock.Listener listener)
    {
        m_clock.setListener(listener);
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

    public void setTimeSettings(TimeSettings timeSettings)
    {
        m_clock.setTimeSettings(timeSettings);
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

    public void startClock()
    {
        if (! m_clock.isRunning())
            m_clock.startMove(getToMove());
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

    private final Board m_board;

    private final BoardUpdater m_boardUpdater = new BoardUpdater();

    private GameTree m_tree;

    private Node m_current;

    private final Clock m_clock;

    private ConstNode getRoot()
    {
        return m_tree.getRoot();
    }

    private void restoreClock(ConstNode node, GoColor color)
    {
        Move move = node.getMove();
        if (move == null)
        {
            if (node == getTree().getRootConst())
                resetClock();
            return;
        }
        if (move.getColor() != color)
            return;
        double timeLeft = node.getTimeLeft(color);
        int movesLeft = node.getMovesLeft(color);
        if (! Double.isNaN(timeLeft))
            m_clock.setTimeLeft(color, (long)(timeLeft * 1000), movesLeft);
    }

    private void updateBoard()
    {
        m_boardUpdater.update(m_tree, m_current, m_board);
    }
}
