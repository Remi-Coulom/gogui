//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.PrintStream;

//----------------------------------------------------------------------------

public class Histogram
    extends Statistics
{
    public Histogram(double min, double max, double step)
    {
        m_min = min;
        m_step = step;
        m_size = (int)((max - min) / step) + 1;
        m_array = new int[m_size];
    }

    public void add(double value)
    {
        super.add(value);
        int i = (int)((value - m_min) / m_step);
        ++m_array[i];
    }

    public int getCount(int i)
    {
        return m_array[i];
    }

    public int getSize()
    {
        return m_size;
    }

    public double getStep()
    {
        return m_step;
    }

    public double getValue(int i)
    {
        return m_min + i * m_step;
    }

    public void printHtml(PrintStream out)
    {
        out.print("<p>\n" +
                  "<table cellspacing=\"1\" cellpadding=\"0\""
                  + " rules=\"groups\">\n");
        int min;
        for (min = 0; min < m_size - 1 && m_array[min] == 0; ++min)
            ;
        int max;
        for (max = m_size - 1; max > 0 && m_array[max] == 0; --max)
            ;
        for (int i = min; i <= max; ++i)
        {
            int scale = 630;
            int width = m_array[i] * scale / getCount();
            if (getValue(i) >= 0 && getValue(i - 1) < 0)
                out.print("<tbody>\n");
            out.print("<tr><td align=\"right\"><small>" + getValue(i)
                      + "</small></td><td><table cellspacing=\"0\"" +
                      " cellpadding=\"0\" width=\"" + scale + "\"><tr>" +
                      "<td bgcolor=\"#666666\" width=\"" + width +
                      "\"></td>" + "<td bgcolor=\"#cccccc\" width=\""
                      + (scale - width) + "\"><small>"
                      + m_array[i]
                      + "</small></td></tr></table></td></tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n");
    }

    private final int m_size;

    private final double m_min;

    private final double m_step;

    private int[] m_array;
}

//----------------------------------------------------------------------------
