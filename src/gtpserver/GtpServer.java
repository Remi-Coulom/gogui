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
    /** Constructor.
        @param remoteHost If null then the server waits for incoming
        connections, otherwise it connects to a remote computer with this name.
        @param userFile file containing login information that is sent to the
        remote host. Only used if remoteHost is set.
    */
    public GtpServer(boolean verbose, boolean loop, String program,
                     String remoteHost, int port, String userFile)
        throws Exception
    {
        Runtime runtime = Runtime.getRuntime();
        ServerSocket serverSocket = null;
        if (remoteHost == null)
            serverSocket = new ServerSocket(port, 1);
        while (true)
        {
            Process process = runtime.exec(StringUtils.tokenize(program));
            Thread stdErrThread = new StdErrThread(process);
            stdErrThread.start();
            Socket socket;
            if (remoteHost == null)
            {
                if (verbose)
                    System.err.println("gtpserver: Waiting for connection...");
                socket = serverSocket.accept();
            }
            else
                socket = connectToRemote(remoteHost, port, userFile);
            if (verbose)
                System.err.println("gtpserver: Connected with "
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
            if (remoteHost == null || ! loop)
                break;
        }
        if (remoteHost == null)
            serverSocket.close();
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "help",
                "loop",
                "port:",
                "remote:",
                "user:",
                "verbose",
                "version",
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            boolean verbose = opt.isSet("verbose");
            boolean loop = opt.isSet("loop");
            if (loop && opt.isSet("remote"))
            {
                System.err.println("Option -loop cannot be used with -remote");
                System.exit(-1);
            }
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpServer " + Version.get());
                System.exit(0);
            }
            if (! opt.isSet("port"))
            {
                System.err.println("Please specify port with option -port");
                System.exit(-1);
            }
            int port = opt.getInteger("port");
            String remoteHost = opt.getString("remote", null);
            String userFile = opt.getString("user", null);
            if (userFile != null && remoteHost == null)
            {
                System.err.println("Option -user only valid with -remote");
                System.exit(-1);
            }
            Vector arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            GtpServer gtpServer = new GtpServer(verbose, loop, program,
                                                remoteHost, port, userFile);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            System.err.println(StringUtils.formatException(t));
            System.exit(-1);
        }
    }

    private static Socket connectToRemote(String remoteHost, int port,
                                          String userFile) throws Exception
    {
        System.err.println("Connecting to " + remoteHost + " " + port);
        Socket socket = new Socket(remoteHost, port);
        if (userFile != null)
        {
            System.err.println("Sending login information from file "
                               + userFile);
            InputStream inputStream = new FileInputStream(new File(userFile));
            OutputStream outputStream = socket.getOutputStream();
            byte buffer[] = new byte[1024];
            while (true)
            {
                int n = inputStream.read(buffer);
                if (n < 0)
                    break;
                outputStream.write(buffer, 0, n);
            }
            inputStream.close();
        }
        System.err.println("Connected.");
        return socket;
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: java -jar gtpserver.jar [options] program\n" +
                  "\n" +
                  "-config  config file\n" +
                  "-help    display this help and exit\n" +
                  "-loop    restart after connection finished\n" +
                  "-port    port of network connection\n" +
                  "-remote  connect to remote host\n" +
                  "-user    login information for remote host\n" +
                  "-verbose print debugging messages\n" +
                  "-version print version and exit\n");
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
            System.err.println(StringUtils.formatException(e));
        }
    }

    private Reader m_err;
}

//-----------------------------------------------------------------------------
