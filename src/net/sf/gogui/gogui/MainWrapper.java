// MainWrapper.java

package net.sf.gogui.gogui;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.sf.gogui.util.ErrorMessage;

/** Wrapper for starting GoGui.
    Loads the main class with the reflection API to set Mac AWT and other
    properties before any AWT class is loaded. */
public final class MainWrapper
{
    public static void main(String[] args) throws Exception
    {
        System.setProperty("apple.awt.brushMetalLook", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        // Use GDI rendering on Windows, there are repaint problems using
        // DDraw (last tested with Java 1.6 on Windows 7).
        // Also, using DDraw does not work well with Wine (http://winehq.com,
        // last tested versions 1.2.2 and 1.3.16) and Wine can be useful to
        // test the Windows installer for GoGui on Linux.
        System.setProperty("sun.java2d.noddraw", "true");
        GoGuiSettings settings;
        try
        {
            settings =
                new GoGuiSettings(args,
                                  Class.forName("net.sf.gogui.gogui.GoGui"));
            if (settings.m_noStartup)
                return;
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        catch (ClassNotFoundException e)
        {
            System.err.println(e.getMessage());
            return;
        }
        Class<?> mainClass = Class.forName("net.sf.gogui.gogui.Main");
        Class<?> settingsClass =
            Class.forName("net.sf.gogui.gogui.GoGuiSettings");
        Method mainMethod = mainClass.getMethod("main", settingsClass);
        assert (mainMethod.getModifiers() & Modifier.STATIC) != 0;
        assert mainMethod.getReturnType() == void.class;
        mainMethod.invoke(null, settings);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private MainWrapper()
    {
    }
}
