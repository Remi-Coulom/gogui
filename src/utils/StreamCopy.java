//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class StreamCopy
    extends Thread
{
    public StreamCopy(boolean verbose, InputStream src, OutputStream dest,
                      boolean close)
    {
        m_verbose = verbose;
        m_src = src;
        m_dest = dest;
        m_close = close;
    }

    public void run()
    {
        try
        {
            byte buffer[] = new byte[1024];
            while (true)
            {
                int n = m_src.read(buffer);
                if (n < 0)
                {
                    if (m_close)
                        m_dest.close();
                    break;
                }
                if (m_verbose)
                    System.err.write(buffer, 0, n);
                m_dest.write(buffer, 0, n);
                m_dest.flush();
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            System.err.println(msg);
        }
    }

    boolean m_verbose;

    boolean m_close;

    InputStream m_src;

    OutputStream m_dest;
};

//-----------------------------------------------------------------------------
