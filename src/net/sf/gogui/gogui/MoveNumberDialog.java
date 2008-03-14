// MoveNumberDialog.java

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.gui.MessageDialogs;
import static net.sf.gogui.gogui.I18n.i18n;

/** Ask for a move number in a variation given by a node. */
public final class MoveNumberDialog
{
    public static ConstNode show(Component parent, ConstNode node,
                                 MessageDialogs messageDialogs)
    {
        int number = NodeUtil.getMoveNumber(node);
        Object value =
            JOptionPane.showInputDialog(parent, i18n("LB_MOVENUMBER"),
                                        i18n("TIT_INPUT"),
                                        JOptionPane.PLAIN_MESSAGE, null, null,
                                        Integer.toString(number));
        if (value == null || value.equals(""))
            return null;
        try
        {
            number = Integer.parseInt((String)value);
            node = NodeUtil.findByMoveNumber(node, number);
            if (node == null)
            {
                messageDialogs.showError(parent,
                                         i18n("MSG_MOVENUMBER_NOT_EXISTING"),
                                         i18n("MSG_MOVENUMBER_NOT_EXISTING_2"),
                                         false);
                return null;
            }
            return node;
        }
        catch (NumberFormatException e)
        {
            messageDialogs.showError(parent,
                                     i18n("MSG_MOVENUMBER_NO_NUMBER"),
                                     i18n("MSG_MOVENUMBER_NO_NUMBER_2"),
                                     false);
            return null;
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private MoveNumberDialog()
    {
    }
}
