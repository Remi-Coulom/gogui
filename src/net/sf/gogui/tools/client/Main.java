// Main.java

package net.sf.gogui.tools.client;

import net.sf.gogui.util.Options;
import net.sf.gogui.util.StreamCopy;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;

/** Connects to a remote Go program supporting GTP. */
public final class Main
{
    private static Socket connect(String hostname, int port, int timeout)
        throws IOException
    {
        int totalTime = 0;
        while (true)
        {
            try
            {
                return new Socket(hostname, port);
            }
            catch (ConnectException connectException)
            {
                if (totalTime >= timeout)
                    throw connectException;
                String text = "Connect failed; retrying in 5 sec...";
                System.err.println(text);
                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException interruptedException)
                {
                }
                totalTime += 5;
            }
        }
    }

    public Main(String hostname, int port, int timeout) throws Exception
    {
        Socket socket = connect(hostname, port, timeout);
        Thread fromNet =
            new Thread(new StreamCopy(false, socket.getInputStream(),
                                      System.out, false));
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
        // The two possible reasons for termination of gogui-client are:
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
                "config:",
                "help",
                "timeout:",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.contains("version"))
            {
                System.out.println("gogui-client " + Version.get());
                System.exit(0);
            }
            int timeout = opt.getInteger("timeout", 10, 0);
            ArrayList<String> arguments = opt.getArguments();
            if (arguments.size() != 2)
            {
                printUsage(System.err);
                System.exit(1);
            }
            String hostname = arguments.get(0);
            int port = Integer.parseInt(arguments.get(1));
            new Main(hostname, port, timeout);
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(1);
        }
    }

    private static void printUsage(PrintStream out)
    {
        String text =
            "Usage: gogui-client [options] hostname port\n" +
            "\n" +
            "-config  config file\n" +
            "-help    display this help and exit\n" +
            "-timeout stop trying to connect after n seconds (default 10)\n" +
            "-version print version and exit\n";
        out.print(text);
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
        catch (Throwable e)
        {
            StringUtil.printException(e);
        }
    }

    private final Socket m_socket;
}
