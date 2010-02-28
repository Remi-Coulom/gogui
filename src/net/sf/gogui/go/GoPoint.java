// GoPoint.java

package net.sf.gogui.go;

import java.util.Locale;
import net.sf.gogui.util.StringUtil;

/** Intersection on the Go board.
    This class is immutable and references to the same point are unique.
    Instances can be created with GoPoint.get().
    Point coordinates start with 0, the point (0,0) corresponds to "A1"
    and is on the lower left corner of the board, if the board is drawn on
    the screen. */
public final class GoPoint
    implements Comparable<GoPoint>
{
    /** Maximum board size.
        Set such that all points can be converted to strings with one letter
        and a number, i.e. the largest point is Z25. */
    public static final int MAX_SIZE = 25;

    /** Default board size. */
    public static final int DEFAULT_SIZE = 19;

    /** Upper limit (exclusive) for one-dimensional point index.
        @see #getIndex */
    public static final int NUMBER_INDEXES = MAX_SIZE * MAX_SIZE;

    /** Compare two points.
        The order of the points is: A1, B1, ..., A2, B2, ... */
    public int compareTo(GoPoint p)
    {
        if (m_index < p.m_index)
            return -1;
        else if (m_index > p.m_index)
            return 1;
        else
            return 0;
    }

    /** Indicate if this object is equal to another object.
        Since point instances are unique, this function does the same as
        Object.equals and is only added explicitely to avoid warnings about
        classes with a compareTo, but no equals-function. */
    public boolean equals(Object obj)
    {
        return (this == obj);
    }

    /** Compare, including the case that the points can be null. */
    public static boolean equals(GoPoint point1, GoPoint point2)
    {
        return (point1 == point2);
    }

    /** Factory method for creating a point.
        @param x x-coordinate <code>[0...GoPoint.MAX_SIZE - 1]</code>
        @param y y-coordinate <code>[0...GoPoint.MAX_SIZE - 1]</code>
        @return Unique reference to a point with these coordinates. */
    public static GoPoint get(int x, int y)
    {
        assert x >= 0;
        assert y >= 0;
        assert x < MAX_SIZE;
        assert y < MAX_SIZE;
        GoPoint point = s_points[x][y];
        assert point != null;
        return point;
    }

    /** Integer for using points as indices in an array.
        The index of A1 is zero and the indices count upwards from left
        to right and bottom to top over a board with the maximum size
        GoPoint.MAX_SIZE. */
    public int getIndex()
    {
        return m_index;
    }

    /** See getIndex(). */
    public static int getIndex(int x, int y)
    {
        return y * MAX_SIZE + x;
    }

    public int hashCode()
    {
        return m_index;
    }

    /** Return point below.
        @return The point below this point (x, y - 1). */
    public GoPoint down()
    {
        if (m_y > 0)
            return get(m_x, m_y - 1);
        else
            return this;
    }

    /** X-Coordinate. */
    public int getX()
    {
        return m_x;
    }

    /** Y-Coordinate. */
    public int getY()
    {
        return m_y;
    }

    public boolean isOnBoard(int size)
    {
        assert size > 0;
        assert size <= MAX_SIZE;
        return (m_x < size && m_y < size);
    }

    /** Return point left.
        @return the point below this point (x - 1, y) */
    public GoPoint left()
    {
        if (m_x > 0)
            return get(m_x - 1, m_y);
        else
            return this;
    }

    /** Parse point or null (PASS).
        Parsing is case-insensitive, leading and trailing whitespace is
        ignored. "PASS" returns null, invalid strings throw an
        InvalidPointException. */
    public static GoPoint parsePoint(String string, int boardSize)
        throws InvalidPointException
    {
        string = string.trim().toUpperCase(Locale.ENGLISH);
        if (string.equals("PASS"))
            return null;
        if (string.length() < 2)
            throw new InvalidPointException(string);
        char xChar = string.charAt(0);
        if (xChar >= 'J')
            --xChar;
        int x = xChar - 'A';
        int y;
        try
        {
            y = Integer.parseInt(string.substring(1)) - 1;
        }
        catch (NumberFormatException e)
        {
            throw new InvalidPointException(string);
        }
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
            throw new InvalidPointException(string);
        return GoPoint.get(x, y);
    }

    public static PointList parsePointList(String s, int boardSize)
        throws InvalidPointException
    {
        PointList list = new PointList();
        for (String p : StringUtil.splitArguments(s))
            if (! p.equals(""))
                list.add(parsePoint(p, boardSize));
        return list;
    }

    /** Return point right.
        @param max Current board size.
        @return The point to the right of this point (x, y + 1)
        or this point if no such point exists. */
    public GoPoint right(int max)
    {
        if (m_x < max - 1)
            return get(m_x + 1, m_y);
        else
            return this;
    }

    /** Convert to a string.
        @return String representation of this point. */
    public String toString()
    {
        return m_string;
    }

    /** Convert a point or null point (pass) to a string.
        @param point Point or null for pass moves
        @return point.toString() or "PASS" if point is null */
    public static String toString(GoPoint point)
    {
        if (point == null)
            return "PASS";
        return point.toString();
    }

    /** Convert a list of points to a string.
        Points are separated by a single space.
        If pointList is null, "(null)" is returned. */
    public static String toString(ConstPointList pointList)
    {
        if (pointList == null)
            return "(null)";
        int length = pointList.size();
        StringBuilder buffer = new StringBuilder(length * 4);
        for (int i = 0; i < length; ++i)
        {
            buffer.append(pointList.get(i));
            if (i < length - 1)
                buffer.append(' ');
        }
        return buffer.toString();
    }

    /** Return point above.
        @param max Current board size.
        @return The point above this point (x, y + 1) or this point if no such
        point exists. */
    public GoPoint up(int max)
    {
        if (m_y < max - 1)
            return get(m_x, m_y + 1);
        else
            return this;
    }

    private static GoPoint[][] s_points;

    private final int m_x;

    private final int m_y;

    private final int m_index;

    private final String m_string;

    static
    {
        s_points = new GoPoint[MAX_SIZE][MAX_SIZE];
        for (int x = 0; x < MAX_SIZE; ++x)
            for (int y = 0; y < MAX_SIZE; ++y)
                s_points[x][y] = new GoPoint(x, y);
    }

    private GoPoint(int x, int y)
    {
        m_x = x;
        m_y = y;
        char xChar = (char)('A' + x);
        if (xChar >= 'I')
            ++xChar;
        m_string = xChar + Integer.toString(m_y + 1);
        m_index = getIndex(x, y);
    }
}
