//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;

//----------------------------------------------------------------------------

/** Static utility functions for platform detection and platform-dependent
    behavior.
*/
public class Platform
{
    /** Handler for events from the Application Menu on MacOS. */
    public interface SpecialMacHandler
    {
        public boolean handleAbout();

        public boolean handleOpenFile(String filename);

        public boolean handleQuit();
    }

    /** Check if the platform is Mac OS X */
    public static boolean isMac()
    {
        // According to the article "Tailoring Java Applications for Mac OS X"
        // (Technical Note TN2042) it is better to check for mrj.version than
        // to parse os.name
        return (System.getProperty("mrj.version") != null);
    }

    /** Check if the platform is Unix */
    public static boolean isUnix()
    {
        String osName = System.getProperty("os.name");
        return osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0;
    }

    /** Check if the platform is Windows */
    public static boolean isWindows()
    {
        return System.getProperty("os.name").startsWith("Windows");
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
        if (isMac())
        {
            try
            {
                String[] cmd = { "/usr/bin/open", url.toString() };
                ProcessUtils.runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
        }
        else if (isWindows())
        {
            try
            {
                String[] cmd = { "rundll32", "url.dll,FileProtocolHandler",
                                 url.toString() };
                ProcessUtils.runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
        }
        else if (isUnix())
        {
            try
            {
                String[] cmd = { "kfmclient", "exec", url.toString() };
                ProcessUtils.runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
            try
            {
                String[] cmd = { "firefox", url.toString() };
                ProcessUtils.runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
            try
            {
                String[] cmd = { "mozilla", url.toString() };
                ProcessUtils.runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
            try
            {
                String[] cmd = { "opera", url.toString() };
                ProcessUtils.runProcess(cmd);
                return true;
            }
            catch (IOException e)
            {
            }
        }
        return false;
    }

    /** Register handler for events from the Application Menu on MacOS */
    public static void registerSpecialMacHandler(SpecialMacHandler handler)
    {
        try
        {
            Object[] args = { handler };
            Class[] arglist = { Platform.SpecialMacHandler.class };
            Class registerClass =
                Class.forName("specialmac.RegisterSpecialMacHandler");
            Constructor constructor = registerClass.getConstructor(arglist);
            constructor.newInstance(args);
        }
        catch(Exception e)
        {
            StringUtils.printException(e);
        }
    }

}

//----------------------------------------------------------------------------
