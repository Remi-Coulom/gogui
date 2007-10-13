//----------------------------------------------------------------------------
// XmlReaderTest.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.sf.gogui.game.ConstGameTree;
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
            getTree("<?xml version='1.0' encoding='utf-8'?>" +
                    "<Go><GoGame><Nodes><Node>" +
                    "<Comment>" +
                    "<P>abc</P>" +
                    "<P>   </P>" +
                    "<P>abc</P>" +
                    "</Comment>" +
                    "</Node></Nodes></GoGame></Go>");
        assertEquals("abc\n   \nabc", tree.getRootConst().getComment());
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
