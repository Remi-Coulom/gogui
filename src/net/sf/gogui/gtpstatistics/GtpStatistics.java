//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.util.Table;

/** Run commands of a GTP engine on all positions in a game collection.
    @todo Use GtpSynchronizer
*/
public class GtpStatistics
{
    public void run(String program, ArrayList sgfFiles, int size,
                    ArrayList commands, ArrayList beginCommands,
                    ArrayList finalCommands, boolean verbose,
                    boolean allowSetup, boolean backward)
        throws ErrorMessage, GtpError, IOException
    {
        run(new GtpClient(program, verbose, null), program, sgfFiles, size,
            commands, beginCommands, finalCommands, allowSetup, backward);
    }

    /** Construct with existing GTP engine.
        @param program Program command (null, if gtp is not an instance of
        GtpClient)
    */
    public void run(GtpClientBase gtp, String program, ArrayList sgfFiles,
                    int size, ArrayList commands, ArrayList beginCommands,
                    ArrayList finalCommands, boolean allowSetup,
                    boolean backward)
        throws ErrorMessage, IOException
    {
        new FileCheck(sgfFiles, size, allowSetup);
        m_size = size;
        m_allowSetup = allowSetup;
        m_backward = backward;
        initCommands(commands, beginCommands, finalCommands);
        ArrayList columnHeaders = new ArrayList();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        for (int i = 0; i < m_commands.size(); ++i)
            columnHeaders.add(getCommand(i).m_columnTitle);
        m_table = new Table(columnHeaders);
        m_table.setProperty("Size", Integer.toString(size));
        m_gtp = gtp;
        m_gtp.queryProtocolVersion();
        m_gtp.queryName();
        if (program != null)
            m_table.setProperty("Program", program);
        m_table.setProperty("Name", m_gtp.getName());
        m_table.setProperty("Version", m_gtp.queryVersion());
        String host = Platform.getHostInfo();
        m_table.setProperty("Host", host);
        m_table.setProperty("Date", StringUtil.getDate());
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile((String)sgfFiles.get(i));
        m_gtp.send("quit");
        m_table.setProperty("Games", Integer.toString(m_numberGames));
        m_table.setProperty("Backward", backward ? "yes" : "no");
    }

    /** Set maximum move number for positions to run the commands on.
        Default is Integer.MAX_VALUE.
    */
    public void setMax(int max)
    {
        m_max = max;
    }

    /** Set minimum move number for positions to run the commands on.
        Default is zero.
    */
    public void setMin(int min)
    {
        m_min = min;
    }

    /** Save result table of last run. */
    public void saveTable(File output) throws IOException
    {
        FileWriter writer = new FileWriter(output);
        try
        {
            m_table.save(writer);
        }
        finally
        {
            writer.close();
        }
    }

    private static class Command
    {
        public boolean m_begin;

        public boolean m_final;

        public String m_command;

        public String m_columnTitle;
    }

    private boolean m_allowSetup;

    private boolean m_backward;

    private int m_max = Integer.MAX_VALUE;

    private int m_min = 0;

    private int m_numberGames;

    private int m_size;

    private double m_lastCpuTime = 0;

    private GtpClientBase m_gtp;

    private static final NumberFormat m_format1 =
        StringUtil.getNumberFormat(1);

    private static final NumberFormat m_format2 =
        StringUtil.getNumberFormat(2);

    private Table m_table;

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
        int size = tree.getBoardSize();
        if (size != m_size)
            throw new ErrorMessage(name + " has not size " + m_size);
        ConstNode root = tree.getRoot();
        GoColor toMove = GoColor.BLACK;
        for (ConstNode node = root; node != null; node = node.getChildConst())
        {
            if (node.hasSetup())
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
            String arg[] = StringUtil.splitArguments(response);
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
                return Move.get(toMove, point) == move ? "1" : "0";
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
        File file = new File(name);
        InputStream in = new FileInputStream(file);
        SgfReader reader = new SgfReader(in, file, null, 0);
        ++m_numberGames;
        GameTree tree = reader.getTree();
        checkGame(tree, name);
        int size = tree.getBoardSize();
        m_gtp.sendBoardsize(size);
        m_gtp.sendClearBoard(size);
        ConstNode root = tree.getRoot();
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
        try
        {
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
        catch (ErrorMessage e)
        {
            assert(false);
        }
    }

    private void iteratePositions(ConstNode root, String name) throws GtpError
    {
        int number = 0;
        GoColor toMove = GoColor.BLACK;
        for (ConstNode node = root; node != null; node = node.getChildConst())
        {
            if (node.hasSetup())
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

    private void iteratePositionsBackward(ConstNode root, String name)
        throws GtpError
    {
        ConstNode node = root;
        GoColor toMove = GoColor.BLACK;
        while (true)
        {
            if (node.hasSetup())
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
            ConstNode child = node.getChildConst();
            if (child == null)
                break;
            node = child;
        }
        int number = 1;
        boolean finalCommands = (node == root);
        boolean regularCommands = (number >= m_min && number <= m_max);
        handlePosition(name, toMove, null, number, true, regularCommands,
                       finalCommands);
        for ( ; node != root; node = node.getFatherConst())
        {
            // checked in checkGame
            assert(! node.hasSetup());
            Move move = node.getMove();
            if (move != null)
            {
                m_gtp.send("undo");
                ++number;
                finalCommands = (node.getFatherConst() == root);
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
    private GoColor sendSetup(ConstNode node) throws GtpError
    {
        ArrayList moves = new ArrayList();
        NodeUtil.getAllAsMoves(node, moves);
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

