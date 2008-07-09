// TableTest.java

package net.sf.gogui.util;

import java.io.StringReader;
import java.util.ArrayList;

public final class TableTest
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

    public void testGetDouble() throws ErrorMessage
    {
        ArrayList<String> columnTitles = new ArrayList<String>();
        columnTitles.add("Column 1");
        columnTitles.add("Column 2");
        Table table = new Table(columnTitles);
        table.startRow();
        table.set(0, "1.23");
        table.set(1, "abc");
        table.startRow();
        table.set(0, "");
        table.set(1, null);
        assertEquals(1.23, table.getDouble(0, 0), 1e-3);
        boolean errorThrown = false;
        try
        {
            table.getDouble(1, 0);
        }
        catch (ErrorMessage e)
        {
            errorThrown = true;
        }
        assertTrue(errorThrown);
        errorThrown = false;
        try
        {
            table.getDouble(0, 1);
        }
        catch (ErrorMessage e)
        {
            errorThrown = true;
        }
        assertTrue(errorThrown);
        errorThrown = false;
        try
        {
            table.getDouble(1, 1);
        }
        catch (ErrorMessage e)
        {
            errorThrown = true;
        }
        assertTrue(errorThrown);
    }

    public void testGetInt() throws ErrorMessage
    {
        ArrayList<String> columnTitles = new ArrayList<String>();
        columnTitles.add("Column 1");
        columnTitles.add("Column 2");
        Table table = new Table(columnTitles);
        table.startRow();
        table.set(0, "123");
        table.set(1, "abc");
        table.startRow();
        table.set(0, "");
        table.set(1, null);
        assertEquals(123, table.getInt(0, 0));
        boolean errorThrown = false;
        try
        {
            table.getInt(1, 0);
        }
        catch (ErrorMessage e)
        {
            errorThrown = true;
        }
        assertTrue(errorThrown);
        errorThrown = false;
        try
        {
            table.getInt(0, 1);
        }
        catch (ErrorMessage e)
        {
            errorThrown = true;
        }
        assertTrue(errorThrown);
        errorThrown = false;
        try
        {
            table.getInt(1, 1);
        }
        catch (ErrorMessage e)
        {
            errorThrown = true;
        }
        assertTrue(errorThrown);
    }

    private static Table get(String string) throws Exception
    {
        Table table = new Table();
        table.read(new StringReader(string));
        return table;
    }
}
