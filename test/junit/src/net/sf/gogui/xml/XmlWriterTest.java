// XmlWriterTest.java

package net.sf.gogui.xml;

import java.io.ByteArrayOutputStream;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.StringInfoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;

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

    /** Test linebreak-to-paragraph conversion in comments.
        See appendix "XML Format" in the GoGui documentation.
        This should allow loss-less conversion from SGF to XML and back. */
    public void testCommentParagraphs() throws Exception
    {
        Node root = new Node();
        root.setComment("line1\nline2");
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node>\n" +
                     "<Comment>\n" +
                     "<P>line1</P>\n" +
                     "<P>line2</P>\n" +
                     "</Comment>\n" +
                     "</Node>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));

        root.setComment("line1\nline2\n");
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node>\n" +
                     "<Comment>\n" +
                     "<P>line1</P>\n" +
                     "<P>line2</P>\n" +
                     "<P/>\n" +
                     "</Comment>\n" +
                     "</Node>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    /** Test that a root node that is empty apart from game info and should
        be written, because the child has a move is written using a
        self-closing tag. */
    public void testEmptyRootWithGameInfo() throws Exception
    {
        Node root = new Node();
        root.append(new Node(Move.get(BLACK, null)));
        root.createGameInfo().setKomi(new Komi(5.5));
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "<Komi>5.5</Komi>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node/>\n" +
                     "<Black number=\"1\" at=\"\"/>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    /** Test that special charcaters are escaped in BlackRank. */
    public void testEscapeBlackRank() throws Exception
    {
        Node root = new Node();
        root.createGameInfo().set(StringInfoColor.RANK, BLACK,
                                  "9d, Tengen & Oza");
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "<BlackRank>9d, Tengen &amp; Oza</BlackRank>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node/>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    /** Test that invalid XML characters in comment are not written.
        This happened when an SGF file was loaded that had the control
        character Unicode 0x13 in its comment, and then saved again as XML.
        Not all UTF-8 characters are valid in XML, see
        http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char. */
    public void testInvalidXml() throws Exception
    {
        Node root = new Node();
        root.setComment("foo\u0013bar");
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node>\n" +
                     "<Comment>\n" +
                     "<P>foobar</P>\n" +
                     "</Comment>\n" +
                     "</Node>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    /** Test that a node containing only a move is not embedded in an
        unnecessary Node element. */
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

    /** Test that root node having only a player set is written correctly.
        The player should be written using the legacy SGF PL-property
        (BlackToPlay, WhiteToPlay are not usable, because they don't have a
        legal parent according to go.dtd 2007) and the this SGF property is
        embedded in a Node element. */
    public void testSetupPlayer() throws Exception
    {
        Node root = new Node();
        root.setPlayer(WHITE);
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node>\n" +
                     "<SGF type=\"PL\"><Arg>W</Arg></SGF>\n" +
                     "</Node>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    /** Test that time left information in node without move is written.
        Since go.dtd 2007 allows timeleft attributes only for the elements
        Black and White, the timeleft information should be written using
        legacy SGF BL/TL properties. */
    public void testTimeLeftWithoutMove() throws Exception
    {
        Node root = new Node();
        root.setTimeLeft(BLACK, 2.0);
        root.setTimeLeft(WHITE, 2.0);
        assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                     "<Go>\n" +
                     "<GoGame>\n" +
                     "<Information>\n" +
                     "<BoardSize>19</BoardSize>\n" +
                     "</Information>\n" +
                     "<Nodes>\n" +
                     "<Node>\n" +
                     "<SGF type=\"BL\"><Arg>2.0</Arg></SGF>\n" +
                     "<SGF type=\"WL\"><Arg>2.0</Arg></SGF>\n" +
                     "</Node>\n" +
                     "</Nodes>\n" +
                     "</GoGame>\n" +
                     "</Go>\n",
                     getText(19, root));
    }

    private static String getText(int boardSize, Node root)
    {
        GameTree tree = new GameTree(boardSize, root);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XmlWriter(out, tree, null);
        return out.toString();
    }
}
