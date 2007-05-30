//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.TimeSettings;

public final class SgfWriterTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(SgfWriterTest.class);
    }

    public void testWriteTimeSettings() throws Exception
    {
        GameTree tree = new GameTree();
        TimeSettings settings = new TimeSettings(3600000, 60000, 10);
        tree.getRoot().getGameInformation().setTimeSettings(settings);
        String s = writeToString(tree);
        assertTrue(s.indexOf("TM[3600]") >= 0);
        assertTrue(s.indexOf("OT[10 moves / 1 min]") >= 0);
    }

    private static String writeToString(ConstGameTree tree)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SgfWriter(out, tree, null, null);
        String s = null;
        try
        {
            s = out.toString(SgfWriter.ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            fail();
        }
        return s;
    }
}
