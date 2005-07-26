//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import junit.framework.TestCase;

//----------------------------------------------------------------------------

public class ScoreTest
    extends TestCase
{
    public void testFormat()
    {
        assertEquals(Score.formatResult(15.01), "B+15");
        assertEquals(Score.formatResult(-5.5), "W+5.5");
    }
}

//----------------------------------------------------------------------------
