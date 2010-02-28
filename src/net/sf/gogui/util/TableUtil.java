// TableUtil.java

package net.sf.gogui.util;

import java.util.ArrayList;

/** Utility functions for class Table. */
public final class TableUtil
{
    /** Check if all elements in a column are empty.
        @param table The table.
        @param column The column title.
        @return True, if all elements in this column are null or strings
        containing only whitespaces. */
    public static boolean allEmpty(Table table, String column)
        throws Table.InvalidLocation
    {
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(column, row);
            if (! StringUtil.isEmpty(value))
                return false;
        }
        return true;
    }

    /** Append row from other table.
        The tables need to have the same number of columns.
        @param to The table to append to.
        @param from The table to take the row from.
        @param row The index of the row in table from. */
    public static void appendRow(Table to, Table from, int row)
    {
        assert to.getNumberColumns() == from.getNumberColumns();
        to.startRow();
        for (int column = 0; column < to.getNumberColumns(); ++column)
            to.set(column, from.get(column, row));
    }

    /** Find row with required values for two columns.
        @param table The table.
        @param compareColumn1 The first column title.
        @param compareValue1 The required value for the first column.
        @param compareColumn2 The second column title.
        @param compareValue2 The required value for the second column.
        @return The row with matching values for both columns or -1,
        if no such row exists. */
    public static int findRow(Table table,
                              String compareColumn1, String compareValue1,
                              String compareColumn2, String compareValue2)
        throws Table.InvalidLocation
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
        ArrayList<String> columnTitles = new ArrayList<String>(2);
        columnTitles.add(name);
        columnTitles.add("Count");
        Table result = new Table(columnTitles);
        for (int i = 0; i < histogram.getSize(); ++i)
        {
            int count = histogram.getCount(i);
            if (count == 0)
                continue;
            result.startRow();
            try
            {
                result.set(name, histogram.getValue(i));
                result.set("Count", count);
            }
            catch (Table.InvalidLocation e)
            {
                assert false;
            }
        }
        return result;
    }

    /** Get elements of a column without null and whitespace-only elements. */
    public static ArrayList<String> getColumnNotEmpty(Table table,
                                                      String column)
        throws Table.InvalidLocation
    {
        ArrayList<String> result = new ArrayList<String>();
        int col = table.getColumnIndex(column);
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(col, row);
            if (! StringUtil.isEmpty(value))
                result.add(value);
        }
        return result;
    }

    public static ArrayList<String> getColumnUnique(Table table, String column)
        throws Table.InvalidLocation
    {
        ArrayList<String> result = new ArrayList<String>();
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
        throws Table.InvalidLocation
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
        throws Table.InvalidLocation
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
        throws Table.InvalidLocation
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
        throws Table.InvalidLocation
    {
        ArrayList<String> columnTitles = new ArrayList<String>(1);
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
        throws Table.InvalidLocation
    {
        ArrayList<String> columnTitles = new ArrayList<String>(2);
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
        throws Table.InvalidLocation
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

    /** Make constructor unavailable; class is for namespace only. */
    private TableUtil()
    {
    }
}
