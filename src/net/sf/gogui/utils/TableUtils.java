//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.util.Vector;

//----------------------------------------------------------------------------

public class TableUtils
{
    public static Table select(Table table, String compareColumn,
                               String compareValue, String selectColumn1,
                               String selectColumn2)
    {
        Vector columnTitles = new Vector(2);
        columnTitles.add(selectColumn1);
        columnTitles.add(selectColumn2);
        Table result = new Table(columnTitles);
        for (int i = 0; i < table.getNumberRows(); ++i)
        {
            String value = table.get(compareColumn, i);
            if (value == null || ! value.equals(compareValue))
                continue;
            result.startRow();
            result.set(selectColumn1, table.get(selectColumn1, i));
            result.set(selectColumn2, table.get(selectColumn2, i));
        }
        return result;
    }
}

//----------------------------------------------------------------------------
