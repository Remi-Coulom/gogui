//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.util.ErrorMessage;

public final class TimeSettingsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TimeSettingsTest.class);
    }

    public void testParse()
    {
        TimeSettings settings = parse("30");
        assertEquals(settings.getPreByoyomi(), 30 * 60 * 1000);
        assertFalse(settings.getUseByoyomi());
    }

    private TimeSettings parse(String s)
    {
        try
        {
            return TimeSettings.parse(s);
        }
        catch (ErrorMessage e)
        {
            fail();
            return null;
        }
    }
}
