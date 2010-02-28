// Statistics.java

package net.sf.gogui.util;

/** Collects statistical features of sample values. */
public class Statistics
{
    /** Add value.
        @param value The value to add. */
    public void add(double value)
    {
        m_min = Math.min(value, m_min);
        m_max = Math.max(value, m_max);
        m_sum += value;
        m_sumSq += (value * value);
        ++m_count;
    }

    /** Get number of values added.
        @return Number of values added. */
    public int getCount()
    {
        return m_count;
    }

    /** Get standard deviation.
        @return The standard deviation (square root of variance). */
    public double getDeviation()
    {
        return Math.sqrt(getVariance());
    }

    /** Get standard error.
        @return The standard error (standard deviation divided by square root
        of the number of values). */
    public double getError()
    {
        if (m_count == 0)
            return 0;
        return getDeviation() / Math.sqrt(m_count);
    }

    /** Get mean value.
        @return The mean of all values. */
    public double getMean()
    {
        if (m_count == 0)
            return 0;
        return m_sum / m_count;
    }

    /** Get maximum value.
        @return The maximum of the value or Double.NEGATIVE_INFINITY, if no
        values. */
    public double getMax()
    {
        return m_max;
    }

    /** Get maximum error.
        Returns the error assuming that every n values are 100 per cent
        correlated.
        @param n The number of values that are assumed to be 100 per cent
        correlated.
        @return The standard error for this assumption (standard deviation
        divided by square root of number of values divided by n). */
    public double getMaxError(int n)
    {
        if (m_count == 0)
            return 0;
        return getDeviation() / Math.sqrt((double)m_count / n);
    }

    /** Get minumum value.
        @return The minumum of the value or Double.POSITIVE_INFINITY, if no
        values. */
    public double getMin()
    {
        return m_min;
    }

    /** Get sum of values.
        @return The sum of all values. */
    public double getSum()
    {
        return m_sum;
    }

    /** Get variance.
        @return The variance (sum of squares of differences between values
        and mean). */
    public double getVariance()
    {
        if (m_count == 0)
            return 0;
        double mean = getMean();
        return m_sumSq / m_count - mean * mean;
    }

    private int m_count;

    private double m_max = Double.NEGATIVE_INFINITY;

    private double m_min = Double.POSITIVE_INFINITY;

    private double m_sum;

    private double m_sumSq;
}
