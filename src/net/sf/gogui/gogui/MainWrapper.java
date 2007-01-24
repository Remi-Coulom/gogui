//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import net.sf.gogui.util.ErrorMessage;

/** Wrapper for starting GoGui.
    Loads the main class with the reflection API to set Mac AWT
    properties before any AWT class is loaded.
*/
public final class MainWrapper
{
    public static void main(String [] args) throws Exception
    {
        // GoGui is not localized, avoid a mix between English and local
        // language in Swing dialogs
        Locale.setDefault(Locale.ENGLISH);
        System.setProperty("apple.awt.brushMetalLook", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        // On Windows, use GDI instead of DirectDraw to avoid screen flicker
        // see http://mindprod.com/jgloss/flicker.html
        System.setProperty("sun.java2d.noddraw", "true");
        // Java 1.6 does not detect antialiasing settings automatically on some
        // desktops (e.g. KDE) so we enforce LCD type antialiasing
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
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
        Class [] mainArgs = new Class[1];
        mainArgs[0] = Class.forName("net.sf.gogui.gogui.GoGuiSettings");
        Class mainClass = Class.forName("net.sf.gogui.gogui.Main");
        Method mainMethod = mainClass.getMethod("main", mainArgs);
        assert((mainMethod.getModifiers() & Modifier.STATIC) != 0);
        assert(mainMethod.getReturnType() == void.class); 
        Object[] objArgs = new Object[1];
        objArgs[0] = settings;
        mainMethod.invoke(null, objArgs);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private MainWrapper()
    {
    }
}

