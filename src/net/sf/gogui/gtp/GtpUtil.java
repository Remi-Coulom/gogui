// GtpUtil.java

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.StringUtil;

/** Utility functions used in package gtp. */
public final class GtpUtil
{
    /** Get GTP time settings command .
        @param settings The time settings. If null, this function will return
        a GTP command for "no time limit" ("time_settings 0 1 0" with zero
        byoyomi stones), which could confuse some programs, so it should be
        only sent if necessary (when changing from a state with time settings
        to a state with no time settings). */
    public static String getTimeSettingsCommand(TimeSettings settings)
    {
        if (settings == null)
            return "time_settings 0 1 0";
        long preByoyomi = settings.getPreByoyomi() / 1000;
        long byoyomi = 0;
        long byoyomiMoves = 0;
        if (settings.getUseByoyomi())
        {
            byoyomi = settings.getByoyomi() / 1000;
            byoyomiMoves = settings.getByoyomiMoves();
        }
        return "time_settings " + preByoyomi + " " + byoyomi + " "
            + byoyomiMoves;
    }

    /** Check if command line contains a command.
        @return false if command line contains only whitespaces or only a
        comment */
    public static boolean isCommand(String line)
    {
        line = line.trim();
        return (! line.equals("") && ! line.startsWith("#"));
    }

    /** Check if command changes the board state.
        Compares command to known GTP commands that change the board state,
        such that a controller that keeps track of the board state cannot
        blindly forward them to a GTP engine without updating its internal
        board state too. Includes all such commands from GTP protocol version
        1 and 2, the <code>quit</code> command, and some known GTP extension
        commands (e.g. <code>gg-undo</code> from GNU Go and
        <code>gogui-play_sequence</code> from GoGui). Does not include
        non-criticlal state changing commands like <code>komi</code>.
        @param line The command or complete command line
        @return <code>true</code> if command is a state-changing command */
    public static boolean isStateChangingCommand(String line)
    {
        GtpCommand cmd = new GtpCommand(line);
        String c = cmd.getCommand();
        return (c.equals("boardsize")
                || c.equals("black")
                || c.equals("clear_board")
                || c.equals("fixed_handicap")
                || c.equals("genmove")
                || c.equals("genmove_black")
                || c.equals("genmove_cleanup")
                || c.equals("genmove_white")
                || c.equals("gg-undo")
                || c.equals("gogui-play_sequence")
                || c.equals("kgs-genmove_cleanup")
                || c.equals("loadsgf")
                || c.equals("place_free_handicap")
                || c.equals("play")
                || c.equals("play_sequence")
                || c.equals("quit")
                || c.equals("set_free_handicap")
                || c.equals("undo")
                || c.equals("white"));
    }

    public static double[][] parseDoubleBoard(String response, int boardSize)
        throws GtpResponseFormatError
    {
        try
        {
            double result[][] = new double[boardSize][boardSize];
            String s[][] = parseStringBoard(response, boardSize);
            for (int x = 0; x < boardSize; ++x)
                for (int y = 0; y < boardSize; ++y)
                    result[x][y] = Double.parseDouble(s[x][y]);
            return result;
        }
        catch (NumberFormatException e)
        {
            throw new GtpResponseFormatError("Floating point number expected");
        }
    }

    public static GoPoint parsePoint(String s, int boardSize)
        throws GtpResponseFormatError
    {
        try
        {
            return GoPoint.parsePoint(s, boardSize);
        }
        catch (InvalidPointException e)
        {
            throw new GtpResponseFormatError("Invalid point " + s + " (size "
                                          + boardSize + ")");
        }
    }

    public static PointList parsePointList(String s, int boardSize)
        throws GtpResponseFormatError
    {
        try
        {
            return GoPoint.parsePointList(s, boardSize);
        }
        catch (InvalidPointException e)
        {
            throw new GtpResponseFormatError(e.getMessage());
        }
    }

    /** Find all points contained in string. */
    public static PointList parsePointString(String text)
    {
        return parsePointString(text, GoPoint.MAX_SIZE);
    }

