//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.StringUtil;

/** GoGui main function. */
public final class Main
{
    /** GoGui main function. */
    public static void main(String[] args)
    {
        GoGuiSettings settings;
        try
        {
            settings =
                new GoGuiSettings(args,
                                  Class.forName("net.sf.gogui.gogui.GoGui"));
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
            SimpleDialogs.showError(null, StringUtil.printException(t));
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
            SimpleDialogs.showError(null, StringUtil.printException(t));
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
        GuiUtil.initLookAndFeel(settings.m_lookAndFeel);
        new GoGui(settings.m_program, settings.m_file, settings.m_move,
                  settings.m_time, settings.m_verbose,
                  settings.m_computerBlack, settings.m_computerWhite,
                  settings.m_auto, settings.m_gtpFile, settings.m_gtpCommand,
                  settings.m_initAnalyze);
    }
}

