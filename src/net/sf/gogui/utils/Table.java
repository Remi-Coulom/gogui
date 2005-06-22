//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

//----------------------------------------------------------------------------

public class Table
{
    public Table(Vector columnTitles)
    {        
        m_columnTitles = columnTitles;
        m_numberColumns = columnTitles.size();
        m_rows = new Vector();
    }

    public String get(String columnTitle, int row)
    {
        int column = getColumnIndex(columnTitle);
        return (String)getRow(row).get(column);
    }

    public int getNumberRows()
    {
        return m_rows.size();
    }

    public void save(Writer out) throws IOException
    {
        for (int i = 0; i < m_numberColumns; ++i)
        {
            out.write(getTitle(i));
            if (i < m_numberColumns - 1)
                out.write('\t');
            else
                out.write('\n');
        }
        for (int i = 0; i < m_rows.size(); ++i)
        {
            Vector row = (Vector)m_rows.get(i);
            for (int j = 0; j < m_numberColumns; ++j)
            {
                String value = (String)row.get(j);
                if (value == null)
                    out.write("(null)");
                else
                    out.write(value);
                if (j < m_numberColumns - 1)
                    out.write('\t');
                else
                    out.write('\n');
            }
        }
    }
    
    public void startRow()
    {
        Vector row = new Vector(m_numberColumns);
        for (int i = 0; i < m_numberColumns; ++i)
            row.add(null);
        m_rows.add(row);
        m_lastRow = row;
    }
    
    public void set(String column, int value)
    {
        set(column, Integer.toString(value));
    }

    public void set(String column, String value)
    {
        int index = getColumnIndex(column);
        assert(m_lastRow.get(index) == null);
        m_lastRow.set(index, value);
    }
    
    private int m_numberColumns;

    private Vector m_columnTitles;

    private Vector m_lastRow;

    private Vector m_rows;

    private int getColumnIndex(String column)
    {
        for (int i = 0; i < m_numberColumns; ++i)
        {
            String title = getTitle(i);
            if (title.equals(column))
                return i;
        }
        assert(false);
        return -1;
    }

    private Vector getRow(int index)
    {
        return (Vector)m_rows.get(index);
    }

    private String getTitle(int index)
    {
        return (String)m_columnTitles.get(index);
    }
}

//----------------------------------------------------------------------------
