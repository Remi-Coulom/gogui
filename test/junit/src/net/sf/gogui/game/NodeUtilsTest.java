//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

//----------------------------------------------------------------------------

public class NodeUtilsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(NodeUtilsTest.class);
    }

    public void testFindByMoveNumber()
    {
        assertTrue(NodeUtils.findByMoveNumber(m_node0, 1) == m_node2);
        assertTrue(NodeUtils.findByMoveNumber(m_node0, 2) == m_node3);
        assertTrue(NodeUtils.findByMoveNumber(m_node4, 2) == m_node3);
        assertTrue(NodeUtils.findByMoveNumber(m_node0, 3) == m_node5);
        assertNull(NodeUtils.findByMoveNumber(m_node0, 4));
        assertTrue(NodeUtils.findByMoveNumber(m_node0, 0) == m_node0);
        assertNull(NodeUtils.findByMoveNumber(m_node0, -1));
        assertTrue(NodeUtils.findByMoveNumber(m_node7, 1) == m_node8);
    }

    public void testFindByVariation()
    {
        assertTrue(NodeUtils.findByVariation(m_node0, "") == m_node0);
        assertTrue(NodeUtils.findByVariation(m_node0, "1") == m_node2);
        assertTrue(NodeUtils.findByVariation(m_node0, "2") == m_node7);
        assertNull(NodeUtils.findByVariation(m_node0, "3"));
        assertTrue(NodeUtils.findByVariation(m_node0, "2.1") == m_node8);
        assertTrue(NodeUtils.findByVariation(m_node0, "2.2") == m_node9);
    }

    public void testGetBackToMainVariation()
    {
        assertTrue(NodeUtils.getBackToMainVariation(m_node1) == m_node1);
        assertTrue(NodeUtils.getBackToMainVariation(m_node5) == m_node5);
        assertTrue(NodeUtils.getBackToMainVariation(m_node7) == m_node1);
        assertTrue(NodeUtils.getBackToMainVariation(m_node8) == m_node1);
        assertTrue(NodeUtils.getBackToMainVariation(m_node9) == m_node1);
    }

    public void testGetChildrenMoves()
    {
        ArrayList moves = NodeUtils.getChildrenMoves(m_node7);
        assertEquals(moves.size(), 3);
        assertTrue(moves.get(0) == GoPoint.create(0, 0));
        assertTrue(moves.get(1) == GoPoint.create(0, 0));
        assertTrue(moves.get(2) == GoPoint.create(0, 1));
    }

    public void testGetChildWithMove()
    {
        assertTrue(NodeUtils.getChildWithMove(m_node7,
                                              createMove(0, 0, GoColor.BLACK))
                   == m_node8);
        assertNull(NodeUtils.getChildWithMove(m_node7,
                                              createMove(2, 3,
                                                         GoColor.BLACK)));
    }

    public void testGetDepth()
    {
        assertEquals(NodeUtils.getDepth(m_node0), 0);
        assertEquals(NodeUtils.getDepth(m_node2), 2);
        assertEquals(NodeUtils.getDepth(m_node6), 6);
        assertEquals(NodeUtils.getDepth(m_node8), 3);
    }

    public void testGetLast()
    {
        assertTrue(NodeUtils.getLast(m_node1) == m_node6);
        assertTrue(NodeUtils.getLast(m_node7) == m_node8);
        assertTrue(NodeUtils.getLast(m_node10) == m_node10);
    }

    public void testGetMoveNumber()
    {
        assertEquals(NodeUtils.getMoveNumber(m_node0), 0);
        assertEquals(NodeUtils.getMoveNumber(m_node1), 0);
        assertEquals(NodeUtils.getMoveNumber(m_node2), 1);
        assertEquals(NodeUtils.getMoveNumber(m_node3), 2);
        assertEquals(NodeUtils.getMoveNumber(m_node4), 2);
        assertEquals(NodeUtils.getMoveNumber(m_node5), 3);
        assertEquals(NodeUtils.getMoveNumber(m_node6), 3);
    }

    public void testGetMovesLeft()
    {
        assertEquals(NodeUtils.getMovesLeft(m_node0), 3);
        assertEquals(NodeUtils.getMovesLeft(m_node1), 3);
        assertEquals(NodeUtils.getMovesLeft(m_node2), 2);
        assertEquals(NodeUtils.getMovesLeft(m_node3), 1);
        assertEquals(NodeUtils.getMovesLeft(m_node4), 1);
        assertEquals(NodeUtils.getMovesLeft(m_node5), 0);
        assertEquals(NodeUtils.getMovesLeft(m_node6), 0);
        assertEquals(NodeUtils.getMovesLeft(m_node7), 1);
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
        m_node2 = new Node(createMove(2, 2, GoColor.BLACK));
        m_node1.append(m_node2);
        m_node3 = new Node(createMove(5, 3, GoColor.WHITE));
        m_node2.append(m_node3);
        m_node4 = new Node();
        m_node3.append(m_node4);
        m_node5 = new Node(createMove(6, 3, GoColor.BLACK));
        m_node4.append(m_node5);
        m_node6 = new Node();
        m_node5.append(m_node6);
        m_node7 = new Node();
        m_node1.append(m_node7);
        m_node8 = new Node(createMove(0, 0, GoColor.BLACK));
        m_node7.append(m_node8);
        m_node9 = new Node(createMove(0, 0, GoColor.BLACK));
        m_node7.append(m_node9);
        m_node10 = new Node(createMove(0, 1, GoColor.WHITE));
        m_node7.append(m_node10);
        m_node11 = new Node(Move.create(null, GoColor.BLACK));
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

    private static Move createMove(int x, int y, GoColor color)
    {
        return Move.create(GoPoint.create(x, y), color);
    }
}

//----------------------------------------------------------------------------
