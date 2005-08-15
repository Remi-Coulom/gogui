//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.StringReader;

//----------------------------------------------------------------------------

public class TableUtilsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TableUtilsTest.class);
    }

    public void testFromHistogramSingleValue() throws Exception
    {
        Histogram histo = new Histogram(-1, 1, 1);
        histo.add(0);
        Table table = TableUtils.fromHistogram(histo, "");
        assertEquals(table.getNumberColumns(), 2);
        assertEquals(table.getNumberRows(), 1);
    }
}

//----------------------------------------------------------------------------
