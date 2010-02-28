// Statistics.java

package net.sf.gogui.tools.statistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.util.Table;

/** Run commands of a GTP engine on all positions in a game collection. */
public class Statistics
{
    public void run(String program, ArrayList<String> sgfFiles, int size,
                    ArrayList<String> commands,
                    ArrayList<String> beginCommands,
                    ArrayList<String> finalCommands, boolean verbose,
                    boolean allowSetup, boolean backward, boolean random)
        throws ErrorMessage, GtpError, IOException
    {
        run(new GtpClient(program, null, verbose, null), program, sgfFiles,
            size, commands, beginCommands, finalCommands, allowSetup,
            backward, random);
    }

    /** Construct with existing GTP engine.
        @param gtp The GTP engine
        @param program Program command (null, if gtp is not an instance of
        GtpClient)
        @param sgfFiles List containing the SGF file names
        @param size The board size used in the games (all games must have the
        same board size)
        @param commands List containing the commands to run in every position
        @param beginCommands List containing the commands to run in the first
        position
        @param finalCommands List containing the commands to run in the last
        position
        @param allowSetup true, if setup stones in the games are allowed and
        should not generate en error
        @param backward true, if games should be iterated backwards (counting
        the moves starting with one at the last move)
        @param random true, if only one random position should be selected
        from each game */
    public void run(GtpClientBase gtp, String program,
                    ArrayList<String> sgfFiles, int size,
                    ArrayList<String> commands,
                    ArrayList<String> beginCommands,
                    ArrayList<String> finalCommands, boolean allowSetup,
                    boolean backward, boolean random)
        throws ErrorMessage, IOException
    {
        new FileCheck(sgfFiles, size, allowSetup);
        m_size = size;
        m_allowSetup = allowSetup;
        m_backward = backward;
        m_random = random;
        initCommands(commands, beginCommands, finalCommands);
        ArrayList<String> columnHeaders = new ArrayList<String>();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        for (int i = 0; i < m_commands.size(); ++i)
            columnHeaders.add(getCommand(i).m_columnTitle);
        m_table = new Table(columnHeaders);
        m_table.setProperty("Size", Integer.toString(size));
        m_gtp = gtp;
        m_synchronizer = new GtpSynchronizer(m_gtp);
        m_gtp.queryProtocolVersion();
        m_gtp.queryName();
        if (program != null)
            m_table.setProperty("Program", program);
        m_table.setProperty("Name", m_gtp.getLabel());
        m_table.setProperty("Version", m_gtp.queryVersion());
        String host = Platform.getHostInfo();
        m_table.setProperty("Host", host);
        m_table.setProperty("Date", StringUtil.getDate());
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile(sgfFiles.get(i));
        m_gtp.send("quit");
        m_gtp.close();
        m_gtp.waitForExit();
        m_table.setProperty("Games", Integer.toString(m_numberGames));
        m_table.setProperty("Backward", backward ? "yes" : "no");
        m_table.setProperty("Random", random ? "yes" : "no");
    }

    /** Set maximum move number for positions to run the commands on.
        Default is Integer.MAX_VALUE. */
    public void setMax(int max)
    {
        m_max = max;
    }

    /** Set minimum move number for positions to run the commands on.
        Default is zero. */
    public void setMin(int min)
    {
        m_min = min;
    }

