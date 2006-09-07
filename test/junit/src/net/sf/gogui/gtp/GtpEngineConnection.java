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

/** In-process GTP client connection to a GtpEngine.
    For accessing an in-process GtpEngine as a GtpClient for testing purposes.
    Redirects the input and output streams of a GtpClient to the GtpEngine.
    If it is not required for the test, that the interface to the GtpEngine
    is on the streams level, use GtpEngineClient instead because of the
    better performance.
*/
public class GtpEngineConnection
{
    public GtpEngineConnection(GtpEngine engine) throws IOException, GtpError
    {
        // For unknown reasons, the PipedInputStream solution is
        // unbelievably slow (Java 1.5, Linux), e.g. it slowed down
        // GtpAdapterTest by a factor of 100!
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
