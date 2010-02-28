// Interrupt.java

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.gtp.GtpError;
import static net.sf.gogui.gogui.I18n.i18n;
import net.sf.gogui.gui.GuiGtpClient;
import net.sf.gogui.gui.MessageDialogs;
import net.sf.gogui.util.Platform;

/** Interrupt command. */
public final class Interrupt
{
    /** Interrupt command.
        Confirm interrupt by user and send interrupt comment line if
        supported by the program, otherwise kill the program.
        @return true if interrupt comment line was sent. */
    public boolean run(Component parent, GuiGtpClient gtp,
                       MessageDialogs messageDialogs)
    {
        if (! gtp.isInterruptSupported())
        {
            Object[] options =
                { i18n("LB_INTERRUPT_TERMINATE"), i18n("LB_CANCEL") };
            Object message = i18n("MSG_INTERRUPT_NO_SUPPORT");
            int type = JOptionPane.WARNING_MESSAGE;
            if (Platform.isMac())
                type = JOptionPane.PLAIN_MESSAGE;
            int n = JOptionPane.showOptionDialog(parent, message,
                                                 i18n("TIT_QUESTION"),
                                                 JOptionPane.YES_NO_OPTION,
                                                 type, null, options,
                                                 options[1]);
            if (n == 0)
                gtp.destroyGtp();
            return false;
        }
        if (! gtp.isCommandInProgress())
            return false;
        try
        {
            gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            messageDialogs.showError(parent, i18n("MSG_INTERRUPT_FAILED"), e);
            return false;
        }
        return true;
    }
}
