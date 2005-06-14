//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** GoGui main function. */
public class Main
{
    /** GoGui main function. */
    public static void main(String[] args)
    {
        // Setting these Mac system properties here worked with older versions
        // of Mac Java, for newer ones, they have to be set when starting the
        // VM (see options in scripts and mac/Info.plist)
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        System.setProperty("apple.awt.brushMetalLook", "true");
        GoGuiSettings settings;
        try
        {
            settings = new GoGuiSettings(args);
            if (settings.m_noStartup)
                return;
            startGoGui(settings);
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtils.printException(t));
            System.exit(-1);
        }
    }

    public static void main(GoGuiSettings settings)
    {
        try
        {
            startGoGui(settings);
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtils.printException(t));
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void startGoGui(GoGuiSettings settings)
        throws GtpError
    {
        assert(! settings.m_noStartup);
        new GoGui(settings.m_program, settings.m_preferences, settings.m_file,
                  settings.m_move, settings.m_time, settings.m_verbose,
                  settings.m_computerBlack, settings.m_computerWhite,
                  settings.m_auto, settings.m_gtpFile, settings.m_gtpCommand,
                  settings.m_initAnalyze, settings.m_fastPaint);
    }
}

//----------------------------------------------------------------------------
