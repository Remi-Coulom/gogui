//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.gui.SimpleDialogs;

/** Ask for a variation. */
public final class GotoVariationDialog
{
    public static ConstNode show(Component parent, GameTree tree,
                                 ConstNode currentNode)
    {
        String variation = NodeUtil.getVariationString(currentNode);
        variation =
            JOptionPane.showInputDialog(parent, "Variation", variation);
        if (variation == null || variation.equals(""))
            return null;
        ConstNode root = tree.getRoot();
        ConstNode node = NodeUtil.findByVariation(root, variation);
        if (node == null)
            SimpleDialogs.showError(parent, "Invalid variation");
        return node;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GotoVariationDialog()
    {
    }
}

