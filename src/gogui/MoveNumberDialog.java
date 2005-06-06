//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import game.Node;
import game.NodeUtils;
import gui.SimpleDialogs;

//----------------------------------------------------------------------------

/** Ask for a move number in a variation given by a node. */
public class MoveNumberDialog
{
    public static Node show(Component parent, Node node)
    {
        int number = NodeUtils.getMoveNumber(node);        
        String value = Integer.toString(number);
        value = JOptionPane.showInputDialog(parent, "Move Number", value);
        if (value == null || value.equals(""))
            return null;
        try
        {
            number = Integer.parseInt(value);
            node = NodeUtils.findByMoveNumber(node, number);
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
}

//----------------------------------------------------------------------------
