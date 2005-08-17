//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

//----------------------------------------------------------------------------

/** In-process GTP client connection to a GtpEngine. */
public class GtpEngineConnection
{
    public GtpEngineConnection(GtpEngine engine) throws IOException, GtpError
    {
        PipedInputStream gtpInput = new PipedInputStream();
        final OutputStream out = new PipedOutputStream(gtpInput);
        final PipedInputStream in = new PipedInputStream();
        PipedOutputStream gtpOutput = new PipedOutputStream(in);
        m_engine = engine;
        Thread thread = new Thread()
            {
                public void run()
                {
                    try
                    {
                        m_engine.mainLoop(in, out);
                    }
                    catch (IOException e)
                    {
                    }
                }
            };
        thread.start();
        m_gtp = new GtpClient(gtpInput, gtpOutput, false, null);
    }

    public GtpClient getGtpClient()
    {
        return m_gtp;
    }

    public void interruptCommand()
    {
    }

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        throw new GtpError("unknown command");
    }

    private GtpClient m_gtp;

    private GtpEngine m_engine;
}

//----------------------------------------------------------------------------
