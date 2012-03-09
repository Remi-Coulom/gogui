// NodeUtilTest.java

package net.sf.gogui.game;

import java.util.regex.Pattern;
import net.sf.gogui.go.ConstPointList;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
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

    public void testBackward()
    {
        assertTrue(NodeUtil.backward(m_node10, 2) == m_node1);
        assertTrue(NodeUtil.backward(m_node3, 10) == m_node0);
    }

    public void testCommentContains()
    {
        Node node = new Node();
        Pattern pattern = Pattern.compile("foo.*bar");
        assertFalse(NodeUtil.commentContains(node, pattern));
        node.setComment("fooxbar");
        assertTrue(NodeUtil.commentContains(node, pattern));
        node.setComment("bar\nfooxbar");
        assertTrue(NodeUtil.commentContains(node, pattern));
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
        assertNull(NodeUtil.findByVariation(m_node0, "foobar"));
        assertNull(NodeUtil.findByVariation(m_node0, "123.4"));
        assertNull(NodeUtil.findByVariation(m_node0, "0"));
    }

    public void testGetBackToMainVariation()
    {
        assertTrue(NodeUtil.getBackToMainVariation(m_node1) == m_node1);
        assertTrue(NodeUtil.getBackToMainVariation(m_node5) == m_node5);
        assertTrue(NodeUtil.getBackToMainVariation(m_node7) == m_node2);
        assertTrue(NodeUtil.getBackToMainVariation(m_node8) == m_node2);
        assertTrue(NodeUtil.getBackToMainVariation(m_node9) == m_node2);
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
                                             Move.get(BLACK, 0, 0))
                   == m_node8);
        assertNull(NodeUtil.getChildWithMove(m_node7,
                                             Move.get(BLACK, 2, 3)));
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

    public void testSubtreeSize()
    {
        assertEquals(12, NodeUtil.subtreeSize(m_node0));
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
        n0 - n1 - n2(B C3) - n3(W F4) - n4 - n5(B G4) - n6
                \ n7 - n8(B A1)
                     \ n9(B A1)
                     \ n10(B A2)
                     \ n11(B PASS)
        </pre> */
    protected void setUp() throws Exception
    {
        super.setUp();
        m_node0 = new Node();
        m_node1 = new Node();
        m_node0.append(m_node1);
        m_node2 = new Node(Move.get(BLACK, 2, 2));
        m_node1.append(m_node2);
        m_node3 = new Node(Move.get(WHITE, 5, 3));
        m_node2.append(m_node3);
        m_node4 = new Node();
        m_node3.append(m_node4);
        m_node5 = new Node(Move.get(BLACK, 6, 3));
        m_node4.append(m_node5);
        m_node6 = new Node();
        m_node5.append(m_node6);
        m_node7 = new Node();
        m_node1.append(m_node7);
        m_node8 = new Node(Move.get(BLACK, 0, 0));
        m_node7.append(m_node8);
        m_node9 = new Node(Move.get(BLACK, 0, 0));
        m_node7.append(m_node9);
        m_node10 = new Node(Move.get(WHITE, 0, 1));
        m_node7.append(m_node10);
        m_node11 = new Node(Move.getPass(BLACK));
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
