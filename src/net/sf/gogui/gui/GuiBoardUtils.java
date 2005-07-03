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
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

public class GuiBoardUtils
{
    public static void showMarkup(GuiBoard guiBoard, Node node)
    {
        Vector mark;
        mark = node.getMarked(Node.MARKED);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMark((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(Node.MARKED_CIRCLE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMarkCircle((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(Node.MARKED_SQUARE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMarkSquare((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(Node.MARKED_TRIANGLE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMarkTriangle((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(Node.MARKED_SELECT);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setSelect((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(Node.MARKED_TERRITORY_BLACK);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setTerritory((GoPoint)(mark.get(i)), GoColor.BLACK);
        mark = node.getMarked(Node.MARKED_TERRITORY_WHITE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setTerritory((GoPoint)(mark.get(i)), GoColor.WHITE);
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
