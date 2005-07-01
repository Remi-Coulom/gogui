//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import java.text.DecimalFormat;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.sgf.SgfReader;
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
    public Analyze(String fileName, String output, int precision,
                   int interval)
        throws Exception
    {
        m_output = output;
        m_outputName = new File(output).getName();
        m_precision = precision;
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
        File file = new File(m_output + ".html");
        initGameInfo();
        PrintStream out = new PrintStream(new FileOutputStream(file));
        startHtml(out, "Statistics Summary");
        startInfo(out, "Statistics Summary");
        writeInfo(out);
        endInfo(out);
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
        File pngFile = new File(m_output + ".count.png");
        int numberMoves = table.getNumberRows() * (m_interval + 1);
        Plot plot = generatePlotMove(getImgWidth(numberMoves),
                                     Color.DARK_GRAY);
        plot.plot(pngFile, table, "Move", "Count", null);
        out.print("<table border=\"0\">\n" +
                  "<tr><td align=\"center\">\n" +
                  "<small>Count</small><br>" +
                  "<img src=\"" + pngFile.getName() + "\">\n" +
                  "</td></tr>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            commandStatistics = computeCommandStatistics(i);
            m_commandStatistics.add(commandStatistics);
            String command = getCommand(i);
            table = commandStatistics.m_tableMoveIntervals;
            plot = generatePlotMove(getImgWidth(numberMoves),
                                    getColor(command));
            pngFile = new File(getAvgPlotFile(i, m_output));
            plot.plot(pngFile, table, "Move", "Mean", "MaxError");
            out.print("<tr><td align=\"center\">\n" +
                      getCommandLink(i) + "<br>" +
                      "<img src=\"" + getAvgPlotFile(i, m_outputName)
                      + "\">\n" + "</td></tr>\n");
        }
        out.print("</table>\n" +
                  "<hr>\n");
        out.print("<p>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            out.print("<table align=\"left\" border=\"0\">" +
                      "<tr><td align=\"center\">" + getCommandLink(i)
                      + "<br><img src=\"" + getHistoFile(i, m_outputName)
                      + "\"></td></tr></table>\n");
        }
        out.print("<br clear=\"left\">\n" +
                    "</p>\n" +
                    "<hr>\n");
        writeCommandsTable(out);
        out.print("<hr>\n");
        writeGameTable(out);
        out.print("<hr>\n");
        out.print("</body>\n" +
                  "</html>\n");
        out.close();
    }

    private static final Color[] m_plotColor = {
        Color.decode("#ff0000"),
        Color.decode("#ff9800"),
        Color.decode("#009800"),
        Color.decode("#00c0c0"),
        Color.decode("#0000ff"),
        Color.decode("#980098")
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

    private final String m_output;

    private final String m_outputName;

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

    private String getAvgPlotFile(int commandIndex, String base)
    {
        return base + ".command-" + commandIndex + ".avg.png";
    }

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private String getCommandFile(int commandIndex, String base)
    {
        return base + ".command-" + commandIndex + ".html";
    }

    private CommandStatistics getCommandStatistics(int commandIndex)
    {
        return (CommandStatistics)m_commandStatistics.get(commandIndex);
    }

    private String getGameFile(int gameIndex, String base)
    {
        return base + ".game-" + gameIndex + ".html";
    }

    private String getHistoFile(int commandIndex, String base)
    {
        return base + ".command-" + commandIndex + ".histo.png";
    }

    private String getHistoFile(int commandIndex, int moveIntervalIndex,
                                String base)
    {
        return base + ".command-" + commandIndex + ".interval-"
            + moveIntervalIndex + ".histo.png";
    }

    private String getHistoFinalFile(int commandIndex, String base)
    {
        return base + ".command-" + commandIndex + ".final.png";
    }

    private String getPlotFile(int gameIndex, int commandIndex, String base)
    {
        return base + ".game-" + gameIndex + ".command-" + commandIndex
            + ".png";
    }

    private String getCommandLink(int commandIndex)
    {
        return "<a href=\"" + getCommandFile(commandIndex, m_outputName)
            + "\"><small>" + getCommand(commandIndex) + "</small></a>";
    }

    private CommandStatistics computeCommandStatistics(int index)
        throws Exception
    {
        String command = getCommand(index);
        return new CommandStatistics(command, m_table, m_tableFinal,
                                     m_interval,
                                     getHistoFile(index, m_output),
                                     getHistoFinalFile(index, m_output),
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
        String fileName = getPlotFile(gameIndex, commandIndex, m_output);
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
        String fileName = getCommandFile(commandIndex, m_output);
        PrintStream out = new PrintStream(new FileOutputStream(fileName));
        startHtml(out, command);
        startInfo(out, command);
        writeHtmlRow(out, "Index", commandIndex);
        endInfo(out);
        out.print("<p><img src=\""
                  + getAvgPlotFile(commandIndex, m_outputName)
                  + "\"></p>\n");
        writeCommandStatistics(out, commandIndex);
        out.print("<hr>\n");
        out.print("<table border=\"0\" cellspacing=\"0\""
                  + " cellpadding=\"5\">\n");
        out.print("<tr><td align=\"center\" valign=\"center\">"
                  + "<small>All</small><br>"
                  + "<img src=\"" + getHistoFile(commandIndex, m_outputName)
                  + "\"></td>\n");
        out.print("<td align=\"center\" valign=\"bottom\">"
                  + "<small>Final</small><br>"
                  + "<img src=\""
                  + getHistoFinalFile(commandIndex, m_outputName)
                  + "\"></td></tr>\n" +
                  "</table>\n");
        out.print("<p>\n");
        for (int i = 0; i < commandStatistics.getNumberMoveIntervals(); ++i)
        {
            Histogram histogram
                = commandStatistics.getStatistics(i).m_histogram;
            Table histoTable = TableUtils.fromHistogram(histogram, command);
            String histoFile = getHistoFile(commandIndex, i, m_output);
            Color color = getColor(command);
            Plot plot = new Plot(150, 150, color, m_precision);
            commandStatistics.setHistogramProperties(plot);
            plot.plot(new File(histoFile), histoTable, command, "Count",
                      null);
            String label = (i * m_interval + 1) + "-"
                + ((i + 1) * m_interval);
            out.print("<table align=\"left\" border=\"0\">" +
                      "<tr><td align=\"center\"><small>" + label
                      + "</small><br><img src=\""
                      + getHistoFile(commandIndex, i, m_outputName)
                      + "\"></td></tr></table>\n");
        }
        out.print("<br clear=\"left\">\n" +
                  "</p>\n" +
                  "<hr>\n");
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
        out.print("<th>MinError</th>");
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
        out.print("<th>MaxError</th>");
        for (int i = 0; i < numberMoveIntervals; ++i)
        {
            double err = commandStatistics.getStatistics(i).getMaxError();
            out.print("<td>" + formatFloat(err) + "</td>");
        }
        out.print("<td>" + formatFloat(finalStatistics.getMaxError())
                  + "</td>\n");
        out.print("<td>" + formatFloat(statisticsAll.getMaxError())
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
        out.print("<table class=\"smalltable\">\n" +
                  "<thead><tr>"
                  + "<th>Command</th>"
                  + "<th>Mean</th>"
                  + "<th>Deviation</th>"
                  + "<th>MinError</th>"
                  + "<th>MaxError</th>"
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
            PositionStatistics statisticsAll
                = commandStatistics.m_statisticsAll;
            Statistics stat = statisticsAll.m_statistics;
            String command = getCommand(i);
            out.print("<tr>"
                      + "<td><a href=\"" + getCommandFile(i, m_outputName)
                      + "\">" + command + "</a></td>"
                      + "<td>" + formatFloat(stat.getMean()) + "</td>"
                      + "<td>" + formatFloat(stat.getDeviation()) + "</td>"
                      + "<td>" + formatFloat(stat.getError()) + "</td>"
                      + "<td>" + formatFloat(statisticsAll.getMaxError())
                      + "</td>"
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
        String file = getGameFile(gameNumber, m_output);
        PrintStream out = new PrintStream(new FileOutputStream(file));
        String title = "Game " + gameNumber + " (" + name + ")";
        startHtml(out, title);
        startInfo(out, title);
        writeHtmlRow(out, "Index", gameNumber);
        writeHtmlRow(out, "File",
                     "<a href=\"" + game + "\">" + game + "</a>");
        try
        {
            InputStream in = new FileInputStream(new File(game));
            SgfReader reader = new SgfReader(in, game, null, 0);
            GameInformation info = reader.getGameTree().getGameInformation();
            String playerBlack = info.m_playerBlack;
            if (playerBlack == null)
                playerBlack = "?";
            String playerWhite = info.m_playerWhite;
            if (playerWhite == null)
                playerWhite = "?";
            String result = info.m_result;
            if (result == null)
                result = "?";
            writeHtmlRow(out, "Black", playerBlack);
            writeHtmlRow(out, "White", playerWhite);
            writeHtmlRow(out, "Result", result);
            in.close();
        }
        catch (Exception e)
        {
        }
        endInfo(out);
        out.print("<p>\n" +
                  "<table border=\"0\">\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            String command = getCommand(i);
            generatePlot(i, gameNumber, game);
            out.print("<tr><td align=\"center\">" + getCommandLink(i)
                      + "<br><img src=\""
                      + getPlotFile(gameNumber, i, m_outputName)
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
            String plotFile = getPlotFile(i, commandIndex, m_outputName);
            out.print("<tr><td align=\"center\"><small><a href=\""
                      + getGameFile(i, m_outputName)
                      + "\">Game " + (i + 1) + "</a> (<a href=\""
                      + info.m_file + "\">" + info.m_name
                      + "</a>):</small><br>\n" +
                      "<img src=\"" + plotFile + "\"></td></tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n");
    }

    private void writeGameTable(PrintStream out)
        throws Exception
    {
        out.print("<table class=\"smalltable\">\n" +
                  "<thead><tr><th>Game</th><th>File</th><th>Positions</th>"
                  + "</tr></thead>\n");
        for (int i = 0; i < m_gameInfo.size(); ++i)
        {
            GameInfo info = (GameInfo)(m_gameInfo.get(i));
            String file = getGameFile(i, m_outputName);
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
