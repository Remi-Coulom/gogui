//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.io.*;
import java.net.*;

//----------------------------------------------------------------------------

public class Platform
{
    /** Check if the platform is Mac OS X */
    public static boolean isMac()
    {
        // According to the article "Tailoring Java Apllications for Mac OS X"
        // (Technical Note TN2042) it is better to check for mrj.version than
        // to parse os.name
        return (System.getProperty("mrj.version") != null);
    }

    /** Try to open a URL in en external browser.
        Tries /usr/bin/open if Platform.isMac(), otherwise (in this order):
        - kfmclient (the KDE browser)
        - mozilla
        - rundll32 url.dll,FileProtocolHandler (Windows)
        @return false if everything failed
    */
    public static boolean openInExternalBrowser(URL url)
    {
        if (Platform.isMac())
        {
            try
            {
                String[] cmd = { "/usr/bin/open", url.toString() };
                runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
        }
        else
        {
            try
            {
                String[] cmd = { "kfmclient", "exec", url.toString() };
                runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
            try
            {
                String[] cmd = { "mozilla", url.toString() };
                runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
            try
            {
                String[] cmd = { "rundll32", "url.dll,FileProtocolHandler",
                                 url.toString() };
                runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
        }
        return false;
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
