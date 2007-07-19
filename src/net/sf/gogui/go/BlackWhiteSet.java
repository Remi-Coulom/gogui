//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

/** A set containing one element for Black and one for White. */
public class BlackWhiteSet<T>
{
    public BlackWhiteSet()
    {
    }

    public BlackWhiteSet(T elementBlack, T elementWhite)
    {
        m_elementBlack = elementBlack;
        m_elementWhite = elementWhite;
    }

    public T get(GoColor c)
    {
        if (c == BLACK)
            return m_elementBlack;
        else
        {
            assert c == WHITE;
            return m_elementWhite;
        }
    }

    public void set(GoColor c, T element)
    {
        if (c == BLACK)
            m_elementBlack = element;
        else
        {
            assert c == WHITE;
            m_elementWhite = element;
        }
    }

    private T m_elementBlack;

    private T m_elementWhite;
}
