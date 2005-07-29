//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Vector;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.Histogram;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** Analyze the game results and produce a HTML formatted report. */
public class Analyze
{
    public Analyze(String filename, boolean force) throws Exception
    {
        File file = new File(filename);
        readFile(file);
        File htmlFile =
            new File(FileUtils.replaceExtension(file, "dat", "html"));
        File dataFile =
            new File(FileUtils.replaceExtension(file, "dat", "summary.dat"));
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

    private boolean m_hasReferee;

    private int m_duplicates;

    private int m_errors;

    private int m_games;

    private int m_gamesUsed;

    private int m_lineNumber;

    private String m_black = "Black";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private String m_white = "White";

    private String m_referee = "-";

    private String m_blackCommand = "";

    private String m_refereeCommand = "";

    private String m_whiteCommand = "";

    private String m_size = "";

    private String m_komi = "";

    private String m_date = "";

    private String m_host = "";

    private String m_openings;

    private final Vector m_entries = new Vector(128, 128);

    private final Statistics m_length = new Statistics();

    private final ResultStatistics m_statisticsBlack = new ResultStatistics();

    private final ResultStatistics m_statisticsReferee
        = new ResultStatistics();

    private final ResultStatistics m_statisticsWhite = new ResultStatistics();

    private final Statistics m_cpuBlack = new Statistics();

    private final Statistics m_cpuWhite = new Statistics();

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

    private String getCommentValue(String comment, String key)
    {
        assert(comment.startsWith(key));
        return comment.substring(key.length()).trim();
    }

    /** Get comment value and replace spaces by HTML non-breaking spaces */
    private String getCommentValueNbsp(String comment, String key)
    {
        return getCommentValue(comment, key).replaceAll(" ", "&nbsp;");
    }

    private void handleComment(String comment)
    {
        comment = comment.trim();
        if (comment.startsWith("Black:"))
            m_black = getCommentValueNbsp(comment, "Black:");
        else if (comment.startsWith("White:"))
            m_white = getCommentValueNbsp(comment, "White:");
        else if (comment.startsWith("Referee:"))
        {
            m_referee = getCommentValueNbsp(comment, "Referee:");
            m_hasReferee =
                (! m_referee.equals("") && ! m_referee.equals("-"));
        }
        else if (comment.startsWith("BlackCommand:"))
            m_blackCommand = getCommentValue(comment, "BlackCommand:");
        else if (comment.startsWith("RefereeCommand:"))
            m_refereeCommand = getCommentValue(comment, "RefereeCommand:");
        else if (comment.startsWith("WhiteCommand:"))
            m_whiteCommand = getCommentValue(comment, "WhiteCommand:");
        else if (comment.startsWith("Size:"))
            m_size = getCommentValue(comment, "Size:");
        else if (comment.startsWith("Komi:"))
            m_komi = getCommentValue(comment, "Komi:");
        else if (comment.startsWith("Openings:"))
            m_openings = getCommentValue(comment, "Openings:");
        else if (comment.startsWith("Date:"))
            m_date = getCommentValue(comment, "Date:");
        else if (comment.startsWith("Host:"))
            m_host = getCommentValue(comment, "Host:");
    }

    private void handleLine(String line) throws ErrorMessage
    {
        line = line.trim();
        if (line.startsWith("#"))
        {
            handleComment(line.substring(1));
            return;
        }
        String[] array = line.split("\\t");
        if (array.length < 10 || array.length > 11)
            throwErrorMessage("Wrong file format");
        try
        {
            int gameIndex = Integer.parseInt(array[0]);
            String resultBlack = array[1];
            String resultWhite = array[2];
            String resultReferee = array[3];
            boolean alternated = (Integer.parseInt(array[4]) != 0);
            String duplicate = array[5];
            int length = Integer.parseInt(array[6]);
            double cpuBlack = Double.parseDouble(array[7]);
            double cpuWhite = Double.parseDouble(array[8]);
            boolean error = (Integer.parseInt(array[9]) != 0);
            String errorMessage = "";
            if (array.length == 11)
                errorMessage = array[10];
            m_entries.add(new Entry(gameIndex, resultBlack, resultWhite,
                                    resultReferee, alternated, duplicate,
                                    length, cpuBlack, cpuWhite, error,
                                    errorMessage));

        }
        catch (NumberFormatException e)
        {
            throwErrorMessage("Wrong file format");
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
            }
        }
        catch (NumberFormatException e)
        {
        }
        statistics.m_unknownResult.add(hasResult ? 0 : 1);
        if (hasResult)
            statistics.m_win.add(win ? 1 : 0);
        statistics.m_unknownScore.add(hasScore ? 0 : 1);
        if (hasScore)
            statistics.m_histo.add(score);
    }

