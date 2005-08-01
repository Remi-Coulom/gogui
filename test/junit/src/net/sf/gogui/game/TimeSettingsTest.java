//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import junit.framework.TestCase;
import net.sf.gogui.utils.ErrorMessage;

//----------------------------------------------------------------------------

public class TimeSettingsTest
    extends TestCase
{
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

//----------------------------------------------------------------------------
