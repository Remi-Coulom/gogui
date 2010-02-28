// BlackWhiteEmptySet.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;

/** A set containing one element for Black, one for White, and one for
    Empty. */
public class BlackWhiteEmptySet<T>
{
    public BlackWhiteEmptySet()
    {
    }

    public BlackWhiteEmptySet(T elementBlack, T elementWhite, T elementEmpty)
    {
        m_elementBlack = elementBlack;
        m_elementWhite = elementWhite;
        m_elementEmpty = elementEmpty;
    }

    public T get(GoColor c)
    {
        if (c == BLACK)
            return m_elementBlack;
        else if (c == WHITE)
            return m_elementWhite;
        else
        {
            assert c == EMPTY;
            return m_elementEmpty;
        }
    }

    public void set(GoColor c, T element)
    {
        if (c == BLACK)
            m_elementBlack = element;
        else if (c == WHITE)
            m_elementWhite = element;
        else
        {
            assert c == EMPTY;
            m_elementEmpty = element;
        }
    }

    private T m_elementBlack;

    private T m_elementWhite;

    private T m_elementEmpty;
}
