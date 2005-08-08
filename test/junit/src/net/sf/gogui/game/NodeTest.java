//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

//----------------------------------------------------------------------------

public class NodeTest
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
}

//----------------------------------------------------------------------------
