//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.util.Vector;

//----------------------------------------------------------------------------

public class TableUtils
{
    public static void appendRow(Table to, Table from, int row)
    {
        assert(to.getNumberColumns() == from.getNumberColumns());
        to.startRow();
        for (int column = 0; column < to.getNumberColumns(); ++column)
            to.set(column, from.get(column, row));
    }

    public static int findRow(Table table,
                              String compareColumn1, String compareValue1,
                              String compareColumn2, String compareValue2)
    {
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value1 = table.get(compareColumn1, row);
            if (value1 == null || ! value1.equals(compareValue1))
                continue;
            String value2 = table.get(compareColumn2, row);
            if (value2 == null || ! value2.equals(compareValue2))
                continue;
            return row;
        }
        return -1;
    }

    public static Table fromHistogram(Histogram histogram, String name)
    {
        Vector columnTitles = new Vector(2);
        columnTitles.add(name);
        columnTitles.add("Count");
        Table result = new Table(columnTitles);
        for (int i = 0; i < histogram.getSize(); ++i)
        {
            int count = histogram.getCount(i);
            if (count == 0)
                continue;
            result.startRow();
            result.set(name, histogram.getValue(i));
            result.set("Count", count);
        }
        return result;
    }

    public static Vector getColumnUnique(Table table, String column)
    {
        Vector result = new Vector();
        int col = table.getColumnIndex(column);
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(col, row);
            if (value != null && ! value.equals("")
                && ! result.contains(value))
                result.add(value);
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

    public static Statistics getStatistics(Table table, String column)
    {
        Statistics statistics = new Statistics();
        int col = table.getColumnIndex(column);
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            try
            {
                String value = table.get(col, row);
                if (value == null)
                    continue;
                double doubleValue = Double.parseDouble(value);
                statistics.add(doubleValue);
            }
            catch (NumberFormatException e)
            {
            }
        }
        return statistics;
    }

    public static boolean isNumberValue(String string)
    {
        if (string == null)
            return false;
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
                               String compareValue)
    {
        Table result = new Table(table.getColumnTitles());
        int numberColumns = table.getNumberColumns();
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(compareColumn, row);
            if (value == null || ! value.equals(compareValue))
                continue;
            result.startRow();
            for (int column = 0; column < numberColumns; ++column)
                result.set(column, table.get(column, row));
        }
        return result;
    }

    public static Table select(Table table, String compareColumn,
                               String compareValue, String selectColumn)
    {
        Vector columnTitles = new Vector(1);
        columnTitles.add(selectColumn);
        Table result = new Table(columnTitles);
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(compareColumn, row);
            if (value == null || ! value.equals(compareValue))
                continue;
            result.startRow();
            result.set(selectColumn, table.get(selectColumn, row));
        }
        return result;
    }

    public static Table select(Table table, String compareColumn,
                               String compareValue, String selectColumn1,
                               String selectColumn2)
    {
        Vector columnTitles = new Vector(2);
        columnTitles.add(selectColumn1);
        columnTitles.add(selectColumn2);
        Table result = new Table(columnTitles);
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(compareColumn, row);
            if (value == null || ! value.equals(compareValue))
                continue;
            result.startRow();
            result.set(selectColumn1, table.get(selectColumn1, row));
            result.set(selectColumn2, table.get(selectColumn2, row));
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
