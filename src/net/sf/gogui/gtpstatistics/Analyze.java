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
        writeInfo();
        m_out.print("</table>\n" +
                    "<hr>\n" +
                    "<h2>Interval Averages</h2>\n");
        Vector columnTitles = new Vector();
        columnTitles.add("Move");
        columnTitles.add("Count");
        Table table = new Table(columnTitles);
        CommandResult commandResult = getCommandResult(0);
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            table.startRow();
            table.set("Move", i * interval + interval / 2);
            table.set("Count",
                      commandResult.m_statisticsAtMove[i].getCount());
        }
        String extension = "number.png";
        File jpgFile
            = new File(FileUtils.replaceExtension(fileName, "dat",
                                                  extension));
        int numberMoves = table.getNumberRows() * (m_interval + 1);
        new Plot(jpgFile, table, "Count", null,
                 getImgWidth(numberMoves), m_imgHeight,
                 Color.DARK_GRAY);
        m_out.print("<p>\n" +
                    "<img src=\"" + jpgFile.toString() + "\">\n" +
                    "</p>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            commandResult = getCommandResult(i);
            m_commandResults.add(commandResult);
            String command = getCommand(i);
            extension = command + ".png";
            jpgFile
                = new File(FileUtils.replaceExtension(fileName, "dat",
                                                      extension));
            table = commandResult.m_table;
            new Plot(jpgFile, table, command, "Error",
                     getImgWidth(numberMoves), m_imgHeight,
                     getColor(command));
            m_out.print("<p>\n" +
                        "<img src=\"" + jpgFile.toString() + "\">\n" +
                        "</p>\n");
        }
        m_out.print("<hr>\n");
        writeGameTable(fileName);
        m_out.print("<hr>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            writeCommandResult(i, fileName);
            writeGamePlots(i, fileName);
            m_out.print("<hr>\n");
        }
        m_out.print("</body>\n" +
                    "</html>\n");
        m_out.close();
    }

    private static final class CommandResult
    {
        public final int m_maxElement;

        public final int m_numberElements;

        public final Statistics m_statistics;

        public final Statistics[] m_statisticsAtMove;

        public final Table m_table;

        public CommandResult(String command, Table table, int interval)
        {
            m_statistics = new Statistics();
            m_numberElements = 500 / interval;
            m_statisticsAtMove =  new Statistics[m_numberElements + 1];
            for (int i = 0; i < m_numberElements + 1; ++i)
                m_statisticsAtMove[i] = new Statistics();
            int maxElement = 0;
            for (int i = 0; i < table.getNumberRows(); ++i)
            {
                String value = table.get(command, i);
                if (value.equals("(null)"))
                    continue;
                double doubleValue;
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

    private PrintStream m_out;

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private Table m_table;

    private Vector m_commandResults;

    private Vector m_commands;

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private CommandResult getCommandResult(int index)
    {
        return new CommandResult(getCommand(index), m_table, m_interval);
    }

    private void generatePlot(String columnTitle, String fileName,
                              String gameFile) throws Exception
    {
        Table table = TableUtils.select(m_table, "File", gameFile,
                                        "Move", columnTitle);
        int numberPositions = table.getNumberRows();
        new Plot(new File(fileName), table, columnTitle, null,
                 getImgWidth(numberPositions), m_imgHeight,
                 getColor(columnTitle));
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
        return Math.min(numberMoves * 9, 1150);
    }

    private void startHtml(PrintStream out, String title)
    {
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Statistics</title>\n" +
                  "<meta name=\"generator\" content=\"GtpStatistics "
                  + Version.get() + "\">\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + m_colorHeader + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>" + title + "</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                  + "\">\n");
    }

    private void writeCommandResult(int index, String fileName)
        throws Exception
    {
        CommandResult commandResult
            = (CommandResult)m_commandResults.get(index);
        String command = getCommand(index);
        m_out.print("<h2>" + command + "</h2>\n");
        m_out.print("</table>\n" +
                    "</p>\n" +
                    "<table border=\"0\">\n" +
                    "<thead><tr bgcolor=\"" + m_colorHeader + "\">"
                    + "<th>Move</th><th>Number</th><th>Mean</th>"
                    + "<th>Error</th></tr></thead>\n");
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(m_precision);
        for (int i = 0; i <= commandResult.m_maxElement; ++i)
        {
            m_out.print("<tr bgcolor=\"" + m_colorInfo + "\"><td>");
            if (i >= commandResult.m_numberElements)
                m_out.print(">" + (i * m_interval));
            else
            {
                m_out.print(i * m_interval);
                if (m_interval > 1)
                    m_out.print("-" + ((i + 1) * m_interval - 1));
            }
            Statistics stat = commandResult.m_statisticsAtMove[i];
            m_out.print("</td><td>" + stat.getCount() + "</td><td>"
                        + format.format(stat.getMean()) + "</td><td>"
                        + format.format(stat.getErrorMean())
                        + "</td></tr>\n");
        }
        Statistics statistics = commandResult.m_statistics;
        m_out.print("<tfoot><tr bgcolor=\"" + m_colorHeader + "\">"
                    + "<td>All</td><td>" + statistics.getCount() + "</td>"
                    + "<td>" + format.format(statistics.getMean()) + "</td>"
                    + "<td>" + format.format(statistics.getErrorMean())
                    + "</td>");
        
        m_out.print("</table>\n");
    }

    private void writeGamePage(String fileName, String gameFile, String name,
                               int gameNumber)
        throws Exception
    {
        PrintStream out = new PrintStream(new FileOutputStream(fileName));
        startHtml(out, name);
        writeHtmlRow(out, "File", "<a href=\"" + gameFile + "\">" + gameFile
                     + "</a>");
        writeHtmlRow(out, "Number", gameNumber);
        out.print("</table>\n" +
                  "<hr>\n" +
                  "<p>\n" +
                  "<table border=\"0\">\n");
        for (int i = 2; i < m_table.getNumberColumns(); ++i)
        {
            String title = m_table.getColumnTitle(i);
            String extension = title + ".jpg";
            String jpgFile = FileUtils.replaceExtension(new File(fileName),
                                                        "html", extension);
            generatePlot(title, jpgFile, gameFile);
            out.print("<tr><td><img src=\"" + jpgFile + "\"></td></tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n" +
                  "<hr>\n" +
                  "<p>\n" +
                  "<table border=\"0\" cellpadding=\"0\">\n" +
                  "<thead><tr bgcolor=\"" + m_colorHeader + "\">");
        for (int i = 1; i < m_table.getNumberColumns(); ++i)
            out.print("<th><small>" + m_table.getColumnTitle(i)
                      + "</small></th>");
        out.print("</tr></thead>\n");
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            if (! m_table.get("File", i).equals(gameFile))
                continue;
            out.print("<tr bgcolor=\"" + m_colorInfo + "\">");
            for (int j = 1; j < m_table.getNumberColumns(); ++j)
            {
                String columnTitle = m_table.getColumnTitle(j);
                out.print("<td><small>" + m_table.get(columnTitle, i)
                          + "</small></td>");
            }
            out.print("</tr></thead>\n");
        }
        out.print("</table>\n" +
                  "</p>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeGamePlots(int index, String fileName) throws Exception
    {
        String command = getCommand(index);
        m_out.print("<p>\n");
        String lastGame = null;
        int gameNumber = 1;
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            String game = m_table.get("File", i);
            if (lastGame != null && game.equals(lastGame))
                continue;
            String extension = gameNumber + "." + command + ".jpg";
            String jpgFile = FileUtils.replaceExtension(new File(fileName),
                                                        "dat", extension);
            m_out.print("<p>" + game + "<br><img src=\"" + jpgFile
                        + "\"></p>\n");
            ++gameNumber;
            lastGame = game;
        }
        m_out.print("</p>\n");
    }

    private void writeGameTable(String fileName) throws Exception
    {
        m_out.print("<h2>Games</h2>\n" +
                    "<table border=\"0\">\n" +
                    "<thead><tr bgcolor=\"" + m_colorHeader + "\">"
                    + "<th>Number</th><th>File</th></tr></thead>\n");
        String lastGame = null;
        int gameNumber = 1;
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            String game = m_table.get("File", i);
            if (lastGame != null && game.equals(lastGame))
                continue;
            String extension = gameNumber + ".html";
            String gameFile = FileUtils.replaceExtension(new File(fileName),
                                                         "dat", extension);
            String name = new File(game).getName();
            m_out.print("<tr bgcolor=\"" + m_colorInfo + "\">"
                        + "<td align=\"right\"><a href=\"" + gameFile + "\">"
                        + gameNumber + "</a></td><td><a href=\"" + gameFile
                        + "\">" + name + "</a></td></tr>\n");
            writeGamePage(gameFile, game, name, gameNumber);
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
