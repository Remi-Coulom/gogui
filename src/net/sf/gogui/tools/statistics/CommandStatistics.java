// CommandStatistics.java

package net.sf.gogui.tools.statistics;

import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import net.sf.gogui.util.Histogram;
import net.sf.gogui.util.Table;
import net.sf.gogui.util.TableUtil;

/** Collect GTP response statistics for a command. */
public final class CommandStatistics
{
    public final boolean m_isBeginCommand;

    public final int m_maxMove;

    public final DecimalFormat m_format;

    public final PositionStatistics m_statisticsAll;

    public final PositionStatistics m_statisticsFinal;

    public final ArrayList<PositionStatistics> m_statisticsAtMove;

    public final Table m_tableAtMove;

    public CommandStatistics(String command, Table table, Table tableFinal,
                             File histoFile, File histoFileFinal,
                             Color color, int precision)
        throws Exception
    {
        m_statisticsAll = new PositionStatistics(command, table, false, 0, 0);
        double min = m_statisticsAll.getMin();
        double max = m_statisticsAll.getMax();
        m_statisticsFinal
            = new PositionStatistics(command, tableFinal, true, min, max);
        m_statisticsAtMove = new ArrayList<PositionStatistics>();
        ArrayList<String> columnTitles = new ArrayList<String>();
        columnTitles.add("Move");
        columnTitles.add("Mean");
        columnTitles.add("Error");
        m_tableAtMove = new Table(columnTitles);
        Table tableAtMove;
        m_maxMove = (int)(TableUtil.getMax(table, "Move") + 1);
        boolean isBeginCommand = true;
        for (int move = 1; move <= m_maxMove; ++move)
        {
            tableAtMove = TableUtil.selectIntRange(table, "Move", move,
                                                    move);
            PositionStatistics statisticsAtMove
                = new PositionStatistics(command, tableAtMove, true, min,
                                         max);
            m_statisticsAtMove.add(statisticsAtMove);
            int count = statisticsAtMove.getCount();
            if (count > 0)
            {
                if (move > 1)
                    isBeginCommand = false;
                m_tableAtMove.startRow();
                m_tableAtMove.set("Move", move);
                m_tableAtMove.set("Mean", statisticsAtMove.getMean());
                m_tableAtMove.set("Error", statisticsAtMove.getError());
            }
        }
        m_isBeginCommand = isBeginCommand;
        m_format = getFormat(precision, min, max);
        if (getCount() > 0)
        {
            Histogram histogram = m_statisticsAll.m_histogram;
            Table histoTable = TableUtil.fromHistogram(histogram, command);
            Plot plot = new Plot(200, 150, color, precision);
            setHistogramProperties(plot);
            plot.plot(histoFile, histoTable, command, "Count", null);
            histogram = m_statisticsFinal.m_histogram;
            if (m_statisticsFinal.getCount() > 0)
            {
                histoTable = TableUtil.fromHistogram(histogram, command);
                plot = new Plot(200, 150, color, precision);
                setHistogramProperties(plot);
                plot.plot(histoFileFinal, histoTable, command, "Count", null);
            }
        }
    }

    public int getCount()
    {
        return m_statisticsAll.getCount();
    }

    public PositionStatistics getStatistics(int moveInterval)
    {
        return (PositionStatistics)m_statisticsAtMove.get(moveInterval);
    }

    public boolean onlyBoolValues()
    {
        return m_statisticsAll.m_onlyBoolValues;
    }

    public void setHistogramProperties(Plot plot)
    {
        Histogram histogram = m_statisticsAll.m_histogram;
        double step = histogram.getStep();
        plot.setPlotStyleBars(step);
        plot.setYMin(0);
        plot.setNoPlotYZero();
        if (onlyBoolValues())
        {
            plot.setXLabelsBool();
        }
        else
        {
            plot.setXMin(histogram.getMin() - step / 2);
            plot.setXMax(histogram.getMax() + step / 2);
            plot.setFormatX(m_format);
        }
    }

    private static DecimalFormat getFormat(int precision, double min,
                                           double max)
    {
        DecimalFormat format = new DecimalFormat();
        double absMax = Math.max(Math.abs(min), Math.abs(max));
        if (absMax < 10000)
        {
            format.setMaximumFractionDigits(precision);
            format.setGroupingUsed(false);
            return format;
        }
        StringBuilder pattern = new StringBuilder();
        pattern.append("0.");
        for (int i = 0; i < precision; ++i)
            pattern.append('#');
        pattern.append("E0");
        format.applyPattern(pattern.toString());
        return format;
    }
}
