// GtpCommand.java

package net.sf.gogui.gtp;

import java.util.Locale;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.StringUtil;

/** GTP command.
    Handles parsing the command line and storing the response to the command.
    Arguments containing whitespaces can be quoted with double quotes (").
    The responses are allowed to contain consecutive new lines.
    They will be replaced in GtpEngine.mainLoop() by lines containing a single
    space to form a valid GTP response. */
public class GtpCommand
{
    /** Construct command from command line.
        @param line The full command line including ID. */
    public GtpCommand(String line)
    {
        StringBuilder buffer = preprocessLine(line);
        assert ! line.trim().equals("");
        String[] array = StringUtil.splitArguments(buffer.toString());
        assert array.length > 0;
        int commandIndex = 0;
        try
        {
            m_id = Integer.parseInt(array[0]);
            m_hasId = true;
            m_line = buffer.substring(array[0].length()).trim();
            commandIndex = 1;
        }
        catch (NumberFormatException e)
        {
            m_hasId = false;
            m_id = -1;
            m_line = buffer.toString();
        }
        m_response = new StringBuilder();
        if (commandIndex >= array.length)
        {
            m_command = "";
            m_arg = null;
            return;
        }
        m_command = array[commandIndex];
        int nuArg = array.length - commandIndex - 1;
        m_arg = new String[nuArg];
        for (int i = 0; i < nuArg; ++i)
            m_arg[i] = array[commandIndex + i + 1];
    }

    /** Check that command has no arguments.
        @throws GtpError If command has any arguments. */
    public void checkArgNone() throws GtpError
    {
        checkNuArg(0);
    }

    /** Check that command has n arguments.
        @throws GtpError If command has not n arguments. */
    public void checkNuArg(int n) throws GtpError
    {
        if (getNuArg() != n)
        {
            if (n == 0)
                throw new GtpError("no arguments allowed");
            if (n == 1)
                throw new GtpError("need argument");
            throw new GtpError("need " + n + " arguments");
        }
    }

    /** Check that command has not more than n arguments.
        @throws GtpError If command has more than n arguments. */
    public void checkNuArgLessEqual(int n) throws GtpError
    {
        if (getNuArg() > n)
            throw new GtpError("too many arguments");
    }

    /** Check if command has an ID.
        @return true, if command has an ID. */
    public boolean hasId()
    {
        return m_hasId;
    }

    public String getArg() throws GtpError
    {
        checkNuArg(1);
        return getArg(0);
    }

    /** Get argument.
        @param i The index of the argument (starting with zero).
        @return The argument.
        @throws GtpError If command has not enough arguments. */
    public String getArg(int i) throws GtpError
    {
        if (i >= getNuArg())
            throw new GtpError("missing argument " + (i + 1));
        return m_arg[i];
    }

    /** Get argument line.
        Get a string containing all arguments (the command line without
        ID and command; leading and trailing whitespaces trimmed).
        @return The argument line. */
    public String getArgLine()
    {
        int pos = m_line.indexOf(m_command) + m_command.length();
        return m_line.substring(pos).trim();
    }

    /** Get single color argument.
        Valid color strings are "b", "w", "black", "white" and the
        corresponding uppercase strings.
        @return The color.
        @throws GtpError If command has not exactly one argument or argument
        is not a color. */
    public GoColor getColorArg() throws GtpError
    {
        checkNuArg(1);
        return getColorArg(0);
    }

    /** Get color argument.
        Valid color strings are "b", "w", "black", "white" and the
        corresponding uppercase strings.
        @param i The index of the argument (starting with zero).
        @return The color.
        @throws GtpError If command has not enough arguments or argument is
        not a color. */
    public GoColor getColorArg(int i) throws GtpError
    {
        String arg = getArg(i).toLowerCase(Locale.ENGLISH);
        if (arg.equals("b") || arg.equals("black"))
            return BLACK;
        if (arg.equals("w") || arg.equals("white"))
            return WHITE;
        throw new GtpError("argument " + (i + 1) + " must be black or white");
    }

    /** Get command.
        @return The command string (command line without ID and arguments,
        leading and trailing whitespaces trimmed). */
    public String getCommand()
    {
        return m_command;
    }

    /** Get single floating point number argument.
        @return The color.
        @throws GtpError If command has not exactly one argument or argument
        is not a floating point number. */
    public double getDoubleArg() throws GtpError
    {
        checkNuArg(1);
        return getDoubleArg(0);
    }

    /** Get floating point number argument.
        @param i The index of the argument (starting with zero).
        @return The color.
        @throws GtpError If command has not enough arguments or argument is
        not a floating point number. */
    public double getDoubleArg(int i) throws GtpError
    {
        String arg = getArg(i);
        try
        {
            return Double.parseDouble(arg);
        }
        catch (NumberFormatException e)
        {
            throw new GtpError("argument " + (i + 1) + " must be float");
        }
    }

