// StreamCopy.java

package net.sf.gogui.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Thread copying the output of one stream to another stream. */
public class StreamCopy
    implements Runnable
{
    /** Constructor.
        @param verbose Also copy everything to stderr
        @param src Source stream
        @param dest Destination stream
        @param close Close destination after eof in source */
    public StreamCopy(boolean verbose, InputStream src, OutputStream dest,
                      boolean close)
    {
        m_verbose = verbose;
        m_src = src;
        m_dest = dest;
        m_close = close;
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
                if (m_verbose)
                    System.err.write(buffer, 0, n);
                m_dest.write(buffer, 0, n);
                m_dest.flush();
            }
        }
        catch (Throwable e)
        {
            StringUtil.printException(e);
        }
        finally
        {
            if (m_close)
            {
                try
                {
                    m_dest.close();
                }
                catch (IOException e)
                {
                    StringUtil.printException(e);
                }
            }
        }
    }

    private final boolean m_verbose;

    private final boolean m_close;

    private final InputStream m_src;

    private final OutputStream m_dest;
}
