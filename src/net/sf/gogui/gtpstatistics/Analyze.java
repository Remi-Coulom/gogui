//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.utils.Table;
import net.sf.gogui.utils.TableUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class Analyze
{
    public Analyze(String fileName, String output, int precision)
        throws Exception
    {
        if (output.equals(""))
            m_output = FileUtils.removeExtension(new File(fileName), "dat");
        else if (new File(output).isDirectory())
        {
            File name = new File((new File(fileName)).getName());
            m_output = output + File.separator
                + FileUtils.removeExtension(name, "dat");
        }
        else
            m_output = output;
        m_precision = precision;
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
        findGameGlobalCommands();
        PrintStream out = new PrintStream(new FileOutputStream(file));
        startHtml(out, "Statistics Summary");
        startInfo(out, "Statistics Summary");
        writeInfo(out);
        endInfo(out);
        out.print("<table border=\"0\">\n" +
                  "<tr><td>\n");
        writePlot(out, "<small>positions</small>",
                  getCountFile().getName(),
                  "<a href=\"" + getCountDataFile().getName()
                  + "\"><small>data</small></a>");
        out.print("</td></tr>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            CommandStatistics commandStatistics = computeCommandStatistics(i);
            m_commandStatistics.add(commandStatistics);
            if (commandStatistics.getCount() > 0
                && ! commandStatistics.m_isBeginCommand)
            {
                String command = getCommand(i);
                Table table = commandStatistics.m_tableAtMove;
                Plot plot = generatePlotMove(getImgWidth(m_maxMove),
                                             getColor(command));
                plot.setFormatY(commandStatistics.m_format);
                File pngFile = getAvgPlotFile(i);
                File dataFile = getAvgDataFile(i);
                plot.setPlotStyleNoLines();
                plot.plot(pngFile, table, "Move", "Mean", "Error");
                FileWriter writer = new FileWriter(dataFile);
                table.save(writer, false);
                writer.close();
                out.print("<tr><td>\n");
                writePlot(out, getCommandLink(i), pngFile.getName(),
                          "<a href=\"" + dataFile.getName()
                          + "\"><small>data</small></a>");
                out.print("</td></tr>\n");
            }
        }
        out.print("</table>\n" +
                  "<hr>\n" +
                  "<table border=\"0\" cellpadding=\"0\">\n" +
                  "<tr>\n");
        int n = 0;
        for (int i = 0; i < m_commands.size(); ++i)
        {
            CommandStatistics commandStatistics = getCommandStatistics(i);
            if (commandStatistics.getCount() == 0)
                continue;
            out.print("<td valign=\"bottom\" bgcolor=\"" + m_colorHeader
                      + "\">\n");
            writePlot(out, getCommandLink(i), getHistoFile(i).getName(), "");
            out.print("</td>\n");
            ++n;
            if (n % 5 == 0)
                out.print("</tr><tr>\n");
        }
        out.print("</tr>\n" +
                  "</table>\n" +
                  "<hr>\n");
        writeCommandsTable(out);
        out.print("<hr>\n");
        writeGameTable(out);
        finishHtml(out);
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

    /** Command having at most one result per game. */
    private static class GameGlobalCommand
    {
        public GameGlobalCommand(String name, Vector results)
        {
            m_name = name;
            m_results = results;
            initAllEmpty();
        }

        public boolean allEmpty()
        {
            return m_allEmpty;
        }

        public String getName()
        {
            return m_name;
        }

        public String getResult(int game)
        {
            return (String)m_results.get(game);
        }

        private boolean m_allEmpty;

        private String m_name;

        /** Results per game. */
        private Vector m_results;

        private void initAllEmpty()
        {
            m_allEmpty = false;
            for (int game = 0; game < m_results.size(); ++game)
            {
                String result = getResult(game);
                if (result != null && ! result.trim().equals(""))
                    return;
            }
            m_allEmpty = true;
        }
    }

    private static class GameInfo
    {
        public String m_file;

        public String m_name;

        public int m_finalPosition;

        public int m_numberPositions;
    }

    private int m_imgHeight = 100;

    private int m_maxMove;

    private int m_movePrintInterval;

    private int m_precision;

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#b5c8f0";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private final String m_output;

    private Table m_table;

    private Table m_tableFinal;

    private Vector m_commandStatistics;

    private Vector m_commands;

    /** Vector<GameGlobalCommand> */
    private Vector m_gameGlobalCommands;

    /** Vector<GameInfo> */
    private Vector m_gameInfo;    

    private void endInfo(PrintStream out)
    {
        out.print("</table></td></tr>\n" +
                  "</table>\n" +
                  "<hr>\n");
    }

    private void findGameGlobalCommands()
    {
        m_gameGlobalCommands = new Vector();
        for (int i = 0; i < m_commands.size(); ++i)
        {
            String command = getCommand(i);
            boolean isGameGlobal = true;
            Vector gameResult = new Vector();
            for (int j = 0; j < m_gameInfo.size(); ++j)
            {
                GameInfo info = (GameInfo)(m_gameInfo.get(j));
                Table table = TableUtils.select(m_table, "File", info.m_file,
                                                command);
                Vector unique = TableUtils.getColumnUnique(table, command);
                if (unique.size() > 1)
                {
                    isGameGlobal = false;
                    break;
                }
                else if (unique.size() == 1)
                    gameResult.add(unique.get(0));
                else
                    gameResult.add("");
            }
            if (isGameGlobal)
            {
                GameGlobalCommand gameGlobalCommand
                    = new GameGlobalCommand(command, gameResult);
                m_gameGlobalCommands.add(gameGlobalCommand);
            }
        }
    }

    private void finishHtml(PrintStream out)
    {
        out.print("<hr>\n" +
                  "<address><small>Generated on " + StringUtils.getDateShort()
                  + " by GtpStatistics "
                  + Version.get() + " (<a href=\"http://gogui.sf.net\">"
                  + "http://gogui.sf.net</a>)</small></address>\n" +
                  "</body>\n" +
                  "</html>\n");
    }

    private File getAvgDataFile(int commandIndex)
    {
        return new File(m_output + ".command-" + commandIndex + ".avg.dat");
    }

    private File getAvgPlotFile(int commandIndex)
    {
        return new File(m_output + ".command-" + commandIndex + ".avg.png");
    }

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private File getCommandFile(int commandIndex)
    {
        return new File(m_output + ".command-" + commandIndex + ".html");
    }

    private CommandStatistics getCommandStatistics(int commandIndex)
    {
        return (CommandStatistics)m_commandStatistics.get(commandIndex);
    }

    private File getCountFile()
    {
        return new File(m_output + ".count.png");
    }

    private File getCountDataFile()
    {
        return new File(m_output + ".count.dat");
    }

    private File getGameFile(int gameIndex)
    {
        return new File(m_output + ".game-" + gameIndex + ".html");
    }

    private GameGlobalCommand getGameGlobalCommand(int index)
    {
        return (GameGlobalCommand)m_gameGlobalCommands.get(index);
    }

    private File getHistoFile(int commandIndex)
    {
        return new File(m_output + ".command-" + commandIndex + ".histo.png");
    }

    private File getHistoFile(int commandIndex, int moveIntervalIndex)
    {
        return new File(m_output + ".command-" + commandIndex + ".interval-"
                        + moveIntervalIndex + ".histo.png");
    }

    private File getHistoFinalFile(int commandIndex)
    {
        return new File(m_output + ".command-" + commandIndex + ".final.png");
    }

    private File getPlotFile(int gameIndex, int commandIndex)
    {
        return new File(m_output + ".game-" + gameIndex + ".command-"
                        + commandIndex + ".png");
    }

    private String getCommandLink(int commandIndex)
    {
        String link = "<small>" + getCommand(commandIndex) + "</small>";
        CommandStatistics statistics = getCommandStatistics(commandIndex);
        if (statistics.getCount() == 0 || statistics.m_isBeginCommand)
            return link;
        return "<a href=\"" + getCommandFile(commandIndex).getName()
            + "\">" + link + "</a>";
    }

    private CommandStatistics computeCommandStatistics(int index)
        throws Exception
    {
        String command = getCommand(index);
        boolean plotLines = true;
        return new CommandStatistics(command, m_table, m_tableFinal,
                                     getHistoFile(index),
                                     getHistoFinalFile(index),
                                     getColor(command), m_precision);
    }

    private Plot generatePlotMove(int width, Color color)
    {
        Plot plot = new Plot(width, m_imgHeight, color, m_precision);
        plot.setSolidLineInterval(10);
        plot.setXMin(0);
        plot.setXMax(m_maxMove);
        plot.setXTics(5);
        plot.setXLabelPerTic(2);
        return plot;
    }

    private void generatePlot(int commandIndex, DecimalFormat format,
                              int gameIndex, String gameFile) throws Exception
    {
        String command = getCommand(commandIndex);
        Table table = TableUtils.select(m_table, "File", gameFile,
                                        "Move", command);
        int numberPositions = table.getNumberRows();
        File file = getPlotFile(gameIndex, commandIndex);
        Plot plot = generatePlotMove(getImgWidth(m_maxMove),
                                     getColor(command));
        plot.setFormatY(format);
        plot.plot(file, table, "Move", command, null);
    }

    private Color getColor(String command)
    {
        int index = m_table.getColumnIndex(command);
        return m_plotColor[(index - 2) % m_plotColor.length];
    }

    private String getGameLink(File fromFile, int gameNumber,
                               boolean shortName)
    {
        GameInfo info = (GameInfo)(m_gameInfo.get(gameNumber));
        File gameFile = new File(info.m_file);
        try
        {
            String path
                = FileUtils.getRelativePath(fromFile, gameFile)
                + gameFile.getName();
            return "<a href=\"" + path + "\">"
                + (shortName ? gameFile.getName(): path)+ "</a>";
        }
        catch (IOException e)
        {
            return info.m_file;
        }
    }

    private int getImgWidth(int numberMoves)
    {
        return Math.max(10, Math.min(numberMoves * 9, 1040));
    }

    private void initGameInfo() throws IOException
    {
        m_gameInfo = new Vector();
        String last = null;
        GameInfo info = null;
        m_maxMove = 0;
        int numberColumns = m_table.getNumberColumns();
        int[] count = new int[0];
        for (int row = 0; row < m_table.getNumberRows(); ++row)
        {
            String file = m_table.get("File", row);
            int move = Integer.parseInt(m_table.get("Move", row));
            if (move >= count.length)
            {
                int[] newCount = new int[move + 1];
                for (int i = 0; i < count.length; ++i)
                    newCount[i] = count[i];
                count = newCount;
            }
            ++count[move];
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
        m_movePrintInterval = 1;
        while (m_movePrintInterval < m_maxMove / 30)
        {
            m_movePrintInterval *= 5;
            if (m_movePrintInterval >= m_maxMove / 30)
                break;
            m_movePrintInterval *= 2;
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
        Vector columnTitles = new Vector();
        columnTitles.add("Move");
        columnTitles.add("Count");
        Table table = new Table(columnTitles);
        for (int i = 0; i < count.length; ++i)
        {
            if (count[i] == 0)
                continue;
            table.startRow();
            table.set("Move", i);
            table.set("Count", count[i]);
        }
        Plot plot = generatePlotMove(getImgWidth(m_maxMove), Color.DARK_GRAY);
        plot.setNoPlotYZero();
        plot.plot(getCountFile(), table, "Move", "Count", null);
        FileWriter writer = new FileWriter(getCountDataFile());
        table.save(writer, false);
        writer.close();
    }

    private boolean isGameGlobalCommand(String command)
    {
        for (int i = 0; i < m_gameGlobalCommands.size(); ++i)
            if (getGameGlobalCommand(i).getName().equals(command))
                return true;
        return false;
    }

    private void startHtml(PrintStream out, String title)
    {
        String charset = StringUtils.getDefaultEncoding();
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>" + title + "</title>\n" +
                  "<meta http-equiv=\"Content-Type\""
                  + " content=\"text/html; charset=" + charset + "\">\n" +
                  "<meta name=\"generator\" content=\"GtpStatistics "
                  + Version.get() + "\">\n" +
                  "<style type=\"text/css\">\n" +
                  "<!--\n" +
                  "a:link { color:#0000ee }\n" +
                  "a:visited { color:#551a8b }\n" +
                  ".smalltable { font-size:80%; }\n" +
                  ".smalltable td { background-color:" + m_colorInfo
                  + "; text-align:center; }\n" +
                  ".smalltable th { background-color:" + m_colorHeader
                  + ";  vertical-align:top; }\n" +
                  ".smalltable table { border:0; cellpadding:0; }\n" +
                  "-->\n" +
                  "</style>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\">\n");
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
                  + "\" >\n" +
                  "<tr><td><table style=\"font-size:80%\""
                  +" cellpadding=\"0\">\n");
    }

    private void writeCommandPage(int commandIndex)
        throws Exception
    {
        String command = getCommand(commandIndex);
        CommandStatistics commandStatistics
            = getCommandStatistics(commandIndex);
        File file = getCommandFile(commandIndex);
        PrintStream out = new PrintStream(new FileOutputStream(file));
        startHtml(out, command);
        startInfo(out, command);
        writeInfoBasics(out);
        writeHtmlRow(out, "Command Index", commandIndex);
        endInfo(out);
        if (! commandStatistics.m_isBeginCommand)
            out.print("<p><img src=\""
                      + getAvgPlotFile(commandIndex).getName()
                      + "\"></p>\n");
        writeCommandStatistics(out, commandIndex);
        out.print("<hr>\n");
        out.print("<table border=\"0\" cellspacing=\"0\""
                  + " cellpadding=\"5\">\n");
        out.print("<tr><td>"
                  + "<small>All</small><br>"
                  + "<img src=\"" + getHistoFile(commandIndex).getName()
                  + "\"></td>\n");
        
        if (commandStatistics.m_statisticsFinal.getCount() > 0)
            out.print("<td>"
                      + "<small>Final</small><br>"
                      + "<img src=\""
                      + getHistoFinalFile(commandIndex).getName()
                      + "\"></td>");
        out.print("</tr>\n" +
                  "</table>\n");
        for (int i = 0; i < m_maxMove; i += m_movePrintInterval)
        {
            Histogram histogram
                = commandStatistics.getStatistics(i).m_histogram;
            if (commandStatistics.getStatistics(i).getCount() == 0)
                continue;
            Table histoTable
                = TableUtils.fromHistogram(histogram, command);
            File histoFile = getHistoFile(commandIndex, i);
            Color color = getColor(command);
            Plot plot = new Plot(180, 135, color, m_precision);
            commandStatistics.setHistogramProperties(plot);
            plot.plot(histoFile, histoTable, command, "Count", null);
            out.print("<table align=\"left\" border=\"0\">" +
                      "<tr><td align=\"center\"><small>" + i
                      + "</small><br><img src=\""
                      + getHistoFile(commandIndex, i).getName()
                      + "\"></td></tr></table>\n");
        }
        out.print("<br clear=\"left\">\n" +
                  "<hr>\n");
        writeGamePlots(out, commandIndex);
        finishHtml(out);
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
        out.print("<tr>");
        out.print("<th>Move</th>");
        writeStatisticsTableHeader(out);
        out.print("</tr>\n");
        DecimalFormat format = commandStatistics.m_format;
        for (int i = 0; i < m_maxMove; i += m_movePrintInterval)
        {
            PositionStatistics statisticsAtMove
                = commandStatistics.getStatistics(i);
            out.print("<tr>" +
                      "<td>" + i + "</td>");
            writeStatisticsTableData(out, statisticsAtMove, format, false);
            out.print("</tr>\n");
        }
        out.print("<tr style=\"font-weight:bold\">" +
                  "<td>Final</td>");
        writeStatisticsTableData(out, finalStatistics, format, false);
        out.print("</tr>\n");
        out.print("<tr style=\"font-weight:bold\">" +
                  "<td>All</td>");
        writeStatisticsTableData(out, statisticsAll, format,
                                 ! isGameGlobalCommand(command));
        out.print("</tr>\n");
        out.print("</table>\n");
    }

    private void writeCommandsTable(PrintStream out) throws Exception
    {
        out.print("<table class=\"smalltable\">\n" +
                  "<thead><tr>"
                  + "<th>Command</th>");
        writeStatisticsTableHeader(out);
        out.print("</tr></thead>\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            CommandStatistics commandStatistics = getCommandStatistics(i);
            int count = commandStatistics.getCount();
            if (count > 0 && ! commandStatistics.m_isBeginCommand)
                writeCommandPage(i);
            PositionStatistics statisticsAll
                = commandStatistics.m_statisticsAll;
            out.print("<tr><td style=\"background-color:" + m_colorHeader
                      + "\">" + getCommandLink(i) + "</td>");
            writeStatisticsTableData(out, statisticsAll,
                                     commandStatistics.m_format,
                                     ! isGameGlobalCommand(getCommand(i)));
            out.print("</tr>\n");
        }
        out.print("</table>\n");
    }

    private void writeGamePage(String game, String name, int gameNumber)
        throws Exception
    {
        File file = getGameFile(gameNumber);
        PrintStream out = new PrintStream(new FileOutputStream(file));
        String title = "Game " + (gameNumber + 1) + " (" + name + ")";
        startHtml(out, title);
        startInfo(out, title);
        writeInfoBasics(out);
        writeHtmlRow(out, "Game Index", gameNumber);
        writeHtmlRow(out, "File", getGameLink(file, gameNumber, false));
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
        out.print("<table border=\"0\">\n");
        for (int i = 0; i < m_commands.size(); ++i)
        {
            CommandStatistics commandStatistics = getCommandStatistics(i);
            if (commandStatistics.getCount() > 0
                && ! commandStatistics.m_isBeginCommand)
            {
                String command = getCommand(i);
                generatePlot(i, commandStatistics.m_format, gameNumber, game);
                out.print("<tr><td align=\"center\">" + getCommandLink(i)
                          + "<br><img src=\""
                          + getPlotFile(gameNumber, i).getName()
                          + "\"></td></tr>\n");
            }
        }
        out.print("</table>\n" +
                  "<hr>\n");
        Table table = TableUtils.select(m_table, "File", game);
        out.print("<table class=\"smalltable\">\n" +
                  "<thead><tr>");
        for (int i = 1; i < table.getNumberColumns(); ++i)
        {
            String command = table.getColumnTitle(i);
            if (! TableUtils.allEmpty(table, command))
                out.print("<th>" + command + "</th>");
        }
        out.print("</tr></thead>\n");
        for (int i = 0; i < table.getNumberRows(); ++i)
        {
            out.print("<tr>");
            for (int j = 1; j < table.getNumberColumns(); ++j)
            {
                String command = table.getColumnTitle(j);
                if (TableUtils.allEmpty(table, command))
                    continue;
                String value = table.get(command, i);
                if (value == null)
                    value = "";
                out.print("<td>" + value + "</td>");
            }
            out.print("</tr>\n");
        }
        out.print("</table>\n");
        finishHtml(out);
        out.close();
    }

    private void writeGamePlots(PrintStream out, int commandIndex)
        throws Exception
    {
        String command = getCommand(commandIndex);
        out.print("<table border=\"0\" cellpadding=\"0\""
                  + " cellspacing=\"0\">\n");
        for (int i = 0; i < m_gameInfo.size(); ++i)
        {
            GameInfo info = (GameInfo)(m_gameInfo.get(i));
            String plotFile = getPlotFile(i, commandIndex).getName();
            File file = getGameFile(i);
            out.print("<tr><td align=\"left\"><small><a href=\""
                      + file.getName() + "\">Game " + (i + 1) + "</a> ("
                      + getGameLink(file, i, true)
                      + "):</small><br>\n" +
                      "<img src=\"" + plotFile + "\"></td></tr>\n");
        }
        out.print("</table>\n");
    }

    private void writeGameTable(PrintStream out)
        throws Exception
    {
        out.print("<table class=\"smalltable\">\n" +
                  "<thead><tr><th>Game</th><th>File</th><th>Positions</th>");
        for (int i = 0; i < m_gameGlobalCommands.size(); ++i)
            if (! getGameGlobalCommand(i).allEmpty())
                out.print("<th>" + getGameGlobalCommand(i).m_name + "</th>");
        out.print("</tr></thead>\n");
        for (int i = 0; i < m_gameInfo.size(); ++i)
        {
            GameInfo info = (GameInfo)(m_gameInfo.get(i));
            String file = getGameFile(i).getName();
            out.print("<tr><td style=\"background-color:" + m_colorHeader
                      + "\"><a href=\"" + file + "\">Game " + (i + 1)
                      + "</a></td><td>" + info.m_name
                      + "</td><td>" + info.m_numberPositions + "</td>");
            for (int j = 0; j < m_gameGlobalCommands.size(); ++j)
                if (! getGameGlobalCommand(j).allEmpty())
                    out.print("<td>" + getGameGlobalCommand(j).getResult(i)
                              + "</td>");
            out.print("</tr>\n");
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

    private void writeInfoBasics(PrintStream out) throws Exception
    {
        writeTableProperty(out, "Name");
        writeTableProperty(out, "Version");
        writeTableProperty(out, "Date");
        writeTableProperty(out, "Host");
        writeTableProperty(out, "Program");
    }

    private void writeInfo(PrintStream out) throws Exception
    {
        writeInfoBasics(out);
        writeTableProperty(out, "Size");
        writeTableProperty(out, "Games");
        writeHtmlRow(out, "Positions", m_table.getNumberRows());
    }

    private void writePlot(PrintStream out, String title, String file,
                           String info)
    {
        out.print("<table width=\"100%\" cellspacing=\"0\""
                  + " cellpadding=\"0\">\n" +
                  "<tr><td><table width=\"100%\" border=\"0\""
                  + " cellpadding=\"0\" bgcolor=\"" + m_colorHeader
                  + "\">\n" +
                  "<tr><td");
        if (! info.equals(""))
            out.print(" width=\"90%\"");
        out.print(" align=\"center\">\n" +
                  title + "\n" + 
                  "</td>");
        if (! info.equals(""))
            out.print("<td align=\"right\">\n" + info + "\n</td>");
        out.print("</tr></table></td></tr>\n" +
                  "<tr><td bgcolor=\"" + m_colorInfo
                  + "\">\n" +
                  "<img src=\"" + file + "\"></td></tr>\n" +
                  "</table>\n");
    }

    private void writeStatisticsTableData(PrintStream out,
                                          PositionStatistics statistics,
                                          DecimalFormat format,
                                          boolean withMaxError)
    {
        boolean empty = (statistics.getCount() == 0);
        boolean greaterOne = (statistics.getCount() > 1);
        out.print("<td>");
        if (! empty)
            out.print(format.format(statistics.getMean()));
        out.print("</td><td>");
        if (greaterOne)
            out.print(format.format(statistics.getDeviation()));
        else if (! empty)
            out.print("");
        out.print("</td><td>");
        if (greaterOne)
            out.print(format.format(statistics.getError()));
        else if (! empty)
            out.print("");
        out.print("</td><td>");
        if (greaterOne && withMaxError)
        {
            int movesPerGame = m_table.getNumberRows() / m_gameInfo.size();
            out.print(format.format(statistics.getMaxError(movesPerGame)));
        }
        else if (! empty)
            out.print("");
        out.print("</td><td>");
        if (greaterOne)
            out.print(format.format(statistics.getMin()));
        else if (! empty)
            out.print("");
        out.print("</td><td>");
        if (greaterOne)
            out.print(format.format(statistics.getMax()));
        else if (! empty)
            out.print("");
        out.print("</td><td>");
        if (greaterOne)
            out.print(format.format(statistics.getSum()));
        else if (! empty)
            out.print("");
        out.print("</td><td>");
        out.print(statistics.getCount());
        out.print("</td><td>");
        out.print(statistics.m_numberNoResult);
        out.print("</td>");
    }

    private void writeStatisticsTableHeader(PrintStream out)
    {
        out.print("<th>Mean</th>"
                  + "<th>Deviation</th>"
                  + "<th>Error</th>"
                  + "<th>MaxError</th>"
                  + "<th>Min</th>"
                  + "<th>Max</th>"
                  + "<th>Sum</th>"
                  + "<th>Count</th>"
                  + "<th>Unknown</th>");
    }

    private void writeTableProperty(PrintStream out, String key)
        throws Exception
    {
        writeHtmlRow(out, key, m_table.getProperty(key, "?"));
    }
}

//----------------------------------------------------------------------------
