// ProgramTest.java

package net.sf.gogui.gui;

import java.util.ArrayList;

public final class ProgramTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(ProgramTest.class);
    }

    public void testSetUniqueLabel()
    {
        ArrayList<Program> programs = new ArrayList<Program>();
        programs.add(new Program("Foo", "Foo", "", "", ""));
        Program p;

        // Label already exists
        p = new Program("Foo", "Foo", "", "", "");
        p.setUniqueLabel(programs);
        assertEquals("Foo (2)", p.m_label);
    }
}
