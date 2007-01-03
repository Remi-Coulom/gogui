//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.Histogram;
import net.sf.gogui.util.Statistics;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.util.Table;
import net.sf.gogui.version.Version;

/** Analyze the game results and produce a HTML formatted report. */
public class Analyze
{
    public Analyze(String filename, boolean force) throws Exception
    {
        File file = new File(filename);
        readTable(file);
        File htmlFile =
            new File(FileUtil.replaceExtension(file, "dat", "html"));
        File dataFile =
            new File(FileUtil.replaceExtension(file, "dat", "summary.dat"));
        if (! force)
        {
            if (htmlFile.exists())
                throw new ErrorMessage("File " + htmlFile + " exists");
            if (dataFile.exists())
                throw new ErrorMessage("File " + dataFile + " exists");
        }
        calcStatistics();
        writeHtml(htmlFile);
        writeData(dataFile);
    }

    private static final class ResultStatistics
    {
        public Statistics m_unknownResult = new Statistics();

        public Statistics m_unknownScore = new Statistics();

        public Statistics m_win = new Statistics();

        public Histogram m_histo = new Histogram(-400, 400, 10);
    }

    private int m_duplicates;

    private int m_errors;

    private int m_games;

    private int m_gamesUsed;

    private static final String COLOR_HEADER = "#91aee8";

    private static final String COLOR_INFO = "#e0e0e0";

    private final ArrayList m_entries = new ArrayList(128);

    private final Statistics m_length = new Statistics();

    private final ResultStatistics m_statisticsBlack = new ResultStatistics();

    private final ResultStatistics m_statisticsReferee
        = new ResultStatistics();

    private final ResultStatistics m_statisticsWhite = new ResultStatistics();

    private final Statistics m_cpuBlack = new Statistics();

    private final Statistics m_cpuWhite = new Statistics();

    private Table m_table;

    private void calcStatistics()
    {
        for (int i = 0; i < m_entries.size(); ++i)
        {
            Entry e = (Entry)m_entries.get(i);
            ++m_games;
            if (e.m_error)
            {
                ++m_errors;
                continue;
            }
            if (! e.m_duplicate.equals("") && ! e.m_duplicate.equals("-"))
            {
                ++m_duplicates;
                continue;
            }
            ++m_gamesUsed;
            parseResult(e.m_resultBlack, m_statisticsBlack);
            parseResult(e.m_resultWhite, m_statisticsWhite);
            parseResult(e.m_resultReferee, m_statisticsReferee);
            m_cpuBlack.add(e.m_cpuBlack);
            m_cpuWhite.add(e.m_cpuWhite);
            m_length.add(e.m_length);
        }
    }

    private void parseResult(String result, ResultStatistics statistics)
    {
        boolean hasResult = false;
        boolean hasScore = false;
        boolean win = false;
        double score = 0f;
        String s = result.trim();
        try
        {
            if (! s.equals("?"))
            {
                if (s.indexOf("B+") >= 0)
                {
                    hasResult = true;
                    win = true;
                    score = Double.parseDouble(s.substring(2));
                    hasScore = true;
                }
                else if (s.indexOf("W+") >= 0)
                {
                    hasResult = true;
                    win = false;
                    score = -Double.parseDouble(s.substring(2));
                    hasScore = true;
                }
                else if (! s.equals(""))
                    System.err.println("Ignored invalid score: " + result);
            }
        }
        catch (NumberFormatException e)
        {
            System.err.println("Ignored invalid score: " + result);
        }
        if (score < statistics.m_histo.getHistoMin()
            || score > statistics.m_histo.getHistoMax())
        {
            System.err.println("Ignored invalid score: " + result);
            hasScore = false;
        }
        statistics.m_unknownResult.add(hasResult ? 0 : 1);
        if (hasResult)
            statistics.m_win.add(win ? 1 : 0);
        statistics.m_unknownScore.add(hasScore ? 0 : 1);
        if (hasScore)
            statistics.m_histo.add(score);
    }

