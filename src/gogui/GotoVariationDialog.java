//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import game.GameTree;
import game.Node;
import game.NodeUtils;
import gui.SimpleDialogs;

//----------------------------------------------------------------------------

/** Ask for a variation. */
public class GotoVariationDialog
{
    public static Node show(Component parent, GameTree tree, Node currentNode)
    {
        String variation = NodeUtils.getVariationString(currentNode);
        variation =
            JOptionPane.showInputDialog(parent, "Variation", variation);
        if (variation == null || variation.equals(""))
            return null;
        Node root = tree.getRoot();
        Node node = NodeUtils.findByVariation(root, variation);
        if (node == null)
            SimpleDialogs.showError(parent, "Invalid variation");
        return node;
    }
}

//----------------------------------------------------------------------------
