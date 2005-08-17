//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

/** Utility functions for class GuiBoard. */
public final class GuiBoardUtils
{
    public static Color getColor(String string)
    {
        if (string.equals("blue"))
            return Color.blue;
        if (string.equals("cyan"))
            return Color.cyan;
        if (string.equals("green"))
            return Color.green;
        if (string.equals("gray"))
            return Color.lightGray;
        if (string.equals("magenta"))
            return Color.magenta;
        if (string.equals("pink"))
            return Color.pink;
        if (string.equals("red"))
            return Color.red;
        if (string.equals("yellow"))
            return Color.yellow;
        if (string.equals("black"))
            return Color.black;
        if (string.equals("white"))
            return Color.white;
        try
        {
            return Color.decode(string);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    public static void showColorBoard(GuiBoard guiBoard, String[][] colors,
                                      Board board)
    {
        for (int i = 0; i < board.getNumberPoints(); ++i)
        {
            GoPoint point = board.getPoint(i);
            int x = point.getX();
            int y = point.getY();
            guiBoard.setFieldBackground(point, getColor(colors[x][y]));
        }
    }

    public static void showMarkup(GuiBoard guiBoard, Node node)
    {
        ArrayList mark;
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

    /** Make constructor unavailable; class is for namespace only. */
    private GuiBoardUtils()
    {
    }
}

//----------------------------------------------------------------------------
