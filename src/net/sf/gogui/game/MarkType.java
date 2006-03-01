//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

//----------------------------------------------------------------------------

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
    
    public static final MarkType[] ALL = {
        MARK,
        CIRCLE,
        SQUARE,
        TRIANGLE,
        SELECT,
        TERRITORY_BLACK,
        TERRITORY_WHITE
    };

    public int compareTo(Object object)
    {
        int index = ((MarkType)object).m_index;
        if (m_index == index)
            return 0;
        return m_index < index ? -1 : 1;
    }

    public String toString()
    {
        return m_string;
    }

    private static int s_numberTypes;

    /** Index if mark types are stored in a map. */
    private final int m_index;
    
    private final String m_string;
    
    private MarkType(String string)
    {
        m_string = string;
        m_index = s_numberTypes++;
    }
}

//----------------------------------------------------------------------------
