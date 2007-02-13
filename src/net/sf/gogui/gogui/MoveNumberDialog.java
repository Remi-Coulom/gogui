//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.gui.SimpleDialogs;

/** Ask for a move number in a variation given by a node. */
public final class MoveNumberDialog
{
    public static ConstNode show(Component parent, ConstNode node)
    {
        int number = NodeUtil.getMoveNumber(node);        
        Object value =
            JOptionPane.showInputDialog(parent, "Move Number", "Input",
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
                SimpleDialogs.showError(parent, "No move with this number");
                return null;
            }
            return node;
        }
        catch (NumberFormatException e)
        {
            SimpleDialogs.showError(parent, "Not a number");
            return null;
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private MoveNumberDialog()
    {
    }
}

