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
    public Analyze(String fileName, int precision) throws Exception
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
        File file = new File(FileUtils.replaceExtension(new File(fileName),
                                                        "dat", "html"));
        m_out = new PrintStream(new FileOutputStream(file));
        m_out.print("<html>\n" +
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
                    "<h1>Statistics</h1>\n" +
                    "</td></tr>\n" +
                    "</table>\n" +
                    "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                    + "\">\n");
        writeInfo();
        m_out.print("</table>\n");
        writeCommandResult("reg_genmove");
        for (int i = 0; i < m_commands.size(); ++i)
            writeCommandResult(getCommand(i));
        m_out.print("</body>\n" +
                    "</html>\n");
        m_out.close();
    }

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

    private void writeCommandResult(String command)
        throws Exception
    {
        Statistics statistics = new Statistics();
        final int intervalSize = 25;
        int numberElements = 10;
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
                = Integer.parseInt(m_table.get("Move", i)) / intervalSize;
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
                m_out.print(">" + (i * intervalSize));
            else
                m_out.print((i * intervalSize) + "-"
                          + ((i + 1) * intervalSize - 1));
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

    private void writeHtmlRow(String label, String value) throws Exception
    {
        m_out.print("<tr><th align=\"left\">" + label + ":</th>"
                    + "<td align=\"left\">" + value + "</td></tr>\n");
    }

    private void writeInfo()
    {
        writeProperty("Name");
        writeProperty("Version");
        writeProperty("Date");
        writeProperty("Host");
        writeProperty("Program");
        writeProperty("Games");
        m_out.print("<tr><th align=\"left\">Positions:</th><td>"
                    + m_table.getNumberRows() + "</td></tr>\n");
    }

    private void writeProperty(String name)
    {
        m_out.print("<tr><th align=\"left\">" + name + ":</th><td>"
                    + m_table.getProperty(name, "?") + "</td></tr>\n");
    }
}

//----------------------------------------------------------------------------
