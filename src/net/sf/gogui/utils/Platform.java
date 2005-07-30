//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
        /** Handle about menu event.
            @return true if event was handled successfully.
        */
        boolean handleAbout();

        /** Handle open file event.
            @param filename name of file.
            @return true if event was handled successfully.
        */
        boolean handleOpenFile(String filename);

        /** Handle quit application event.
            @return true if event was handled successfully, false if quit
            should be aborted.
        */
        boolean handleQuit();
    }

    /** Return information on this computer.
        Returns host name and cpu information (if /proc/cpuinfo exists).
    */
    public static String getHostInfo()
    {
        String info;
        try
        {
            info = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            info = "?";
        }
        try
        {
            String[] cmdArray = { "/bin/sh", "-c",
                                  "grep '^model name' /proc/cpuinfo" };
            String result = ProcessUtils.runCommand(cmdArray);
            int start = result.indexOf(":");
            if (start >= 0)
            {
                info = info + " (";
                int end = result.indexOf("\n");
                if (end >= 0)
                    info = info + result.substring(start + 1, end).trim();
                else
                    info = info + result.substring(start + 1).trim();
                info = info + ")";
            }
        }
        catch (IOException e)
        {
        }
        return info;
    }

    /** Check if the platform is Mac OS X. */
    public static boolean isMac()
    {
        // According to the article "Tailoring Java Applications for Mac OS X"
        // (Technical Note TN2042) it is better to check for mrj.version than
        // to parse os.name
        return (System.getProperty("mrj.version") != null);
    }

    /** Check if the platform is Unix. */
    public static boolean isUnix()
    {
        String osName = System.getProperty("os.name");
        return osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0;
    }

    /** Check if the platform is Windows. */
    public static boolean isWindows()
    {
        return System.getProperty("os.name").startsWith("Windows");
    }

    /** Try to open a URL in en external browser.
        Tries /usr/bin/open if Platform.isMac(),
        rundll32 url.dll,FileProtocolHandler if Platform.isWindows(),
        and if isUnix() in this order:
        - kfmclient
        - firefox
        - mozilla
        - opera
        @param url URL to open.
        @return false if everything failed.
    */
    public static boolean openInExternalBrowser(URL url)
    {
        if (isMac())
        {
            String[] cmd = { "/usr/bin/open", url.toString() };
            if (runProcess(cmd))
                return true;
        }
        else if (isWindows())
        {
            String[] cmd = { "rundll32", "url.dll,FileProtocolHandler",
                             url.toString() };
            if (runProcess(cmd))
                return true;
        }
        else if (isUnix())
        {
            {
                String[] cmd = { "kfmclient", "openURL", url.toString() };
                if (runProcess(cmd))
                    return true;
            }
            {
                String[] cmd = { "firefox", url.toString() };
                if (runProcess(cmd))
                    return true;
            }
            {
                String[] cmd = { "mozilla", url.toString() };
                if (runProcess(cmd))
                    return true;
            }
            {
                String[] cmd = { "opera", url.toString() };
                if (runProcess(cmd))
                    return true;
            }
        }
        return false;
    }

    /** Register handler for events from the Application Menu on MacOS.
        @param handler Handler to register.
    */
    public static void registerSpecialMacHandler(SpecialMacHandler handler)
    {
        try
        {
            Object[] args = { handler };
            Class[] arglist = { Platform.SpecialMacHandler.class };
            String name = "net.sf.gogui.specialmac.RegisterSpecialMacHandler";
            Class registerClass = Class.forName(name);
            Constructor constructor = registerClass.getConstructor(arglist);
            constructor.newInstance(args);
        }
        catch (Exception e)
        {
            StringUtils.printException(e);
        }
    }

    private static boolean runProcess(String[] cmd)
    {
        try
        {
            ProcessUtils.runProcess(cmd);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }
}

//----------------------------------------------------------------------------
