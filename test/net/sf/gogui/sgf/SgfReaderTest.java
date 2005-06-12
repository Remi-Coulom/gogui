//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.InputStream;
import junit.framework.TestCase;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.TimeSettings;

//----------------------------------------------------------------------------

public class SgfReaderTest
    extends TestCase
{
    public void testRead() throws Exception
    {
        readSgfFile("verbose-property-names.sgf", false, true);
        checkTimeSettings("time-settings-1.sgf", 1800000, 60000, 5);
    }

    private SgfReader getReader(String name)
        throws SgfReader.SgfError, Exception
    {
        InputStream in = getClass().getResourceAsStream(name);
        if (in == null)
            throw new Exception("Resource " + name + " not found");
        return new SgfReader(in, null, null, 0);
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