    /** Get single integer argument.
        @return The color.
        @throws GtpError If command has not exactly one argument or argument
        is not an integer. */
    public int getIntArg() throws GtpError
    {
        checkNuArg(1);
        return getIntArg(0);
    }

    /** Get integer argument.
        @param i The index of the argument (starting with zero).
        @return The color.
        @throws GtpError If command has not enough arguments or argument is
        not an integer. */
    public int getIntArg(int i) throws GtpError
    {
        String arg = getArg(i);
        try
        {
            return Integer.parseInt(arg);
        }
        catch (NumberFormatException e)
        {
            throw new GtpError("argument " + (i + 1) + " must be integer");
        }
    }

    /** Get integer argument in a range.
        @param i The index of the argument (starting with zero).
        @param min Minimum allowed value.
        @param max Maximum allowed value.
        @return The color.
        @throws GtpError If command has not enough arguments or argument is
        not an integer in the allowed range. */
    public int getIntArg(int i, int min, int max) throws GtpError
    {
        int n = getIntArg(i);
        if (n < min)
            throw new GtpError("argument " + (i + 1)
                               + " must be greater/equal " + min);
        if (n > max)
            throw new GtpError("argument " + (i + 1)
                               + " must be less/equal " + max);
        return n;
    }

    /** Get point argument.
        Valid point strings are as in GtpUtil.parsePoint (uppercase or
        lowercase coordinates, e.g. "A1", or "pass").
        @param i The index of the argument (starting with zero).
        @param boardSize The board size (points will be checked to be within
        this board size).
        @return The point.
        @throws GtpError If command has not enough arguments or argument is
        not a valid point. */
    public GoPoint getPointArg(int i, int boardSize) throws GtpError
    {
        try
        {
            return GoPoint.parsePoint(getArg(i), boardSize);
        }
        catch (InvalidPointException e)
        {
            throw new GtpError("argument " + (i + 1) + " is not a point");
        }
    }

    /** Get point arguments.
        Valid point strings are as in GtpUtil.parsePoint (uppercase or
        lowercase coordinates, e.g. "A1", or "pass").
        All arguments will be parsed as points.
        @param boardSize The board size (points will be checked to be within
        this board size).
        @return Point list containg the points.
        @throws GtpError If at least one argument is not a valid point. */
    public PointList getPointListArg(int boardSize) throws GtpError
    {
        PointList pointList = new PointList();
        for (int i = 0; i < getNuArg(); ++i)
            pointList.add(getPointArg(i, boardSize));
        return pointList;
    }

    /** Full command line without ID.
        @return The command line without ID. */
    public String getLine()
    {
        return m_line;
    }

    /** Get number of arguments.
        @return The number of arguments. */
    public int getNuArg()
    {
        return m_arg.length;
    }

    /** Get string buffer for construction the response.
        The response to the command can be constructed by appending to this
        string buffer. */
    public StringBuilder getResponse()
    {
        return m_response;
    }

    /** Get command ID.
        It is allowed to call this function if command has no ID, but the
        returned value is undefined.
        @return The command ID. */
    public int getId()
    {
        return m_id;
    }

    /** Check if command is quit command.
        DEPRECATED: Fix GtpEngine to use only GtpEngine.m_quit
        @return true, if command name is "quit". */
    public boolean isQuit()
    {
        return m_line.trim().equals("quit");
    }

    /** Set the response.
        Clears the string buffer containg the response and sets it to the
        given string.
        @param response The string containing the new response. */
    public void setResponse(String response)
    {
        m_response.setLength(0);
        m_response.append(response);
    }

    private boolean m_hasId;

    private int m_id;

    private String m_line;

    private final String m_command;

    private final String[] m_arg;

    private final StringBuilder m_response;

    /** Preprocess command line.
        Replaces control characters by spaces, removes redundant spaces
        and appended comment. */
    private static StringBuilder preprocessLine(String line)
    {
        int len = line.length();
        StringBuilder buffer = new StringBuilder(len);
        boolean wasLastSpace = false;
        for (int i = 0; i < len; ++i)
        {
            char c = line.charAt(i);
            if (c == '#')
                break;
            if (Character.isISOControl(c))
                continue;
            if (Character.isWhitespace(c))
            {
                if (! wasLastSpace)
                {
                    buffer.append(' ');
                    wasLastSpace = true;
                }
            }
            else
            {
                buffer.append(c);
                wasLastSpace = false;
            }
        }
        return buffer;
    }
}
