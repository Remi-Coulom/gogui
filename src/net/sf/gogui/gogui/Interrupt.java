//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gui.GuiGtpClient;
import net.sf.gogui.gui.OptionalMessage;
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
        if (m_question == null)
            m_question = new OptionalMessage(parent);
        if (! m_question.showQuestion("Interrupt " + gtp.getProgramName()
                                      + "?", null))
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

    private OptionalMessage m_question;
}

