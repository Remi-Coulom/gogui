// BoardSizeDialog.java

package net.sf.gogui.gui;

import java.awt.Component;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import net.sf.gogui.go.GoPoint;
import static net.sf.gogui.gui.I18n.i18n;

/** Dialog for entering a board size. */
public final class BoardSizeDialog
{
    /** Run dialog.
        @return Board size or -1 if aborted. */
    public static int show(Component parent, int size,
                           MessageDialogs messageDialogs)
    {
        Object value =
            JOptionPane.showInputDialog(parent, i18n("LB_BOARDSIZE_DIALOG"),
                                        i18n("TIT_INPUT"),
                                        JOptionPane.PLAIN_MESSAGE, null, null,
                                        Integer.toString(size));
        if (value == null)
            return -1;
        size = -1;
        try
        {
            size = Integer.parseInt((String)value);
            if (size < 1 || size > GoPoint.MAX_SIZE)
                size = -1;
        }
        catch (NumberFormatException e)
        {
        }
        if (size == -1)
        {
            String optionalMessage =
                MessageFormat.format(i18n("MSG_BOARDSIZE_DIALOG_INVALID_2"),
                                     GoPoint.MAX_SIZE);
            messageDialogs.showError(parent,
                                     i18n("MSG_BOARDSIZE_DIALOG_INVALID"),
                                     optionalMessage, false);
        }
        return size;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private BoardSizeDialog()
    {
    }
}
