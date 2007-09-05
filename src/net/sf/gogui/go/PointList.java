//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.util.ArrayList;

/** List containing points. */
public final class PointList
    implements ConstPointList
{
    /** Construct empty point list. */
    public PointList()
    {
        this(0);
    }

    /** Construct empty point list with initial capacity.
        @param initialCapacity The number of points to reserve memory for.
    */
    public PointList(int initialCapacity)
    {
        m_list = new ArrayList(initialCapacity);
    }

    /** Construct point list with a single element.
        @param p The initial point element.
    */
    public PointList(GoPoint p)
    {
        this(1);
        add(p);
    }

    /** Construct point list as a copy of another point list.
        @param list The list to copy the points from.
    */
    public PointList(ConstPointList list)
    {
        m_list = new ArrayList(((PointList)list).m_list);
    }

    /** Add point at the end of the list. */
    public void add(GoPoint p)
    {
        m_list.add(p);
    }

    /** Add points of another list  at the end of this list. */
    public void addAll(ConstPointList list)
    {
        m_list.addAll(((PointList)list).m_list);
    }

    /** Remove all points from the list. */
    public void clear()
    {
        m_list.clear();
    }

    public boolean contains(GoPoint p)
    {
        return m_list.contains(p);
    }

    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;
        PointList list = (PointList)object;
        return list.m_list.equals(m_list);
    }

    /** Get the point at the specified position. */
    public GoPoint get(int index)
    {
        return (GoPoint)m_list.get(index);
    }

    /** Get an empty constant point list.
        Can be used at places where an empty temporary point list is needed
        that is never modified to avoid memory allocation.
    */
    public static ConstPointList getEmptyList()
    {
        return EMPTY_LIST;
    }

    public int hashCode()
    {
        return m_list.hashCode();
    }

    public boolean isEmpty()
    {
        return m_list.isEmpty();
    }

    /** Remove first occurence of a point from the list.
        Does not change the order of the remaining elements.
        Does nothing if point is not in the list.
        @param p The point to remove.
        @return <code>true</code>, if the point was found and removed.
    */
    public boolean remove(GoPoint p)
    {
        return m_list.remove(p);
    }

    public int size()
    {
        return m_list.size();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < size(); ++i)
        {
            if (i > 0)
                buffer.append(' ');
            buffer.append(get(i).toString());
        }
        return buffer.toString();
    }

    /** Convert point list to string.
        Null arguments will be converted to an empty string.
    */
    public static String toString(ConstPointList list)
    {
        if (list == null)
            return "";
        else
            return list.toString();
    }

    private static final ConstPointList EMPTY_LIST = new PointList();

    private ArrayList m_list;
}
