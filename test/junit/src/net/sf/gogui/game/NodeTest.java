// NodeTest.java

package net.sf.gogui.game;

import java.util.Iterator;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;

public final class NodeTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(NodeTest.class);
    }

    public void testChildren()
    {
        Node root = new Node();
        Node node = new Node();
        Node child1 = new Node();
        Node child2 = new Node();
        root.append(node);
        node.append(child1);
        node.append(child2);
        assertEquals(root.getNumberChildren(), 1);
        assertEquals(node.getNumberChildren(), 2);
        assertEquals(child1.getNumberChildren(), 0);
        assertEquals(child2.getNumberChildren(), 0);
        assertTrue(root.getChild() == node);
        assertTrue(node.getChild() == child1);
        assertNull(child1.getChild());
        assertNull(child2.getChild());
        assertEquals(root.getChildIndex(node), 0);
        assertEquals(node.getChildIndex(child1), 0);
        assertEquals(node.getChildIndex(child2), 1);
        assertNull(root.getFather());
        assertTrue(node.getFather() == root);
        assertTrue(child1.getFather() == node);
        assertTrue(child2.getFather() == node);
    }

    public void testIsChildOf()
    {
        Node father = new Node();
        Node child1 = new Node();
        Node child2 = new Node();
        assertFalse(child1.isChildOf(father));
        father.append(child1);
        assertTrue(child1.isChildOf(father));
        father.append(child2);
        assertTrue(child2.isChildOf(father));
        assertTrue(child2.isChildOf(father));
    }

    public void testMakeFirstChild()
    {
        Node father = new Node();
        Node child1 = new Node();
        Node child2 = new Node();
        father.append(child1);
        father.append(child2);
        father.makeFirstChild(child2);
        assertEquals(child2, father.getChild(0));
        assertEquals(child1, father.getChild(1));
    }

    public void testRemoveChild()
    {
        Node father = new Node();
        Node child1 = new Node();
        Node child2 = new Node();
        Node child3 = new Node();
        father.append(child1);
        father.removeChild(child1);
        assertNull(child1.getFather());
        assertEquals(0, father.getNumberChildren());
        father.append(child1);
        father.append(child2);
        father.append(child3);
        father.removeChild(child1);
        assertEquals(2, father.getNumberChildren());
        assertEquals(child2, father.getChild(0));
        assertEquals(child3, father.getChild(1));
        father.removeChild(child2);
        assertNull(child2.getFather());
        assertEquals(1, father.getNumberChildren());
        assertEquals(child3, father.getChild(0));
        father.removeChild(child3);
        assertNull(child3.getFather());
        assertEquals(0, father.getNumberChildren());
    }

    public void testSetup()
    {
        Node node = new Node();
        assertEquals(0, node.getSetup(BLACK).size());
        assertEquals(0, node.getSetup(EMPTY).size());
        assertEquals(0, node.getSetup(WHITE).size());
        GoPoint point = GoPoint.get(0, 0);
        node.addStone(BLACK, point);
        assertEquals(1, node.getSetup(BLACK).size());
        assertEquals(point, node.getSetup(BLACK).get(0));
        assertEquals(0, node.getSetup(EMPTY).size());
        assertEquals(0, node.getSetup(WHITE).size());
        node = new Node();
        node.addStone(WHITE, point);
        assertEquals(1, node.getSetup(WHITE).size());
        assertEquals(point, node.getSetup(WHITE).get(0));
        assertEquals(0, node.getSetup(EMPTY).size());
        assertEquals(0, node.getSetup(BLACK).size());
        node = new Node();
        node.addStone(EMPTY, point);
        assertEquals(1, node.getSetup(EMPTY).size());
        assertEquals(point, node.getSetup(EMPTY).get(0));
        assertEquals(0, node.getSetup(BLACK).size());
        assertEquals(0, node.getSetup(WHITE).size());
        GoPoint point2 = GoPoint.get(1, 0);
        node.addStone(EMPTY, point2);
        assertEquals(2, node.getSetup(EMPTY).size());
        assertEquals(point, node.getSetup(EMPTY).get(0));
        assertEquals(point2, node.getSetup(EMPTY).get(1));
        assertEquals(0, node.getSetup(BLACK).size());
        assertEquals(0, node.getSetup(WHITE).size());
    }

    public void testSortSetup()
    {
        Node node = new Node();
        GoPoint p1 = GoPoint.get(1, 1);
        GoPoint p2 = GoPoint.get(2, 2);
        GoPoint p3 = GoPoint.get(3, 3);
        GoPoint p4 = GoPoint.get(4, 4);
        GoPoint p5 = GoPoint.get(5, 5);
        GoPoint p6 = GoPoint.get(6, 6);
        GoPoint p7 = GoPoint.get(7, 7);
        GoPoint p8 = GoPoint.get(8, 8);
        GoPoint p9 = GoPoint.get(9, 9);
        node.addStone(BLACK, p2);
        node.addStone(BLACK, p1);
        node.addStone(BLACK, p3);
        node.addStone(WHITE, p6);
        node.addStone(WHITE, p5);
        node.addStone(WHITE, p4);
        node.addStone(EMPTY, p7);
        node.addStone(EMPTY, p9);
        node.addStone(EMPTY, p8);
        node.sortSetup();
        Iterator<GoPoint> it;
        it = node.getSetup(BLACK).iterator();
        assertEquals(p1, it.next());
        assertEquals(p2, it.next());
        assertEquals(p3, it.next());
        it = node.getSetup(WHITE).iterator();
        assertEquals(p4, it.next());
        assertEquals(p5, it.next());
        assertEquals(p6, it.next());
        it = node.getSetup(EMPTY).iterator();
        assertEquals(p7, it.next());
        assertEquals(p8, it.next());
        assertEquals(p9, it.next());
    }
}
