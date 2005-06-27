//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

//----------------------------------------------------------------------------

public class Statistics
{
    public Statistics()
    {
    }

    public void addValue(double value)
    {
        m_min = Math.min(value, m_min);
        m_max = Math.max(value, m_max);
        m_sum += value;
        m_sumSq += (value * value);
        ++m_count;
    }

    public int getCount()
    {
        return m_count;
    }

    public double getDeviation()
    {
        return Math.sqrt(getVariance());
    }

    public double getErrorMean()
    {
        if (m_count == 0)
            return 0;
        return getDeviation() / Math.sqrt(m_count);
    }

    public double getMean()
    {
        if (m_count == 0)
            return 0;
        return m_sum / m_count;
    }

    public double getMax()
    {
        return m_max;
    }

    public double getMin()
    {
        return m_min;
    }

    public double getSum()
    {
        return m_sum;
    }

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
