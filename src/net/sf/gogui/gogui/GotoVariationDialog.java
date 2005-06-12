//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.gui.SimpleDialogs;

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

    /** Make constructor unavailable; class is for namespace only. */
    private GotoVariationDialog()
    {
    }
}

//----------------------------------------------------------------------------
