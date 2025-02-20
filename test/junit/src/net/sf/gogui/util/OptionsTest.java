// OptionsTest.java

package net.sf.gogui.util;

import java.util.ArrayList;

public final class OptionsTest
    extends junit.framework.TestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(OptionsTest.class);
    }

    public void testBasic() throws ErrorMessage
    {
        String[] specs = {
            "flag1",
            "value1:",
            "value2:",
            "flag2",
            "value3:",
            "value4:"
        };
        String[] args = {
            "arg1",
            "-value1", "42",
            "-flag2",
            "-value3", "-9223372036854775807",
            "-value4", "-1",
            "arg2"
        };
        Options opt = new Options(args, specs);
        assertFalse(opt.contains("flag1"));
        assertTrue(opt.contains("flag2"));
        assertTrue(opt.contains("value1"));
        assertFalse(opt.contains("value2"));
        assertEquals("42", opt.get("value1"));
        assertEquals(42, opt.getInteger("value1"));
        assertEquals(-98, opt.getInteger("value2", -98));
        assertEquals(-9223372036854775807L, opt.getLong("value3"));
        ArrayList<String> arguments = opt.getArguments();
        assertEquals(2, arguments.size());
        assertEquals("arg1", arguments.get(0));
        assertEquals("arg2", arguments.get(1));
    }

    public void testCheckNoArguments() throws ErrorMessage
    {
        String[] specs = {
            "option:"
        };
        {
            String[] args = {
                "-option", "value",
                "arg"
            };
            Options opt = new Options(args, specs);
            boolean errorThrown = false;
            try
            {
                opt.checkNoArguments();
            }
            catch (ErrorMessage e)
            {
                errorThrown = true;
            }
            assertTrue(errorThrown);
        }
        {
            String[] args = {
                "-option", "value"
            };
            Options opt = new Options(args, specs);
            try
            {
                opt.checkNoArguments();
            }
            catch (ErrorMessage e)
            {
                fail();
            }
        }
    }

    public void testStopParsing() throws ErrorMessage
    {
        String[] specs = { "flag1", "value1:", "value2:", "flag2" };
        String[] args = { "-value1", "foo", "--", "-arg1" };
        Options opt = new Options(args, specs);
        ArrayList<String> arguments = opt.getArguments();
        assertEquals(1, arguments.size());
        assertEquals("-arg1", arguments.get(0));
    }
}
