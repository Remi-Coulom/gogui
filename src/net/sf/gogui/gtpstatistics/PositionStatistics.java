//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.util.Vector;
import net.sf.gogui.utils.Histogram;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Table;
import net.sf.gogui.utils.TableUtils;

//----------------------------------------------------------------------------

public final class PositionStatistics
{
    public final boolean m_onlyBoolValues;

    public final boolean m_onlyIntValues;

    public final int m_numberNoResult;

    public final Histogram m_histogram;

    public final Statistics m_statistics;

    /** Statistics assuming maximum correlation of positions of the same game.
        Contains one value for each game, computed from the average of all
        positions from this game.
    */
    public final Statistics m_statisticsMaxError;

    public final Table m_histoTable;

    public final Table m_table;

    public PositionStatistics(String command, Table table,
                              boolean noAutoScaleHisto,
                              double histoMin, double histoMax)
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
                m_statistics.add(Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
                ++numberNoResult;
            }
        }
        m_onlyBoolValues = onlyBoolValues;
        m_onlyIntValues = onlyBoolValues;
        m_numberNoResult = numberNoResult;
        double min = m_statistics.getMin();
        double max = m_statistics.getMax();
        if (! noAutoScaleHisto)
        {
            histoMin = min;
            histoMax = max;
        }
        double diff = histoMax - histoMin;
        int maxBins = 25;
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
            try
            {
                m_histogram.add(Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
                continue;
            }
        }
        m_histoTable = TableUtils.fromHistogram(m_histogram, command);
        m_statisticsMaxError = new Statistics();
        Vector files = TableUtils.getColumnUnique(table, "File");
        for (int i = 0; i < files.size(); ++i)
        {
            String file = (String)(files.get(i));
            Table tableFile = TableUtils.select(table, "File", file);
            Statistics statisticsFile
                = TableUtils.getStatistics(tableFile, command);
            m_statisticsMaxError.add(statisticsFile.getMean());
        }
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
    public double getMaxError()
    {
        return m_statisticsMaxError.getError();
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

//----------------------------------------------------------------------------