    private void readFile(File file) throws Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        m_lineNumber = 0;
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            ++m_lineNumber;
            handleLine(line);
        }
        reader.close();
    }

    private void throwErrorMessage(String message) throws ErrorMessage
    {
        throw new ErrorMessage("Line " + m_lineNumber + ": " + message);
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
        NumberFormat format = StringUtils.getNumberFormat(1);
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>" + m_black + " - " + m_white + "</title>\n" +
                  "<meta name=\"generator\" content=\"TwoGtp "
                  + Version.get() + "\">\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"#0000ee\"" +
                  " vlink=\"#551a8b\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + m_colorHeader + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>" + m_black + " - " + m_white + "</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                  + "\">\n");
        writeHtmlRow(out, "Black", m_black);
        writeHtmlRow(out, "White", m_white);
        writeHtmlRow(out, "Size", m_size);
        writeHtmlRow(out, "Komi", m_komi);
        if (m_openings != null)
            writeHtmlRow(out, "Openings", m_openings);
        writeHtmlRow(out, "Date", m_date);
        writeHtmlRow(out, "Host", m_host);
        if (m_hasReferee)
            writeHtmlRow(out, "Referee", m_referee);
        writeHtmlRow(out, "Black command", m_blackCommand);
        writeHtmlRow(out, "White command", m_whiteCommand);
        if (m_hasReferee)
            writeHtmlRow(out, "Referee command", m_refereeCommand);
        writeHtmlRow(out, "Games", m_games);
        writeHtmlRow(out, "Errors", m_errors);
        writeHtmlRow(out, "Duplicates", m_duplicates);
        writeHtmlRow(out, "Games used", m_gamesUsed);
        writeHtmlRow(out, "Game length", m_length, format);
        writeHtmlRow(out, "CpuTime Black", m_cpuBlack, format);
        writeHtmlRow(out, "CpuTime White", m_cpuWhite, format);
        out.print("</table>\n" +
                  "<hr>\n");
        if (m_hasReferee)
        {
            writeHtmlResults(out, m_referee, m_statisticsReferee);
            out.println("<hr>");
        }
        writeHtmlResults(out, m_black, m_statisticsBlack);
        out.println("<hr>");
        writeHtmlResults(out, m_white, m_statisticsWhite);
        out.println("<hr>");
        out.print("<table border=\"0\">\n" +
                  "<thead>\n" +
                  "<tr bgcolor=\"" + m_colorHeader + "\">\n" +
                  "<th>Game</th>\n");
        if (m_hasReferee)
            out.print("<th>Result [" + m_referee + "]</th>\n");
        out.print("<th>Result [" + m_black + "]</th>\n" +
                  "<th>Result [" + m_white + "]</th>\n");
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
            out.print("<tr align=\"center\" bgcolor=\"" + m_colorInfo
                      + "\"><td><a href=\"" + name + "\">" + name
                      + "</a></td>\n");
            if (m_hasReferee)
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
                  "<hr>\n");
        out.print("</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeHtmlResults(PrintStream out, String name,
                                  ResultStatistics statistics)
        throws Exception
    {
        NumberFormat format = StringUtils.getNumberFormat(1);
        out.print("<h2>Result [" + name + "]</h2>\n" +
                  "<p>\n" +
                  "<table border=\"0\">\n");
        writeHtmlRow(out, "Black score", statistics.m_histo, format);
        writeHtmlRowPercentData(out, "Black wins", statistics.m_win, format);
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
        String value =
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
        out.print("<tr><th align=\"left\">" + label + ":</th>"
                  + "<td align=\"left\">"
                  + format.format(statistics.getMean() * 100) + "% (&plusmn;"
                  + format.format(statistics.getError() * 100)
                  + ")</td></tr>\n");
    }

    private void writeData(File file) throws Exception
    {
        PrintStream out = new PrintStream(new FileOutputStream(file));
        NumberFormat format1 = StringUtils.getNumberFormat(1);
        NumberFormat format2 = StringUtils.getNumberFormat(2);
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

//----------------------------------------------------------------------------

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
    }
}

//----------------------------------------------------------------------------
