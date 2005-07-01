//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

public class GuiBoardUtils
{
    public static void showMarkup(GuiBoard guiBoard, Board board, Node node)
    {
        Vector markSquare = node.getMarkSquare();
        if (markSquare != null)
            for (int i = 0; i < markSquare.size(); ++i)
                guiBoard.setMarkup((GoPoint)(markSquare.get(i)), true);
        Map labels = node.getLabels();
        if (labels != null)
        {
            Iterator i = labels.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry entry = (Map.Entry)i.next();
                GoPoint point = (GoPoint)entry.getKey();
                String value = (String)entry.getValue();
                guiBoard.setLabel(point, value);
            }
        }
    }
}

//----------------------------------------------------------------------------