    /** Find all points contained in string. */
    public static PointList parsePointString(String text, int boardSize)
    {
        String regex = "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1\\d|[1-9]))\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        PointList list = new PointList(32);
        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();
            GoPoint point;
            try
            {
                point = parsePoint(text.substring(start, end), boardSize);
            }
            catch (GtpResponseFormatError e)
            {
                assert false;
                continue;
            }
            list.add(point);
        }
        return list;
    }

    public static void parsePointStringList(String s, PointList pointList,
                                            ArrayList<String> stringList,
                                            int boardsize)
        throws GtpResponseFormatError
    {
        pointList.clear();
        stringList.clear();
        String array[] = StringUtil.splitArguments(s);
        boolean nextIsPoint = true;
        GoPoint point = null;
        for (int i = 0; i < array.length; ++i)
            if (! array[i].equals(""))
            {
                if (nextIsPoint)
                {
                    point = parsePoint(array[i], boardsize);
                    nextIsPoint = false;
                }
                else
                {
                    nextIsPoint = true;
                    pointList.add(point);
                    stringList.add(array[i]);
                }
            }
        if (! nextIsPoint)
            throw new GtpResponseFormatError("Missing string");
    }

    public static String[][] parseStringBoard(String s, int boardSize)
        throws GtpResponseFormatError
    {
        String result[][] = new String[boardSize][boardSize];
        try
        {
            BufferedReader reader = new BufferedReader(new StringReader(s));
            for (int y = boardSize - 1; y >= 0; --y)
            {
                String line = reader.readLine();
                if (line == null)
                    throw new GtpResponseFormatError("Incomplete string board");
                if (line.trim().equals(""))
                {
                    ++y;
                    continue;
                }
                String[] args = StringUtil.splitArguments(line);
                if (args.length < boardSize)
                    throw new GtpResponseFormatError("Incomplete string board");
                for (int x = 0; x < boardSize; ++x)
                    result[x][y] = args[x];
            }
        }
        catch (IOException e)
        {
            throw new GtpResponseFormatError("I/O error");
        }
        return result;
    }

    /** Find all moves contained in string. */
    public static Move[] parseVariation(String s, GoColor toMove,
                                        int boardSize)
    {
        ArrayList<Move> list = new ArrayList<Move>(32);
        String token[] = StringUtil.splitArguments(s);
        boolean isColorSet = true;
        for (int i = 0; i < token.length; ++i)
        {
            String t = token[i].toLowerCase(Locale.ENGLISH);
            if (t.equals("b") || t.equals("black"))
            {
                toMove = BLACK;
                isColorSet = true;
            }
            else if (t.equals("w") || t.equals("white"))
            {
                toMove = WHITE;
                isColorSet = true;
            }
            else
            {
                GoPoint point;
                try
                {
                    point = parsePoint(t, boardSize);
                }
                catch (GtpResponseFormatError e)
                {
                    continue;
                }
                if (! isColorSet)
                    toMove = toMove.otherColor();
                list.add(Move.get(toMove, point));
                isColorSet = false;
            }
        }
        Move result[] = new Move[list.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (Move)list.get(i);
        return result;
    }

    /** Inform a GTP engine about the clock state.
        Sends the clock state to the engine, if the clock is initialized and
        the engine supports the time_left standard GTP command. Otherwise,
        does nothing. */
    public static void sendTimeLeft(GtpClientBase gtp, ConstClock clock,
                                    GoColor c)
    {
        if (! clock.isInitialized() || ! gtp.isSupported("time_left"))
            return;
        String color = (c == BLACK ? "b" : "w");
        long timeLeft = clock.getTimeLeft(c) / 1000;
        long movesLeft = 0;
        if (clock.getTimeSettings().getUseByoyomi()
            && clock.isInByoyomi(c))
            movesLeft = clock.getMovesLeft(c);
        try
        {
            gtp.send("time_left " + color + " " + timeLeft + " "
                     + movesLeft);
        }
        catch (GtpError e)
        {
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GtpUtil()
    {
    }
}
