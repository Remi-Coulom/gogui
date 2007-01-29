//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** Const functions of go.PointList.
    @see PointList
*/
public interface ConstPointList
{
    boolean contains(GoPoint p);

    boolean equals(Object object);

    GoPoint get(int index);

    int hashCode();

    boolean isEmpty();

    int size();
}
