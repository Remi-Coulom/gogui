//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Utility functions for parsing GTP responses. */
public class GtpUtils
{
    public static double[][] parseDoubleBoard(String response, String title,
                                              int boardSize) throws GtpError
    {
        try
        {
            double result[][] = new double[boardSize][boardSize];
            String s[][] = parseStringBoard(response, title, boardSize);
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
            throw new GtpError("Invalid point or move");
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

    public static Vector parsePointListVector(String s, int boardSize)
        throws GtpError
    {
        try
        {
            return GoPoint.parsePointListVector(s, boardSize);
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
        Vector vector = new Vector(32, 32);
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
            vector.add(point);
        }
        GoPoint result[] = new GoPoint[vector.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (GoPoint)vector.get(i);
        return result;
    }

    public static void parsePointStringList(String s, Vector pointList,
                                            Vector stringList,
                                            int boardsize) throws GtpError
    {
        pointList.clear();
        stringList.clear();
        String array[] = StringUtils.tokenize(s);
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
            throw new GtpError("Missing string.");
    }

    public static String[][] parseStringBoard(String s, String title,
                                              int boardSize) throws GtpError
    {
        String result[][] = new String[boardSize][boardSize];
        try
        {
            BufferedReader reader = new BufferedReader(new StringReader(s));
            if (title != null && ! title.trim().equals(""))
            {
                String pattern = title + ":";
                while (true)
                {
                    String line = reader.readLine();
                    if (line == null)
                        throw new GtpError(title + " not found.");
                    if (line.trim().equals(pattern))
                        break;
                }
            }
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
                String[] tokens = StringUtils.tokenize(line);
                if (tokens.length < boardSize)
                    throw new GtpError("Incomplete string board");
                for (int x = 0; x < boardSize; ++x)
                    result[x][y] = tokens[x];
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
        Vector vector = new Vector(32, 32);
        String token[] = StringUtils.tokenize(s);
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
                vector.add(Move.create(point, toMove));
                isColorSet = false;
            }
        }
        Move result[] = new Move[vector.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (Move)vector.get(i);
        return result;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GtpUtils()
    {
    }
}

//----------------------------------------------------------------------------
