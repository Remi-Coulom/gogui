// GameTreeTest.java

package net.sf.gogui.game;

public final class GameTreeTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GameTreeTest.class);
    }

    public void testHasVariations()
    {
        GameTree tree = new GameTree();
        Node root = tree.getRoot();
        Node node1 = appendNewNode(root);
        Node node2 = appendNewNode(node1);
        appendNewNode(node2);
        assertFalse(tree.hasVariations());
        appendNewNode(node1);
        assertTrue(tree.hasVariations());
    }

    public void testKeepOnlyMainVariation()
    {
        GameTree tree = new GameTree();
        Node root = tree.getRoot();
        Node node1 = appendNewNode(root);
        Node node2 = appendNewNode(node1);
        appendNewNode(node2);
        appendNewNode(node1);
        tree.keepOnlyMainVariation();
        assertFalse(tree.hasVariations());
    }

    private static Node appendNewNode(Node father)
    {
        Node child = new Node();
        father.append(child);
        return child;
    }
}
