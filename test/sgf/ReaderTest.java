//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package sgf;

import java.io.InputStream;
import junit.framework.TestCase;

//----------------------------------------------------------------------------

public class ReaderTest
    extends TestCase
{
    public void testRead() throws Exception
    {
        readSgfFile("verbose-property-names.sgf", false, true);
    }

    private void readSgfFile(String name, boolean expectFailure,
                             boolean expectWarnings) throws Exception
    {
        try
        {
            InputStream in = getClass().getResourceAsStream(name);
            if (in == null)
                throw new Exception("Resource " + name + " not found");
            Reader reader = new Reader(in, null, null, 0);
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
