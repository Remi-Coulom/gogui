//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import junit.framework.TestCase;

//----------------------------------------------------------------------------

public class GameInformationTest
    extends TestCase
{
    public void testFormatKomi()
    {
        assertEquals(GameInformation.roundKomi(1.99), "2");
        assertEquals(GameInformation.roundKomi(2.51), "2.5");
    }
}

//----------------------------------------------------------------------------
