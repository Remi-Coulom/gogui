//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.go.GoPoint;

/** Dialog for entering a board size. */
public final class BoardSizeDialog
{
    /** Run dialog.
        @return Board size or -1 if aborted. */
    public static int show(Component parent, int size,
                           MessageDialogs messageDialogs)
    {
        Object value =
            JOptionPane.showInputDialog(parent, "Board size", "Input",
                                        JOptionPane.PLAIN_MESSAGE, null, null,
                                        Integer.toString(size));
        if (value == null)
            return -1;
        size = -1;
        try
        {
            size = Integer.parseInt((String)value);
            if (size < 1 || size > GoPoint.MAXSIZE)
                size = -1;
        }
        catch (NumberFormatException e)
        {
        }
        if (size == -1)
        {
            String mainMessage = "Invalid size";
            String optionalMessage =
                "Valid board sizes are numbers between 1 and "
                + GoPoint.MAXSIZE + ".";
            messageDialogs.showError(parent, mainMessage, optionalMessage,
                                     false);
        }
        return size;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private BoardSizeDialog()
    {
    }
}

