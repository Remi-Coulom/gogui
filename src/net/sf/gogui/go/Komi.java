//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** Value of komi.
    This class is immutable.
*/
public final class Komi
{
    /** Constructor.
        @param komi The value for the komi. Will be rounded to a multiple of
        0.5
    */
    public Komi(double komi)
    {
        m_value = (int)(Math.round(komi * 2.0));
    }

    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;        
        Komi komi = (Komi)object;
        return (komi.m_value == m_value);
    }

    public int hashCode()
    {
        return m_value;
    }

    /** Parse komi from string.
        @param s The string (null not allowed), empty string means unknown
        komi.
        @return The komi or null if unknown komi.
    */
    public static Komi parseKomi(String s) throws InvalidKomiException
    {
        assert s != null;
        if (s.trim().equals(""))
            return null;
        try
        {
            double komi = Double.parseDouble(s);
            return new Komi(komi);
        }
        catch (NumberFormatException e)
        {
            throw new InvalidKomiException(s);
        }
    }

    public double toDouble()
    {
        return m_value / 2.0;
    }

    public String toString()
    {
        if (m_value % 2 == 0)
            return Integer.toString(m_value / 2);
        else
            return (m_value / 2) + ".5";
    }

    /** The value of the komi multiplied by two. */
    private final int m_value;
}
