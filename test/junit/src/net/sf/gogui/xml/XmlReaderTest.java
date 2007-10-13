//----------------------------------------------------------------------------
// XmlReaderTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.util.ErrorMessage;

public final class XmlReaderTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(XmlReaderTest.class);
    }

    /** Test that paragraphs containing only empty characters are preserved.
        See the appendix "XML Format" of the GoGui documentation that this is
        how GoGui promises to handle paragraph content.
    */
    public void testComment() throws Exception
    {
        ConstGameTree tree =
            getTree("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Go><GoGame><Nodes><Node>" +
                    "<Comment>" +
                    "<P>abc</P>" +
                    "<P>   </P>" +
                    "<P>abc</P>" +
                    "</Comment>" +
                    "</Node></Nodes></GoGame></Go>");
        assertEquals("abc\n   \nabc", tree.getRootConst().getComment());
    }

    /** Test that the implicit root node is pruned if it is empty and
        the GoGame has a name attribute.
        This bug occured, because the reader stored a game name in a legacy
        SGF property GN, which caused the implicit root node no longer to be
        empty.
    */
    public void testPruneEmptyRootIfGameName() throws Exception
    {
        // Here, comment needs to hava a Node parent according to the DTD,
        // but since the first node has no move content and the implicit
        // root node is empty, the node with the comment should be the root
        ConstGameTree tree =
            getTree("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Go><GoGame name=\"gameName\"><Nodes>" +
                    "<Node><Comment><P>abc</P></Comment></Node>" +
                    "</Nodes></GoGame></Go>");
        ConstNode root = tree.getRootConst();
        assertFalse(root.hasChildren());
        assertEquals("abc", root.getComment());
    }

    private static XmlReader getReader(String text) throws ErrorMessage
    {
        InputStream in = new ByteArrayInputStream(text.getBytes());
        return new XmlReader(in);
    }

    private static ConstGameTree getTree(String text) throws ErrorMessage
    {
        XmlReader reader = getReader(text);
        assertNull(reader.getWarnings());
        return reader.getTree();
    }
}
