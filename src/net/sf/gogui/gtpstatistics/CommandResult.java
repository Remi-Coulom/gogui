//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Vector;
import java.text.DecimalFormat;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.Histogram;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.Table;
import net.sf.gogui.utils.TableUtils;

//----------------------------------------------------------------------------

public final class CommandResult
{
    public final int m_maxElement;

    public final int m_numberElements;

    public final Histogram m_histogram;

    public final Statistics m_statistics;

    public final Statistics[] m_statisticsAtMove;

    public final Table m_table;

    public CommandResult(String command, Table table, int interval,
                         String histoFile, Color color, int precision)
        throws Exception
    {
        m_statistics = new Statistics();
        m_numberElements = 500 / interval;
        m_statisticsAtMove =  new Statistics[m_numberElements + 1];
        for (int i = 0; i < m_numberElements + 1; ++i)
            m_statisticsAtMove[i] = new Statistics();
        int maxElement = 0;
        boolean onlyIntValues = true;
        for (int i = 0; i < table.getNumberRows(); ++i)
        {
            String value = table.get(command, i);
            double doubleValue;
            try
            {
                Integer.parseInt(value);
            }
            catch (NumberFormatException e)
            {
                onlyIntValues = false;
            }
            try
            {
                doubleValue = Double.parseDouble(value);
                m_statistics.addValue(doubleValue);
            }
            catch (NumberFormatException e)
            {
                continue;
            }
            int move = Integer.parseInt(table.get("Move", i));
            if (move <= 0)
                throw new Exception("Invalid move in table row " + i);
            int intervalIndex = (move - 1) / interval;
            int element = Math.min(intervalIndex, m_numberElements);
            maxElement = Math.max(maxElement, element);
            m_statisticsAtMove[element].addValue(doubleValue);
        }
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
        Table histoTable = TableUtils.fromHistogram(m_histogram, command);
        Plot plot = new Plot(250, 250, color, precision);
        plot.setPlotStyleBars();
        plot.setYMin(0);
        plot.setTitle(command);
        plot.plot(new File(histoFile), histoTable, command, "Count",
                  null);
        m_maxElement = maxElement;
        Vector columnTitles = new Vector();
        columnTitles.add("Move");
        columnTitles.add(command);
        columnTitles.add("Error");
        m_table = new Table(columnTitles);
        for (int i = 0; i <= m_maxElement; ++i)
        {
            m_table.startRow();
            m_table.set("Move", i * interval + interval / 2);
            m_table.set(command, m_statisticsAtMove[i].getMean());
            m_table.set("Error", m_statisticsAtMove[i].getErrorMean());
        }
    }
}

//----------------------------------------------------------------------------
