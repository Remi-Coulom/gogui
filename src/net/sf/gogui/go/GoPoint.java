//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Intersection on the Go board.
    This class is immutable and references to the same point are unique.
    Point coordinates start with 0, the point (0,0) corresponds to "A1".
*/
public final class GoPoint
    implements Comparable
{
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

    public static GoPoint create(int x, int y)
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
        Returns the point below this point (x, y - 1)
    */
    public GoPoint down()
    {
        if (m_y > 0)
            return create(m_x, m_y - 1);
        else
            return this;
    }

    public int getX()
    {
        return m_x;
    }

    public int getY()
    {
        return m_y;
    }

    /** Return point left.
        Returns the point below this point (x - 1, y)
    */
    public GoPoint left()
    {
        if (m_x > 0)
            return create(m_x - 1, m_y);
        else
            return this;
    }

    /** Parse point or null (pass).
        Parsing is case-insensitive, leading and trailing whitespace is
        ignored. "PASS" returns null, invalid strings throw a InvalidPoint
        exception.
    */
    public static GoPoint parsePoint(String string, int boardSize)
        throws InvalidPoint
    {
        string = string.trim().toUpperCase();
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
        return GoPoint.create(x, y);
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
        String p[] = StringUtils.splitArguments(s);
        for (int i = 0; i < p.length; ++i)
            if (! p[i].equals(""))
                list.add(parsePoint(p[i], boardSize));
        return list;
    }

    /** Return point right.
        Returns the point below this point (x + 1, y)
    */
    public GoPoint right(int max)
    {
        if (m_x < max - 1)
            return create(m_x + 1, m_y);
        else
            return this;
    }

    /** Convert to a string. */
    public String toString()
    {
        return m_string;
    }
    
    /** Convert a point or null point (pass) to a string.
        If point is null, "PASS" is returned.
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
    
    /** Return point abovebelow.
        Returns the point below this point (x, y + 1)
    */
    public GoPoint up(int max)
    {
        if (m_y < max - 1)
            return create(m_x, m_y + 1);
        else
            return this;
    }

    private static int s_size;

    private static GoPoint[][] s_points;

    private final int m_x;

    private final int m_y;

    private final String m_string;

    private static String m_xString[] =
    {
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "O",
        "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    static
    {
        assert(m_xString.length == MAXSIZE);
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
        m_string = m_xString[m_x] + Integer.toString(m_y + 1);
    }
}

//----------------------------------------------------------------------------
