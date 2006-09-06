//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpserver;

import net.sf.gogui.util.Options;
import net.sf.gogui.util.ProcessUtil;
import net.sf.gogui.util.StreamCopy;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Connects a Go program supporting GTP to a socket. */
public final class GtpServer
{
    /** @param verbose Log everything sent and received to stderr
        @param loop Restart program and wait for new connection after
        connection is closed (only for incoming connections)
        @param program Command line for Go program
        @param remoteHost If null then the server waits for incoming
        connections, otherwise it connects to a remote computer with this
        name.
        @param port Port number at remote host
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
            Process process
                = runtime.exec(StringUtil.splitArguments(program));
            Thread stdErrThread = new ProcessUtil.StdErrThread(process);
            stdErrThread.start();
            Socket socket;
            if (serverSocket == null)
                socket = connectToRemote(remoteHost, port, userFile);
            else
            {
                if (verbose)
                    System.err.println("Waiting for connection...");
                socket = serverSocket.accept();
            }
            if (verbose)
                System.err.println("gtpserver: Connected with "
                                   + socket.getInetAddress());
            Thread fromNet =
                new Thread(new StreamCopy(verbose, socket.getInputStream(),
                                          process.getOutputStream(), true));
            Thread toNet =
                new Thread(new StreamCopy(verbose, process.getInputStream(),
                                          socket.getOutputStream(), false));
            fromNet.start();
            toNet.start();
            toNet.join();
            socket.shutdownOutput();
            fromNet.join();
            socket.close();
            process.waitFor();
            if (remoteHost != null || ! loop)
                break;
        }
        if (serverSocket != null)
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
            Options opt = Options.parse(args, options);
            boolean verbose = opt.isSet("verbose");
            boolean loop = opt.isSet("loop");
            if (loop && opt.isSet("remote"))
            {
                System.err.println("Option -loop can't be used with -remote");
                System.exit(-1);
            }
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpServer " + Version.get());
                return;
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
            ArrayList arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            new GtpServer(verbose, loop, program, remoteHost, port, userFile);
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
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
        System.err.println("Connected");
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
    
//----------------------------------------------------------------------------
