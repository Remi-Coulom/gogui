//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.text.DecimalFormat;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.Table;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class Analyze
{
    public Analyze(String fileName, int precision, int interval)
        throws Exception
    {
        m_table = new Table();
        m_table.read(new File(fileName));
        if (m_table.getNumberColumns() < 2
            || ! m_table.getColumnTitle(0).equals("File")
            || ! m_table.getColumnTitle(1).equals("Move"))
            throw new ErrorMessage("Invalid table format");
        m_commands = new Vector();
        for (int i = 2; i < m_table.getNumberColumns(); ++i)
            m_commands.add(m_table.getColumnTitle(i));
        m_precision = precision;
        m_interval = interval;
        File file = new File(FileUtils.replaceExtension(new File(fileName),
                                                        "dat", "html"));
        m_out = new PrintStream(new FileOutputStream(file));
        startHtml(m_out, "GtpStatistics Summary");
        writeInfo();
        m_out.print("</table>\n");
        for (int i = 0; i < m_commands.size(); ++i)
            writeCommandResult(getCommand(i));
        m_out.print("<hr>\n" +
                    "<h2>Games</h2>\n" +
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
                        + gameNumber + "</a></td><td>" + name
                        + "</td></tr>\n");
            writeGamePage(gameFile, game, name, gameNumber);
            ++gameNumber;
            lastGame = game;
        }
        m_out.print("</table>\n" +
                    "</body>\n" +
                    "</html>\n");
        m_out.close();
    }

    private int m_interval;

    private int m_numberGames;

    private int m_precision;

    private PrintStream m_out;

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private static final String m_colorGreen = "#5eaf5e";

    private static final String m_colorRed = "#ff5454";

    private Table m_table;

    private Vector m_commands;

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
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

    private void writeCommandResult(String command)
        throws Exception
    {
        Statistics statistics = new Statistics();
        int numberElements = 500 / m_interval;
        Statistics[] statisticsAtMove =  new Statistics[numberElements + 1];
        for (int i = 0; i < numberElements + 1; ++i)
            statisticsAtMove[i] = new Statistics();
        int maxElement = 0;
        for (int i = 0; i < m_table.getNumberRows(); ++i)
        {
            String value = m_table.get(command, i);
            if (value.equals("(null)"))
                continue;
            double doubleValue;
            try
            {
                doubleValue = Double.parseDouble(value);
                statistics.addValue(doubleValue);
            }
            catch (NumberFormatException e)
            {
                continue;
            }
            int interval
                = Integer.parseInt(m_table.get("Move", i)) / m_interval;
            int element = Math.min(interval, numberElements);
            maxElement = Math.max(maxElement, element);
            statisticsAtMove[element].addValue(doubleValue);
        }
        m_out.print("<hr>\n" +
                    "<h2>" + command + "</h2>\n");
        m_out.print("</table>\n" +
                    "</p>\n" +
                    "<table border=\"0\">\n" +
                    "<thead><tr bgcolor=\"" + m_colorHeader + "\">"
                    + "<th>Move</th><th>Number</th><th>Mean</th>"
                    + "<th>Error</th></tr></thead>\n");
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(m_precision);
        for (int i = 0; i <= maxElement; ++i)
        {
            m_out.print("<tr bgcolor=\"" + m_colorInfo + "\"><td>");
            if (i >= numberElements)
                m_out.print(">" + (i * m_interval));
            else
                m_out.print((i * m_interval) + "-"
                            + ((i + 1) * m_interval - 1));
            Statistics stat = statisticsAtMove[i];
            m_out.print("</td><td>" + stat.getCount() + "</td><td>"
                        + format.format(stat.getMean()) + "</td><td>"
                        + format.format(stat.getErrorMean())
                        + "</td></tr>\n");
        }
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
        writeTableProperty("Games");
        writeHtmlRow(m_out, "Positions", m_table.getNumberRows());
    }

    private void writeTableProperty(String key) throws Exception
    {
        writeHtmlRow(m_out, key, m_table.getProperty(key, "?"));
    }
}

//----------------------------------------------------------------------------
