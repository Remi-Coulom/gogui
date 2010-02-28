// ProcessUtil.java

package net.sf.gogui.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ExitWaiter
    extends Thread
{
    public ExitWaiter(Object monitor, Process process)
    {
        m_monitor = monitor;
        m_process = process;
    }

    public boolean isFinished()
    {
        synchronized (m_mutex)
        {
            return m_isFinished;
        }
    }

    public void run()
    {
        try
        {
            m_process.waitFor();
        }
        catch (InterruptedException e)
        {
        }
        synchronized (m_mutex)
        {
            m_isFinished = true;
        }
        synchronized (m_monitor)
        {
            m_monitor.notifyAll();
        }
    }

    private boolean m_isFinished;

    private final Object m_monitor;

    private final Object m_mutex = new Object();

    private final Process m_process;
};

/** Static utility functions and classes related to processes. */
public class ProcessUtil
{
    /** Copies standard error of a process to System.err. */
    public static class StdErrThread
        extends Thread
    {
        public StdErrThread(Process process)
        {
            super(new StreamCopy(false, process.getErrorStream(), System.err,
                                 false));
        }
    }

    /** Run a process and return its standard output as a string. */
    public static String runCommand(String[] cmdArray) throws IOException
    {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmdArray);
        Thread discardErr = new StreamDiscard(process.getErrorStream());
        discardErr.start();
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try
        {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                result.append(line);
                result.append('\n');
            }
            try
            {
                if (process.waitFor() != 0)
                    throw new IOException("Process returned error status");
            }
            catch (InterruptedException e)
            {
                throw new IOException("InterruptedException");
            }
            return result.toString();
        }
        finally
        {
            reader.close();
        }
    }

    /** Run a process.
        Forwards the stdout/stderr of the child process to stderr of the
        calling process. */
    public static void runProcess(String[] cmdArray) throws IOException
    {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmdArray);
        Thread copyOut =
            new Thread(new StreamCopy(false, process.getInputStream(),
                                      System.err, false));
        copyOut.start();
        Thread copyErr =
            new Thread(new StreamCopy(false, process.getErrorStream(),
                                      System.err, false));
        copyErr.start();
    }

    public static boolean waitForExit(Process process, long timeout)
    {
        Object monitor = new Object();
        ExitWaiter exitWaiter = new ExitWaiter(monitor, process);
        synchronized (monitor)
        {
            exitWaiter.start();
            try
            {
                monitor.wait(timeout);
                return exitWaiter.isFinished();
            }
            catch (InterruptedException e)
            {
                return false;
            }
        }
    }
}
