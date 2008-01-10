//----------------------------------------------------------------------------
// BoardUpdaterTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

public final class BoardUpdaterTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(BoardUpdaterTest.class);
    }

    public void setUp()
    {
        m_updater = new BoardUpdater();
    }

    public void testSetup()
    {
        GameTree tree = new GameTree();
        Node root = tree.getRoot();
        root.addStone(GoColor.BLACK, GoPoint.get(1, 1));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 2));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 3));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 4));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 5));
        Node child = new Node();
        child.addStone(GoColor.WHITE, GoPoint.get(2, 1));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 2));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 3));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 4));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 5));
        child.addStone(GoColor.BLACK, GoPoint.get(3, 1));
        root.append(child);
        Board board = new Board(19);
        m_updater.update(tree, child, board);
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 1)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 2)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 3)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 4)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 5)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 1)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 2)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 3)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 4)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 5)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(3, 1)));
    }

    /** Test that setting a stone on an point occupied by the second move
        in a game works.
        Checks for a bug that was only triggered, if the changed stone color
        was the second move played, not the first move,
    */
    public void testSetupOnOccupiedMoveTwo()
    {
        GameTree tree = new GameTree();
        Node root = tree.getRoot();
        Node node1 = new Node();
        root.append(node1);
        node1.setMove(Move.get(GoColor.BLACK, GoPoint.get(4, 4)));
        Node node2 = new Node();
        node1.append(node2);
        node2.setMove(Move.get(GoColor.WHITE, GoPoint.get(3, 3)));
        Node node3 = new Node();
        node2.append(node3);
        node3.addStone(GoColor.BLACK, GoPoint.get(3, 3));
        Board board = new Board(19);
        m_updater.update(tree, node3, board);
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(3, 3)));
    }

    private BoardUpdater m_updater;
}
