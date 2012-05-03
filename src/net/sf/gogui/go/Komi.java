// Komi.java

package net.sf.gogui.go;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/** Value of komi.
    This class is immutable. */
public final class Komi
{
    /** Constructor.
        @param komi The value for the komi. */
    public Komi(double komi)
    {
        m_value = komi;
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
        // As in Double.hashCode()
        long v = Double.doubleToLongBits(m_value);
        return (int)(v ^ (v >>> 32));
    }

    public boolean isMultipleOf(double multiple)
    {
        return Math.IEEEremainder(m_value, multiple) == 0;
    }

    /** Parse komi from string.
        @param s The string (null not allowed), empty string means unknown
        komi.
        @return The komi or null if unknown komi. */
    public static Komi parseKomi(String s) throws InvalidKomiException
    {
        assert s != null;
        if (s.trim().equals(""))
            return null;
        try
        {
            // Also accept , instead of .
            double komi = Double.parseDouble(s.replace(',', '.'));
            return new Komi(komi);
        }
        catch (NumberFormatException e)
        {
            throw new InvalidKomiException(s);
        }
    }

    public double toDouble()
    {
        return m_value;
    }

    /** Like Komi.toString() but interprets null argument as zero komi. */
    static public String toString(Komi komi)
    {
        if (komi == null)
            return "0";
        return komi.toString();
    }

    public String toString()
    {
        DecimalFormat format =
            (DecimalFormat)(NumberFormat.getInstance(Locale.ENGLISH));
        format.setGroupingUsed(false);
        format.setDecimalSeparatorAlwaysShown(false);
        return format.format(m_value);
    }

    private final double m_value;
}
