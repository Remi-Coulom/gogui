// PositionStatistics.java

package net.sf.gogui.tools.statistics;

import net.sf.gogui.util.Histogram;
import net.sf.gogui.util.Statistics;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Table;
import net.sf.gogui.util.TableUtil;

/** Collect GTP response statistics for a set of positions. */
public final class PositionStatistics
{
    public final boolean m_onlyBoolValues;

    public final boolean m_onlyIntValues;

    public final int m_numberNoResult;

    public final Histogram m_histogram;

    public final Statistics m_statistics;

    public final Table m_histoTable;

    public PositionStatistics(String command, Table table,
                              boolean noAutoScaleHisto,
                              double histoMin, double histoMax)
        throws Table.InvalidLocation, ErrorMessage
    {
        m_statistics = new Statistics();
        boolean onlyIntValues = true;
        int numberNoResult = 0;
        boolean onlyBoolValues = true;
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String value = table.get(command, row);
            if (value == null)
            {
                ++numberNoResult;
                continue;
            }
            if (TableUtil.isNumberValue(value))
            {
                if (! TableUtil.isIntValue(value))
                    onlyIntValues = false;
                if (! TableUtil.isBoolValue(value))
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
            if (move < 0)
                throw new ErrorMessage("Invalid move in table");
            try
            {
                m_statistics.add(Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
                ++numberNoResult;
            }
        }
        m_onlyBoolValues = onlyBoolValues;
        m_onlyIntValues = onlyIntValues;
        m_numberNoResult = numberNoResult;
        double min = m_statistics.getMin();
        double max = m_statistics.getMax();
        if (! noAutoScaleHisto)
        {
            histoMin = min;
            histoMax = max;
        }
        double diff = histoMax - histoMin;
        int maxBins = 20;
        if (onlyIntValues)
        {
            int step = Math.max(1, (int)(diff / maxBins + 1));
            m_histogram = new Histogram(histoMin, histoMax, step);
        }
        else
            m_histogram = new Histogram(histoMin, histoMax, diff / maxBins);
        for (int i = 0; i < table.getNumberRows(); ++i)
        {
            String value = table.get(command, i);
            if (value == null)
                continue;
            try
            {
                m_histogram.add(Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
                continue;
            }
        }
        m_histoTable = TableUtil.fromHistogram(m_histogram, command);
    }

    public int getCount()
    {
        return m_statistics.getCount();
    }

    public double getError()
    {
        return m_statistics.getError();
    }

    public double getDeviation()
    {
        return m_statistics.getDeviation();
    }

    public double getMax()
    {
        return m_statistics.getMax();
    }

    public double getMaxError(int n)
    {
        return m_statistics.getMaxError(n);
    }

    public double getMean()
    {
        return m_statistics.getMean();
    }

    public double getMin()
    {
        return m_statistics.getMin();
    }

    public double getSum()
    {
        return m_statistics.getSum();
    }
}
