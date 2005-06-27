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
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class Analyze
{
    public Analyze(String fileName, int precision, int interval)
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
        int boardSize;
        try
        {
            boardSize = Integer.parseInt(m_table.getProperty("Size", ""));
        }
        catch (NumberFormatException e)
        {
            boardSize = 19;
        }
        m_commands = new Vector();
        for (int i = 2; i < m_table.getNumberColumns(); ++i)
            m_commands.add(m_table.getColumnTitle(i));
        m_commandResults = new Vector(m_commands.size());
        File file = new File(FileUtils.replaceExtension(fileName,
                                                        "dat", "html"));
        m_out = new PrintStream(new FileOutputStream(file));
        startHtml(m_out, "GtpStatistics Summary");
        startInfo(m_out, "GtpStatistics Summary");
        writeInfo();
        endInfo(m_out);
        m_out.print("<h2>Averages</h2>\n");
        Vector columnTitles = new Vector();
        columnTitles.add("Move");
        columnTitles.add("Count");
        Table table = new Table(columnTitles);
        CommandResult commandResult = computeCommandResult(0);
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            table.startRow();
            table.set("Move", i * interval + interval / 2);
            table.set("Count",
                      commandResult.m_statisticsAtMove[i].getCount());
        }
        String extension = "count.png";
        File pngFile
            = new File(FileUtils.replaceExtension(fileName, "dat",
                                                  extension));
        int numberMoves = table.getNumberRows() * (m_interval + 1);
        Plot plot = new Plot(getImgWidth(numberMoves), m_imgHeight,
                             Color.DARK_GRAY, m_precision);
        plot.setSolidLineInterval(10);
        plot.setXTics(5);
        plot.plot(pngFile, table, "Move", "Count", null);
        m_out.print("<p>\n" +
                    "<img src=\"" + pngFile.toString() + "\">\n" +
                    "</p>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            commandResult = computeCommandResult(i);
            m_commandResults.add(commandResult);
            String command = getCommand(i);
            pngFile = new File(getAvgPlotFile(i));
            table = commandResult.m_table;
            plot = new Plot(getImgWidth(numberMoves), m_imgHeight,
                            getColor(command), m_precision);
            plot.setSolidLineInterval(10);
            plot.setXTics(5);
            plot.plot(pngFile, table, "Move", command, "Error");
            m_out.print("<p>\n" +
                        "<img src=\"" + pngFile.toString() + "\">\n" +
                        "</p>\n");
        }
        m_out.print("<hr>\n");
        m_out.print("<h2>Histograms</h2>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            m_out.print("<img src=\"" + getHistoFile(i) + "\">\n");
        }
        m_out.print("<hr>\n");
        writeCommandsTable();
        m_out.print("<hr>\n");
        writeGameTable(fileName);
        m_out.print("<hr>\n");
        m_out.print("</body>\n" +
                    "</html>\n");
        m_out.close();
    }

    private static final class CommandResult
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
                int intervalIndex
                    = Integer.parseInt(table.get("Move", i)) / interval;
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

    private static final Color[] m_plotColor = {
        Color.decode("#ff5454"),
        Color.decode("#738ab8"),
        Color.decode("#5eaf5e"),
        Color.decode("#ffa954")
    };

    private int m_imgHeight = 130;

    private int m_interval;

    private int m_numberGames;

    private int m_precision;

    private DecimalFormat m_formatInt;

    private DecimalFormat m_formatFloat;

    private PrintStream m_out;

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private final String m_fileName;

    private Table m_table;

    private Vector m_commandResults;

    private Vector m_commands;

    private String formatFloat(double value)
    {
        return m_formatFloat.format(value);
    }

    private String formatInt(double value)
    {
        return m_formatInt.format(value);
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

    private CommandResult getCommandResult(int commandIndex)
    {
        return (CommandResult)m_commandResults.get(commandIndex);
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

    private String getPlotFile(int gameIndex, int commandIndex)
    {
        String extension =
            "game-" + gameIndex + ".command-" + commandIndex + ".png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private String getAvgPlotFile(int commandIndex)
    {
        String extension = "command-" + commandIndex + ".avg.png";
        return FileUtils.replaceExtension(m_fileName, "dat", extension);
    }

    private CommandResult computeCommandResult(int index) throws Exception
    {
        String command = getCommand(index);
        return new CommandResult(command, m_table, m_interval,
                                 getHistoFile(index),
                                 getColor(command), m_precision);
    }

    private void generatePlot(int commandIndex, int gameIndex,
                              String gameFile) throws Exception
    {
        String command = getCommand(commandIndex);
        Table table = TableUtils.select(m_table, "File", gameFile,
                                        "Move", command);
        int numberPositions = table.getNumberRows();
        String fileName = getPlotFile(gameIndex, commandIndex);
        Plot plot = new Plot(getImgWidth(numberPositions),
                             m_imgHeight, getColor(command), m_precision);
        plot.setSolidLineInterval(10);
        plot.setXTics(5);
        plot.plot(new File(fileName), table, "Move", command, null);
    }

    private Color getColor(String command)
    {
        int i = 0;
        for (i = 2; i < m_table.getNumberColumns(); ++i)
            if (m_table.getColumnTitle(i).equals(command))
                break;
        i = (i - 2) % m_plotColor.length;
        return m_plotColor[i];
    }

    private int getImgWidth(int numberMoves)
    {
        return Math.max(10, Math.min(numberMoves * 9, 1000));
    }

    private void startHtml(PrintStream out, String title)
    {
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Statistics</title>\n" +
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
        String fileName = getCommandFile(commandIndex);
        PrintStream out = new PrintStream(new FileOutputStream(fileName));
        startHtml(out, "Command " + command);
        startInfo(out, "Command " + command);
        writeHtmlRow(out, "Command Number", commandIndex);
        writeHtmlRow(out, "Command", command);
        endInfo(out);
        out.print("<h2>Average</h2>\n" +                  
                  "<p><img src=\"" + getAvgPlotFile(commandIndex)
                  + "\"></p>\n");
        writeCommandResult(out, commandIndex);
        out.print("<hr>\n" +
                  "<h2>Histogram</h2>\n");
        out.print("<p><img src=\"" + getHistoFile(commandIndex)
                  + "\"></p>\n");
        out.print("<hr>\n" +
                  "<h2>Games</h2>\n");
        writeGamePlots(out, commandIndex);
        out.print("<hr>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeCommandResult(PrintStream out, int commandIndex)
        throws Exception
    {
        CommandResult commandResult = getCommandResult(commandIndex);
        String command = getCommand(commandIndex);
        Statistics statistics = commandResult.m_statistics;
        out.print("<table class=\"smalltable\">\n");
        out.print("<tbody>");
        out.print("<tr>");
        out.print("<th>Move</th>");
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            out.print("<th>");
            if (i >= commandResult.m_numberElements)
                out.print(">" + (i * m_interval));
            else
            {
                out.print(i * m_interval);
                if (m_interval > 1)
                    out.print("-" + ((i + 1) * m_interval - 1));
            }
            out.print("</th>");
        }
        out.print("<th>All</th>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Mean</th>");
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            double mean = commandResult.m_statisticsAtMove[i].getMean();
            out.print("<td>" + formatFloat(mean) + "</td>");
        }
        out.print("<td>" + formatFloat(statistics.getMean()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Error</th>");
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            double err = commandResult.m_statisticsAtMove[i].getErrorMean();
            out.print("<td>" + formatFloat(err) + "</td>");
        }
        out.print("<td>" + formatFloat(statistics.getErrorMean())
                  + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Min</th>");
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            double min = commandResult.m_statisticsAtMove[i].getMin();
            out.print("<td>" + formatFloat(min) + "</td>");
        }
        out.print("<td>" + formatFloat(statistics.getMin()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Max</th>");
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            double max = commandResult.m_statisticsAtMove[i].getMax();
            out.print("<td>" + formatFloat(max) + "</td>");
        }
        out.print("<td>" + formatFloat(statistics.getMax()) + "</td>\n");
        out.print("</tr>\n");
        out.print("<tr>\n");
        out.print("<th>Count</th>");
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
            out.print("<td>" + commandResult.m_statisticsAtMove[i].getCount()
                      + "</td>");
        out.print("<td>" + statistics.getCount() + "</td>\n");
        out.print("</tr>\n");
        out.print("</tbody>\n");
        out.print("</table>\n");
    }

    private void writeCommandsTable() throws Exception
    {
        m_out.print("<h2>Commands</h2>\n" +
                    "<table class=\"smalltable\">\n" +
                    "<thead><tr>"
                    + "<th>Command</th>"
                    + "<th>Mean</th>"
                    + "<th>Error</th>"
                    + "<th>Min</th>"
                    + "<th>Max</th>"
                    + "<th>Count</th>"
                    + "</tr></thead>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            writeCommandPage(i);
            CommandResult commandResult = getCommandResult(i);
            Statistics stat = commandResult.m_statistics;
            String command = getCommand(i);
            m_out.print("<tr>"
                        + "<td><a href=\"" + getCommandFile(i) + "\">"
                        + command + "</a></td>"
                        + "<td>" + formatFloat(stat.getMean()) + "</td>"
                        + "<td>" + formatFloat(stat.getErrorMean()) + "</td>"
                        + "<td>" + formatFloat(stat.getMin()) + "</td>"
                        + "<td>" + formatFloat(stat.getMax()) + "</td>"
                        + "<td>" + formatFloat(stat.getCount()) + "</td>"
                        + "</tr>\n");
        }
        m_out.print("</table>\n");
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
            out.print("<tr><td><img src=\"" + getPlotFile(gameNumber, i)
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
        String lastGame = null;
        int gameNumber = 1;
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            String game = m_table.get("File", i);
            if (lastGame != null && game.equals(lastGame))
                continue;
            String plotFile = getPlotFile(gameNumber, commandIndex);
            out.print("<tr><td><small><a href=\"" + getGameFile(gameNumber)
                      + "\">Game " + gameNumber + "</a> (<a href=\"" + game
                      + "\">" + (new File(game)).getName()
                      + "</a>):</small><br>\n" +
                      "<img src=\"" + plotFile + "\"></td></tr>\n");
            ++gameNumber;
            lastGame = game;
        }
        out.print("</table>\n" +
                  "</p>\n");
    }

    private void writeGameTable(String fileName) throws Exception
    {
        m_out.print("<h2>Games</h2>\n" +
                    "<table class=\"smalltable\">\n" +
                    "<thead><tr><th>Game</th><th>File</th></tr></thead>\n");
        String lastGame = null;
        int gameNumber = 1;
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            String game = m_table.get("File", i);
            if (lastGame != null && game.equals(lastGame))
                continue;
            String file = getGameFile(gameNumber);
            String name = new File(game).getName();
            m_out.print("<tr><td align=\"right\"><a href=\"" + file
                        + "\">Game " + gameNumber + "</a></td><td>" + name
                        + "</td></tr>\n");
            writeGamePage(game, name, gameNumber);
            ++gameNumber;
            lastGame = game;
        }
        m_out.print("</table>\n");
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

    private void writeInfo() throws Exception
    {
        writeTableProperty("Name");
        writeTableProperty("Version");
        writeTableProperty("Date");
        writeTableProperty("Host");
        writeTableProperty("Program");
        writeTableProperty("Size");
        writeTableProperty("Games");
        writeHtmlRow(m_out, "Positions", m_table.getNumberRows());
        writeHtmlRow(m_out, "Interval", m_interval);
    }

    private void writeTableProperty(String key) throws Exception
    {
        writeHtmlRow(m_out, key, m_table.getProperty(key, "?"));
    }
}

//----------------------------------------------------------------------------
