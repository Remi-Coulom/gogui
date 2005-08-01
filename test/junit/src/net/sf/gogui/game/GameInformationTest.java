//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

//----------------------------------------------------------------------------

public class GameInformationTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GameInformationTest.class);
    }

    public void testFormatKomi()
    {
        assertEquals(GameInformation.roundKomi(1.99), "2");
        assertEquals(GameInformation.roundKomi(2.51), "2.5");
    }
}

//----------------------------------------------------------------------------
