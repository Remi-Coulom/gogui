//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import go.*;
import utils.*;

//-----------------------------------------------------------------------------

public class TwoGtp
    extends GtpServer
{
    public TwoGtp(InputStream in, OutputStream out, String black, String white,
                  int size, Float komi, int numberGames, boolean alternate,
                  String sgfFile, boolean verbose)
        throws Exception
    {
        super(in, out);
        if (black.equals(""))
            throw new Exception("No black program set.");
        if (white.equals(""))
            throw new Exception("No white program set.");
        m_sgfFile = sgfFile;
        findInitialGameIndex();
        readGames();
        m_black = new Gtp(black, verbose, null);
        m_black.setLogPrefix("B");
        m_white = new Gtp(white, verbose, null);
        m_white.setLogPrefix("W");
        m_inconsistentState = false;
        m_black.queryProtocolVersion();
        m_white.queryProtocolVersion();
        m_blackName = getName(m_black);
        m_whiteName = getName(m_white);        
        m_blackVersion = getVersion(m_black);
        m_whiteVersion = getVersion(m_white);        
        m_black.querySupportedCommands();
        m_white.querySupportedCommands();
        m_size = size;
        m_komi = komi;
        m_alternate = alternate;
        m_numberGames = numberGames;
        m_board = new Board(size < 1 ? 19 : size);
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
        else if (cmd.equals("gogui_title"))
            response.append(getTitle());
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
        else if (cmd.equals("komi"))
            komi(cmdLine);
        else if (cmd.equals("scoring_system"))
            sendIfSupported(cmd, cmdLine);
        else if (cmd.equals("name"))
            response.append("TwoGtp");
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
                            "gogui_title\n" +
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
                "alternate",
                "auto",
                "compare",
                "black:",
                "games:",
                "help",
                "komi:",
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
                    "-alternate alternate colors\n" +
                    "-auto      autoplay games\n" +
                    "-black     command for black program\n" +
                    "-compare   compare list of sgf files\n" +
                    "-games     number of games (0=unlimited)\n" +
                    "-help      display this help and exit\n" +
                    "-komi      komi\n" +
                    "-sgffile   filename prefix\n" +
                    "-size      board size for autoplay (default 19)\n" +
                    "-verbose   log GTP streams to stderr\n" +
                    "-white     command for white program\n";
                System.out.print(helpText);
                System.exit(0);
            }
            boolean compare = opt.isSet("compare");
            if (compare)
            {
                compare(opt.getArguments());
                System.exit(0);
            }
            boolean alternate = opt.isSet("alternate");
            boolean auto = opt.isSet("auto");
            boolean verbose = opt.isSet("verbose");
            String black = opt.getString("black", "");
            String white = opt.getString("white", "");
            int size = opt.getInteger("size", 0, 0);
            Float komi = null;
            if (opt.isSet("komi"))
                komi = new Float(opt.getFloat("komi"));
            else if (auto)
                komi = new Float(5.5);
            int defaultGames = (auto ? 1 : 0);
            int games = opt.getInteger("games", defaultGames, 0);
            String sgfFile = opt.getString("sgffile", "");
            if (opt.isSet("games") && sgfFile.equals(""))
                throw new Exception("Use option -sgffile with -games.");
            TwoGtp twoGtp = new TwoGtp(System.in, System.out, black, white,
                                       size, komi, games, alternate, sgfFile,
                                       verbose);
            if (auto)
                twoGtp.autoPlay();
            else
                twoGtp.mainLoop();
            twoGtp.close();
        }
        catch (Error e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (RuntimeException e)
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

    private boolean m_alternate;

    private boolean m_gameSaved;

    private boolean m_inconsistentState;

    private int m_gameIndex;

    private int m_numberGames;

    private int m_size;

    private Board m_board;

    private String m_blackName;

    private String m_blackVersion;

    private String m_sgfFile;

    private String m_whiteName;

    private String m_whiteVersion;

    private Float m_komi;

    private Vector m_games = new Vector(100, 100);;

    private Gtp m_black;

    private Gtp m_white;

    private void autoPlay() throws Exception
    {
        System.in.close();
        StringBuffer response = new StringBuffer(256);
        while (m_gameIndex < m_numberGames)
        {
            boolean error = false;
            response.setLength(0);
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
            if (error)
                handleEndOfGame(true, response.toString());
            if (m_black.isProgramDead())
                throw new Exception("Black program died");
            if (m_white.isProgramDead())
                throw new Exception("White program died");
        }
        m_black.sendCommand("quit");
        m_white.sendCommand("quit");
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

    private static String checkDuplicate(Board board, Vector moves,
                                         Vector games)
    {
        String result = "-";
        for (int numberGame = 0; numberGame < games.size(); ++numberGame)
        {
            Vector gameMoves = (Vector)games.get(numberGame);
            for (int rot = 0; rot < Board.NUMBER_ROTATIONS; ++rot)
            {
                int numberDifferent = 0;
                int moveNumber = moves.size();
                final int maxDifferent = moveNumber / 5;
                if (gameMoves.size() != moveNumber)
                {
                    numberDifferent = Math.abs(gameMoves.size() - moveNumber);
                    moveNumber = Math.min(gameMoves.size(), moveNumber);
                }
                for (int i = 0;
                     numberDifferent <= maxDifferent && i < moveNumber; ++i)
                {
                    Move move = (Move)moves.get(i);
                    Point point = move.getPoint();
                    Color color = move.getColor();
                    Move gameMove = (Move)gameMoves.get(i);
                    Point gamePoint = board.rotate(rot, gameMove.getPoint());
                    Color gameColor = gameMove.getColor();                    
                    if (! color.equals(gameColor)
                        || ! Point.equals(point, gamePoint))
                        ++numberDifferent;
                }
                if (numberDifferent == 0)
                    return Integer.toString(numberGame);
                else if (numberDifferent < maxDifferent)
                    result = Integer.toString(numberGame) + "?";
            }
        }
        return result;
    }

    private boolean checkInconsistentState(StringBuffer response)
    {
        if (m_inconsistentState)
            response.append("Inconsistent state");
        return m_inconsistentState;
    }

    private static void compare(Vector filenames) throws Exception
    {
        Board board = null;
        Vector games = new Vector();
        for (int gameNumber = 0; gameNumber < filenames.size(); ++gameNumber)
        {
            String filename = (String)filenames.get(gameNumber);
            File file = new File(filename);
            sgf.Reader reader =
                new sgf.Reader(new FileReader(file), file.toString());
            int size = reader.getBoardSize();
            if (board == null)
                board = new Board(size);
            else if (size != board.getSize())
                throw new Exception("Board size in " + filename +
                                    " does not match other games");
            if (reader.getSetupBlack().size() > 0
                || reader.getSetupWhite().size() > 0)
                throw new Exception("File " + filename +
                                    " contains setup stones");
            Vector moves = reader.getMoves();
            String duplicate = checkDuplicate(board, moves, games);
            System.out.println(Integer.toString(gameNumber) + " " +
                               filename + " " + duplicate);
            games.add(moves);
        }
    }

    private void findInitialGameIndex()
    {
        m_gameIndex = 0;
        try
        {
            InputStream inputStream = new FileInputStream(getResultFile());
            Reader reader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while (true)
            {
                line = bufferedReader.readLine();
                if (line == null)
                    break;
                if (line.trim().startsWith("#"))
                    continue;
                String[] elements = StringUtils.split(line, '\t');
                int gameIndex = Integer.parseInt(elements[0]);
                if (gameIndex + 1 > m_gameIndex)
                    m_gameIndex = gameIndex + 1;
            }
            bufferedReader.close();
        }
        catch (FileNotFoundException e)
        {
        }
        catch (IOException e)
        {
        }
    }

    private File getFile(int gameIndex)
    {
        return new File(m_sgfFile + "-" + gameIndex + ".sgf");
    }

    private Vector getMoves()
    {
        Vector moves = new Vector(128, 128);
        for (int i = 0; i < m_board.getMoveNumber(); ++i)
            moves.add(m_board.getMove(i));
        return moves;
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

    private static String getVersion(Gtp gtp)
    {
        try
        {
            String version = gtp.sendCommand("version");
            if (! version.trim().equals(""))
                return version;
        }
        catch (Gtp.Error e)
        {
        }
        return "";
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

    private File getResultFile()
    {
        return new File(m_sgfFile + ".dat");
    }

    private String getTitle()
    {
        StringBuffer buffer = new StringBuffer();
        String blackName = m_blackName;
        String whiteName = m_whiteName;
        if (blackName.equals(whiteName))
        {
            if (! m_blackVersion.equals(""))
                m_blackName = m_blackName + ":" + m_blackVersion;
            if (! m_whiteVersion.equals(""))
                m_whiteName = m_whiteName + ":" + m_whiteVersion;
        }
        if (isAlternated())
        {
            blackName = m_whiteName;
            whiteName = m_blackName;
        }
        buffer.append(blackName);
        buffer.append(" - ");
        buffer.append(whiteName);
        if (! m_sgfFile.equals(""))
        {
            buffer.append(" (");
            buffer.append(m_gameIndex + 1);
            buffer.append(")");
        }
        return buffer.toString();
    }

    private void handleEndOfGame(boolean error, String errorMessage)
    {
        try
        {
            String resultBlack = getResult(m_black);
            String resultWhite = getResult(m_white);
            if (isAlternated())
            {
                resultBlack = inverseResult(resultBlack);
                resultWhite = inverseResult(resultWhite);
            }
            Vector moves = getMoves();
            String duplicate = checkDuplicate(m_board, moves, m_games);
            saveResult(resultBlack, resultWhite, isAlternated(), duplicate,
                       moves.size(), error, errorMessage);
            saveGame(resultBlack, resultWhite);
            ++m_gameIndex;
            m_games.add(moves);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Could not save game: " + e.getMessage());
        }
    }

    private String inverseResult(String result)
    {
        if (result.indexOf("B") >= 0)
            return StringUtils.replace(result, "B", "W");
        else if (result.indexOf("W") >= 0)
            return StringUtils.replace(result, "W", "B");
        else
            return result;
    }

    private boolean isAlternated()
    {
        return (m_alternate && m_gameIndex % 2 != 0);
    }

    private void komi(String cmdLine)
    {
        if (m_komi != null)
            return;
        sendIfSupported("komi", cmdLine);
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
            m_black.sendCommandBoardsize(size);
            m_black.sendCommandClearBoard(size);
        }
        catch (Gtp.Error e)
        {
            response.append("B: " + e.getMessage());
            return false;
        }
        try
        {
            m_white.sendCommandBoardsize(size);
            m_white.sendCommandClearBoard(size);
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
        if (m_komi != null)
            sendIfSupported("komi", "komi " + m_komi.floatValue());
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

    private void readGames()
    {
        for (int n = 0; getFile(n).exists(); ++n)
        {
            File file = getFile(n);
            if (! file.exists())
                return;
            try
            {
                sgf.Reader reader =
                    new sgf.Reader(new FileReader(file), file.toString());
                m_games.add(reader.getMoves());
            }
            catch (FileNotFoundException e)
            {
                System.err.println("Error reading " + file + ": " +
                                   e.getMessage());
            }
            catch (sgf.Reader.Error e)
            {
                System.err.println("Error reading " + file + ": " +
                                   e.getMessage());
            }
        }
    }

    private void saveGame(String resultBlack, String resultWhite)
        throws FileNotFoundException
    {
        if (m_sgfFile.equals(""))
            return;
        String blackName = m_blackName;
        String whiteName = m_whiteName;
        String blackCommand = m_black.getProgramCommand();
        String whiteCommand = m_white.getProgramCommand();
        if (isAlternated())
        {
            blackName = m_whiteName;
            whiteName = m_blackName;
            blackCommand = m_white.getProgramCommand();
            whiteCommand = m_black.getProgramCommand();
            String resultTmp = inverseResult(resultWhite);
            resultWhite = inverseResult(resultBlack);
            resultBlack = resultTmp;
        }
        String host = "?";
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
        }
        String gameComment =
            "B: " + blackCommand +
            "\nW: " + whiteCommand +
            "\nHost: " + host +
            "\nResult according to B: " + resultBlack +
            "\nResult according to W: " + resultWhite;
        File file = getFile(m_gameIndex);
        OutputStream out = new FileOutputStream(file);
        new sgf.Writer(out, m_board, file, "TwoGtp", null, 0,
                       blackName, whiteName, gameComment, null);
    }

    private void saveResult(String resultBlack, String resultWhite,
                            boolean alternated, String duplicate,
                            int numberMoves, boolean error,
                            String errorMessage)
         throws FileNotFoundException
    {
        if (m_sgfFile.equals(""))
            return;
        File file = getResultFile();
        if (! file.exists())
        {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            PrintStream out = new PrintStream(fileOutputStream);
            out.println("# Game\tResB\tResW\tAlt\tDup\tLen\tErr\tErrMsg");
            out.close();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        PrintStream out = new PrintStream(fileOutputStream);
        out.println(Integer.toString(m_gameIndex) + "\t" + resultBlack + "\t" +
                    resultWhite + "\t" + (alternated ? "1" : "0" ) + "\t" +
                    duplicate + "\t" + numberMoves + "\t" +
                    (error ? "1" : "0" ) + "\t" + errorMessage);
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
        boolean alternate = isAlternated();
        if ((color == Color.BLACK && ! alternate)
            || (color == Color.WHITE && alternate))
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
        if (m_board.bothPassed() && ! m_gameSaved)
        {
            handleEndOfGame(false, "");
            m_gameSaved = true;
        }
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
