// StreamDiscard.java

package net.sf.gogui.util;

import java.io.InputStream;

/** Thread discarding an output stream. */
public class StreamDiscard
    extends Thread
{
    public StreamDiscard(InputStream src)
    {
        m_src = src;
    }

    /** Run method.
        Exceptions caught are written to stderr. */
    public void run()
    {
        try
        {
            byte buffer[] = new byte[1024];
            while (true)
            {
                int n = m_src.read(buffer);
                if (n < 0)
                    break;
                if (n == 0)
                {
                    // Not sure if this is necessary.
                    sleep(100);
                    continue;
                }
            }
        }
        catch (Throwable e)
        {
            StringUtil.printException(e);
        }
    }

    private final InputStream m_src;
}
