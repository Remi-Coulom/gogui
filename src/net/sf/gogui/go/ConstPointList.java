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

    GoPoint get(int index);

    boolean isEmpty();

    int size();
}
