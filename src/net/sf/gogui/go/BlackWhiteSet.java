// BlackWhiteSet.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.util.ObjectUtil;

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

    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;
        BlackWhiteSet set = (BlackWhiteSet)object;
        return (ObjectUtil.equals(set.m_elementBlack, m_elementBlack)
                && ObjectUtil.equals(set.m_elementWhite, m_elementWhite));
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

    public int hashCode()
    {
        int hashCode = 0;
        if (m_elementBlack != null)
            hashCode |= m_elementBlack.hashCode();
        if (m_elementWhite != null)
            hashCode |= m_elementWhite.hashCode();
        return hashCode;
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
