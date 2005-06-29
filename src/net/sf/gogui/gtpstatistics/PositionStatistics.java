//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import net.sf.gogui.utils.Histogram;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Table;
import net.sf.gogui.utils.TableUtils;

//----------------------------------------------------------------------------

public final class PositionStatistics
{
    public final boolean m_onlyBoolValues;

    public final int m_numberNoResult;

    public final Histogram m_histogram;

    public final Statistics m_statistics;

    public final Table m_histoTable;

    public final Table m_table;

    public PositionStatistics(String command, Table table)
        throws ErrorMessage
    {
        m_table = table;
        m_statistics = new Statistics();
        boolean onlyIntValues = true;
        int numberNoResult = 0;
        boolean onlyBoolValues = true;
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(command, row);
            if (TableUtils.isNumberValue(value))
            {
                if (! TableUtils.isIntValue(value))
                    onlyIntValues = false;
                if (! TableUtils.isBoolValue(value))
                    onlyBoolValues = false;
            }
            int move;
            try
            {
                move = Integer.parseInt(table.get("Move", row));
            }
            catch (NumberFormatException e)
            {
                throw new ErrorMessage("Invalid move in table");
            }
            if (move <= 0)
                throw new ErrorMessage("Invalid move in table");
            double doubleValue;
            try
            {
                m_statistics.addValue(Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
                ++numberNoResult;
            }
        }
        m_onlyBoolValues = onlyBoolValues;
        m_numberNoResult = numberNoResult;
        double min = m_statistics.getMin();
        double max = m_statistics.getMax();
        double diff = max - min;
        if (onlyIntValues)
            m_histogram = new Histogram(min, max, Math.max(1, diff / 35));
        else
            m_histogram = new Histogram(min, max, diff / 35);
        for (int i = 0; i < table.getNumberRows(); ++i)
        {
            String value = table.get(command, i);
            try
            {
                m_histogram.addValue(Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
                continue;
            }
        }
        m_histoTable = TableUtils.fromHistogram(m_histogram, command);
    }

    public int getCount()
    {
        return m_statistics.getCount();
    }

    public double getErrorMean()
    {
        return m_statistics.getErrorMean();
    }

    public double getDeviation()
    {
        return m_statistics.getDeviation();
    }

    public double getMax()
    {
        return m_statistics.getMax();
    }
    public double getMean()
    {
        return m_statistics.getMean();
    }

    public double getMin()
    {
        return m_statistics.getMax();
    }

    public double getSum()
    {
        return m_statistics.getSum();
    }
}

//----------------------------------------------------------------------------
