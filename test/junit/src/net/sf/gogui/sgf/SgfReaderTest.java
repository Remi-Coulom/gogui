// SgfReaderTest.java

package net.sf.gogui.sgf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.ConstSgfProperties;
import net.sf.gogui.game.TimeSettings;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Move;
import net.sf.gogui.version.Version;

public final class SgfReaderTest
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

    public void testDuplicateProperty() throws Exception
    {
        readSgfFileString("(;C[foo]C[bar])", false, true);
    }

    public void testFF4Example() throws Exception
    {
        SgfReader reader = getReader("ff4_ex.1.sgf");
        checkFF4Example(reader);
    }

    /** Test parsing of human-readable move encoding as used by SmartGo. */
    public void testHumanReadable() throws Exception
    {
        SgfReader reader = getReader("human-readable.sgf");
        GameTree tree = reader.getTree();
        Node node = tree.getRoot();
        assertNull(node.getMove());
        node = node.getChild();
        assertEquals(Move.get(BLACK, 16, 15), node.getMove());
        node = node.getChild();
        assertEquals(Move.get(WHITE, 3, 3), node.getMove());
        node = node.getChild();
        assertEquals(Move.get(BLACK, 2, 15), node.getMove());
        node = node.getChild();
        assertEquals(Move.get(WHITE, 15, 3), node.getMove());
        node = node.getChild();
        assertEquals(Move.get(BLACK, 16, 5), node.getMove());
        node = node.getChild();
        assertEquals(Move.get(WHITE, null), node.getMove());
        node = node.getChild();
        assertNull(node);
    }

    public void testInvalidMove() throws Exception
    {
        readSgfFile("invalidmove.sgf", true, false);
    }

    /** Test that linebreaks in a text value is handled correctly. */
    public void testLinebreaks() throws Exception
    {
        // Test soft and hard linebreaks (see SGF spec)
        ConstGameTree tree = readSgfFileString("(;C[foo\nbar \\\nfoo])");
        assertEquals("foo\nbar foo", tree.getRootConst().getComment());

        // Test that the linebreaks LF, CR, LFCR, CRLF are handled correctly
        tree = readSgfFileString("(;C[foo\nbar])");
        assertEquals("foo\nbar", tree.getRootConst().getComment());
        tree = readSgfFileString("(;C[foo\rbar])");
        assertEquals("foo\nbar", tree.getRootConst().getComment());
        tree = readSgfFileString("(;C[foo\n\rbar])");
        assertEquals("foo\nbar", tree.getRootConst().getComment());
        tree = readSgfFileString("(;C[foo\r\nbar])");
        assertEquals("foo\nbar", tree.getRootConst().getComment());
        tree = readSgfFileString("(;C[foo\r\n\rbar])");
        assertEquals("foo\n\nbar", tree.getRootConst().getComment());
        tree = readSgfFileString("(;C[foo\r\n\n\rbar])");
        assertEquals("foo\n\nbar", tree.getRootConst().getComment());

        // Test escaped line breaks immediately before end of value
        tree = readSgfFileString("(;XY[foo\\\n])");
        assertEquals("foo", getSgfPropertyValue(tree.getRootConst(), "XY"));
        tree = readSgfFileString("(;XY[foo\\\r])");
        assertEquals("foo", getSgfPropertyValue(tree.getRootConst(), "XY"));
        tree = readSgfFileString("(;XY[foo\\\n\r])");
        assertEquals("foo", getSgfPropertyValue(tree.getRootConst(), "XY"));
        tree = readSgfFileString("(;XY[foo\\\r\n])");
        assertEquals("foo", getSgfPropertyValue(tree.getRootConst(), "XY"));
    }

    public void testRead() throws Exception
    {
        readSgfFile("verbose-property-names.sgf", false, false);
    }

    /** Test that spaces in size property value are ignored.
        I don't think they are allowed by the SGF standard, but there is
        no reason to create an error in this case. */
    public void testSizeWithSpaces() throws Exception
    {
        ConstGameTree tree = readSgfFileString("(;FF[4]SZ[ 13 ])");
        assertEquals(13, tree.getBoardSize());
    }

    public void testSizeAfterPoints() throws Exception
    {
        readSgfFile("size-after-valid-points.sgf", false, false);
        readSgfFile("size-after-invalid-points.sgf", true, false);
    }

    /** Test that OT property in unknown format is preserved if not changed. */
    public void testTimeSettingsPreserveOvertime() throws Exception
    {
        // Unknown OT context should not generate a warning, since there is
        // no format requirement defined in SGF
        ConstGameTree tree =
            readSgfFile("time-settings-unknown-ot.sgf", false, false);
        ConstNode root = tree.getRootConst();
        ConstGameInfo info = root.getGameInfoConst();
        TimeSettings settings = info.getTimeSettings();
        assertNotNull(settings);
        assertFalse(settings.getUseByoyomi());
        assertEquals(1800000L, settings.getPreByoyomi());
        ConstSgfProperties sgf = root.getSgfPropertiesConst();
        assertNotNull(sgf);
        assertEquals("8 xyz 16", sgf.getValue("OT", 0));
    }

    /** Test that OT property in unknown format is replaced if changed. */
    public void testTimeSettingsReplaceOvertime() throws Exception
    {
        SgfReader reader = getReader("time-settings-unknown-ot.sgf");
        GameTree tree = reader.getTree();
        Node root = tree.getRoot();
        TimeSettings settings = new TimeSettings(1800000, 30000, 5);
        root.getGameInfo().setTimeSettings(settings);
        File file = File.createTempFile("gogui", null);
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, tree, "GoGui", Version.get());
        out.close();
        reader = new SgfReader(new FileInputStream(file), file, null, 0);
        tree = reader.getTree();
        root = tree.getRoot();
        file.delete();
        settings = root.getGameInfo().getTimeSettings();
        assertNotNull(settings);
        assertTrue(settings.getUseByoyomi());
        assertEquals(1800000L, settings.getPreByoyomi());
        assertEquals(30000L, settings.getByoyomi());
        assertEquals(5, settings.getByoyomiMoves());
        ConstSgfProperties sgf = root.getSgfPropertiesConst();
        if (sgf != null)
            assertFalse(sgf.hasKey("OT"));
    }

    /** Test FF4 example after writing and reading again.
        This is actually a test for SgfWriter. */
    public void testWriter() throws Exception
    {
        SgfReader reader = getReader("ff4_ex.1.sgf");
        File file = File.createTempFile("gogui", null);
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, reader.getTree(), "GoGui", Version.get());
        out.close();
        reader = new SgfReader(new FileInputStream(file), file, null, 0);
        checkFF4Example(reader);
        file.delete();
    }

    public void checkFF4Example(SgfReader reader) throws Exception
    {
        GameTree tree = reader.getTree();
        assertEquals(tree.getBoardSize(), 19);
        ConstNode root = tree.getRoot();
        assertEquals(NodeUtil.subtreeSize(root), 54);
        assertEquals(root.getNumberChildren(), 5);
        ConstNode node;
        node = root.getChildConst(1);
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 16, 16, 0);
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 0, 0, 9);
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 1, 1, 0);
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 0);
        assertEquals(node.getPlayer(), WHITE);
        node = root.getChildConst(2);
        assertEquals(node.getNumberChildren(), 1);
        checkSetup(node, 35, 37, 0);
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 1);
        assertEquals(node.getMarkedConst(MarkType.MARK).size(), 9);
        assertEquals(node.getMarkedConst(MarkType.CIRCLE).size(), 9);
        assertEquals(node.getMarkedConst(MarkType.SQUARE).size(), 9);
        assertEquals(node.getMarkedConst(MarkType.TRIANGLE).size(), 9);
        assertEquals(node.getMarkedConst(MarkType.SELECT).size(), 9);
        assertEquals(node.getMarkedConst(MarkType.TERRITORY_BLACK).size(), 18);
        assertEquals(node.getMarkedConst(MarkType.TERRITORY_WHITE).size(), 19);
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 1);
        assertEquals(node.getLabelsUnmodifiable().size(), 22);
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
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 0);
        checkSgfProperty(node, "AR", "aa:sc", "sa:ac", "aa:sa", "aa:ac",
                         "cd:cj", "gd:md", "fh:ij", "kj:nh");
        checkSgfProperty(node, "DD", "kq:os", "dq:hs");
        checkSgfProperty(node, "LN", "pj:pd", "nf:ff", "ih:fj", "kh:nj");
        node = root.getChildConst(3);
        assertEquals(node.getNumberChildren(), 6);
        // Note: These test rely on the fact that the line endings in the FF4
        // file were not auto-replaced by some versioning system
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
        node = node.getChildConst();
        assertEquals(node.getNumberChildren(), 4);
    }

    private void checkLabel(ConstNode node, String pointString, String label)
        throws InvalidPointException
    {
        GoPoint point = GoPoint.parsePoint(pointString, 19);
        assertEquals(node.getLabel(point), label);
    }

    private void checkSgfProperty(ConstNode node, String property,
                                  String... value)
        throws InvalidPointException
    {
        ConstSgfProperties sgf = node.getSgfPropertiesConst();
        for (int i = 0; i < value.length; ++i)
            assertEquals(value[i], sgf.getValue(property, i));
    }

    private void checkSetup(ConstNode node, int black, int white, int empty)
        throws InvalidPointException
    {
        assertEquals(node.getSetup(BLACK).size(), black);
        assertEquals(node.getSetup(WHITE).size(), white);
        assertEquals(node.getSetup(EMPTY).size(), empty);
        assertNull(node.getMove());
    }

    private SgfReader getReader(String name) throws SgfError, Exception
    {
        InputStream in = getClass().getResourceAsStream(name);
        if (in == null)
            throw new Exception("Resource " + name + " not found");
        return new SgfReader(in, null, null, 0);
    }

    private SgfReader getReaderString(String text) throws SgfError, Exception
    {
        InputStream in = new ByteArrayInputStream(text.getBytes());
        return new SgfReader(in, null, null, 0);
    }

    private static String getSgfPropertyValue(ConstNode node, String key)
    {
        return node.getSgfPropertiesConst().getValue(key, 0);
    }

    private ConstGameTree readSgfFile(String name, boolean expectFailure,
                                      boolean expectWarnings) throws Exception
    {
        SgfReader reader;
        try
        {
            reader = getReader(name);
            readSgfFile(reader, expectFailure, expectWarnings);
        }
        catch (SgfError error)
        {
            if (! expectFailure)
                fail(error.getMessage());
            return null;
        }
        if (expectFailure)
            fail("Reading should result in a failure");
        return reader.getTree();
    }

    private void readSgfFile(SgfReader reader, boolean expectFailure,
                             boolean expectWarnings) throws Exception
    {
        if (expectWarnings && reader.getWarnings() == null)
            fail("Reading should result in warnings");
        if (! expectWarnings && reader.getWarnings() != null)
        {
            fail("Reading should result in no warnings:\n"
                 + reader.getWarnings());
        }
    }

    private ConstGameTree readSgfFileString(String name, boolean expectFailure,
                                            boolean expectWarnings)
        throws Exception
    {
        SgfReader reader;
        try
        {
            reader = getReaderString(name);
            readSgfFile(reader, expectFailure, expectWarnings);
        }
        catch (SgfError error)
        {
            if (! expectFailure)
                fail(error.getMessage());
            return null;
        }
        if (expectFailure)
            fail("Reading should result in a failure");
        return reader.getTree();
    }

    private ConstGameTree readSgfFileString(String name) throws Exception
    {
        return readSgfFileString(name, false, false);
    }
}
