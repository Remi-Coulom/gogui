//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.io.*;

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

}

//----------------------------------------------------------------------------
