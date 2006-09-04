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
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.utils.Table;

//----------------------------------------------------------------------------

/** Run commands of a GTP engine on all positions in a game collection. */
public class GtpStatistics
{
    public GtpStatistics(String program, ArrayList sgfFiles, File output,
                         int size, ArrayList commands,
                         ArrayList beginCommands, ArrayList finalCommands,
                         boolean verbose, boolean force, boolean allowSetup,
                         int min, int max, boolean backward)
        throws Exception
    {
        if (output.exists() && ! force)
            throw new ErrorMessage("File " + output + " already exists");
        new FileCheck(sgfFiles, size, allowSetup);
        m_size = size;
        m_result = false;
        m_allowSetup = allowSetup;
        m_min = min;
        m_max = max;
        m_backward = backward;
        initCommands(commands, beginCommands, finalCommands);
        ArrayList columnHeaders = new ArrayList();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        for (int i = 0; i < m_commands.size(); ++i)
            columnHeaders.add(getCommand(i).m_columnTitle);
        m_table = new Table(columnHeaders);
        m_table.setProperty("Size", Integer.toString(size));
        m_gtp = new GtpClient(program, verbose, null);
        m_gtp.queryProtocolVersion();
        m_table.setProperty("Program", program);
        try
        {
            m_table.setProperty("Name", m_gtp.send("name"));
        }
        catch (GtpError e)
        {
            m_table.setProperty("Name", "");
            if (m_gtp.isProgramDead())
                throw e;
        }
        try
        {
            m_table.setProperty("Version", m_gtp.send("version"));
        }
        catch (GtpError e)
        {
            m_table.setProperty("Version", "");
        }
        String host = Platform.getHostInfo();
        m_table.setProperty("Host", host);
        m_table.setProperty("Date", StringUtils.getDate());
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile((String)sgfFiles.get(i));
        m_table.setProperty("Games", Integer.toString(m_numberGames));
        m_table.setProperty("Backward", backward ? "yes" : "no");
        FileWriter writer = new FileWriter(output);
        m_table.save(writer);
        writer.close();
    }

    public boolean getResult()
    {
        return m_result;
    }

    private static class Command
    {
        public boolean m_begin;

        public boolean m_final;

        public String m_command;

        public String m_columnTitle;
    }

    private final boolean m_allowSetup;

    private final boolean m_backward;

    private boolean m_result;

    private final int m_max;

    private final int m_min;

    private int m_numberGames;

    private final int m_size;

    private double m_lastCpuTime = 0;

    private final GtpClient m_gtp;

    private final NumberFormat m_format1 = StringUtils.getNumberFormat(1);

    private final NumberFormat m_format2 = StringUtils.getNumberFormat(2);

    private final Table m_table;

    private ArrayList m_commands;

    private void addCommand(String commandLine, boolean isBegin,
                            boolean isFinal) throws ErrorMessage
    {
        commandLine = commandLine.trim();
        if (commandLine.equals(""))
            throw new ErrorMessage("Empty command not allowed");
        Command command = new Command();
        command.m_command = commandLine;
        command.m_begin = isBegin;
        command.m_final = isFinal;
        int numberSame = 0;
        Command firstSame = null;
        for (int i = 0; i < m_commands.size(); ++i)
            if (getCommand(i).m_command.equals(commandLine))
            {
                firstSame = getCommand(i);
                ++numberSame;
            }
        if (numberSame == 0)
            command.m_columnTitle = commandLine;
        else
        {
            if (numberSame == 1)
                firstSame.m_columnTitle = firstSame.m_columnTitle + " (1)";
            command.m_columnTitle = commandLine + " ("
                + (numberSame + 1) + ")";
        }
        m_commands.add(command);
    }

    private void addCommands(ArrayList commands, boolean isBegin,
                             boolean isFinal) throws ErrorMessage
    {
        for (int i = 0; i < commands.size(); ++i)
            addCommand((String)commands.get(i), isBegin, isFinal);
    }

