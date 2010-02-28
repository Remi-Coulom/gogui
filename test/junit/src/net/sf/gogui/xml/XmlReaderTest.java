// XmlReaderTest.java

package net.sf.gogui.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.TimeSettings;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.Move;
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
        how GoGui promises to handle paragraph content. */
    public void testComment() throws Exception
    {
        ConstGameTree tree =
            getTree("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Go><GoGame><Nodes><Node>" +
                    "<Comment>" +
                    "<P/>" +
                    "<P>abc</P>" +
                    "<P>   </P>" +
                    "<P>abc</P>" +
                    "</Comment>" +
                    "</Node></Nodes></GoGame></Go>");
        assertEquals("\nabc\n   \nabc", tree.getRootConst().getComment());
    }

    /** Test that overtime information is read from SGF element. */
    public void testOvertime() throws Exception
    {
        ConstGameTree tree =
            getTree("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Go><GoGame>" +
                    "<Information><Time>18000</Time></Information>" +
                    "<Nodes><Node>" +
                    "<SGF type=\"OT\"><Arg>1 moves / 1 min</Arg></SGF>" +
                    "</Node></Nodes></GoGame></Go>");
        ConstGameInfo info = tree.getRootConst().getGameInfoConst();
        TimeSettings timeSettings = info.getTimeSettings();
        assertEquals(18000000L, timeSettings.getPreByoyomi());
        assertEquals(60000L, timeSettings.getByoyomi());
        assertEquals(1, timeSettings.getByoyomiMoves());
    }

    /** Test that the implicit root node is pruned if it is empty and
        the GoGame has a name attribute.
        This bug occured, because the reader stored a game name in a legacy
        SGF property GN, which caused the implicit root node no longer to be
        empty. */
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

    public void testTime() throws Exception
    {
        ConstGameTree tree =
            getTree("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Go><GoGame><Information><Time>3600</Time>" +
                    "</Information></GoGame></Go>");
        ConstGameInfo info = tree.getRootConst().getGameInfoConst();
        TimeSettings timeSettings = info.getTimeSettings();
        assertEquals(3600000L, timeSettings.getPreByoyomi());
    }

    public void testVariation() throws Exception
    {
        ConstGameTree tree =
            getTree("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Go><GoGame><Information/><Nodes><Node/>" +
                    "<Node><Black at=\"Q16\"/></Node>" +
                    "<Variation><Black at=\"Q15\"/></Variation>" +
                    "<Node><White at=\"D4\"/></Node>" +
                    "</Nodes></GoGame></Go>");
        ConstNode root = tree.getRootConst();
        assertEquals(2, root.getNumberChildren());
        ConstNode node1 = root.getChildConst(0);
        assertEquals(1, node1.getNumberChildren());
        assertEquals(Move.get(BLACK, 15, 15), node1.getMove());
        ConstNode node2 = root.getChildConst(1);
        assertFalse(node2.hasChildren());
        assertEquals(Move.get(BLACK, 15, 14), node2.getMove());
        ConstNode node3 = node1.getChildConst();
        assertFalse(node3.hasChildren());
        assertEquals(Move.get(WHITE, 3, 3), node3.getMove());
    }

    private static XmlReader getReader(String text) throws ErrorMessage
    {
        InputStream in = new ByteArrayInputStream(text.getBytes());
        return new XmlReader(in, null, 0);
    }

    private static ConstGameTree getTree(String text) throws ErrorMessage
    {
        XmlReader reader = getReader(text);
        assertNull(reader.getWarnings());
        return reader.getTree();
    }
}
