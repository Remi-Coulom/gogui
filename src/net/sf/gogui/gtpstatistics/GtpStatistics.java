//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.text.NumberFormat;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Table;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class GtpStatistics
{
    public GtpStatistics(String program, Vector sgfFiles, Vector commands,
                         boolean verbose)
        throws Exception
    {
        m_result = false;
        m_commands = commands;
        Vector columnHeaders = new Vector();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        if (m_runRegGenMove)
            columnHeaders.add("reg_genmove");
        if (commands != null)
            for (int i = 0; i < commands.size(); ++i)
                columnHeaders.add(getCommand(i));
        m_table = new Table(columnHeaders);
        m_gtp = new Gtp(program, verbose, null);
        m_program = program;
        try
        {
            m_name = m_gtp.sendCommand("name");
        }
        catch (GtpError e)
        {
            m_name = "";
            if (m_gtp.isProgramDead())
                throw e;
        }
        try
        {
            m_version = m_gtp.sendCommand("version");
        }
        catch (GtpError e)
        {
            m_version = "";
        }
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile((String)sgfFiles.get(i));
        FileWriter writer = new FileWriter("gtpstatistics.dat");
        m_table.save(writer);
        writer.close();
        writeHtml();
    }

    public boolean getResult()
    {
        return m_result;
    }

    private boolean m_result;

    private boolean m_runRegGenMove = true;

    private int m_numberGames;

    private int m_numberPositions;

    private Gtp m_gtp;

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private static final String m_colorGreen = "#5eaf5e";

    private static final String m_colorRed = "#ff5454";

    private String m_name;

    private String m_program;

    private String m_version;

    private Table m_table;

    private Vector m_commands;

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private void handleFile(String name)
        throws ErrorMessage, FileNotFoundException, GtpError,
               SgfReader.SgfError
    {
        InputStream in = new FileInputStream(new File(name));
        SgfReader reader = new SgfReader(in, name, null, 0);
        ++m_numberGames;
        GameTree tree = reader.getGameTree();
        GameInformation info = tree.getGameInformation();
        int size = info.m_boardSize;
        m_gtp.sendCommandBoardsize(size);
        m_gtp.sendCommandClearBoard(size);
        Node root = tree.getRoot();
        if (root.getMove() != null)
            throw new ErrorMessage(name + " has move in root node");
        int number = 0;
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
                throw new ErrorMessage(name + " contains setup stones");
            Move move = node.getMove();
            if (move != null)
            {
                ++number;
                handlePosition(name, move, number);
                m_gtp.sendCommandPlay(move);
            }
        }
    }

    private void handlePosition(String name, Move move, int number)
        throws GtpError
    {
        ++m_numberPositions;
        m_table.startRow();
        m_table.set("File", name);
        m_table.set("Move", number);
        if (m_runRegGenMove && move != null)
        {
            boolean result = runRegGenMove(move);
            m_table.set("reg_genmove", result ? "1" : "0");
        }
        if (m_commands == null)
            return;
        for (int i = 0; i < m_commands.size(); ++i)
        {
            String command = getCommand(i);
            String result = m_gtp.sendCommand(command);
            m_table.set(command, result);
        }
    }

    private boolean runRegGenMove(Move move) throws GtpError
    {
        String response = m_gtp.sendCommand("reg_genmove " + move.getColor());
        response = response.trim().toUpperCase();
        return (response.equals(GoPoint.toString(move.getPoint())));
    }

    private void writeHtml()
        throws FileNotFoundException
    {
        File file = new File("gtpstatistics.html");
        PrintStream out = new PrintStream(new FileOutputStream(file));
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
                  "<h1>Statistics</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                  + "\">\n");
        writeInfo(out);
        out.print("</table>\n");
        writeCommandResult("reg_genmove", out);
        for (int i = 0; i < m_commands.size(); ++i)
            writeCommandResult(getCommand(i), out);
        out.print("</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeCommandResult(String command, PrintStream out)
    {
        out.print("<hr>\n" +
                  "<h2>" + command + "</h2>\n" +
                  "<p>\n" +
                  "</p>\n");
    }

    private void writeInfo(PrintStream out)
    {
        String host;
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            host = "?";
        }
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                           DateFormat.FULL);
        Date date = Calendar.getInstance().getTime();
        out.print("<tr><th align=\"left\">Name:</th><td>" + m_name
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\">Version:</th><td>" + m_version
                  + "</td></tr>\n");
        out.print("<tr><th align=\"left\">Date:</th><td>" + format.format(date)
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\">Host:</th><td>" + host
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\" valign=\"top\">Command:</th>\n" +
                  "<td valign=\"top\"><tt>" + m_program
                  + "</tt></td></tr>\n" +
                  "<tr><th align=\"left\" valign=\"top\">Games:</th>\n" +
                  "<td valign=\"top\"><tt>" + m_numberGames
                  + "</tt></td></tr>\n" +
                  "<tr><th align=\"left\" valign=\"top\">Positions:</th>\n" +
                  "<td valign=\"top\"><tt>" + m_numberPositions
                  + "</tt></td></tr>\n");
    }
}
    
//----------------------------------------------------------------------------
