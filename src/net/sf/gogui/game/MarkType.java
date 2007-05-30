//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;

/** Markup types for points in nodes of a game tree. */
public final class MarkType
    implements Comparable
{
    public static final MarkType MARK = new MarkType("mark");

    public static final MarkType CIRCLE = new MarkType("circle");

    public static final MarkType SQUARE = new MarkType("square");

    public static final MarkType TRIANGLE = new MarkType("triangle");

    public static final MarkType SELECT = new MarkType("select");

    public static final MarkType TERRITORY_BLACK =
        new MarkType("territory-b");

    public static final MarkType TERRITORY_WHITE =
        new MarkType("territory-w");

    public int compareTo(Object obj)
    {
        int index = ((MarkType)obj).m_index;
        if (m_index == index)
            return 0;
        return m_index < index ? -1 : 1;
    }

    public boolean equals(Object obj)
    {
        return (this == obj);
    }

    public static int getNumberTypes()
    {
        return s_types.size();
    }

    public static MarkType getType(int i)
    {
        return (MarkType)s_types.get(i);
    }

    public int hashCode()
    {
        return m_index;
    }

    public String toString()
    {
        return m_string;
    }

    private static ArrayList s_types;

    /** Index if mark types are stored in a map. */
    private final int m_index;

    private final String m_string;

    private MarkType(String string)
    {
        if (s_types == null)
            s_types = new ArrayList(7);
        m_string = string;
        m_index = s_types.size();
        s_types.add(this);
    }
}
