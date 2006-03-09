//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** GoGui main function. */
public final class Main
{
    /** GoGui main function. */
    public static void main(String[] args)
    {
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
        throws GtpError, ErrorMessage
    {
        assert(! settings.m_noStartup);
        GuiUtils.initLookAndFeel(settings.m_lookAndFeel);
        new GoGui(settings.m_program, settings.m_preferences, settings.m_file,
                  settings.m_move, settings.m_time, settings.m_verbose,
                  settings.m_computerBlack, settings.m_computerWhite,
                  settings.m_auto, settings.m_gtpFile, settings.m_gtpCommand,
                  settings.m_initAnalyze, settings.m_fastPaint);
    }
}

//----------------------------------------------------------------------------
