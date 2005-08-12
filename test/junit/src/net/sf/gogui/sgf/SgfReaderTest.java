//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class SgfReaderTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(SgfReaderTest.class);
    }

    public void testRead() throws Exception
    {
        readSgfFile("verbose-property-names.sgf", false, true);
        checkTimeSettings("time-settings-1.sgf", 1800000, 60000, 5);
    }

    public void testFF4Example() throws Exception
    {
        SgfReader reader = getReader("ff4_ex.sgf");
        checkFF4Example(reader);
    }

    /** Test FF4 example after writing and reading again.
        This is actually a test for SgfWriter.
    */
    public void testWriter() throws Exception
    {
        SgfReader reader = getReader("ff4_ex.sgf");
        File file = File.createTempFile("gogui", null);
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, reader.getGameTree(), file, "GoGui",
                      Version.get());
        out.close();
        reader = new SgfReader(new FileInputStream(file), null, null, 0);
        checkFF4Example(reader);
        file.delete();
    }

    private SgfReader getReader(String name)
        throws SgfReader.SgfError, Exception
    {
        InputStream in = getClass().getResourceAsStream(name);
        if (in == null)
            throw new Exception("Resource " + name + " not found");
        return new SgfReader(in, null, null, 0);
    }    

    public void checkFF4Example(SgfReader reader) throws Exception
    {
        GameTree gameTree = reader.getGameTree();
        GameInformation info = gameTree.getGameInformation();
        assertEquals(info.m_boardSize, 19);
        Node root = gameTree.getRoot();
        assertEquals(NodeUtils.subtreeSize(root), 54);
        assertEquals(root.getNumberChildren(), 5);
        Node node;
        node = root.getChild(1);
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 16, 16, 0);
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 0, 0, 9);
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 1, 1, 0);
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 0);
        assertEquals(node.getPlayer(), GoColor.WHITE);
        node = root.getChild(2);
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 35, 37, 0);
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 1);
        assertEquals(node.getMarked(Node.MARKED).size(), 9);
        assertEquals(node.getMarked(Node.MARKED_CIRCLE).size(), 9);
        assertEquals(node.getMarked(Node.MARKED_SQUARE).size(), 9);
        assertEquals(node.getMarked(Node.MARKED_TRIANGLE).size(), 9);
        assertEquals(node.getMarked(Node.MARKED_SELECT).size(), 9);
        assertEquals(node.getMarked(Node.MARKED_TERRITORY_BLACK).size(), 18);
        assertEquals(node.getMarked(Node.MARKED_TERRITORY_WHITE).size(), 19);
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 1);
        assertEquals(node.getLabels().size(), 22);
        checkLabel(node, "D17", "1");
        checkLabel(node, "F17", "2");
        checkLabel(node, "O17", "3");
        checkLabel(node, "Q17", "4");
        checkLabel(node, "D10", "a");
        checkLabel(node, "F10", "b");
        checkLabel(node, "O10", "c");
        checkLabel(node, "Q10", "d");
        checkLabel(node, "G7", "AB");
        checkLabel(node, "G6", "ABC");
        checkLabel(node, "G5", "ABCD");
        checkLabel(node, "G4", "ABCDE");
        checkLabel(node, "G3", "ABCDEF");
        checkLabel(node, "G2", "ABCDEFG");
        checkLabel(node, "G1", "ABCDEFGH");
        checkLabel(node, "N7", "12");
        checkLabel(node, "N6", "123");
        checkLabel(node, "N5", "1234");
        checkLabel(node, "N4", "12345");
        checkLabel(node, "N3", "123456");
        checkLabel(node, "N2", "1234567");
        checkLabel(node, "N1", "12345678");
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 0);
        checkSgfProperty(node, "AR",
                         "[aa:sc][sa:ac][aa:sa][aa:ac][cd:cj][gd:md][fh:ij]"
                         + "[kj:nh]");
        checkSgfProperty(node, "DD", "[kq:os][dq:hs]");
        checkSgfProperty(node, "LN", "[pj:pd][nf:ff][ih:fj][kh:nj]");
        node = root.getChild(3);
        assertEquals(node.getNumberChildren(), 6);
        assertEquals(node.getComment(),
                     "There are hard linebreaks & soft linebreaks.\n" +
                     "Soft linebreaks are linebreaks preceeded by '\\' like"
                     + " this one >ok<. Hard line breaks are all other"
                     + " linebreaks.\n" +
                     "Soft linebreaks are converted to >nothing<, i.e."
                     + " removed.\n" +
                     "\n" +
                     "Note that linebreaks are coded differently on different"
                     + " systems.\n" +
                     "\n" +
                     "Examples (>ok< shouldn't be split):\n" +
                     "\n" +
                     "linebreak 1 \"\\n\": >ok<\n" +
                     "linebreak 2 \"\\n\\r\": >ok<\n" +
                     "linebreak 3 \"\\r\\n\": >ok<\n" +
                     "linebreak 4 \"\\r\": >ok<");
        node = node.getChild();
        assertEquals(node.getNumberChildren(), 4);
    }

    private void checkLabel(Node node, String pointString, String label)
        throws GoPoint.InvalidPoint
    {
        GoPoint point = GoPoint.parsePoint(pointString, 19);
        assertEquals(node.getLabel(point), label);
    }

    private void checkSgfProperty(Node node, String property, String value)
        throws GoPoint.InvalidPoint
    {
        assertEquals(value, node.getSgfProperties().get(property));
    }

    private void checkSetup(Node node, int black, int white, int empty)
        throws GoPoint.InvalidPoint
    {
        assertEquals(node.getNumberAddBlack(), black);
        assertEquals(node.getNumberAddWhite(), white);
        assertEquals(node.getNumberAddEmpty(), empty);
        assertNull(node.getMove());
    }

    private void checkTimeSettings(String name, long preByoyomi, long byoyomi,
                                   int byoyomiMoves) throws Exception
    {
        SgfReader reader = getReader(name);
        GameTree gameTree = reader.getGameTree();
        GameInformation gameInformation = gameTree.getGameInformation();
        TimeSettings timeSettings = gameInformation.m_timeSettings;
        assertNotNull(timeSettings);
        assertEquals(timeSettings.getPreByoyomi(), preByoyomi);
        assertEquals(timeSettings.getByoyomi(), byoyomi);
        assertEquals(timeSettings.getByoyomiMoves(), byoyomiMoves);
    }

    private void readSgfFile(String name, boolean expectFailure,
                             boolean expectWarnings) throws Exception
    {
        try
        {
            SgfReader reader = getReader(name);
            if (expectWarnings && reader.getWarnings() == null)
                fail("Reading " + name + " should result in warnings");
            if (! expectWarnings && reader.getWarnings() != null)
                fail("Reading " + name + " should result in no warnings");
        }
        catch (SgfReader.SgfError error)
        {
            if (! expectFailure)
                fail(error.getMessage());
            return;
        }
        if (expectFailure)
            fail("Reading " + name + " should result in a failure");
    }
}

//----------------------------------------------------------------------------
