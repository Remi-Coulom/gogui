//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package sgf;

import java.io.InputStream;
import junit.framework.TestCase;
import game.GameTree;
import game.GameInformation;
import game.TimeSettings;

//----------------------------------------------------------------------------

public class ReaderTest
    extends TestCase
{
    public void testRead() throws Exception
    {
        readSgfFile("verbose-property-names.sgf", false, true);
        checkTimeSettings("time-settings-1.sgf", 1800000, 60000, 5);
    }

    private Reader getReader(String name) throws Reader.SgfError, Exception
    {
        InputStream in = getClass().getResourceAsStream(name);
        if (in == null)
            throw new Exception("Resource " + name + " not found");
        return new Reader(in, null, null, 0);
    }

    private void checkTimeSettings(String name, long preByoyomi, long byoyomi,
                                   int byoyomiMoves) throws Exception
    {
        Reader reader = getReader(name);
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
            Reader reader = getReader(name);
            if (expectWarnings && reader.getWarnings() == null)
                fail("Reading " + name + " should result in warnings");
            if (! expectWarnings && reader.getWarnings() != null)
                fail("Reading " + name + " should result in no warnings");
        }
        catch (Reader.SgfError error)
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
