//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import net.sf.gogui.utils.ErrorMessage;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//----------------------------------------------------------------------------
 
/** Wrapper for starting GoGui.
    Loads the SplashScreen class with the reflection API to set Mac AWT
    properties before any AWT class is loaded.
*/
public final class MacMain
{
    public static final void main(String [] args)
    {
        System.setProperty("apple.awt.brushMetalLook", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        GoGuiSettings settings;
        try
        {
            settings = new GoGuiSettings(args);
            if (settings.m_noStartup)
                return;
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        try
        {
            Class [] mainArgs = new Class[1];
            mainArgs[0] = Class.forName("net.sf.gogui.gogui.GoGuiSettings");
            Class mainClass
                = Class.forName("net.sf.gogui.gogui.SplashScreen");
            Method mainMethod = mainClass.getMethod("main", mainArgs);
            assert((mainMethod.getModifiers() & Modifier.STATIC) != 0);
            assert(mainMethod.getReturnType() == void.class); 
            Object[] objArgs = new Object[1];
            objArgs[0] = settings;
            mainMethod.invoke(null, objArgs);
        }
        catch (Exception e)
        {
            fatalError(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private MacMain()
    {
    }

    private static void fatalError(String message)
    {
        printError(message);
        System.exit(-1);
    }
 
    private static void printError(String message)
    {
        System.err.println("SplashScreen: " + message);
    }
 
}

//----------------------------------------------------------------------------
