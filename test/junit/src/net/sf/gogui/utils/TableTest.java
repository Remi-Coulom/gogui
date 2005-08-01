//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.StringReader;

//----------------------------------------------------------------------------

public class TableTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TableTest.class);
    }

    public void testBasic() throws Exception
    {
        Table table = get("#\n" +
                          "#Col1\tCol2\tCol3\n" +
                          "1\t2\t1\n" +
                          "2\tfoo\tbar\n");
        assertEquals(3, table.getNumberColumns());
        assertEquals("Col1", table.getColumnTitle(0));
        assertEquals("Col2", table.getColumnTitle(1));
        assertEquals("Col3", table.getColumnTitle(2));
        assertEquals("1", table.get("Col1", 0));
        assertEquals("2", table.get("Col2", 0));
        assertEquals("1", table.get("Col3", 0));
        assertEquals("2", table.get(0, 1));
        assertEquals("foo", table.get(1, 1));
        assertEquals("bar", table.get(2, 1));
    }

    private static Table get(String string) throws Exception
    {
        Table table = new Table();
        table.read(new StringReader(string));
        return table;
    }
}
