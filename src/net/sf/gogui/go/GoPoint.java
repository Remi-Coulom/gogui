//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.util.StringUtil;

//----------------------------------------------------------------------------

/** Intersection on the Go board.
    This class is immutable and references to the same point are unique.
    Instances can be created with GoPoint.get().
    Point coordinates start with 0, the point (0,0) corresponds to "A1".
*/
public final class GoPoint
    implements Comparable
{
    /** Thrown if parsing a string representation of a GoPoint fails. */
    public static class InvalidPoint extends Exception
    {
        public InvalidPoint(String text)
        {
            super("Invalid point: " + text);
        }
        
        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    /** Maximum board size.
        Set such that all points can be converted to strings with one letter
        and a number, i.e. the largest point is Z25.
    */
    public static final int MAXSIZE = 25;

    /** Default board size. */
    public static final int DEFAULT_SIZE = 19;

    public int compareTo(Object object)
    {
        GoPoint point = (GoPoint)object;
        if (m_y < point.m_y)
            return -1;
        if (m_y > point.m_y)
            return 1;
        assert(m_y == point.m_y);
        if (m_x < point.m_x)
            return -1;
        if (m_x > point.m_x)
            return 1;
        return 0;
    }

    /** Compare, including the case that the points can be null. */
    public static boolean equals(GoPoint point1, GoPoint point2)
    {
        return (point1 == point2);
    }

    /** Factory method for creating a point.
        @param x x-coordinate [0...GoPoint.MAXSIZE - 1]
        @param y y-coordinate [0...GoPoint.MAXSIZE - 1]
        @return Unique reference to a point with these coordinates.
    */
    public static GoPoint get(int x, int y)
    {
        assert(x >= 0);
        assert(y >= 0);
        assert(x < MAXSIZE);
        assert(y < MAXSIZE);
        int max = Math.max(x, y);
        if (max >= s_size)
            grow(max + 1);
        GoPoint point = s_points[x][y];
        assert(point != null);
        return point;
    }

    /** Return point below.
        @return The point below this point (x, y - 1).
    */
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
        assert(size > 0);
        assert(size <= MAXSIZE);
        return (m_x < size && m_y < size);
    }

    /** Return point left.
        @return the point below this point (x - 1, y)
    */
    public GoPoint left()
    {
        if (m_x > 0)
            return get(m_x - 1, m_y);
        else
            return this;
    }

    /** Parse point or null (PASS).
        Parsing is case-insensitive, leading and trailing whitespace is
        ignored. "PASS" returns null, invalid strings throw a InvalidPoint
        exception.
    */
    public static GoPoint parsePoint(String string, int boardSize)
        throws InvalidPoint
    {
        string = string.trim().toUpperCase(Locale.ENGLISH);
        if (string.equals("PASS"))
            return null;
        if (string.length() < 2)
            throw new InvalidPoint(string);
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
            throw new InvalidPoint(string);
        }
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
            throw new InvalidPoint(string);
        return GoPoint.get(x, y);
    }

    public static GoPoint[] parsePointList(String s, int boardSize)
        throws InvalidPoint
    {
        ArrayList list = parsePointListArrayList(s, boardSize);
        GoPoint result[] = new GoPoint[list.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (GoPoint)list.get(i);
        return result;
    }

    public static ArrayList parsePointListArrayList(String s, int boardSize)
        throws InvalidPoint
    {
        ArrayList list = new ArrayList();
        String p[] = StringUtil.splitArguments(s);
        for (int i = 0; i < p.length; ++i)
            if (! p[i].equals(""))
                list.add(parsePoint(p[i], boardSize));
        return list;
    }

    /** Return point right.
        @param max Current board size.
        @return The point to the right of this point (x, y + 1)
        or this point if no such point exists.
    */
    public GoPoint right(int max)
    {
        if (m_x < max - 1)
            return get(m_x + 1, m_y);
        else
            return this;
    }

    /** Convert to a string.
        @return String representation of this point.
    */
    public String toString()
    {
        return m_string;
    }
    
    /** Convert a point or null point (pass) to a string.
        @param point Point or null for pass moves
        @return point.toString() or "PASS" if point is null
    */
    public static String toString(GoPoint point)
    {
        if (point == null)
            return "PASS";
        return point.toString();
    }
    
    /** Convert a list of points to a string.
        Points are separated by a single space.
        If pointList is null, "(null)" is returned.
    */
    public static String toString(ArrayList pointList)
    {
        if (pointList == null)
            return "(null)";
        int length = pointList.size();
        StringBuffer buffer = new StringBuffer(length * 4);
        for (int i = 0; i < length; ++i)
        {
            buffer.append(((GoPoint)pointList.get(i)).toString());
            if (i < length - 1)
                buffer.append(' ');
        }
        return buffer.toString();
    }
    
    /** Return point above.
        @param max Current board size.
        @return The point below this point (x, y + 1) or this point if no such
        point exists.
    */
    public GoPoint up(int max)
    {
        if (m_y < max - 1)
            return get(m_x, m_y + 1);
        else
            return this;
    }

    private static int s_size;

    private static GoPoint[][] s_points;

    private final int m_x;

    private final int m_y;

    private final String m_string;

    private static String s_xString[] =
    {
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "O",
        "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    static
    {
        assert(s_xString.length == MAXSIZE);
        s_size = 0;
        grow(GoPoint.DEFAULT_SIZE);
    };

    private static void grow(int size)
    {
        assert(size > s_size);
        GoPoint[][] points = new GoPoint[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
                if (x < s_size && y < s_size)
                    points[x][y] = s_points[x][y];
                else
                    points[x][y] = new GoPoint(x, y);
        s_points = points;
        s_size = size;
    }

    private GoPoint(int x, int y)
    {
        m_x = x;
        m_y = y;
        m_string = s_xString[m_x] + Integer.toString(m_y + 1);
    }
}

//----------------------------------------------------------------------------
