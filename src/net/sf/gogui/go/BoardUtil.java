// BoardUtil.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

/** Static utility functions related to class Board. */
public final class BoardUtil
{
    /** Number of rotation modes for <code>BoardUtil.rotate()</code>.
        @see #rotate */
    public static final int NUMBER_ROTATIONS = 8;

    /** Copy the state of one board to another.
        Initializes the target board with the size and the setup stones of the
        source board and executes all moves of the source board on the target
        board. */
    public static void copy(Board target, ConstBoard source)
    {
        target.init(source.getSize());
        ConstPointList setupBlack = source.getSetup(BLACK);
        ConstPointList setupWhite = source.getSetup(WHITE);
        GoColor setupPlayer = source.getSetupPlayer();
        if (setupBlack.size() > 0 || setupWhite.size() > 0)
        {
            if (source.isSetupHandicap())
                target.setupHandicap(setupBlack);
            else
                target.setup(setupBlack, setupWhite, setupPlayer);
        }
        for (int i = 0; i < source.getNumberMoves(); ++i)
            target.play(source.getMove(i));
    }

    /** Get board position as text diagram (without additional game
        information).
        Calls <code>toString()</code> with <code>withGameInfo == false</code>.
        @see #toString(ConstBoard, boolean) */
    public static String toString(ConstBoard board)
    {
        return toString(board, true, false);
    }

    /** Get board position as text diagram.
        @param board The board to print.
        @param withGameInfo Print additional game information on the right
        side of the board (at present only number of prisoners)
        @param color Colorize board using ANSI escape sequences
        @return Board position as text diagram. */
    public static String toString(ConstBoard board, boolean withGameInfo,
                                  boolean color)
    {
        StringBuilder s = new StringBuilder(1024);
        int size = board.getSize();
        String separator = System.getProperty("line.separator");
        assert separator != null;
        printXCoords(size, s, separator);
        String ansiStart = "\u001b[";
        for (int y = size - 1; y >= 0; --y)
        {
            printYCoord(y, s, true);
            s.append(' ');
            for (int x = 0; x < size; ++x)
            {
                if (x > 0)
                {
                    if (color)
                    {
                        s.append(ansiStart);
                        s.append("43m");
                    }
                    s.append(' ');
                }
                GoPoint point = GoPoint.get(x, y);
                GoColor c = board.getColor(point);
                if (c == BLACK)
                {
                    if (color)
                    {
                        s.append(ansiStart);
                        s.append("0;30;43m");
                    }
                    s.append('X');
                }
                else if (c == WHITE)
                {
                    if (color)
                    {
                        s.append(ansiStart);
                        s.append("1;37;43m");
                    }
                    s.append('O');
                }
                else
                {
                    if (color)
                    {
                        s.append(ansiStart);
                        s.append("1;30;43m");
                    }
                    if (board.isHandicap(point))
                        s.append('+');
                    else
                        s.append('.');
                }
            }
            if (color)
            {
                s.append(ansiStart);
                s.append("0m");
            }
            s.append(' ');
            printYCoord(y, s, false);
            if (withGameInfo)
                printGameInfo(board, s, y);
            s.append(separator);
        }
        printXCoords(size, s, separator);
        if (! withGameInfo)
        {
            printToMove(board, s);
            s.append(separator);
        }
        return s.toString();
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
        @return The rotated mirrored point */
    public static GoPoint rotate(int rotationMode, GoPoint point, int size)
    {
        assert rotationMode < NUMBER_ROTATIONS;
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

    private static void printGameInfo(ConstBoard board, StringBuilder s,
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
            s.append(board.getCaptured(BLACK));
            s.append("  W ");
            s.append(board.getCaptured(WHITE));
        }
        else if (yIndex <= size - 4)
        {
            int moveNumber = board.getNumberMoves() - yIndex - 1;
            if (moveNumber >= 0)
            {
                s.append("  ");
                s.append(moveNumber + 1);
                s.append(' ');
                s.append(board.getMove(moveNumber));
            }
        }
    }

    private static void printToMove(ConstBoard board, StringBuilder buffer)
    {
        buffer.append(board.getToMove().getCapitalizedName());
        buffer.append(" to play");
    }

    private static void printXCoords(int size, StringBuilder s,
                                     String separator)
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
        s.append(separator);
    }

    private static void printYCoord(int y, StringBuilder s, boolean alignRight)
    {
        String string = Integer.toString(y + 1);
        if (alignRight && string.length() == 1)
            s.append(' ');
        s.append(string);
        if (! alignRight && string.length() == 1)
            s.append(' ');
    }
}
