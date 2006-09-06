//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

public class GtpUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GtpUtilTest.class);
    }

    public void testParsePointString() throws GtpError
    {
        String s = " A1 b2\n textC3 C3 PASS text\tpass";
        GoPoint[] points = GtpUtil.parsePointString(s, 19);
        assertEquals(5, points.length);
        assertSame(GoPoint.get(0, 0), points[0]);
        assertSame(GoPoint.get(1, 1), points[1]);
        assertSame(GoPoint.get(2, 2), points[2]);
        assertSame(null, points[3]);
        assertSame(null, points[4]);
    }

    public void getTimeSettingsCommand()
    {
        TimeSettings settings = new TimeSettings(60000);
        assertEquals(GtpUtil.getTimeSettingsCommand(settings),
                     "time_settings 60 0 0");
        settings = new TimeSettings(0, 60000, 10);
        assertEquals(GtpUtil.getTimeSettingsCommand(settings),
                     "time_settings 0 60 10");
        settings = new TimeSettings(30000, 60000, 10);
        assertEquals(GtpUtil.getTimeSettingsCommand(settings),
                     "time_settings 30 60 10");
    }
}

//----------------------------------------------------------------------------
