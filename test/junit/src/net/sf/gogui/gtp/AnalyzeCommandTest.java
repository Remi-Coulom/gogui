// AnalyzeCommandTest.java

package net.sf.gogui.gtp;

import static net.sf.gogui.go.GoColor.BLACK;

public final class AnalyzeCommandTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(AnalyzeCommandTest.class);
    }

    /** Test that replacement of string argument works if string contains
        backslashes. */
    public void testStringArgWithBackslashes()
    {
        AnalyzeCommand cmd = createCommand("none/Test/test %s");
        cmd.setStringArg("foo\\bar");
        String line = cmd.replaceWildCards(BLACK);
        assertEquals("test foo\\bar", line);
    }

    private static AnalyzeCommand createCommand(String definition)
    {
        return new AnalyzeCommand(new AnalyzeDefinition(definition));
    }
}
