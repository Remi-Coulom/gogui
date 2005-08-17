//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

//----------------------------------------------------------------------------

/** Collects statistical features of sample values. */
public class Statistics
{
    public Statistics()
    {
    }

    /** Add value. */
    public void add(double value)
    {
        m_min = Math.min(value, m_min);
        m_max = Math.max(value, m_max);
        m_sum += value;
        m_sumSq += (value * value);
        ++m_count;
    }

    /** Get number of values added. */
    public int getCount()
    {
        return m_count;
    }

    /** Get standard deviation. */
    public double getDeviation()
    {
        return Math.sqrt(getVariance());
    }

    /** Get standard error. */
    public double getError()
    {
        if (m_count == 0)
            return 0;
        return getDeviation() / Math.sqrt(m_count);
    }

    /** Get mean value. */
    public double getMean()
    {
        if (m_count == 0)
            return 0;
        return m_sum / m_count;
    }

    /** Get maximum value. */
    public double getMax()
    {
        return m_max;
    }

    /** Get maximum error.
        Returns the error assuming that every n values are 100 per cent
        correlated.
    */
    public double getMaxError(int n)
    {
        if (m_count == 0)
            return 0;
        return getDeviation() / Math.sqrt((double)m_count / n);
    }

    /** Get minumum value. */
    public double getMin()
    {
        return m_min;
    }

    /** Get sum of values. */
    public double getSum()
    {
        return m_sum;
    }

    /** Get variance. */
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

//----------------------------------------------------------------------------
