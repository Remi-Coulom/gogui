//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import utils.*;
import version.*;

//-----------------------------------------------------------------------------

class GtpServer
{
    public GtpServer(boolean verbose, boolean loop, String program, int port)
        throws Exception
    {
        Runtime runtime = Runtime.getRuntime();
        ServerSocket serverSocket = new ServerSocket(port, 1);
        while (true)
        {
            Process process = runtime.exec(StringUtils.tokenize(program));
            Thread stdErrThread = new StdErrThread(process);
            stdErrThread.start();
            if (verbose)
                System.err.println("gtpnet: Waiting for connection ...");
            Socket socket = serverSocket.accept();
            if (verbose)
                System.err.println("gtpnet: Connection from "
                                   + socket.getInetAddress());
            StreamCopy fromNet =
                new StreamCopy(verbose, socket.getInputStream(),
                               process.getOutputStream(), true);
            StreamCopy toNet =
                new StreamCopy(verbose, process.getInputStream(),
                               socket.getOutputStream(), false);
            fromNet.start();
            toNet.start();
            toNet.join();
            socket.shutdownOutput();
            fromNet.join();
            socket.close();
            process.waitFor();
            if (! loop)
                break;
        }
        serverSocket.close();
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "help",
                "loop",
                "verbose",
                "version",
            };
            Options opt = new Options(args, options);
            boolean verbose = opt.isSet("verbose");
            boolean loop = opt.isSet("loop");
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpServer " + Version.m_version);
                System.exit(0);
            }
            Vector arguments = opt.getArguments();
            if (arguments.size() != 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            int port = Integer.parseInt((String)arguments.get(1));
            GtpServer gtpServer = new GtpServer(verbose, loop, program, port);
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
        out.print("Usage: java -jar gtpnet.jar [options] program port\n" +
                  "\n" +
                  "  -help    display this help and exit\n" +
                  "  -loop    restart after connection finished\n" +
                  "  -verbose print debugging messages\n" +
                  "  -version print version and exit\n");
    }
}
    
class StdErrThread
    extends Thread
{
    public StdErrThread(Process process)
    {
        m_err = new InputStreamReader(process.getErrorStream());
    }
    
    public void run()
    {
        try
        {
            int size = 1024;
            char[] buffer = new char[size];
            while (true)
            {
                int n = m_err.read(buffer, 0, size);
                if (n < 0)
                    return;
                String s = new String(buffer, 0, n);
                System.err.print(s);
                System.err.flush();
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

    private Reader m_err;
}

//-----------------------------------------------------------------------------
