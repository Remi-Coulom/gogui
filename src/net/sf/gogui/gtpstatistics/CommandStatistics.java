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

public final class CommandStatistics
{
    public final PositionStatistics m_statisticsAll;

    public final PositionStatistics m_statisticsFinal;

    /** Vector<PositionStatistics> */
    public final Vector m_statisticsAtMove;

    public final Table m_tableMoveIntervals;

    public CommandStatistics(String command, Table table, Table tableFinal,
                             int interval, String histoFile,
                             String histoFileFinal, Color color,
                             int precision)
        throws Exception
    {
        m_statisticsAll = new PositionStatistics(command, table, false, 0, 0);
        double min = m_statisticsAll.getMin();
        double max = m_statisticsAll.getMax();
        m_statisticsFinal
            = new PositionStatistics(command, tableFinal, true, min, max);
        m_statisticsAtMove = new Vector();
        Vector columnTitles = new Vector();
        columnTitles.add("Move");
        columnTitles.add("Mean");
        columnTitles.add("Error");
        m_tableMoveIntervals = new Table(columnTitles);
        Table tableAtMove;
        int maxMove = (int)(TableUtils.getMax(table, "Move") + 1);
        for (int move = 1; move <= maxMove; move += interval)
        {
            tableAtMove = TableUtils.selectIntRange(table, "Move", move,
                                                    move + interval - 1);
            PositionStatistics statisticsAtMove
                = new PositionStatistics(command, tableAtMove, true, min,
                                         max);
            m_statisticsAtMove.add(statisticsAtMove);
            m_tableMoveIntervals.startRow();
            m_tableMoveIntervals.set("Move", move + ((interval - 1) / 2));
            m_tableMoveIntervals.set("Mean", statisticsAtMove.getMean());
            m_tableMoveIntervals.set("Error", statisticsAtMove.getError());
        }
        Histogram histogram = m_statisticsAll.m_histogram;
        Table histoTable = TableUtils.fromHistogram(histogram, command);
        Plot plot = new Plot(200, 200, color, precision);
        plot.setPlotStyleBars();
        plot.setYMin(0);
        plot.plot(new File(histoFile), histoTable, command, "Count",
                  null);
        histogram = m_statisticsFinal.m_histogram;
        histoTable = TableUtils.fromHistogram(histogram, command);
        plot = new Plot(150, 150, color, precision);
        plot.setPlotStyleBars();
        plot.setYMin(0);
        plot.setXMin(min);
        plot.setXMax(max);
        plot.plot(new File(histoFileFinal), histoTable, command, "Count",
                  null);
    }

    public int getNumberMoveIntervals()
    {
        return m_statisticsAtMove.size();
    }

    public PositionStatistics getStatistics(int moveInterval)
    {
        return (PositionStatistics)m_statisticsAtMove.get(moveInterval);
    }
}

//----------------------------------------------------------------------------
