//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import go.*;
import utils.*;

//-----------------------------------------------------------------------------

public class TwoGtp
    extends GtpServer
{
    public TwoGtp(InputStream in, OutputStream out, String black, String white,
                  int size, int numberGames, String sgfFile, boolean verbose)
        throws Exception
    {
        super(in, out);
        if (black.equals(""))
            throw new Exception("No black program set.");
        if (white.equals(""))
            throw new Exception("No white program set.");
        m_black = new Gtp(black, verbose, null);
        m_black.setLogPrefix("B");
        m_white = new Gtp(white, verbose, null);
        m_white.setLogPrefix("W");
        m_inconsistentState = false;
        m_black.queryProtocolVersion();
        m_white.queryProtocolVersion();
        m_blackName = getName(m_black);
        m_whiteName = getName(m_white);        
        m_black.querySupportedCommands();
        m_white.querySupportedCommands();
        m_name = "TwoGtp (" + m_blackName + " - " + m_whiteName + ")";
        m_size = size;
        m_sgfFile = sgfFile;
        m_numberGames = numberGames;
        m_board = new Board(size < 1 ? 19 : size);
        findInitialGameIndex();
    }

    public void close()
    {
        m_black.close();
        m_white.close();
        m_black.waitForExit();
        m_white.waitForExit();
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.getCmdArray(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (cmd.equals("final_score")
            || cmd.equals("final_status")
            || cmd.equals("final_status_list"))
            return sendEither(cmdLine, response);
        else if (cmd.equals("loadsgf"))
            return sendBoth(cmdLine, response, true, false);
        else if (cmd.equals("twogtp_black"))
            status = twogtpColor(m_black, cmdLine, response);
        else if (cmd.equals("twogtp_white"))
            status = twogtpColor(m_white, cmdLine, response);
        else if (cmd.equals("quit"))
            status = sendBoth(cmdLine, response, false, false);
        else if (cmd.equals("black"))
            status = play(Color.BLACK, cmdArray, response);
        else if (cmd.equals("white"))
            status = play(Color.WHITE, cmdArray, response);
        else if (cmd.equals("undo"))
            status = undo(response);
        else if (cmd.equals("genmove_black"))
            status = sendGenmove(Color.BLACK, response);
        else if (cmd.equals("genmove_white"))
            status = sendGenmove(Color.WHITE, response);
        else if (cmd.equals("boardsize"))
            status = boardsize(cmdArray, response);
        else if (cmd.equals("komi") || cmd.equals("scoring_system"))
            sendIfSupported(cmd, cmdLine);
        else if (cmd.equals("name"))
            response.append(m_name);
        else if (cmd.equals("version"))
            ;
        else if (cmd.equals("protocol_version"))
            response.append("1");
        else if (cmd.equals("help"))
            response.append("boardsize\n" +
                            "black\n" +
                            "final_score\n" +
                            "final_status\n" +
                            "final_status_list\n" +
                            "genmove_black\n" +
                            "genmove_white\n" +
                            "help\n" +
                            "komi\n" +
                            "loadsgf\n" +
                            "name\n" +
                            "quit\n" +
                            "scoring_system\n" +
                            "twogtp_black\n" +
                            "twogtp_white\n" +
                            "undo\n" +
                            "version\n" +
                            "white\n");
        else if (cmd.equals("genmove"))
        {
            response.append("command not supported in protocol version 1");
            status = false;
        }
        else
        {
            boolean isExtCommandBlack = m_black.isCommandSupported(cmd);
            boolean isExtCommandWhite = m_white.isCommandSupported(cmd);
            if (isExtCommandBlack && ! isExtCommandWhite)
                return sendSingle(m_black, cmdLine, response);
            if (isExtCommandWhite && ! isExtCommandBlack)
                return sendSingle(m_white, cmdLine, response);
            if (isExtCommandWhite && isExtCommandBlack)
            {
                response.append("use twogtp_black/white to specify program");
                return false;
            }
            response.append("unknown command");
            status = false;
        }
        return status;
    }

    public void interruptCommand()
    {
        m_black.sendInterrupt();
        m_white.sendInterrupt();
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "auto",
                "black:",
                "games:",
                "help",
                "sgffile:",
                "size:",
                "verbose",
                "white:"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar twogtp.jar [options]\n" +
                    "\n" +
                    "-auto    autoplay games\n" +
                    "-black   command for black program\n" +
                    "-games   number of games (0=unlimited)\n" +
                    "-help    display this help and exit\n" +
                    "-sgffile filename prefix\n" +
                    "-size    board size for autoplay (default 19)\n" +
                    "-verbose log GTP streams to stderr\n" +
                    "-white   command for white program\n";
                System.out.print(helpText);
                System.exit(0);
            }
            boolean auto = opt.isSet("auto");
            boolean verbose = opt.isSet("verbose");
            String black = opt.getString("black", "");
            String white = opt.getString("white", "");
            int size = opt.getInteger("size", 0, 0);
            int defaultGames = (auto ? 1 : 0);
            int games = opt.getInteger("games", defaultGames, 0);
            String sgfFile = opt.getString("sgffile", "");
            TwoGtp twoGtp = new TwoGtp(System.in, System.out, black, white,
                                       size, games, sgfFile, verbose);
            if (auto)
                twoGtp.autoPlay();
            else
                twoGtp.mainLoop();
            twoGtp.close();
        }
        catch (AssertionError e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            System.exit(-1);
        }
    }

    private boolean m_gameSaved;

    private boolean m_inconsistentState;

    private int m_gameIndex;

    private int m_numberGames;

    private int m_size;

    private Board m_board;

    private String m_blackName;

    private String m_name;

    private String m_sgfFile;

    private String m_whiteName;

    private Gtp m_black;

    private Gtp m_white;

    private void autoPlay() throws Exception
    {
        System.in.close();
        StringBuffer response = new StringBuffer(256);
        while (m_gameIndex < m_numberGames)
        {
            boolean error = false;
            if (newGame(m_size > 0 ? m_size : 19, response))
            {
                int numberPass = 0;
                while (! m_board.bothPassed())
                {
                    response.setLength(0);
                    if (! sendGenmove(m_board.getToMove(), response))
                    {
                        error = true;
                        break;
                    }
                }
            }
            else
                error = true;
            String errorMsg = null;
            if (error)
                throw new Exception(response.toString());
        }
        close();
    }

    private boolean boardsize(String[] cmdArray, StringBuffer response)
    {
        if (cmdArray.length < 2)
        {
            response.append("Missing argument");
            return false;
        }
        if (m_numberGames > 0 && m_gameIndex >= m_numberGames)
        {
            response.append("Maximum number of " + m_numberGames +
                            " games reached");
            return false;
        }
        try
        {
            int size = Integer.parseInt(cmdArray[1]);
            if (size < 1)
            {
                response.append("Invalid argument");
                return false;
            }
            if (m_size > 0 && size != m_size)
            {
                response.append("Size must be ");
                response.append(m_size);
                return false;
            }
            return newGame(size, response);
        }
        catch (NumberFormatException e)
        {
            response.append("Invalid argument");
            return false;
        }
    }

    private void checkEndOfGame()
    {
        if (m_board.bothPassed() && ! m_gameSaved)
        {
            try
            {
                String resultBlack = getResult(m_black);
                String resultWhite = getResult(m_white);
                saveResult(resultBlack, resultWhite);
                saveGame();
            }
            catch (FileNotFoundException e)
            {
                System.err.println("Could not save game: " + e.getMessage());
            }
            m_gameSaved = true;
        }
    }

    private boolean checkInconsistentState(StringBuffer response)
    {
        if (m_inconsistentState)
            response.append("Inconsistent state");
        return m_inconsistentState;
    }

    private void findInitialGameIndex()
    {
        m_gameIndex = 0;
        while (getFile(m_gameIndex).exists())
            ++m_gameIndex;
    }

    private File getFile(int gameIndex)
    {
        return new File(m_sgfFile + "-" + gameIndex + ".sgf");
    }

    private static String getName(Gtp gtp)
    {
        try
        {
            String name = gtp.sendCommand("name");
            if (! name.trim().equals(""))
                return name;
        }
        catch (Gtp.Error e)
        {
        }
        return "Unknown";
    }

    private static String getResult(Gtp gtp)
    {
        try
        {
            return gtp.sendCommand("final_score");
        }
        catch (Gtp.Error e)
        {
            return "?";
        }
    }

    private void mergeResponse(StringBuffer response,
                               String response1, String response2,
                               String prefix1, String prefix2)
    {
        boolean empty1 = (response1 == null || response1.equals(""));
        boolean empty2 = (response2 == null || response2.equals(""));        
        if (empty1 && empty2)
            return;
        boolean isMultiLine =
            ((response1 != null && response1.indexOf('\n') >= 0)
             || (response2 != null && response2.indexOf('\n') >= 0));
        if (! empty1)
        {
            response.append(prefix1);
            if (isMultiLine)
                response.append(":\n");
            else
                response.append(": ");
            response.append(response1);
            if (! empty2)
            {
                if (isMultiLine)
                    response.append("\n");
                else
                    response.append("  ");
            }
        }
        if (! empty2)
        {
            response.append(prefix2);
            if (isMultiLine)
                response.append(":\n");
            else
                response.append(": ");
            response.append(response2);
        }
        if (isMultiLine)
            StringUtils.replace(response, "\n\n", "\n");
    }

    private boolean newGame(int size, StringBuffer response)
    {
        try
        {
            m_black.sendCommandsClearBoard(size);
        }
        catch (Gtp.Error e)
        {
            response.append("B: " + e.getMessage());
            return false;
        }
        try
        {
            m_white.sendCommandsClearBoard(size);
        }
        catch (Gtp.Error e)
        {
            response.append("W: " + e.getMessage());
            m_inconsistentState = true;
            return false;
        }
        m_inconsistentState = false;
        m_board = new Board(size);
        m_gameSaved = false;
        return true;
    }

    private boolean play(Color color, String[] cmdArray, StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        if (cmdArray.length < 2)
        {
            response.append("Missing argument");
            return false;
        }
        Point point = null;
        try
        {
            point = Gtp.parsePoint(cmdArray[1]);
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }
        Move move = new Move(point, color);
        String cmdBlack = m_black.getCommandPlay(move);
        String cmdWhite = m_white.getCommandPlay(move);
        boolean status =
            send(m_black, m_white, cmdBlack, cmdWhite, response, true, true);
        if (status)
            m_board.play(new Move(point, color));
        return status;
    }

    private void saveGame() throws FileNotFoundException
    {
        if (m_sgfFile.equals(""))
            return;
        new sgf.Writer(getFile(m_gameIndex), m_board, "TwoGtp", null, 0,
                       m_blackName, m_whiteName, null, null);
        ++m_gameIndex;
    }

    private void saveResult(String resultBlack, String resultWhite)
         throws FileNotFoundException
    {
        File file = new File(m_sgfFile + ".dat");
        if (! file.exists())
        {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            PrintStream out = new PrintStream(fileOutputStream);
            out.println("# Game\tResB\tResW");
            out.close();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        PrintStream out = new PrintStream(fileOutputStream);
        out.println(Integer.toString(m_gameIndex)
                    + "\t" + resultBlack
                    + "\t" + resultWhite);
        out.close();
    }

    private boolean send(Gtp gtp1, Gtp gtp2, String command1, String command2,
                         StringBuffer response, boolean changesState,
                         boolean tryUndo)
    {
        assert((gtp1 == m_black && gtp2 == m_white)
               || (gtp1 == m_white && gtp2 == m_black));
        if (changesState && checkInconsistentState(response))
            return false;
        String prefix1 = (gtp1 == m_black ? "B" : "W");
        String prefix2 = (gtp2 == m_black ? "B" : "W");
        String response1 = null;
        String response2 = null;
        boolean status = true;
        try
        {
            response1 = gtp1.sendCommand(command1);
        }
        catch (Gtp.Error e)
        {
            response1 = e.getMessage();
            status = false;
            if (changesState)
            {
                mergeResponse(response, response1, response2, prefix1,
                              prefix2);
                return status;
            }
        }
        try
        {
            response2 = gtp2.sendCommand(command2);
        }
        catch (Gtp.Error e)
        {
            response2 = e.getMessage();
            if (changesState && status)
            {
                if (tryUndo)
                {
                    try
                    {
                        gtp1.sendCommand("undo");
                    }
                    catch (Gtp.Error errorUndo)
                    {
                        m_inconsistentState = true;
                    }
                }
                else
                    m_inconsistentState = true;
            }
            status = false;
        }
        mergeResponse(response, response1, response2, prefix1, prefix2);
        return status;
    }

    private boolean sendBoth(String command, StringBuffer response,
                             boolean changesState, boolean tryUndo)
    {
        return send(m_black, m_white, command, command, response, changesState,
                    tryUndo);
    }

    private boolean sendEither(String command, StringBuffer response)
    {
        if (sendSingle(m_black, command, response))
            return true;
        response.setLength(0);
        return sendSingle(m_white, command, response);
    }

    private boolean sendSingle(Gtp gtp, String command, StringBuffer response)
    {
        try
        {
            response.append(gtp.sendCommand(command));
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }        
        return true;
    }

    private boolean sendGenmove(Color color, StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        Gtp gtp1;
        Gtp gtp2;
        String prefix1;
        String prefix2;
        String command;
        String command2;
        if (color == Color.BLACK)
        {
            gtp1 = m_black;
            gtp2 = m_white;
            prefix1 = "B";
            prefix2 = "W";
            command = m_black.getCommandGenmove(color);
            command2 = m_white.getCommandPlay(color);
        }
        else
        {
            gtp1 = m_white;
            gtp2 = m_black;
            prefix1 = "W";
            prefix2 = "B";
            command = m_white.getCommandGenmove(color);
            command2 = m_black.getCommandPlay(color);
        }
        String response1 = null;
        String response2 = null;
        try
        {
            response1 = gtp1.sendCommand(command);
        }
        catch (Gtp.Error e)
        {
            response1 = e.getMessage();
            mergeResponse(response, response1, response2, prefix1,
                          prefix2);
            return false;
        }
        Point point = null;
        try
        {
            point = Gtp.parsePoint(response1);
        }
        catch (Gtp.Error e)
        {
            response.append(prefix1 + " played invalid move");
            m_inconsistentState = true;
            return false;
        }
        command2 = command2 + " " + response1;
        try
        {
            response2 = gtp2.sendCommand(command2);
        }
        catch (Gtp.Error e)
        {
            response2 = e.getMessage();
            try
            {
                gtp1.sendCommand("undo");
            }
            catch (Gtp.Error errorUndo)
            {
                m_inconsistentState = true;
            }
            mergeResponse(response, response1, response2, prefix1, prefix2);
            return false;
        }
        response.append(response1);
        m_board.play(new Move(point, color));
        checkEndOfGame();
        return true;
    }

    private void sendIfSupported(String cmd, String cmdLine)
    {
        sendIfSupported(m_black, cmd, cmdLine);
        sendIfSupported(m_white, cmd, cmdLine);
    }

    private void sendIfSupported(Gtp gtp, String cmd, String cmdLine)
    {
        if (gtp.isCommandSupported(cmd))
        {
            StringBuffer response = new StringBuffer();
            sendSingle(gtp, cmdLine, response);
        }
    }

    private boolean twogtpColor(Gtp gtp, String command, StringBuffer response)
    {
        int index = command.indexOf(' ');
        if (index < 0)
        {
            response.append("Missing argument");
            return false;
        }
        command = command.substring(index).trim();
        return sendSingle(gtp, command, response);
    }

    private boolean undo(StringBuffer response)
    {
        if (checkInconsistentState(response))
            return false;
        boolean status = sendBoth("undo", response, true, false);
        if (status)
            m_board.undo();
        return status;
    }
}

//-----------------------------------------------------------------------------
