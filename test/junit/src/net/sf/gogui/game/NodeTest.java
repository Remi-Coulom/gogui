//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
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

    public void testMakeMainVariation()
    {
        Node father = new Node();
        Node child1 = new Node();
        Node child2 = new Node();
        father.append(child1);
        father.append(child2);
        father.makeMainVariation(child2);
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
        assertEquals(0, node.getAddStones(GoColor.BLACK).size());
        assertEquals(0, node.getAddStones(GoColor.EMPTY).size());
        assertEquals(0, node.getAddStones(GoColor.WHITE).size());
        GoPoint point = GoPoint.get(0, 0);
        node.addStone(GoColor.BLACK, point);
        assertEquals(1, node.getAddStones(GoColor.BLACK).size());
        assertEquals(point, node.getAddStones(GoColor.BLACK).get(0));
        assertEquals(0, node.getAddStones(GoColor.EMPTY).size());
        assertEquals(0, node.getAddStones(GoColor.WHITE).size());
        node = new Node();
        node.addStone(GoColor.WHITE, point);
        assertEquals(1, node.getAddStones(GoColor.WHITE).size());
        assertEquals(point, node.getAddStones(GoColor.WHITE).get(0));
        assertEquals(0, node.getAddStones(GoColor.EMPTY).size());
        assertEquals(0, node.getAddStones(GoColor.BLACK).size());
        node = new Node();
        node.addStone(GoColor.EMPTY, point);
        assertEquals(1, node.getAddStones(GoColor.EMPTY).size());
        assertEquals(point, node.getAddStones(GoColor.EMPTY).get(0));
        assertEquals(0, node.getAddStones(GoColor.BLACK).size());
        assertEquals(0, node.getAddStones(GoColor.WHITE).size());
        GoPoint point2 = GoPoint.get(1, 0);
        node.addStone(GoColor.EMPTY, point2);
        assertEquals(2, node.getAddStones(GoColor.EMPTY).size());
        assertEquals(point, node.getAddStones(GoColor.EMPTY).get(0));
        assertEquals(point2, node.getAddStones(GoColor.EMPTY).get(1));
        assertEquals(0, node.getAddStones(GoColor.BLACK).size());
        assertEquals(0, node.getAddStones(GoColor.WHITE).size());
    }
}
