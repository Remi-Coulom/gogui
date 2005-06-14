//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.util.Vector;
import junit.framework.TestCase;

//----------------------------------------------------------------------------

public class OptionsTest
    extends TestCase
{
    public void testBasic() throws ErrorMessage
    {
        String specs[] = { "flag1", "value1:", "value2:", "flag2" };
        String args[] = { "arg1", "-value1:", "42", "-flag2", "arg2" };
        Options opt = new Options(args, specs);
        assertFalse(opt.isSet("flag1"));
        assertTrue(opt.isSet("flag2"));
        assertTrue(opt.isSet("value1"));
        assertFalse(opt.isSet("value2"));
        assertEquals(opt.getInteger("value1"), 42);
        Vector arguments = opt.getArguments();
        assertEquals(arguments.size(), 2);
        assertEquals(arguments.get(0), "arg1");
        assertEquals(arguments.get(1), "arg2");
    }
}

//----------------------------------------------------------------------------
