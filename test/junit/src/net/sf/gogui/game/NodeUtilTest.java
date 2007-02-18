//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

public final class NodeUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(NodeUtilTest.class);
    }

    public void testFindByMoveNumber()
    {
        assertTrue(NodeUtil.findByMoveNumber(m_node0, 1) == m_node2);
        assertTrue(NodeUtil.findByMoveNumber(m_node0, 2) == m_node3);
        assertTrue(NodeUtil.findByMoveNumber(m_node4, 2) == m_node3);
        assertTrue(NodeUtil.findByMoveNumber(m_node0, 3) == m_node5);
        assertNull(NodeUtil.findByMoveNumber(m_node0, 4));
        assertTrue(NodeUtil.findByMoveNumber(m_node0, 0) == m_node0);
        assertNull(NodeUtil.findByMoveNumber(m_node0, -1));
        assertTrue(NodeUtil.findByMoveNumber(m_node7, 1) == m_node8);
    }

    public void testFindByVariation()
    {
        assertTrue(NodeUtil.findByVariation(m_node0, "") == m_node0);
        assertTrue(NodeUtil.findByVariation(m_node0, "1") == m_node2);
        assertTrue(NodeUtil.findByVariation(m_node0, "2") == m_node7);
        assertNull(NodeUtil.findByVariation(m_node0, "3"));
        assertTrue(NodeUtil.findByVariation(m_node0, "2.1") == m_node8);
        assertTrue(NodeUtil.findByVariation(m_node0, "2.2") == m_node9);
    }

    public void testGetAllAsMovesMoveOnly()
    {
        Move move = Move.get(GoColor.BLACK, 1, 1);
        Node node = new Node(move);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(1, moves.size());
        assertSame(move, moves.get(0));
    }

    public void testGetAllAsMovesMoveAndPlayerBlack()
    {
        Move move = Move.get(GoColor.BLACK, 1, 1);
        Node node = new Node(move);
        node.setPlayer(GoColor.BLACK);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(2, moves.size());
        assertSame(move, moves.get(0));
        assertSame(Move.getPass(GoColor.WHITE), moves.get(1));
    }

    public void testGetAllAsMovesMoveAndPlayerWhite()
    {
        Move move = Move.get(GoColor.WHITE, 1, 1);
        Node node = new Node(move);
        node.setPlayer(GoColor.WHITE);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(2, moves.size());
        assertSame(move, moves.get(0));
        assertSame(Move.getPass(GoColor.BLACK), moves.get(1));
    }

    public void testGetAllAsMovesSetupBlack()
    {
        Node node = new Node();
        GoPoint point = GoPoint.get(1, 1);
        node.addStone(GoColor.BLACK, point);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(1, moves.size());
        assertSame(Move.get(GoColor.BLACK, point), moves.get(0));
    }

    public void testGetAllAsMovesSetupBlackAndPlayer()
    {
        Node node = new Node();
        GoPoint point = GoPoint.get(1, 1);
        node.addStone(GoColor.BLACK, point);
        node.setPlayer(GoColor.WHITE);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(1, moves.size());
        assertSame(Move.get(GoColor.BLACK, point), moves.get(0));
    }

    public void testGetAllAsMovesSetupBlackAndMove()
    {
        Move move = Move.get(GoColor.BLACK, 1, 1);
        Node node = new Node(move);
        GoPoint point1 = GoPoint.get(1, 1);
        GoPoint point2 = GoPoint.get(2, 2);
        node.addStone(GoColor.BLACK, point1);
        node.addStone(GoColor.BLACK, point2);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(3, moves.size());
        assertSame(Move.get(GoColor.BLACK, point1), moves.get(0));
        assertSame(Move.get(GoColor.BLACK, point2), moves.get(1));
        assertSame(move, moves.get(2));
    }

    public void testGetAllAsMovesSetupBoth()
    {
        Node node = new Node();
        GoPoint point1 = GoPoint.get(1, 1);
        GoPoint point2 = GoPoint.get(2, 2);
        node.addStone(GoColor.BLACK, point1);
        node.addStone(GoColor.WHITE, point2);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(2, moves.size());
        assertSame(Move.get(GoColor.BLACK, point1), moves.get(0));
        assertSame(Move.get(GoColor.WHITE, point2), moves.get(1));
    }

    public void testGetAllAsMovesSetupBothAndMove()
    {
        Move move = Move.get(GoColor.BLACK, 1, 1);
        Node node = new Node(move);
        GoPoint point1 = GoPoint.get(1, 1);
        GoPoint point2 = GoPoint.get(2, 2);
        node.addStone(GoColor.BLACK, point1);
        node.addStone(GoColor.WHITE, point2);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(3, moves.size());
        assertSame(Move.get(GoColor.BLACK, point1), moves.get(0));
        assertSame(Move.get(GoColor.WHITE, point2), moves.get(1));
        assertSame(move, moves.get(2));
    }

    public void testGetAllAsMovesSetupBothAndPlayer()
    {
        Node node = new Node();
        GoPoint point1 = GoPoint.get(1, 1);
        GoPoint point2 = GoPoint.get(2, 2);
        node.addStone(GoColor.BLACK, point1);
        node.addStone(GoColor.WHITE, point2);
        node.setPlayer(GoColor.WHITE);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(2, moves.size());
        assertSame(Move.get(GoColor.WHITE, point2), moves.get(0));
        assertSame(Move.get(GoColor.BLACK, point1), moves.get(1));
    }

    public void testGetAllAsMovesSetupWhiteAndPlayer()
    {
        Node node = new Node();
        GoPoint point = GoPoint.get(1, 1);
        node.addStone(GoColor.WHITE, point);
        node.setPlayer(GoColor.WHITE);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(2, moves.size());
        assertSame(Move.get(GoColor.WHITE, point), moves.get(0));
        assertSame(Move.getPass(GoColor.BLACK), moves.get(1));
    }

    public void testGetAllAsMovesSetupWhite()
    {
        Node node = new Node();
        GoPoint point = GoPoint.get(1, 1);
        node.addStone(GoColor.WHITE, point);
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
        assertEquals(1, moves.size());
        assertSame(Move.get(GoColor.WHITE, point), moves.get(0));
    }

    public void testGetBackToMainVariation()
    {
        assertTrue(NodeUtil.getBackToMainVariation(m_node1) == m_node1);
        assertTrue(NodeUtil.getBackToMainVariation(m_node5) == m_node5);
        assertTrue(NodeUtil.getBackToMainVariation(m_node7) == m_node1);
        assertTrue(NodeUtil.getBackToMainVariation(m_node8) == m_node1);
        assertTrue(NodeUtil.getBackToMainVariation(m_node9) == m_node1);
    }

    public void testGetChildrenMoves()
    {
        ConstPointList moves = NodeUtil.getChildrenMoves(m_node7);
        assertEquals(moves.size(), 3);
        assertTrue(moves.get(0) == GoPoint.get(0, 0));
        assertTrue(moves.get(1) == GoPoint.get(0, 0));
        assertTrue(moves.get(2) == GoPoint.get(0, 1));
    }

    public void testGetChildWithMove()
    {
        assertTrue(NodeUtil.getChildWithMove(m_node7,
                                             Move.get(GoColor.BLACK, 0, 0))
                   == m_node8);
        assertNull(NodeUtil.getChildWithMove(m_node7,
                                             Move.get(GoColor.BLACK, 2, 3)));
    }

    public void testGetDepth()
    {
        assertEquals(NodeUtil.getDepth(m_node0), 0);
        assertEquals(NodeUtil.getDepth(m_node2), 2);
        assertEquals(NodeUtil.getDepth(m_node6), 6);
        assertEquals(NodeUtil.getDepth(m_node8), 3);
    }

    public void testGetLast()
    {
        assertTrue(NodeUtil.getLast(m_node1) == m_node6);
        assertTrue(NodeUtil.getLast(m_node7) == m_node8);
        assertTrue(NodeUtil.getLast(m_node10) == m_node10);
    }

    public void testGetMoveNumber()
    {
        assertEquals(NodeUtil.getMoveNumber(m_node0), 0);
        assertEquals(NodeUtil.getMoveNumber(m_node1), 0);
        assertEquals(NodeUtil.getMoveNumber(m_node2), 1);
        assertEquals(NodeUtil.getMoveNumber(m_node3), 2);
        assertEquals(NodeUtil.getMoveNumber(m_node4), 2);
        assertEquals(NodeUtil.getMoveNumber(m_node5), 3);
        assertEquals(NodeUtil.getMoveNumber(m_node6), 3);
    }

    public void testGetMovesLeft()
    {
        assertEquals(NodeUtil.getMovesLeft(m_node0), 3);
        assertEquals(NodeUtil.getMovesLeft(m_node1), 3);
        assertEquals(NodeUtil.getMovesLeft(m_node2), 2);
        assertEquals(NodeUtil.getMovesLeft(m_node3), 1);
        assertEquals(NodeUtil.getMovesLeft(m_node4), 1);
        assertEquals(NodeUtil.getMovesLeft(m_node5), 0);
        assertEquals(NodeUtil.getMovesLeft(m_node6), 0);
        assertEquals(NodeUtil.getMovesLeft(m_node7), 1);
    }

    public void testTruncateChildren()
    {
        Node node = new Node();
        assertEquals(node.getNumberChildren(), 0);
        NodeUtil.truncateChildren(node);
        node.append(new Node());
        node.append(new Node());
        assertEquals(node.getNumberChildren(), 2);
        NodeUtil.truncateChildren(node);
        assertEquals(node.getNumberChildren(), 0);
    }

    /** Create a small test tree.
        <pre>
        n0 - n1 - n2(Bc3) - n3(Wf4) - n4 - n5(Bg4) - n6
                \ n7 - n8(Ba1)
                     \ n9(Ba1)
                     \ n10(Ba2)
                     \ n11(Bpass)
        </pre>
    */
    protected void setUp() throws Exception
    {
        super.setUp();
        m_node0 = new Node();
        m_node1 = new Node();
        m_node0.append(m_node1);
        m_node2 = new Node(Move.get(GoColor.BLACK, 2, 2));
        m_node1.append(m_node2);
        m_node3 = new Node(Move.get(GoColor.WHITE, 5, 3));
        m_node2.append(m_node3);
        m_node4 = new Node();
        m_node3.append(m_node4);
        m_node5 = new Node(Move.get(GoColor.BLACK, 6, 3));
        m_node4.append(m_node5);
        m_node6 = new Node();
        m_node5.append(m_node6);
        m_node7 = new Node();
        m_node1.append(m_node7);
        m_node8 = new Node(Move.get(GoColor.BLACK, 0, 0));
        m_node7.append(m_node8);
        m_node9 = new Node(Move.get(GoColor.BLACK, 0, 0));
        m_node7.append(m_node9);
        m_node10 = new Node(Move.get(GoColor.WHITE, 0, 1));
        m_node7.append(m_node10);
        m_node11 = new Node(Move.getPass(GoColor.BLACK));
        m_node7.append(m_node11);
    }

    private Node m_node0;

    private Node m_node1;

    private Node m_node2;

    private Node m_node3;

    private Node m_node4;

    private Node m_node5;

    private Node m_node6;

    private Node m_node7;

    private Node m_node8;

    private Node m_node9;

    private Node m_node10;

    private Node m_node11;
}

