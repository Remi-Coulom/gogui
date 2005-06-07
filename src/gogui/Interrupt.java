//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import gtp.GtpError;
import gui.CommandThread;
import gui.SimpleDialogs;

//----------------------------------------------------------------------------

/** Interrupt command. */
public class Interrupt
{
    /** Interrupt command.
        Confirm interrupt by user and send interrupt comment line if
        supported by the program, otherwise kill the program.
        @return true if interrupt comment line was sent.
    */
    public static boolean run(Component parent, CommandThread thread)
    {
        if (! thread.isInterruptSupported())
        {
            Object[] options = { "Kill Program", "Cancel" };
            Object message = "Program does not support interrupt";
            int n = JOptionPane.showOptionDialog(parent, message, "Question",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.WARNING_MESSAGE,
                                                 null, options, options[1]);
            if (n == 0)
                thread.destroyGtp();
            return false;
        }
        if (! SimpleDialogs.showQuestion(parent, "Interrupt command?"))
            return false;
        try
        {
            thread.sendInterrupt();
        }
        catch (GtpError e)
        {
            SimpleDialogs.showError(parent, e.getMessage());
            return false;
        }
        return true;
    }
}

//----------------------------------------------------------------------------
