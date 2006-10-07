//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import java.io.PrintStream;

/** Static utility functions related to class Board. */
public final class BoardUtil
{
    /** Number of rotation modes for #rotate(). */
    public static final int NUMBER_ROTATIONS = 8;

    /** Print board position in text format.
        @param board The board to print.
        @param out The stream to print to.
        @param withGameInfo Print game information (prisoners, recent moves)
    */
    public static void print(ConstBoard board, PrintStream out,
                             boolean withGameInfo)
    {
        StringBuffer s = new StringBuffer(1024);
        int size = board.getSize();
        printXCoords(size, s);
        for (int y = size - 1; y >= 0; --y)
        {
            printYCoord(y, s);
            s.append(' ');
            for (int x = 0; x < size; ++x)
            {
                GoPoint point = GoPoint.get(x, y);
                GoColor color = board.getColor(point);
                if (color == GoColor.BLACK)
                    s.append("@ ");
                else if (color == GoColor.WHITE)
                    s.append("O ");
                else
                {
                    if (board.isHandicap(point))
                        s.append("+ ");
                    else
                        s.append(". ");
                }
            }
            printYCoord(y, s);
            if (withGameInfo)
                printGameInfo(board, s, y);
            s.append('\n');
        }
        printXCoords(size, s);
        if (! withGameInfo)
        {
            printToMove(board, s);
            s.append('\n');
        }
        out.print(s);
    }

    /** Rotate/mirror point.
        Rotates and/or mirrors a point on a given board according to a given
        rotation mode.
        <table border="1">
        <tr><th>Mode</th><th>x</th><th>y</th></tr>
        <tr><td>0</td><td>x</td><td>y</td></tr>
        <tr><td>1</td><td>size - x - 1</td><td>y</td></tr>
        <tr><td>2</td><td>x</td><td>size - y - 1</td></tr>
        <tr><td>3</td><td>y</td><td>x</td></tr>
        <tr><td>4</td><td>size - y - 1</td><td>x</td></tr>
        <tr><td>5</td><td>y</td><td>size - x - 1</td></tr>
        <tr><td>6</td><td>size - x - 1</td><td>size - y - 1</td></tr>
        <tr><td>7</td><td>size - y - 1</td><td>size - x - 1</td></tr>
        </table>
        @param rotationMode The rotation mode in [0..NUMBER_ROTATIONS]
        @param point The point to be rotated
        @param size The board size
        @return The rotated mirrored point
    */
    public static GoPoint rotate(int rotationMode, GoPoint point, int size)
    {
        assert(rotationMode < NUMBER_ROTATIONS);
        if (point == null)
            return null;
        int x = point.getX();
        int y = point.getY();
        switch (rotationMode)
        {
        case 0:
            return GoPoint.get(x, y);
        case 1:
            return GoPoint.get(size - x - 1, y);
        case 2:
            return GoPoint.get(x, size - y - 1);
        case 3:
            return GoPoint.get(y, x);
        case 4:
            return GoPoint.get(size - y - 1, x);
        case 5:
            return GoPoint.get(y, size - x - 1);
        case 6:
            return GoPoint.get(size - x - 1, size - y - 1);
        case 7:
            return GoPoint.get(size - y - 1, size - x - 1);
        default:
            return GoPoint.get(x, y);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private BoardUtil()
    {
    }

    private static void printGameInfo(ConstBoard board, StringBuffer s,
                                      int yIndex)
    {
        int size = board.getSize();
        if (yIndex == size - 1)
        {
            s.append("  ");
            printToMove(board, s);
        }
        else if (yIndex == size - 2)
        {
            s.append("  Prisoners: B ");
            s.append(board.getCapturedB());
            s.append("  W ");
            s.append(board.getCapturedW());
        }
    }

    private static void printToMove(ConstBoard board, StringBuffer buffer)
    {
        buffer.append(board.getToMove() == GoColor.BLACK ? "Black" : "White");
        buffer.append(" to move");
    }

    private static void printXCoords(int size, StringBuffer s)
    {
        s.append("   ");
        int x;
        char c;
        for (x = 0, c = 'A'; x < size; ++x, ++c)
        {
            if (c == 'I')
                ++c;
            s.append(c);
            s.append(' ');
        }
        s.append('\n');
    }

    private static void printYCoord(int y, StringBuffer s)
    {
        String string = Integer.toString(y + 1);
        s.append(string);
        if (string.length() == 1)
            s.append(' ');
    }
}

