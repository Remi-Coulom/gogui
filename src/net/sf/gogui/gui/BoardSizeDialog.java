//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

/** Dialog for entering a board size. */
public class BoardSizeDialog
{
    /** Run dialog.
        @return Board size or -1 if aborted. */
    public static int show(Component parent, int size)
    {
        String value = Integer.toString(size);
        value = JOptionPane.showInputDialog(parent, "Board size", value);
        if (value == null)
            return -1;
        size = -1;
        try
        {
            size = Integer.parseInt(value);
            if (size < 1 || size > GoPoint.MAXSIZE)
                size = -1;
        }
        catch (NumberFormatException e)
        {
        }
        if (size == -1)
            SimpleDialogs.showError(parent, "Invalid size");
        return size;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private BoardSizeDialog()
    {
    }
}

//----------------------------------------------------------------------------
