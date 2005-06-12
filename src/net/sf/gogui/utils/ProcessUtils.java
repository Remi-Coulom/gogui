//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.IOException;

//----------------------------------------------------------------------------

class ExitWaiter
    extends Thread
{
    public ExitWaiter(Object monitor, Process process)
    {
        m_monitor = monitor;
        m_process = process;
    }

    public synchronized boolean isFinished()
    {
        return m_isFinished;
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
        synchronized (this)
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

    private final Process m_process;
};

//----------------------------------------------------------------------------

/** Static utility functions and classes related to processes.
*/
public class ProcessUtils
{
    /** Copies standard error of a process to System.err. */
    public static class StdErrThread
        extends StreamCopy
    {
        public StdErrThread(Process process)
        {
            super(false, process.getErrorStream(), System.err, false);
        }        
    }

    /** Run a process.
        Forwards the stdout/stderr of the child process to stderr of the
        calling process.
    */
    public static void runProcess(String[] cmdArray) throws IOException
    {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmdArray);
        Thread copyOut = new StreamCopy(false, process.getInputStream(),
                                        System.err, false);
        copyOut.start();
        Thread copyErr = new StreamCopy(false, process.getErrorStream(),
                                        System.err, false);
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

//----------------------------------------------------------------------------