    private void readTable(File file) throws Exception
    {
        m_table = new Table();
        m_table.read(file);
        try
        {
            for (int i = 0; i < m_table.getNumberRows(); ++i)
            {            
                int gameIndex = m_table.getInt("GAME", i);
                String resultBlack = m_table.get("RES_B", i);
                String resultWhite = m_table.get("RES_W", i);
                String resultReferee = m_table.get("RES_R", i);
                boolean alternated = (m_table.getInt("ALT", i) != 0);
                String duplicate = m_table.get("DUP", i);
                int length = m_table.getInt("LEN", i);
                double cpuBlack = m_table.getDouble("CPU_B", i);
                double cpuWhite = m_table.getDouble("CPU_W", i);
                boolean error = (m_table.getInt("ERR", i) != 0);
                String errorMessage = m_table.get("ERR_MSG", i);;
                m_entries.add(new Entry(gameIndex, resultBlack, resultWhite,
                                        resultReferee, alternated, duplicate,
                                        length, cpuBlack, cpuWhite, error,
                                        errorMessage));   
            }
        }
        catch (NumberFormatException e)
        {
            throw new ErrorMessage("Wrong file format");
        }
    }

    private void writeHtml(File file) throws Exception
    {
        String gamePrefix = "game";
        if (file.getName().endsWith(".html"))
        {
            String name = file.getName();
            gamePrefix = name.substring(0, name.length() - 5);;
        }
        PrintStream out = new PrintStream(new FileOutputStream(file));
        NumberFormat format = StringUtil.getNumberFormat(1);
        String charset = StringUtil.getDefaultEncoding();
        String black = m_table.getProperty("Black", "");
        String white = m_table.getProperty("White", "");
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>" + black + " - " + white + "</title>\n" +
                  "<meta http-equiv=\"Content-Type\""
                  + " content=\"text/html; charset=" + charset + "\">\n" +
                  "<meta name=\"generator\" content=\"TwoGtp "
                  + Version.get() + " (http://gogui.sf.net)\">\n" +
                    "<style type=\"text/css\">\n" +
                    "<!--\n" +
                    "body { margin:0; }\n" +
                    "-->\n" +
                    "</style>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"#0000ee\"" +
                  " vlink=\"#551a8b\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + COLOR_HEADER + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>" + black + " - " + white
                  + "</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + COLOR_INFO
                  + "\">\n");
        String referee = m_table.getProperty("Referee", null);
        if (referee.equals("-") || referee.equals(""))
            referee = null;
        writePropertyHtmlRow(out, "Black");
        writePropertyHtmlRow(out, "White");
        writePropertyHtmlRow(out, "Size");
        writePropertyHtmlRow(out, "Komi");
        if (m_table.hasProperty("Openings"))
            writePropertyHtmlRow(out, "Openings");
        writePropertyHtmlRow(out, "Date");
        writePropertyHtmlRow(out, "Host");
        writePropertyHtmlRow(out, "Referee");
        writePropertyHtmlRow(out, "BlackCommand");
        writePropertyHtmlRow(out, "WhiteCommand");
        if (referee != null)
            writePropertyHtmlRow(out, "RefereeCommand");
        writeHtmlRow(out, "Games", m_games);
        writeHtmlRow(out, "Errors", m_errors);
        writeHtmlRow(out, "Duplicates", m_duplicates);
        writeHtmlRow(out, "Games used", m_gamesUsed);
        writeHtmlRow(out, "Game length", m_length, format);
        writeHtmlRow(out, "CpuTime Black", m_cpuBlack, format);
        writeHtmlRow(out, "CpuTime White", m_cpuWhite, format);
        out.print("</table>\n" +
                  "<hr>\n");
        if (referee != null)
        {
            writeHtmlResults(out, referee, m_statisticsReferee);
            out.println("<hr>");
        }
        writeHtmlResults(out, black, m_statisticsBlack);
        out.println("<hr>");
        writeHtmlResults(out, white, m_statisticsWhite);
        out.println("<hr>");
        out.print("<table border=\"0\" width=\"100%\" cellpadding=\"0\""
                  + " cellspacing=\"1\">\n" +
                  "<thead>\n" +
                  "<tr bgcolor=\"" + COLOR_HEADER + "\">\n" +
                  "<th>Game</th>\n");
        if (referee != null)
            out.print("<th>Result [" + referee + "]</th>\n");
        out.print("<th>Result [" + black + "]</th>\n" +
                  "<th>Result [" + white + "]</th>\n");
        out.print("<th>Colors Exchanged</th>\n" +
                  "<th>Duplicate</th>\n" +
                  "<th>Length</th>\n" +
                  "<th>CpuTime Black</th>\n" +
                  "<th>CpuTime White</th>\n" +
                  "<th>Error</th>\n" +
                  "<th>Error Message</th>\n" +
                  "</tr>\n" +
                  "</thead>\n");
        for (int i = 0; i < m_entries.size(); ++i)
        {
            Entry e = (Entry)m_entries.get(i);
            String name = gamePrefix + "-" + e.m_gameIndex + ".sgf";
            out.print("<tr align=\"center\" bgcolor=\"" + COLOR_INFO
                      + "\"><td><a href=\"" + name + "\">" + name
                      + "</a></td>\n");
            if (referee != null)
                out.print("<td>" + e.m_resultReferee + "</td>");
            out.print("<td>" + e.m_resultBlack + "</td>" +
                      "<td>" + e.m_resultWhite + "</td>");
            out.print("<td>" + (e.m_alternated ? "1" : "0") + "</td>" +
                      "<td>" + e.m_duplicate + "</td>" +
                      "<td>" + e.m_length + "</td>" +
                      "<td>" + e.m_cpuBlack + "</td>" +
                      "<td>" + e.m_cpuWhite + "</td>" +
                      "<td>" + (e.m_error ? "1" : "") + "</td>" +
                      "<td>" + e.m_errorMessage + "</td>" +
                      "</tr>\n");
        }
        out.print("</table>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeHtmlResults(PrintStream out, String name,
                                  ResultStatistics statistics)
        throws Exception
    {
        NumberFormat format = StringUtil.getNumberFormat(1);
        out.print("<div style=\"margin:1em\">\n" +
                  "<h2>Result [" + name + "]</h2>\n" +
                  "<p>\n" +
                  "<table border=\"0\">\n");
        if (statistics.m_histo.getCount() > 0)
            writeHtmlRow(out, "Black score", statistics.m_histo, format);
        if (statistics.m_win.getCount() > 0)
            writeHtmlRowPercentData(out, "Black wins", statistics.m_win,
                                    format);
        out.print("<tr><th align=\"left\">Unknown Result"
                  + ":</th><td align=\"left\">"
                  + format.format(statistics.m_unknownResult.getMean() * 100)
                  + "%" + "</td></tr>\n" +
                  "<tr><th align=\"left\">Unknown Score"
                  + ":</th><td align=\"left\">"
                  + format.format(statistics.m_unknownScore.getMean() * 100)
                  + "%" + "</td></tr>\n" +
                  "</table>\n" +
                  "</p>\n");
        statistics.m_histo.printHtml(out);
        out.print("</div>\n");
    }

