//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import gui.SimpleDialogs;

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
        int boardSize = -1;
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            SimpleDialogs.showError(parent, "Invalid size");
            return -1;
        }
    }
}

//----------------------------------------------------------------------------
