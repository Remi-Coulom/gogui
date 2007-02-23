//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

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
        ArrayList programs = new ArrayList();
        programs.add(new Program("Foo", "Foo", "", "", ""));
        Program p;

        // Label already exists, version not empty
        p = new Program("Foo", "Foo", "1.1", "", "");
        p.setUniqueLabel(programs);
        assertEquals("Foo 1.1", p.m_label);

        // Label already exists, version empty
        p = new Program("Foo", "Foo", "", "", "");
        p.setUniqueLabel(programs);
        assertEquals("Foo (2)", p.m_label);
    }
}
