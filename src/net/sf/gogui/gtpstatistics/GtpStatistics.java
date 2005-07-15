//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Vector;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.utils.Table;

//----------------------------------------------------------------------------

public class GtpStatistics
{
    public GtpStatistics(String program, Vector sgfFiles, int size,
                         Vector commands, Vector beginCommands,
                         Vector finalCommands, boolean verbose, boolean force)
        throws Exception
    {
        File file = new File("gtpstatistics.dat");
        if (file.exists() && ! force)
            throw new ErrorMessage("File " + file + " already exists");
        new FileCheck(sgfFiles, size);
        m_size = size;
        m_result = false;
        m_commands = commands;
        m_beginCommands = beginCommands;
        m_finalCommands = finalCommands;
        checkCommands();
        Vector columnHeaders = new Vector();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        if (beginCommands != null)
            for (int i = 0; i < beginCommands.size(); ++i)
                columnHeaders.add(getBeginCommand(i));
        if (commands != null)
            for (int i = 0; i < commands.size(); ++i)
                columnHeaders.add(getCommand(i));
        if (finalCommands != null)
            for (int i = 0; i < finalCommands.size(); ++i)
                columnHeaders.add(getFinalCommand(i));
        m_table = new Table(columnHeaders);
        m_table.setProperty("Size", Integer.toString(size));
        m_gtp = new Gtp(program, verbose, null);
        m_table.setProperty("Program", program);
        try
        {
            m_table.setProperty("Name", m_gtp.sendCommand("name"));
        }
        catch (GtpError e)
        {
            m_table.setProperty("Name", "");
            if (m_gtp.isProgramDead())
                throw e;
        }
        try
        {
            m_table.setProperty("Version", m_gtp.sendCommand("version"));
        }
        catch (GtpError e)
        {
            m_table.setProperty("Version", "");
        }
        String host;
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            host = "?";
        }
        m_table.setProperty("Host", host);
        m_table.setProperty("Date", StringUtils.getDate());
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile((String)sgfFiles.get(i));
        m_table.setProperty("Games", Integer.toString(m_numberGames));
        FileWriter writer = new FileWriter(file);
        m_table.save(writer);
        writer.close();
    }

    public boolean getResult()
    {
        return m_result;
    }

    private boolean m_result;

    private int m_numberGames;

    private int m_size;

    private double m_lastCpuTime = 0;

    private Gtp m_gtp;

    private NumberFormat m_format1 = StringUtils.getNumberFormat(1);

    private NumberFormat m_format2 = StringUtils.getNumberFormat(2);

    private Table m_table;

    private Vector m_beginCommands;

    private Vector m_commands;

    private Vector m_finalCommands;

    private void checkCommands() throws ErrorMessage
    {
        Vector all = new Vector();
        if (m_commands != null)
            all.addAll(m_commands);
        if (m_beginCommands != null)
            all.addAll(m_beginCommands);
        if (m_finalCommands != null)
            all.addAll(m_finalCommands);
        if (all.size() == 0)
            throw new ErrorMessage("No commands defined");
        for (int i = 0; i < all.size() - 1; ++i)
            for (int j = i + 1; j < all.size(); ++j)
                if (all.get(i).equals(all.get(j)))
                    throw new ErrorMessage("Non-unique commands not"
                                           + " supported: " + all.get(i));
    }

    private String getBeginCommand(int index)
    {
        return (String)m_beginCommands.get(index);
    }

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private String getFinalCommand(int index)
    {
        return (String)m_finalCommands.get(index);
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
        if (size != m_size)
            throw new ErrorMessage(name + " has not size " + m_size);
        m_gtp.sendCommandBoardsize(size);
        m_gtp.sendCommandClearBoard(size);
        Node root = tree.getRoot();
        int number = 0;
        GoColor toMove = GoColor.BLACK;
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
                throw new ErrorMessage(name + " contains setup stones");
            Move move = node.getMove();
            if (move != null)
            {
                if (move.getColor() != toMove)
                    throw new ErrorMessage(name
                                           + "has non-alternating moves");
                ++number;
                handlePosition(name, toMove, move, number, false);
                m_gtp.sendCommandPlay(move);
                toMove = toMove.otherColor();
            }
        }
        if (m_finalCommands != null)
        {
            ++number;
            handlePosition(name, toMove, null, number, true);
            for (int i = 0; i < m_finalCommands.size(); ++i)
            {
                String command = getFinalCommand(i);
                String response
                    = convertResponse(getFinalCommand(i),
                                      m_gtp.sendCommand(command), toMove,
                                      null, true);
                m_table.set(command, response);
            }
        }
    }

    private void handlePosition(String name, GoColor toMove, Move move,
                                int number, boolean isFinal)
        throws GtpError
    {
        System.err.println(name + ":" + number);
        m_table.startRow();
        m_table.set("File", name);
        m_table.set("Move", number);
        if (number == 1 && m_beginCommands != null)
            for (int i = 0; i < m_beginCommands.size(); ++i)
            {
                String command = getBeginCommand(i);
                String result = m_gtp.sendCommand(command);
                m_table.set(command, result);
            }
        if (m_commands != null)
        {
            for (int i = 0; i < m_commands.size(); ++i)
            {
                String command = getCommand(i);
                command = convertCommand(command, toMove);
                String response = m_gtp.sendCommand(command);
                response = convertResponse(getCommand(i), response, toMove,
                                           move, isFinal);
                m_table.set(getCommand(i), response);
            }
        }
    }

    private String convertCommand(String command, GoColor toMove)
    {
        if (command.equals("reg_genmove"))
            return command + ' ' + toMove;
        return command;
    }

    private String convertResponse(String command, String response,
                                   GoColor toMove, Move move,
                                   boolean isFinal) throws GtpError
    {
        if (command.equals("cputime"))
        {
            try
            {
                double cpuTime = Double.parseDouble(response);
                double diff = cpuTime - m_lastCpuTime;
                m_lastCpuTime = cpuTime;
                return m_format2.format(diff);
            }
            catch (NumberFormatException e)
            {
                return response;
            }
        }
        else if (command.equals("estimate_score"))
        {
            String arg[] = StringUtils.tokenize(response);
            if (arg.length == 0)
                return response;
            return convertScore(arg[0]);
        }
        else if (command.equals("final_score"))
        {
            return convertScore(response);
        }
        else if (command.equals("reg_genmove"))
        {
            if (move == null)
                return "";
            try
            {
                GoPoint point = GoPoint.parsePoint(response, m_size); 
                return Move.create(point, toMove) == move ? "1" : "0";
            }
            catch (GoPoint.InvalidPoint e)
            {
                throw new GtpError("Program sent invalid move: " + response);
            }
        }
        return response;
    }

    /** Tries to convert score into number.
        @return Score string or original string, if conversion fails.
    */
    private String convertScore(String string)
    {
        String score = string.trim();
        double sign = 1;
        if (score.startsWith("W+"))
        {
            score = score.substring(2);
            sign = -1;
        }
        else if (score.startsWith("B+"))
            score = score.substring(2);
        try
        {
            return m_format1.format(sign * Double.parseDouble(score));
        }
        catch (NumberFormatException e)
        {
            return string;
        }
    }
}
    
//----------------------------------------------------------------------------
