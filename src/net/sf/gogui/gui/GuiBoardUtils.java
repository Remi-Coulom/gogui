//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

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

    public static boolean[][] getMarkSquare(GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        boolean[][] result = new boolean[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                result[x][y] = guiBoard.getMarkSquare(point);
            }
        return result;
    }

    public static String[][] getLabels(GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        String[][] result = new String[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                result[x][y] = guiBoard.getLabel(point);
            }
        return result;
    }

    public static boolean[][] getSelects(GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        boolean[][] result = new boolean[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                result[x][y] = guiBoard.getSelect(point);
            }
        return result;
    }

    public static void setSelect(GuiBoard guiBoard, ArrayList pointList,
                                 boolean select)
    {
        if (pointList == null)
            return;
        for (int i = 0; i < pointList.size(); ++i)
            guiBoard.setSelect((GoPoint)pointList.get(i), select);
    }

    public static void scoreBegin(GuiBoard guiBoard, Board board,
                                  GoPoint[] isDeadStone)
    {
        board.scoreBegin(isDeadStone);
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                guiBoard.setCrossHair(isDeadStone[i], true);
        calcScore(guiBoard, board);
    }

    public static void scoreSetDead(GuiBoard guiBoard, Board board, GoPoint p)
    {
        GoColor c = board.getColor(p);
        if (c == GoColor.EMPTY)
            return;
        ArrayList stones = new ArrayList(board.getNumberPoints());
        board.getStones(p, c, stones);
        boolean dead = ! board.scoreGetDead((GoPoint)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint stone = (GoPoint)stones.get(i);
            board.scoreSetDead(stone, dead);
            guiBoard.setCrossHair(stone, dead);
        }
        calcScore(guiBoard, board);
    }

    public static void showBWBoard(GuiBoard guiBoard, String[][] board)
    {
        for (int x = 0; x < board.length; ++x)
            for (int y = 0; y < board[x].length; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                String s = board[x][y].toLowerCase();
                if (s.equals("b") || s.equals("black"))
                    guiBoard.setTerritory(point, GoColor.BLACK);
                else if (s.equals("w") || s.equals("white"))
                    guiBoard.setTerritory(point, GoColor.WHITE);
                else
                    guiBoard.setTerritory(point, GoColor.EMPTY);
            }
    }

    public static void showChildrenMoves(GuiBoard guiBoard,
                                         ArrayList childrenMoves)
    {
        guiBoard.clearAllLabels();
        int numberMarked = 0;
        char label = 'A';
        for (int i = 0; i < childrenMoves.size(); ++i)
        {
            GoPoint point = (GoPoint)childrenMoves.get(i);
            String s = guiBoard.getLabel(point);
            if (! s.equals(""))
            {
                if (! s.endsWith("."))
                    guiBoard.setLabel(point, s + ".");
                continue;
            }
            if (numberMarked >= 26)
                guiBoard.setLabel(point, "+");
            else
                guiBoard.setLabel(point, Character.toString(label));
            if (numberMarked < 26)
                ++label;
            ++numberMarked;            
        }
    }

    public static void showColorBoard(GuiBoard guiBoard, String[][] colors)
    {
        for (int x = 0; x < colors.length; ++x)
            for (int y = 0; y < colors[x].length; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                guiBoard.setFieldBackground(point, getColor(colors[x][y]));
            }
    }

    public static void showDoubleBoard(GuiBoard guiBoard, double[][] board)
    {
        for (int x = 0; x < board.length; ++x)
            for (int y = 0; y < board[x].length; ++y)
                guiBoard.setInfluence(GoPoint.get(x, y), board[x][y]);
    }

    public static void showStringBoard(GuiBoard guiBoard,
                                       String[][] board)
    {
        for (int x = 0; x < board.length; ++x)
            for (int y = 0; y < board[x].length; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                guiBoard.setLabel(point, board[x][y]);
            }
    }

    public static void showMarkup(GuiBoard guiBoard, Node node)
    {
        ArrayList mark;
        mark = node.getMarked(MarkType.MARK);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMark((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(MarkType.CIRCLE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMarkCircle((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(MarkType.SQUARE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMarkSquare((GoPoint)(mark.get(i)), true);
        mark = node.getMarked(MarkType.TRIANGLE);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setMarkTriangle((GoPoint)(mark.get(i)), true);
        GuiBoardUtils.setSelect(guiBoard, node.getMarked(MarkType.SELECT),
                                true);
        mark = node.getMarked(MarkType.TERRITORY_BLACK);
        if (mark != null)
            for (int i = 0; i < mark.size(); ++i)
                guiBoard.setTerritory((GoPoint)(mark.get(i)), GoColor.BLACK);
        mark = node.getMarked(MarkType.TERRITORY_WHITE);
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

    public static void showPointList(GuiBoard guiBoard, GoPoint pointList[])
    {
        guiBoard.clearAllMarkup();
        for (int i = 0; i < pointList.length; ++i)
        {
            GoPoint p = pointList[i];
            if (p != null)
                guiBoard.setMarkSquare(p, true);
        }
    }

    public static void showPointStringList(GuiBoard guiBoard,
                                           ArrayList pointList,
                                           ArrayList stringList)
    {
        guiBoard.clearAllLabels();
        for (int i = 0; i < pointList.size(); ++i)
        {
            GoPoint point = (GoPoint)pointList.get(i);
            String string = (String)stringList.get(i);
            if (point != null)
                guiBoard.setLabel(point, string);
        }
    }

    public static void showVariation(GuiBoard guiBoard, Move[] variation)
    {
        guiBoard.clearAllLabels();
        for (int i = 0; i < variation.length; ++i)
        {
            Move move = variation[i];
            if (move.getPoint() != null)
            {
                guiBoard.setColor(move.getPoint(), move.getColor());
                guiBoard.setLabel(move.getPoint(), Integer.toString(i + 1));
            }
        }
    }

    public static void updateFromGoBoard(GuiBoard guiBoard, Board board,
                                         boolean markLastMove)
    {
        for (int i = 0; i < board.getNumberPoints(); ++i)
        {
            GoPoint point = board.getPoint(i);
            guiBoard.setColor(point, board.getColor(point));
        }
        GoPoint point = null;
        int moveNumber = board.getMoveNumber();
        if (moveNumber > 0)
            point = board.getMove(moveNumber - 1).getPoint();
        if (markLastMove)
            guiBoard.markLastMove(point);
        else
            guiBoard.markLastMove(null);
        if (point != null)
            guiBoard.setCursor(point);
        else
        {
            int size = guiBoard.getBoardSize();
            guiBoard.setCursor(GoPoint.get(size / 2, size / 2));
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GuiBoardUtils()
    {
    }

    private static void calcScore(GuiBoard guiBoard, Board board)
    {
        board.calcScore();
        for (int i = 0; i < board.getNumberPoints(); ++i)
        {
            GoPoint p = board.getPoint(i);
            GoColor c = board.getScore(p);
            guiBoard.setTerritory(p, c);
        }
    }
}

//----------------------------------------------------------------------------
