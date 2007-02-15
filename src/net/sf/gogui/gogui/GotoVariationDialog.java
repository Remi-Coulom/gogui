//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.gui.MessageDialogs;

/** Ask for a variation. */
public final class GotoVariationDialog
{
    public static ConstNode show(Component parent, ConstGameTree tree,
                                 ConstNode currentNode,
                                 MessageDialogs messageDialogs)
    {
        String variation = NodeUtil.getVariationString(currentNode);
        Object value =
            JOptionPane.showInputDialog(parent, "Variation", "Input",
                                        JOptionPane.PLAIN_MESSAGE, null, null,
                                        variation);
        if (value == null || value.equals(""))
            return null;
        ConstNode root = tree.getRootConst();
        ConstNode node = NodeUtil.findByVariation(root, (String)value);
        if (node == null)
            messageDialogs.showError(parent, "Invalid variation",
                                     "You need to specify a valid variation.",
                                     false);
        return node;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GotoVariationDialog()
    {
    }
}

