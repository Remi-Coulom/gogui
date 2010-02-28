// ConstPointList.java

package net.sf.gogui.go;

import java.util.Iterator;

/** Const functions of go.PointList.
    @see PointList */
public interface ConstPointList
    extends Iterable<GoPoint>
{
    boolean contains(Object elem);

    boolean equals(Object object);

    GoPoint get(int index);

    int hashCode();

    boolean isEmpty();

    Iterator<GoPoint> iterator();

    int size();

    String toString();
}