    private void checkGame(GameTree tree, String name) throws ErrorMessage
    {
        GameInformation info = tree.getGameInformation();
        int size = info.m_boardSize;
        if (size != m_size)
            throw new ErrorMessage(name + " has not size " + m_size);
        Node root = tree.getRoot();
        GoColor toMove = GoColor.BLACK;
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
            {
                if (m_allowSetup)
                {
                    if (node == root)
                        toMove = GoColor.EMPTY;
                    else
                        throw new ErrorMessage(name + " contains setup stones"
                                               + " in non-root position");
                }
                else
                    throw new ErrorMessage(name + " contains setup stones");
            }
            Move move = node.getMove();
            if (move != null)
            {
                if (toMove == GoColor.EMPTY)
                    toMove = move.getColor();
                if (move.getColor() != toMove)
                    throw new ErrorMessage(name
                                           + "has non-alternating moves");
                toMove = toMove.otherColor();
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
                                   GoColor toMove, Move move) throws GtpError
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
            String arg[] = StringUtils.splitArguments(response);
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
                return Move.get(point, toMove) == move ? "1" : "0";
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

    private void initCommands(ArrayList commands, ArrayList beginCommands,
                              ArrayList finalCommands) throws ErrorMessage
    {
        m_commands = new ArrayList();
        if (beginCommands != null)
            addCommands(beginCommands, true, false);
        if (commands != null)
            addCommands(commands, false, false);
        if (finalCommands != null)
            addCommands(finalCommands, false, true);
        if (m_commands.size() == 0)
            throw new ErrorMessage("No commands defined");
    }

    private Command getCommand(int index)
    {
        return (Command)m_commands.get(index);
    }

    private void handleFile(String name)
        throws ErrorMessage, FileNotFoundException, GtpError,
               SgfReader.SgfError
    {
        InputStream in = new FileInputStream(new File(name));
        SgfReader reader = new SgfReader(in, name, null, 0);
        ++m_numberGames;
        GameTree tree = reader.getGameTree();
        checkGame(tree, name);
        GameInformation info = tree.getGameInformation();
        int size = info.m_boardSize;
        m_gtp.sendBoardsize(size);
        m_gtp.sendClearBoard(size);
        Node root = tree.getRoot();
        if (m_backward)
            iteratePositionsBackward(root, name);
        else
            iteratePositions(root, name);
    }

    private void handlePosition(String name, GoColor toMove, Move move,
                                int number, boolean beginCommands,
                                boolean regularCommands,
                                boolean finalCommands)
        throws GtpError
    {
        System.err.println(name + ":" + number);
        m_table.startRow();
        m_table.set("File", name);
        m_table.set("Move", number);
        for (int i = 0; i < m_commands.size(); ++i)
        {
            Command command = getCommand(i);
            if (command.m_begin && beginCommands)
            {
                String response = send(command.m_command, toMove, move);
                m_table.set(command.m_columnTitle, response);
            }
        }
        for (int i = 0; i < m_commands.size(); ++i)
        {
            Command command = getCommand(i);
            if (! command.m_begin && ! command.m_final && regularCommands)
            {
                String response = send(command.m_command, toMove, move);
                m_table.set(command.m_columnTitle, response);
            }
        }
        for (int i = 0; i < m_commands.size(); ++i)
        {
            Command command = getCommand(i);
            if (command.m_final && finalCommands)
            {
                String response = send(command.m_command, toMove, move);
                m_table.set(command.m_columnTitle, response);
            }
        }
    }

    private void iteratePositions(Node root, String name) throws GtpError
    {
        int number = 0;
        GoColor toMove = GoColor.BLACK;
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
            {
                assert(m_allowSetup && node != root); // checked in checkGame
                toMove = sendSetup(node);
            }
            Move move = node.getMove();
            if (move != null)
            {
                ++number;
                boolean beginCommands = (number == 1);
                boolean regularCommands =
                    (number >= m_min && number <= m_max);
                if (beginCommands || regularCommands)
                    handlePosition(name, toMove, move, number, beginCommands,
                                   regularCommands, false);
                m_gtp.sendPlay(move);
                toMove = toMove.otherColor();
            }
        }
        ++number;
        boolean beginCommands = (number == 1);
        boolean regularCommands = (number >= m_min && number <= m_max);
        handlePosition(name, toMove, null, number, beginCommands,
                       regularCommands, true);
    }

    private void iteratePositionsBackward(Node root, String name)
        throws GtpError
    {
        Node node = root;
        GoColor toMove = GoColor.BLACK;
        while (true)
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
            {
                assert(m_allowSetup && node == root); // checked in checkGame
                toMove = sendSetup(node);
            }
            Move move = node.getMove();
            if (move != null)
            {
                m_gtp.sendPlay(move);
                toMove = toMove.otherColor();
            }
            Node child = node.getChild();
            if (child == null)
                break;
            node = child;
        }
        int number = 1;
        boolean finalCommands = (node == root);
        boolean regularCommands = (number >= m_min && number <= m_max);
        handlePosition(name, toMove, null, number, true, regularCommands,
                       finalCommands);
        for ( ; node != root; node = node.getFather())
        {
            // checked in checkGame
            assert(node.getNumberAddWhite() + node.getNumberAddBlack() == 0);
            Move move = node.getMove();
            if (move != null)
            {
                m_gtp.send("undo");
                ++number;
                finalCommands = (node.getFather() == root);
                regularCommands = (number >= m_min && number <= m_max);
                if (finalCommands || regularCommands)
                    handlePosition(name, toMove, move, number, false,
                                   regularCommands, finalCommands);
                toMove = toMove.otherColor();
            }
        }
    }

    private String send(String command, GoColor toMove, Move move)
        throws GtpError
    {
        String cmd = convertCommand(command, toMove);
        String response = m_gtp.send(cmd);
        return convertResponse(command, response, toMove, move);
    }

    /** Send setup stones as moves.
        @return New color to move.
     */
    private GoColor sendSetup(Node node) throws GtpError
    {
        ArrayList moves = new ArrayList();
        NodeUtils.getAllAsMoves(node, moves);
        assert(moves.size() > 0);
        GoColor toMove = null;
        for (int i = 0; i < moves.size(); ++i)
        {
            Move move = (Move)moves.get(i);
            m_gtp.sendPlay(move);
            toMove = move.getColor().otherColor();
        }
        return toMove;
    }
}
    
//----------------------------------------------------------------------------
