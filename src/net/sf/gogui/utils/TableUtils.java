//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.util.Vector;

//----------------------------------------------------------------------------

public class TableUtils
{
    public static Table fromHistogram(Histogram histogram, String name)
    {
        Vector columnTitles = new Vector(2);
        columnTitles.add(name);
        columnTitles.add("Count");
        Table result = new Table(columnTitles);
        for (int i = 0; i < histogram.getSize(); ++i)
        {
            result.startRow();
            result.set(name, histogram.getValue(i));
            result.set("Count", histogram.getCount(i));
        }
        return result;
    }

    public static double getMax(Table table, String column)
    {
        double max = Double.NEGATIVE_INFINITY;
        int col = table.getColumnIndex(column);
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            try
            {
                double value = Double.parseDouble(table.get(col, row));
                max = Math.max(max, value);
            }
            catch (NumberFormatException e)
            {
            }
        }
        return max;
    }

    public static boolean isNumberValue(String string)
    {
        try
        {
            Double.parseDouble(string);
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }

    public static boolean isBoolValue(String string)
    {
        return (string.equals("0") || string.equals("1"));
    }

    public static boolean isIntValue(String string)
    {
        try
        {
            Integer.parseInt(string);
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }

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

    public static Table selectIntRange(Table table, String compareColumn,
                                       int min, int max)
    {
        Table result = new Table(table.getColumnTitles());
        int numberColumns = table.getNumberColumns();
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(compareColumn, row);
            try
            {
                int intValue = Integer.parseInt(value);
                if (intValue >= min && intValue <= max)
                {
                    result.startRow();
                    for (int column = 0; column < numberColumns; ++column)
                        result.set(column, table.get(column, row));
                }

            }
            catch (NumberFormatException e)
            {
            }
        }
        return result;
    }
}

//----------------------------------------------------------------------------
