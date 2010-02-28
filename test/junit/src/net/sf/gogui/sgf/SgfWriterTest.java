// SgfWriterTest.java

package net.sf.gogui.sgf;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Komi;

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

    /** Test that komi property is written. */
    public void testKomi() throws Exception
    {
        GameTree tree = new GameTree();
        GameInfo info = tree.getRoot().getGameInfo();
        info.setKomi(new Komi(6.5));
        String s = writeToString(tree);
        assertTrue(s.indexOf("KM[6.5]") >= 0);
    }

    /** Test that komi property is written if both handicap and komi are
        used. */
    public void testKomiWithHandicap() throws Exception
    {
        GameTree tree = new GameTree();
        GameInfo info = tree.getRoot().getGameInfo();
        info.setKomi(new Komi(4));
        info.setHandicap(4);
        String s = writeToString(tree);
        assertTrue(s.indexOf("HA[4]") >= 0);
        assertTrue(s.indexOf("KM[4]") >= 0);
    }

    public void testWriteTimeSettings() throws Exception
    {
        GameTree tree = new GameTree();
        TimeSettings settings = new TimeSettings(3600000, 60000, 10);
        tree.getRoot().getGameInfo().setTimeSettings(settings);
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
