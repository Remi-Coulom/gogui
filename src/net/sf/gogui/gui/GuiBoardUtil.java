// GuiBoardUtil.java

package net.sf.gogui.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.CountScore;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Marker;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;

/** Utility functions for class GuiBoard. */
public final class GuiBoardUtil
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

    public static boolean[][] getMark(GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        boolean[][] result = new boolean[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                result[x][y] = guiBoard.getMark(point);
            }
        return result;
    }

    public static boolean[][] getMarkCircle(GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        boolean[][] result = new boolean[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                result[x][y] = guiBoard.getMarkCircle(point);
            }
        return result;
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

    public static boolean[][] getMarkTriangle(GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        boolean[][] result = new boolean[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                result[x][y] = guiBoard.getMarkTriangle(point);
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

    public static void setSelect(GuiBoard guiBoard, ConstPointList pointList,
                                 boolean select)
    {
        if (pointList == null)
            return;
        for (GoPoint p : pointList)
            guiBoard.setSelect(p, select);
    }

    public static void scoreBegin(GuiBoard guiBoard, CountScore countScore,
                                  ConstBoard board, ConstPointList deadStones)
    {
        countScore.begin(board, deadStones);
        if (deadStones != null)
            for (GoPoint p : deadStones)
                guiBoard.setCrossHair(p, true);
        computeScore(guiBoard, countScore, board);
    }

    public static void scoreSetDead(GuiBoard guiBoard, CountScore countScore,
                                    ConstBoard board, GoPoint p)
    {
        GoColor c = board.getColor(p);
        if (c == EMPTY)
            return;
        PointList stones = countScore.changeStatus(p);
        for (GoPoint stone : stones)
            guiBoard.setCrossHair(stone, countScore.isDead(stone));
        computeScore(guiBoard, countScore, board);
    }

    public static void showBWBoard(GuiBoard guiBoard, String[][] board)
    {
        for (int x = 0; x < board.length; ++x)
            for (int y = 0; y < board[x].length; ++y)
            {
                GoPoint point = GoPoint.get(x, y);
                String s = board[x][y].toLowerCase(Locale.ENGLISH);
                if (s.equals("b") || s.equals("black"))
                    guiBoard.setTerritory(point, BLACK);
                else if (s.equals("w") || s.equals("white"))
                    guiBoard.setTerritory(point, WHITE);
                else
                    guiBoard.setTerritory(point, EMPTY);
            }
    }

    /** @deprecated This function was renamed to showMoves(). The forwarding
        functions may be removed in the future */
    public static void showChildrenMoves(GuiBoard guiBoard,
                                         ConstPointList childrenMoves)
    {
        showMoves(guiBoard, childrenMoves);
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

    /** Shows markup on board.
        Existing markup is not cleared (but may be overwritten. */
    public static void showMarkup(GuiBoard guiBoard, ConstNode node)
    {
        ConstPointList mark = node.getMarkedConst(MarkType.MARK);
        if (mark != null)
            for (GoPoint p : mark)
                guiBoard.setMark(p, true);
        mark = node.getMarkedConst(MarkType.CIRCLE);
        if (mark != null)
            for (GoPoint p : mark)
                guiBoard.setMarkCircle(p, true);
        mark = node.getMarkedConst(MarkType.SQUARE);
        if (mark != null)
            for (GoPoint p : mark)
                guiBoard.setMarkSquare(p, true);
        mark = node.getMarkedConst(MarkType.TRIANGLE);
        if (mark != null)
            for (GoPoint p : mark)
                guiBoard.setMarkTriangle(p, true);
        GuiBoardUtil.setSelect(guiBoard, node.getMarkedConst(MarkType.SELECT),
                                true);
        mark = node.getMarkedConst(MarkType.TERRITORY_BLACK);
        if (mark != null)
            for (GoPoint p : mark)
                guiBoard.setTerritory(p, BLACK);
        mark = node.getMarkedConst(MarkType.TERRITORY_WHITE);
        if (mark != null)
            for (GoPoint p : mark)
                guiBoard.setTerritory(p, WHITE);
        Map<GoPoint,String> labels = node.getLabelsUnmodifiable();
        if (labels != null)
        {
            for (Map.Entry<GoPoint,String> entry : labels.entrySet())
            {
                GoPoint point = entry.getKey();
                String value = entry.getValue();
                guiBoard.setLabel(point, value);
            }
        }
    }

    /** Show a list of moves with labels 'A', 'B', 'C' ...
        If the list contains more than 26 unique moves, the label '*' is used.
        If a move appears more than once in the list, the character '&gt;'
        will be appended to the label. */
    public static void showMoves(GuiBoard guiBoard,
                                 ConstPointList childrenMoves)
    {
        Marker marker = new Marker(guiBoard.getBoardSize());
        int numberMarked = 0;
        char label = 'A';
        for (GoPoint p : childrenMoves)
        {
            if (marker.get(p))
            {
                String s = guiBoard.getLabel(p);
                if (! s.endsWith(">"))
                    guiBoard.setLabel(p, s + ">");
                continue;
            }
            marker.set(p);
            if (numberMarked >= 26)
                guiBoard.setLabel(p, "*");
            else
                guiBoard.setLabel(p, Character.toString(label));
            if (numberMarked < 26)
                ++label;
            ++numberMarked;
        }
    }

    public static void showPointList(GuiBoard guiBoard, ConstPointList points)
    {
        guiBoard.clearAllMarkup();
        for (GoPoint p : points)
            if (p != null && p.isOnBoard(guiBoard.getBoardSize()))
                guiBoard.setMarkSquare(p, true);
    }

    public static void showPointStringList(GuiBoard guiBoard,
                                           ConstPointList pointList,
                                           ArrayList<String> stringList)
    {
        guiBoard.clearAllLabels();
        for (int i = 0; i < pointList.size(); ++i)
        {
            GoPoint point = pointList.get(i);
            String string = stringList.get(i);
            if (point != null)
                guiBoard.setLabel(point, string);
        }
    }

    /** Shows moves in variation as stones with move number labels on board.
        If there are several moves on the same point then the only first move
        is shown for short variations (less/equal ten moves); and only the
        last move for long variations. */
    public static void showVariation(GuiBoard guiBoard, Move[] variation)
    {
        guiBoard.clearAllLabels();
        if (variation.length > 10)
            for (int i = 0; i < variation.length; ++i)
            {
                Move move = variation[i];
                if (move.getPoint() != null)
                {
                    String label = Integer.toString(i + 1);
                    guiBoard.setGhostStone(move.getPoint(), move.getColor());
                    guiBoard.setLabel(move.getPoint(), label);
                }
            }
        else
            for (int i = variation.length - 1; i >= 0; --i)
            {
                Move move = variation[i];
                if (move.getPoint() != null)
                {
                    String label = Integer.toString(i + 1);
                    guiBoard.setGhostStone(move.getPoint(), move.getColor());
                    guiBoard.setLabel(move.getPoint(), label);
                }
            }
    }

    public static void updateFromGoBoard(GuiBoard guiBoard, ConstBoard board,
                                         boolean markLastMove,
                                         boolean showMoveNumbers)
    {
        for (GoPoint p : board)
            guiBoard.setColor(p, board.getColor(p));
        GoPoint lastMove = null;
        if (board.getLastMove() != null)
            lastMove = board.getLastMove().getPoint();
        if (markLastMove)
            guiBoard.markLastMove(lastMove);
        else
            guiBoard.markLastMove(null);
        if (showMoveNumbers)
            for (int i = 0; i < board.getNumberMoves(); ++i)
            {
                GoPoint point = board.getMove(i).getPoint();
                if (point != null)
                    guiBoard.setLabel(point, Integer.toString(i + 1));
            }
        if (lastMove == null)
        {
            int size = guiBoard.getBoardSize();
            guiBoard.setCursor(GoPoint.get(size / 2, size / 2));
        }
        else
            guiBoard.setCursor(lastMove);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GuiBoardUtil()
    {
    }

    private static void computeScore(GuiBoard guiBoard,
                                     CountScore countScore,
                                     ConstBoard board)
    {
        countScore.compute();
        for (GoPoint p : board)
        {
            GoColor c = countScore.getColor(p);
            guiBoard.setTerritory(p, c);
        }
    }
}