    /** Don't write information about progress.
        Default is false. */
    public void setQuiet(boolean enable)
    {
        m_quiet = enable;
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

    private boolean m_random;

    private boolean m_quiet;

    private int m_max = Integer.MAX_VALUE;

    private int m_min = 0;

    private int m_numberGames;

    private int m_size;

    private double m_lastCpuTime = 0;

    private GtpClientBase m_gtp;

    private static final NumberFormat FORMAT1 = StringUtil.getNumberFormat(1);

    private static final NumberFormat FORMAT2 = StringUtil.getNumberFormat(2);

    private Table m_table;

    private ArrayList<Command> m_commands;

    private GtpSynchronizer m_synchronizer;

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

    private void addCommands(ArrayList<String> commands, boolean isBegin,
                             boolean isFinal) throws ErrorMessage
    {
        for (String c : commands)
            addCommand(c, isBegin, isFinal);
    }

    private void checkGame(ConstGameTree tree, String name) throws ErrorMessage
    {
        int size = tree.getBoardSize();
        if (size != m_size)
            throw new ErrorMessage(name + " has not size " + m_size);
        ConstNode root = tree.getRootConst();
        GoColor toMove = BLACK;
        for (ConstNode node = root; node != null; node = node.getChildConst())
        {
            if (node.hasSetup())
            {
                if (m_allowSetup)
                {
                    if (node == root)
                        toMove = EMPTY;
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
                if (toMove == EMPTY)
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
                return FORMAT2.format(diff);
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
            catch (InvalidPointException e)
            {
                throw new GtpError("Program sent invalid move: " + response);
            }
        }
        return response;
    }

    /** Tries to convert score into number.
        @return Score string or original string, if conversion fails. */
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
            return FORMAT1.format(sign * Double.parseDouble(score));
        }
        catch (NumberFormatException e)
        {
            return string;
        }
    }

    private void initCommands(ArrayList<String> commands,
                              ArrayList<String> beginCommands,
                              ArrayList<String> finalCommands)
        throws ErrorMessage
    {
        m_commands = new ArrayList<Command>();
        if (beginCommands != null)
            addCommands(beginCommands, true, false);
        if (commands != null)
            addCommands(commands, false, false);
        if (finalCommands != null)
            addCommands(finalCommands, false, true);
        if (m_commands.isEmpty())
            throw new ErrorMessage("No commands defined");
    }

    private Command getCommand(int index)
    {
        return m_commands.get(index);
    }

    private void handleFile(String name)
        throws ErrorMessage, FileNotFoundException, GtpError,
               SgfError
    {
        File file = new File(name);
        InputStream in = new FileInputStream(file);
        SgfReader reader = new SgfReader(in, file, null, 0);
        ++m_numberGames;
        Game game = new Game(reader.getTree());
        checkGame(game.getTree(), name);
        if (m_random)
            iteratePositionsRandom(game, name);
        else if (m_backward)
            iteratePositionsBackward(game, name);
        else
            iteratePositions(game, name);
    }

    private void handlePosition(String name, GoColor toMove, Move move,
                                int number, boolean beginCommands,
                                boolean regularCommands,
                                boolean finalCommands)
        throws GtpError
    {
        if (! m_quiet)
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
        catch (Table.InvalidLocation e)
        {
            System.err.println(e.getMessage());
            // Table was created by this class in correct format
            assert false;
        }
    }

    private void iteratePositions(Game game, String name) throws GtpError
    {
        int number = 0;
        for (ConstNode node = game.getRoot(); node != null;
             node = node.getChildConst())
        {
            game.gotoNode(node);
            synchronize(game);
            Move move = node.getMove();
            boolean beginCommands = ! node.hasFather();
            boolean regularCommands =
                ((move != null || node.hasSetup() || ! node.hasFather())
                 && number >= m_min && number <= m_max);
            boolean finalCommands = ! node.hasChildren();
            if (beginCommands || regularCommands || finalCommands)
                handlePosition(name, node.getToMove(), move, number,
                               beginCommands, regularCommands, finalCommands);
            ++number;
        }
    }

    private void iteratePositionsBackward(Game game, String name)
        throws GtpError
    {
        int number = 0;
        for (ConstNode node = NodeUtil.getLast(game.getRoot()); node != null;
             node = node.getFatherConst())
        {
            game.gotoNode(node);
            synchronize(game);
            Move move = node.getMove();
            boolean beginCommands = ! node.hasChildren();
            boolean regularCommands =
                ((move != null || node.hasSetup() || ! node.hasFather())
                 && number >= m_min && number <= m_max);
            boolean finalCommands = ! node.hasFather();
            if (beginCommands || regularCommands || finalCommands)
                handlePosition(name, node.getToMove(), move, number,
                               beginCommands, regularCommands, finalCommands);
            ++number;
        }
    }

    private void iteratePositionsRandom(Game game, String name)
        throws GtpError
    {
        int minDepth;
        int maxDepth;
        if (m_backward)
        {
            int depth = NodeUtil.getDepth(NodeUtil.getLast(game.getRoot()));
            minDepth = depth - m_max;
            maxDepth = depth - m_min;
        }
        else
        {
            minDepth = m_min;
            maxDepth = m_max;
        }
        ConstNode node = NodeUtil.selectRandom(game.getRoot(), minDepth,
                                               maxDepth);
        if (node == null)
            return;
        int number = NodeUtil.getDepth(node);
        game.gotoNode(node);
        synchronize(game);
        Move move = node.getMove();
        boolean beginCommands = ! node.hasChildren();
        boolean regularCommands =
            (move != null || node.hasSetup() || ! node.hasFather());
        boolean finalCommands = ! node.hasFather();
        if (beginCommands || regularCommands || finalCommands)
            handlePosition(name, node.getToMove(), move, number,
                           beginCommands, regularCommands, finalCommands);
    }

    private String send(String command, GoColor toMove, Move move)
        throws GtpError
    {
        String cmd = convertCommand(command, toMove);
        String response = m_gtp.send(cmd).trim();
        response = response.replaceAll("\t", " ");
        response = response.replaceAll("\n", " ");
        return convertResponse(command, response, toMove, move);
    }

    private void synchronize(ConstGame game) throws GtpError
    {
        ConstNode node = game.getGameInfoNode();
        ConstGameInfo info = game.getGameInfo(node);
        m_synchronizer.synchronize(game.getBoard(), info.getKomi(),
                                   info.getTimeSettings());
    }
}
