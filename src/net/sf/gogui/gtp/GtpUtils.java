//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Utility functions for parsing GTP responses. */
public final class GtpUtils
{
    public static double[][] parseDoubleBoard(String response, int boardSize)
        throws GtpError
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
            throw new GtpError("Floating point number expected");
        }
    }

    public static GoPoint parsePoint(String s, int boardSize) throws GtpError
    {
        try
        {
            return GoPoint.parsePoint(s, boardSize);
        }
        catch (GoPoint.InvalidPoint e)
        {
            throw new GtpError("Invalid point " + s + " (size "
                               + boardSize + ")");
        }
    }
    
    public static GoPoint[] parsePointList(String s, int boardSize)
        throws GtpError
    {
        try
        {
            return GoPoint.parsePointList(s, boardSize);
        }
        catch (GoPoint.InvalidPoint e)
        {
            throw new GtpError("Invalid point or move");
        }
    }

    public static ArrayList parsePointArrayList(String s, int boardSize)
        throws GtpError
    {
        try
        {
            return GoPoint.parsePointListArrayList(s, boardSize);
        }
        catch (GoPoint.InvalidPoint e)
        {
            throw new GtpError("Invalid point or move");
        }
    }

    /** Find all points contained in string. */
    public static GoPoint[] parsePointString(String text, int boardSize)
    {
        String regex = "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1\\d|[1-9]))\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        ArrayList list = new ArrayList(32);
        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();
            GoPoint point;
            try
            {
                point = parsePoint(text.substring(start, end), boardSize);
            }
            catch (GtpError e)
            {
                assert(false);
                continue;
            }
            list.add(point);
        }
        GoPoint result[] = new GoPoint[list.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (GoPoint)list.get(i);
        return result;
    }

    public static void parsePointStringList(String s, ArrayList pointList,
                                            ArrayList stringList,
                                            int boardsize) throws GtpError
    {
        pointList.clear();
        stringList.clear();
        String array[] = StringUtils.splitArguments(s);
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
            throw new GtpError("Missing string");
    }

    public static String[][] parseStringBoard(String s, int boardSize)
        throws GtpError
    {
        String result[][] = new String[boardSize][boardSize];
        try
        {
            BufferedReader reader = new BufferedReader(new StringReader(s));
            for (int y = boardSize - 1; y >= 0; --y)
            {
                String line = reader.readLine();
                if (line == null)
                    throw new GtpError("Incomplete string board");
                if (line.trim().equals(""))
                {
                    ++y;
                    continue;
                }
                String[] args = StringUtils.splitArguments(line);
                if (args.length < boardSize)
                    throw new GtpError("Incomplete string board");
                for (int x = 0; x < boardSize; ++x)
                    result[x][y] = args[x];
            }
        }
        catch (IOException e)
        {
            throw new GtpError("I/O error");
        }
        return result;
    }

    /** Find all moves contained in string. */
    public static Move[] parseVariation(String s, GoColor toMove,
                                        int boardSize)
    {
        ArrayList list = new ArrayList(32);
        String token[] = StringUtils.splitArguments(s);
        boolean isColorSet = true;
        for (int i = 0; i < token.length; ++i)
        {
            String t = token[i].toLowerCase();
            if (t.equals("b") || t.equals("black"))
            {
                toMove = GoColor.BLACK;
                isColorSet = true;
            }
            else if (t.equals("w") || t.equals("white"))
            {
                toMove = GoColor.WHITE;
                isColorSet = true;
            }
            else
            {
                GoPoint point;
                try
                {
                    point = parsePoint(t, boardSize);
                }
                catch (GtpError e)
                {
                    continue;
                }
                if (! isColorSet)
                    toMove = toMove.otherColor();
                list.add(Move.get(point, toMove));
                isColorSet = false;
            }
        }
        Move result[] = new Move[list.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (Move)list.get(i);
        return result;
    }

    /** Construct a gogui-play_sequence command from a list of moves. */
    public static String getPlaySequenceCommand(ArrayList moves)
    {
        StringBuffer cmd = new StringBuffer();
        cmd.append("play_sequence");
        for (int i = 0; i < moves.size(); ++i)
        {
            Move move = (Move)moves.get(i);
            GoColor color = move.getColor();
            if (color == GoColor.BLACK)
                cmd.append(" b ");
            else if (color == GoColor.WHITE)
                cmd.append(" w ");
            else
                cmd.append(" empty ");
            cmd.append(GoPoint.toString(move.getPoint()));
        }
        return cmd.toString();
    }

    public static String getTimeSettingsCommand(TimeSettings settings)
    {
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

    /** Make constructor unavailable; class is for namespace only. */
    private GtpUtils()
    {
    }
}

//----------------------------------------------------------------------------
