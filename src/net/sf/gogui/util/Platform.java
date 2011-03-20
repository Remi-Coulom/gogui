// Platform.java

package net.sf.gogui.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.Locale;

/** Static utility functions for platform detection and platform-dependent
    behavior. */
public class Platform
{
    /** Handler for events from the Application Menu on MacOS. */
    public interface SpecialMacHandler
    {
        /** Handle about menu event.
            @return true if event was handled successfully. */
        boolean handleAbout();

        /** Handle open file event.
            @param filename name of file.
            @return true if event was handled successfully. */
        boolean handleOpenFile(String filename);

        /** Handle quit application event.
            @return true if event was handled successfully, false if quit
            should be aborted. */
        boolean handleQuit();
    }

    public static String getJavaRuntimeName()
    {
        // java.runtime.name is not a standard property
        String name = System.getProperty("java.runtime.name");
        if (name == null)
            name = System.getProperty("java.vm.name");
        return name;
    }

    /** Return information on this computer.
        Returns host name and cpu information (if /proc/cpuinfo exists). */
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
            if (existsProcCpuinfo())
            {
                String[] cmdArray = { "/bin/sh", "-c",
                                      "grep '^model name' /proc/cpuinfo" };
                String result = ProcessUtil.runCommand(cmdArray);
                int start = result.indexOf(':');
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
        }
        catch (IOException e)
        {
        }
        return info;
    }

    /** Check if the platform is Mac OS X. */
    public static boolean isMac()
    {
        return s_isMac;
    }

    /** Check if the platform is Unix. */
    public static boolean isUnix()
    {
        return s_isUnix;
    }

    /** Check if the platform is Windows. */
    public static boolean isWindows()
    {
        return s_isWindows;
    }

    /** Try to open a URL in en external browser.
        Tries /usr/bin/open if Platform.isMac(),
        rundll32 url.dll,FileProtocolHandler if Platform.isWindows(),
        and if isUnix() in this order:
        - xdg-open
        - kfmclient (if KDE is running)
        - firefox
        - mozilla
        - opera
        @param url URL to open.
        @return false if everything failed. */
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
                String[] cmd = { "xdg-open", url.toString() };
                if (runProcess(cmd))
                    return true;
            }
            if (checkKDERunning())
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
        @param handler Handler to register. */
    public static void registerSpecialMacHandler(SpecialMacHandler handler)
    {
        try
        {
            Object[] args = { handler };
            Class[] arglist = { Platform.SpecialMacHandler.class };
            String name = "net.sf.gogui.specialmac.RegisterSpecialMacHandler";
            Class<?> registerClass = Class.forName(name);
            Constructor constructor = registerClass.getConstructor(arglist);
            constructor.newInstance(args);
        }
        catch (Throwable e)
        {
            System.err.println("Could not register handler for Mac events." +
                               " (com.apple.eawt classes not found)");
        }
    }

    private static boolean s_isMac;

    private static boolean s_isUnix;

    private static boolean s_isWindows;

    static
    {
        // See http://developer.apple.com/technotes/tn2002/tn2110.html
        String name = System.getProperty("os.name");
        s_isMac = name.toLowerCase(Locale.getDefault()).startsWith("mac os x");
        s_isUnix = (name.indexOf("nix") >= 0 || name.indexOf("nux") >= 0);
        s_isWindows = name.startsWith("Windows");
    }

    private static boolean checkKDERunning()
    {
        try
        {
            String[] cmdArray = { "dcop" };
            String result = ProcessUtil.runCommand(cmdArray);
            return (result.indexOf("kicker") >= 0);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    private static boolean existsProcCpuinfo()
    {
        return new File("/proc/cpuinfo").exists();
    }

    private static boolean runProcess(String[] cmd)
    {
        try
        {
            ProcessUtil.runProcess(cmd);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }
}