    private void writePropertyHtmlRow(PrintStream out, String key)
        throws Exception
    {
        String value = m_table.getProperty(key, "");
        writeHtmlRow(out, key, value);
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

    private void writeHtmlRow(PrintStream out, String label,
                              Statistics statistics,
                              NumberFormat format) throws Exception
    {
        String value;
        if (statistics.getCount() == 0)
            value = "";
        else
            value =
                format.format(statistics.getMean()) + " (&plusmn;"
                + format.format(statistics.getError())
                + ") <small>min=" + format.format(statistics.getMin())
                + " max=" + format.format(statistics.getMax())
                + " deviation=" + format.format(statistics.getDeviation())
                + "</small>";
        writeHtmlRow(out, label, value);
    }

    private void writeHtmlRowPercentData(PrintStream out, String label,
                                         Statistics statistics,
                                         NumberFormat format) throws Exception
    {
        String value;
        if (statistics.getCount() == 0)
            value = "";
        else
            value =
                format.format(statistics.getMean() * 100) + "% (&plusmn;"
                + format.format(statistics.getError() * 100) + ")";
        writeHtmlRow(out, label, value);
    }

    private void writeData(File file) throws Exception
    {
        PrintStream out = new PrintStream(new FileOutputStream(file));
        NumberFormat format1 = StringUtil.getNumberFormat(1);
        NumberFormat format2 = StringUtil.getNumberFormat(2);
        Histogram histoBlack = m_statisticsBlack.m_histo;
        Histogram histoWhite = m_statisticsWhite.m_histo;
        Histogram histoReferee = m_statisticsReferee.m_histo;
        Statistics winBlack = m_statisticsBlack.m_win;
        Statistics winWhite = m_statisticsWhite.m_win;
        Statistics winReferee = m_statisticsReferee.m_win;
        Statistics unknownBlack = m_statisticsBlack.m_unknownScore;
        Statistics unknownWhite = m_statisticsWhite.m_unknownScore;
        Statistics unknownReferee = m_statisticsReferee.m_unknownScore;
        out.print("#GAMES\tERR\tDUP\tUSED\tRES_B\tERR_B\tWIN_B\tERRW_B\t"
                  + "UNKN_B\tRES_W\tERR_W\tWIN_W\tERRW_W\tUNKN_W\t"
                  + "RES_R\tERR_R\tWIN_R\tERRW_R\tUNKN_R\n" +
                  m_games + "\t" + m_errors + "\t" + m_duplicates + "\t"
                  + m_gamesUsed
                  + "\t" + format1.format(histoBlack.getMean())
                  + "\t" + format1.format(histoBlack.getError())
                  + "\t" + format2.format(winBlack.getMean())
                  + "\t" + format2.format(winBlack.getError())
                  + "\t" + format2.format(unknownBlack.getMean())
                  + "\t" + format1.format(histoWhite.getMean())
                  + "\t" + format1.format(histoWhite.getError())
                  + "\t" + format2.format(winWhite.getMean())
                  + "\t" + format2.format(winWhite.getError())
                  + "\t" + format2.format(unknownWhite.getMean())
                  + "\t" + format1.format(histoReferee.getMean())
                  + "\t" + format1.format(histoReferee.getError())
                  + "\t" + format2.format(winReferee.getMean())
                  + "\t" + format2.format(winReferee.getError())
                  + "\t" + format2.format(unknownReferee.getMean())
                  + "\n");
        out.close();
    }
}

final class Entry
{
    public int m_gameIndex;

    public String m_resultBlack;

    public String m_resultReferee;

    public String m_resultWhite;

    public boolean m_alternated;

    public String m_duplicate;

    public int m_length;

    public double m_cpuBlack;

    public double m_cpuWhite;

    public boolean m_error;

    public String m_errorMessage;

    public Entry(int gameIndex, String resultBlack, String resultWhite,
                 String resultReferee, boolean alternated, String duplicate,
                 int length, double cpuBlack, double cpuWhite, boolean error,
                 String errorMessage)
    {
        m_gameIndex = gameIndex;
        m_resultBlack = resultBlack;
        m_resultWhite = resultWhite;
        m_resultReferee = resultReferee;
        m_alternated = alternated;
        m_duplicate = (duplicate.equals("-") ? "" : duplicate);
        m_length = length;
        m_cpuBlack = cpuBlack;
        m_cpuWhite = cpuWhite;
        m_error = error;
        m_errorMessage = errorMessage;
        if (m_errorMessage == null)
            m_errorMessage = "";
    }
}

