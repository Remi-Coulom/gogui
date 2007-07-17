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
    public static void main(String[] args) throws Exception
    {
        // GoGui is not localized, avoid a mix between English and local
        // language in Swing dialogs
        Locale.setDefault(Locale.ENGLISH);
        System.setProperty("apple.awt.brushMetalLook", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
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
