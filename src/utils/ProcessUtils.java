//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.io.*;

//----------------------------------------------------------------------------

/** Static utility functions related to processes.
*/
public class ProcessUtils
{
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
