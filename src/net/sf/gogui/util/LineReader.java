// LineReader.java

package net.sf.gogui.util;

/** Allows to read lines from a buffer without blocking. */
public class LineReader
{
    /** Add text to buffer. */
    public void add(String s)
    {
        m_buffer.append(s);
        m_hasLines = s.contains("\n");
    }

    /** Check if the buffer contains at least one line. */
    public boolean hasLines()
    {
        return m_hasLines;
    }

    public String getLine()
    {
        String s = m_buffer.toString();
        int pos = s.indexOf('\n');
        if (pos < 0)
            return "";
        String result = s.substring(0, pos + 1);
        m_buffer.delete(0, pos + 1);
        m_hasLines = false;
        return result;
    }

    private boolean m_hasLines;

    private final StringBuilder m_buffer = new StringBuilder(1024);
}
