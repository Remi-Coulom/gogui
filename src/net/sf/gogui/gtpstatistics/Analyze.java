//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import java.text.DecimalFormat;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.Histogram;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.Table;
import net.sf.gogui.utils.TableUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class Analyze
{
    public Analyze(String fileName, int precision, int interval,
                   boolean force)
        throws Exception
    {
        m_precision = precision;
        m_fileName = fileName;
        m_formatInt = new DecimalFormat();
        m_formatInt.setMaximumFractionDigits(0);
        m_formatInt.setGroupingUsed(false);
        m_formatFloat = new DecimalFormat();
        m_formatFloat.setMaximumFractionDigits(precision);
        m_formatFloat.setGroupingUsed(false);
        m_interval = interval;
        m_table = new Table();
        m_table.read(new File(fileName));
        if (m_table.getNumberColumns() < 2
            || ! m_table.getColumnTitle(0).equals("File")
            || ! m_table.getColumnTitle(1).equals("Move"))
            throw new ErrorMessage("Invalid table format");
        m_commands = new Vector();
        for (int i = 2; i < m_table.getNumberColumns(); ++i)
            m_commands.add(m_table.getColumnTitle(i));
        m_commandStatistics = new Vector(m_commands.size());
        File file = new File(FileUtils.replaceExtension(fileName,
                                                        "dat", "html"));
        if (file.exists() && ! force)
            throw new ErrorMessage("File " + file + " already exists");
        initGameInfo();
        PrintStream out = new PrintStream(new FileOutputStream(file));
        startHtml(out, "GtpStatistics Summary");
        startInfo(out, "GtpStatistics Summary");
        writeInfo(out);
        endInfo(out);
        out.print("<h2>Averages</h2>\n");
        Vector columnTitles = new Vector();
        columnTitles.add("Move");
        columnTitles.add("Count");
        Table table = new Table(columnTitles);
        CommandStatistics commandStatistics = computeCommandStatistics(0);
        for (int i = 0; i < commandStatistics.getNumberMoveIntervals(); ++i)
        {
            table.startRow();
            table.set("Move", i * interval + interval / 2);
            table.set("Count", commandStatistics.getStatistics(i).getCount());
        }
        String extension = "count.png";
        File pngFile
            = new File(FileUtils.replaceExtension(fileName, "dat",
                                                  extension));
        int numberMoves = table.getNumberRows() * (m_interval + 1);
        Plot plot = generatePlotMove(getImgWidth(numberMoves),
                                     Color.DARK_GRAY);
        plot.plot(pngFile, table, "Move", "Count", null);
        out.print("<table border=\"0\">\n" +
                  "<tr><td align=\"center\">\n" +
                  "<small>Count</small><br>" +
                  "<img src=\"" + pngFile.toString() + "\">\n" +
                  "</td></tr>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            commandStatistics = computeCommandStatistics(i);
            m_commandStatistics.add(commandStatistics);
            String command = getCommand(i);
            table = commandStatistics.m_tableMoveIntervals;
            plot = generatePlotMove(getImgWidth(numberMoves),
                                    getColor(command));
            pngFile = new File(getAvgPlotFile(i));
            plot.plot(pngFile, table, "Move", "Mean", "Error");
            out.print("<tr><td align=\"center\">\n" +
                      getCommandLink(i) + "<br>" +
                      "<img src=\"" + pngFile.toString() + "\">\n" +
                      "</td></tr>\n");
        }
        out.print("</table>\n" +
                  "<hr>\n");
        out.print("<h2>Histograms All Positions</h2>\n" +
                  "<p>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            out.print("<table align=\"left\" border=\"0\">" +
                      "<tr><td align=\"center\">" + getCommandLink(i)
                      + "<br><img src=\"" + getHistoFile(i)
                      + "\"></td></tr></table>\n");
        }
        out.print("<br clear=\"left\">\n" +
                    "</p>\n" +
                    "<hr>\n");
        writeCommandsTable(out);
        out.print("<hr>\n");
        writeGameTable(out, fileName);
        out.print("<hr>\n");
        out.print("</body>\n" +
                  "</html>\n");
        out.close();
    }

    private static final Color[] m_plotColor = {
        Color.decode("#ff5454"),
        Color.decode("#738ab8"),
        Color.decode("#5eaf5e"),
        Color.decode("#ffa954"),
        Color.decode("#FF66FF"),
        Color.decode("#21C7CD"),
        Color.decode("#FF7340")
    };

    private static class GameInfo
    {
        public String m_file;

        public String m_name;

        public int m_finalPosition;

        public int m_numberPositions;
    }

    private int m_imgHeight = 100;

    private int m_interval;

    private int m_maxMove;

    private int m_numberGames;

    private int m_precision;

    private DecimalFormat m_formatInt;

    private DecimalFormat m_formatFloat;

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private final String m_fileName;

    private Table m_table;

    private Table m_tableFinal;

    private Vector m_commandStatistics;

    private Vector m_commands;

    /** Vector<GameInfo> */
    private Vector m_gameInfo;    

    private String formatFloat(double value)
    {
        return m_formatFloat.format(value);
    }

    private String formatInt(double value)
    {
        return m_formatInt.format(value);
    }

    private String getAvgPlotFile(int commandIndex)
    {
        String extension = "command-" + commandIndex + ".avg.png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private String getCommandFile(int commandIndex)
    {
        String extension = "command-" + commandIndex + ".html";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private CommandStatistics getCommandStatistics(int commandIndex)
    {
        return (CommandStatistics)m_commandStatistics.get(commandIndex);
    }

    private String getGameFile(int gameIndex)
    {
        String extension = "game-" + gameIndex + ".html";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getHistoFile(int commandIndex)
    {
        String extension = "command-" + commandIndex + ".histo.png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getHistoFile(int commandIndex, int moveIntervalIndex)
    {
        String extension = "command-" + commandIndex + ".interval"
            + moveIntervalIndex + ".histo.png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getHistoFinalFile(int commandIndex)
    {
        String extension = "command-" + commandIndex + ".final.png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getPlotFile(int gameIndex, int commandIndex)
    {
        String extension =
            "game-" + gameIndex + ".command-" + commandIndex + ".png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getCommandLink(int commandIndex)
    {
        return "<a href=\"" + getCommandFile(commandIndex) + "\"><small>"
            + getCommand(commandIndex) + "</small></a>";
    }

    private CommandStatistics computeCommandStatistics(int index)
        throws Exception
    {
        String command = getCommand(index);
        return new CommandStatistics(command, m_table, m_tableFinal,
                                     m_interval, getHistoFile(index),
                                     getHistoFinalFile(index),
                                     getColor(command), m_precision);
    }

    private Plot generatePlotMove(int width, Color color)
    {
        Plot plot = new Plot(width, m_imgHeight, color, m_precision);
        plot.setSolidLineInterval(10);
        plot.setXMin(0);
        plot.setXMax(m_maxMove + 1);
        plot.setXTics(5);
        plot.setXLabelPerTic(2);
        return plot;
    }

    private void generatePlot(int commandIndex, int gameIndex,
                              String gameFile) throws Exception
    {
        String command = getCommand(commandIndex);
        Table table = TableUtils.select(m_table, "File", gameFile,
                                        "Move", command);
        int numberPositions = table.getNumberRows();
        String fileName = getPlotFile(gameIndex, commandIndex);
        Plot plot = generatePlotMove(getImgWidth(numberPositions),
                                     getColor(command));
        plot.plot(new File(fileName), table, "Move", command, null);
    }

    private Color getColor(String command)
    {
        int index = m_table.getColumnIndex(command);
        return m_plotColor[(index - 2) % m_plotColor.length];
    }

    private int getImgWidth(int numberMoves)
    {
        return Math.max(10, Math.min(numberMoves * 9, 1000));
    }

    private void initGameInfo()
    {
        m_gameInfo = new Vector();
        String last = null;
        GameInfo info = null;
        m_maxMove = 0;
        int numberColumns = m_table.getNumberColumns();
        for (int row = 0; row < m_table.getNumberRows(); ++row)
        {
            String file = m_table.get("File", row);
            int move = Integer.parseInt(m_table.get("Move", row));
            m_maxMove = Math.max(m_maxMove, move);
            if (last == null || ! file.equals(last))
            {
                if (info != null)
                    m_gameInfo.add(info);
                info = new GameInfo();
                info.m_file = file;
                info.m_name = new File(file).getName();
            }
            ++info.m_numberPositions;
            info.m_finalPosition = move;
            last = file;
        }
        m_gameInfo.add(info);
        m_tableFinal = new Table(m_table.getColumnTitles());
        for (int i = 0; i < m_gameInfo.size(); ++i)
        {
            info = (GameInfo)(m_gameInfo.get(i));
            String file = info.m_file;
            String finalPosition = Integer.toString(info.m_finalPosition);
            int row = TableUtils.findRow(m_table, "File", file, "Move",
                                         finalPosition);
            TableUtils.appendRow(m_tableFinal, m_table, row);
        }
    }

    private void startHtml(PrintStream out, String title)
    {
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>" + title + "</title>\n" +
                  "<meta name=\"generator\" content=\"GtpStatistics "
                  + Version.get() + "\">\n" +
                  "<style type=\"text/css\">\n" +
                  "<!--\n" +
                  ".smalltable { font-size:80%; } " +
                  ".smalltable td { background-color:" + m_colorInfo
                  + "; text-align:center;}\n" +
                  ".smalltable th { background-color:" + m_colorHeader
                  + "; }\n" +
                  ".smalltable table { border:0; cellpadding:0; }\n" +
                  "-->\n" +
                  "</style>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n");
    }

    private void startInfo(PrintStream out, String title)
    {
        out.print("<table border=\"0\" width=\"100%\" bgcolor=\""
                  + m_colorHeader + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>" + title + "</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                  + "\">\n" +
                  "<tr><td><table>\n");
    }

    private void endInfo(PrintStream out)
    {
        out.print("</td></tr></table>\n" +
                  "</table>\n" +
                  "<hr>\n");
    }

    private void writeCommandPage(int commandIndex)
        throws Exception
    {
        String command = getCommand(commandIndex);
        CommandStatistics commandStatistics
            = getCommandStatistics(commandIndex);
        String fileName = getCommandFile(commandIndex);
        PrintStream out = new PrintStream(new FileOutputStream(fileName));
        startHtml(out, command);
        startInfo(out, command);
        writeHtmlRow(out, "Command Number", commandIndex);
        endInfo(out);
        out.print("<h2>Average</h2>\n" +                  
                  "<p><img src=\"" + getAvgPlotFile(commandIndex)
                  + "\"></p>\n");
        writeCommandStatistics(out, commandIndex);
        out.print("<hr>\n" +
                  "<h2>Histograms</h2>\n");
        out.print("<table border=\"0\" cellspacing=\"0\""
                  + " cellpadding=\"5\">\n");
        out.print("<tr><td align=\"center\" valign=\"center\">"
                  + "<small>All Positions</small><br>"
                  + "<img src=\"" + getHistoFile(commandIndex)
                  + "\"></td>\n");
        out.print("<td align=\"center\" valign=\"bottom\">"
                  + "<small>Final Positions</small><br>"
                  + "<img src=\"" + getHistoFinalFile(commandIndex)
                  + "\"></td></tr>\n" +
                  "</table>\n");
        out.print("<p>\n");
        for (int i = 0; i < commandStatistics.getNumberMoveIntervals(); ++i)
        {
            Histogram histogram
                = commandStatistics.getStatistics(i).m_histogram;
            Table histoTable = TableUtils.fromHistogram(histogram, command);
            String histoFile = getHistoFile(commandIndex, i);
            Color color = getColor(command);
            Plot plot = new Plot(150, 150, color, m_precision);
            plot.setPlotStyleBars();
            plot.setYMin(0);
            plot.setXMin(commandStatistics.m_statisticsAll.getMin());
            plot.setXMax(commandStatistics.m_statisticsAll.getMax());
            plot.plot(new File(histoFile), histoTable, command, "Count",
                      null);
            String label = "Move " + (i * m_interval + 1) + "-"
                + ((i + 1) * m_interval);
            out.print("<table align=\"left\" border=\"0\">" +
                      "<tr><td align=\"center\"><small>" + label
                      + "</small><br><img src=\"" + histoFile
                      + "\"></td></tr></table>\n");
        }
        out.print("<br clear=\"left\">\n" +
                  "</p>\n" +
                  "<hr>\n");
        out.print("<h2>Games</h2>\n");
        writeGamePlots(out, commandIndex);
        out.print("<hr>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeCommandStatistics(PrintStream out, int commandIndex)
        throws Exception
    {
        CommandStatistics commandStatistics
            = getCommandStatistics(commandIndex);
        String command = getCommand(commandIndex);
        PositionStatistics statisticsAll = commandStatistics.m_statisticsAll;
        PositionStatistics finalStatistics
            = commandStatistics.m_statisticsFinal;
        out.print("<table class=\"smalltable\">\n");
        out.print("<tbody>");
        out.print("<tr>");
        out.print("<th>Move</th>");
        int numberMoveIntervals = commandStatistics.getNumberMoveIntervals();
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            out.print("<th>");
            out.print(i * m_interval + 1 + "-" + ((i + 1) * m_interval));
            out.print("</th>");
        }
        out.print("<th>Final</th>\n");
        out.print("<th>All</th>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Mean</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double mean = commandStatistics.getStatistics(i).getMean();
            out.print("<td>" + formatFloat(mean) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getMean())
                  + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getMean()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Deviation</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double err = commandStatistics.getStatistics(i).getDeviation();
            out.print("<td>" + formatFloat(err) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getDeviation())
                  + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getDeviation())
                  + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Error</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double err = commandStatistics.getStatistics(i).getError();
            out.print("<td>" + formatFloat(err) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getError())
                  + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getError())
                  + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Min</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double min = commandStatistics.getStatistics(i).getMin();
            out.print("<td>" + formatFloat(min) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getMin()) + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getMin()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Max</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double max = commandStatistics.getStatistics(i).getMax();
            out.print("<td>" + formatFloat(max) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getMax()) + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getMax()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Sum</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double max = commandStatistics.getStatistics(i).getSum();
            out.print("<td>" + formatFloat(max) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getSum()) + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getSum()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Count</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
            out.print("<td>" + commandStatistics.getStatistics(i).getCount()
                      + "</td>");
        out.print("<td>" + finalStatistics.getCount() + "</td>\n");
        out.print("<td>" + statisticsAll.getCount() + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Unknown</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
            out.print("<td>"
                      + commandStatistics.getStatistics(i).m_numberNoResult
                      + "</td>");
        out.print("<td>"
                  + commandStatistics.m_statisticsFinal.m_numberNoResult
                  + "</td>\n");
        out.print("<td>" + commandStatistics.m_statisticsAll.m_numberNoResult
                  + "</td>\n");
        out.print("</tr>\n");
        out.print("</tbody>\n");
        out.print("</table>\n");
    }

    private void writeCommandsTable(PrintStream out) throws Exception
    {
        out.print("<h2>Commands</h2>\n" +
                  "<table class=\"smalltable\">\n" +
                  "<thead><tr>"
                  + "<th>Command</th>"
                  + "<th>Mean</th>"
                  + "<th>Deviation</th>"
                  + "<th>Error</th>"
                  + "<th>Min</th>"
                  + "<th>Max</th>"
                  + "<th>Sum</th>"
                  + "<th>Count</th>"
                  + "<th>Unknown</th>"
                  + "</tr></thead>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            writeCommandPage(i);
            CommandStatistics commandStatistics = getCommandStatistics(i);
            Statistics stat = commandStatistics.m_statisticsAll.m_statistics;
            String command = getCommand(i);
            out.print("<tr>"
                      + "<td><a href=\"" + getCommandFile(i) + "\">"
                      + command + "</a></td>"
                      + "<td>" + formatFloat(stat.getMean()) + "</td>"
                      + "<td>" + formatFloat(stat.getDeviation()) + "</td>"
                      + "<td>" + formatFloat(stat.getError()) + "</td>"
                      + "<td>" + formatFloat(stat.getMin()) + "</td>"
                      + "<td>" + formatFloat(stat.getMax()) + "</td>"
                      + "<td>" + formatFloat(stat.getSum()) + "</td>"
                      + "<td>" + formatFloat(stat.getCount()) + "</td>"
                      + "<td>"
                      + commandStatistics.m_statisticsAll.m_numberNoResult
                      + "</td>"
                      + "</tr>\n");
        }
        out.print("</table>\n");
    }

    private void writeGamePage(String game, String name, int gameNumber)
        throws Exception
    {
        String file = getGameFile(gameNumber);
        PrintStream out = new PrintStream(new FileOutputStream(file));
        String title = "Game " + gameNumber + " (" + name + ")";
        startHtml(out, title);
        startInfo(out, title);
        writeHtmlRow(out, "Game Number", gameNumber);
        writeHtmlRow(out, "File",
                     "<a href=\"" + game + "\">" + game + "</a>");
        endInfo(out);
        out.print("<p>\n" +
                  "<table border=\"0\">\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            String command = getCommand(i);
            generatePlot(i, gameNumber, game);
            out.print("<tr><td align=\"center\">" + getCommandLink(i)
                      + "<br><img src=\"" + getPlotFile(gameNumber, i)
                      + "\"></td></tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n" +
                  "<hr>\n" +
                  "<p>\n" +
                  "<table class=\"smalltable\">\n" +
                  "<thead><tr>");
        for (int i = 1; i < m_table.getNumberColumns(); ++i)
            out.print("<th>" + m_table.getColumnTitle(i) + "</th>");
        out.print("</tr></thead>\n");
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            if (! m_table.get("File", i).equals(game))
                continue;
            out.print("<tr>");
            for (int j = 1; j < m_table.getNumberColumns(); ++j)
            {
                String columnTitle = m_table.getColumnTitle(j);
                out.print("<td>" + m_table.get(columnTitle, i) + "</td>");
            }
            out.print("</tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n" +
                  "<hr>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeGamePlots(PrintStream out, int commandIndex)
        throws Exception
    {
        String command = getCommand(commandIndex);
        out.print("<p>\n" +
                  "<table border=\"0\" cellpadding=\"0\""
                  + " cellspacing=\"0\">\n");
        for (int i = 0; i < m_gameInfo.size(); ++i)
        {
            GameInfo info = (GameInfo)(m_gameInfo.get(i));
            String plotFile = getPlotFile(i, commandIndex);
            out.print("<tr><td align=\"center\"><small><a href=\""
                      + getGameFile(i)
                      + "\">Game " + (i + 1) + "</a> (<a href=\""
                      + info.m_file + "\">" + info.m_name
                      + "</a>):</small><br>\n" +
                      "<img src=\"" + plotFile + "\"></td></tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n");
    }

    private void writeGameTable(PrintStream out, String fileName)
        throws Exception
    {
        out.print("<h2>Games</h2>\n" +
                  "<table class=\"smalltable\">\n" +
                  "<thead><tr><th>Game</th><th>File</th><th>Positions</th>"
                  + "</tr></thead>\n");
        for (int i = 0; i < m_gameInfo.size(); ++i)
        {
            GameInfo info = (GameInfo)(m_gameInfo.get(i));
            String file = getGameFile(i);
            out.print("<tr><td><a href=\"" + file
                      + "\">Game " + (i + 1) + "</a></td><td>" + info.m_name
                      + "</td><td>" + info.m_numberPositions
                      + "</td></tr>\n");
            writeGamePage(info.m_file, info.m_name, i);
        }
        out.print("</table>\n");
    }

    private void writeHtmlRow(PrintStream out, String label,
                              String value) throws Exception
    {
        out.print("<tr><th align=\"left\">" + label + ":</th>"
                  + "<td align=\"left\">" + value + "</td></tr>\n");
    }

    private void writeHtmlRow(PrintStream out, String label,
                              int value) throws Exception
    {
        writeHtmlRow(out, label, Integer.toString(value));
    }

    private void writeInfo(PrintStream out) throws Exception
    {
        writeTableProperty(out, "Name");
        writeTableProperty(out, "Version");
        writeTableProperty(out, "Date");
        writeTableProperty(out, "Host");
        writeTableProperty(out, "Program");
        writeTableProperty(out, "Size");
        writeTableProperty(out, "Games");
        writeHtmlRow(out, "Positions", m_table.getNumberRows());
        writeHtmlRow(out, "Interval", m_interval);
    }

    private void writeTableProperty(PrintStream out, String key)
        throws Exception
    {
        writeHtmlRow(out, key, m_table.getProperty(key, "?"));
    }
}

//----------------------------------------------------------------------------
