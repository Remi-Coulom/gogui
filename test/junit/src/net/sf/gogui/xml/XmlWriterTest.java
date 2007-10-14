//----------------------------------------------------------------------------
// XmlWriterTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import static net.sf.gogui.go.GoColor.BLACK;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.ErrorMessage;

public final class XmlWriterTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(XmlWriterTest.class);
    }

    /** Test that a node containing only a move is not embedded in an
        unnecessary Node element.
    */
    public void testMoveWithoutNode() throws Exception
    {
        Node root = new Node();
        root.append(new Node(Move.get(BLACK, null)));
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node/>\n" +
                     "<Black number=\"1\" at=\"\"/>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    private static String getText(int boardSize, Node root)
    {
        GameTree tree = new GameTree(boardSize, root);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(out, tree, null);
        return out.toString();
    }
}
