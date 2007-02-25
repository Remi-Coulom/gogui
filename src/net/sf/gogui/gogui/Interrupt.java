//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.GuiGtpClient;
import net.sf.gogui.gui.MessageDialogs;
import net.sf.gogui.util.Platform;

/** Interrupt command. */
public final class Interrupt
{
    /** Interrupt command.
        Confirm interrupt by user and send interrupt comment line if
        supported by the program, otherwise kill the program.
        @return true if interrupt comment line was sent.
    */
    public boolean run(Component parent, GuiGtpClient gtp,
                       MessageDialogs messageDialogs)
    {
        if (! gtp.isInterruptSupported())
        {
            Object[] options = { "Kill Program", "Cancel" };
            Object message = "Program does not support interrupt";
            int type = JOptionPane.WARNING_MESSAGE;
            if (Platform.isMac())
                type = JOptionPane.PLAIN_MESSAGE;
            int n = JOptionPane.showOptionDialog(parent, message, "Question",
                                                 JOptionPane.YES_NO_OPTION,
                                                 type, null, options,
                                                 options[1]);
            if (n == 0)
                gtp.destroyGtp();
            return false;
        }        
        String disableKey = "net.sf.gogui.gogui.Interrupt.interrupt";
        String name = gtp.getProgramName();
        if (name == null)
            name = "the Go program";
        if (! messageDialogs.showQuestion(disableKey, parent,
                                          "Interrupt " + name + "?",
                                          "The command in progress might " +
                                          "not complete successfully.",
                                          "Interrupt", true))
            return false;
        try
        {
            gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            messageDialogs.showError(parent, "Interrupting failed", e);
            return false;
        }
        return true;
    }
}

