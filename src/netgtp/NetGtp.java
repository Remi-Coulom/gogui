//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package netgtp;

import java.io.*;
import java.net.*;
import java.util.*;
import utils.*;
import version.*;

//-----------------------------------------------------------------------------

class NetGtp
{
    public NetGtp(String hostname, int port)
        throws Exception
    {
        Socket socket = new Socket(hostname, port);
        StreamCopy fromNet = new StreamCopy(false, socket.getInputStream(),
                                            System.out, false);
        SocketOutputCopy toNet = new SocketOutputCopy(socket);
        fromNet.start();
        toNet.start();
        fromNet.join();
        // Actually I would expect that
        //   System.in.close();
        //   toNet.join();
        //   socket.close();
        // is good enough to terminate thread toNet, but the read on System.in
        // blocks even after a close(). Also it seems not to be possible to
        // use java.nio and have System.in as a interruptible channel. So
        // System.exit() is called to kill this thread. If you find a cleaner
        // solution to terminate both threads, please tell me.
        // The two possible reasons for termination of NetGtp are:
        // - System.in reaches EOF (no more GTP input)
        // - socket input stream reaches EOF (server closes connection
        //   after response to a quit command)
        System.exit(0);
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "help",
                "version"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("NetGtp " + Version.m_version);
                System.exit(0);
            }
            Vector arguments = opt.getArguments();
            if (arguments.size() != 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String hostname = (String)arguments.get(0);
            int port = Integer.parseInt((String)arguments.get(1));
            NetGtp netGtp = new NetGtp(hostname, port);
        }
        catch (AssertionError e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            System.exit(-1);
        }
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: java -jar netgtp.jar [options] hostname port\n" +
                  "\n" +
                  "  -help    display this help and exit\n" +
                  "  -version print version and exit\n");
    }
}
    
class SocketOutputCopy
    extends Thread
{
    public SocketOutputCopy(Socket socket)
    {
        m_socket = socket;
    }

    public void run()
    {
        try
        {
            InputStream src = System.in;
            OutputStream dest = m_socket.getOutputStream();
            byte buffer[] = new byte[1024];
            while (true)
            {
                int n = src.read(buffer);
                if (n < 0)
                {
                    m_socket.shutdownOutput();
                    break;
                }
                dest.write(buffer, 0, n);
                dest.flush();
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

    Socket m_socket;
};

//-----------------------------------------------------------------------------
