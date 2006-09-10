//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

//----------------------------------------------------------------------------

/** Thread copying the output of one stream to another stream. */
public class StreamCopy
    implements Runnable
{
    /** @param verbose Also copy everything to stderr
        @param src Source stream
        @param dest Destination stream
        @param close Close destination after eof in source
    */
    public StreamCopy(boolean verbose, InputStream src, OutputStream dest,
                      boolean close)
    {
        m_verbose = verbose;
        m_src = src;
        m_dest = dest;
        m_close = close;
    }

    /** Run method.
        Exceptions caught are written to stderr.
    */
    public void run()
    {
        ReadableByteChannel srcChannel = Channels.newChannel(m_src);
        WritableByteChannel destChannel = Channels.newChannel(m_dest);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try
        {
            while (srcChannel.read(buffer) != -1)
            {
                buffer.flip();
                destChannel.write(buffer);
                buffer.compact();
            }
        if (m_close)
            m_dest.close();
        }
        catch (Throwable e)
        {
            StringUtil.printException(e);
        }
    }

    private final boolean m_verbose;

    private final boolean m_close;

    private final InputStream m_src;

    private final OutputStream m_dest;
};

//----------------------------------------------------------------------------
