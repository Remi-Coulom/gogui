//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package go;

import java.io.*;

//----------------------------------------------------------------------------

/** Static utility functions related to class Board. */
public class BoardUtils
{
    public static void print(Board board, PrintStream out)
    {
        StringBuffer s = new StringBuffer(1024);
        int size = board.getSize();
        printXCoords(size, s);
        for (int y = size - 1; y >= 0; --y)
        {
            printYCoord(y, s);
            s.append(" ");
            for (int x = 0; x < size; ++x)
            {
                Point point = board.getPoint(x, y);
                Color color = board.getColor(point);
                if (color == Color.BLACK)
                    s.append("@ ");
                else if (color == Color.WHITE)
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
            printGameInfo(board, s, y);
            s.append("\n");
        }
        printXCoords(size, s);
        out.print(s);
    }

    private static void printGameInfo(Board board, StringBuffer s, int yIndex)
    {
        int size = board.getSize();
        if (yIndex == size - 1)
        {
            s.append("  ");
            s.append(board.getToMove() == Color.BLACK ? "Black" : "White");
            s.append(" to move.");
        }
        else if (yIndex == size - 2)
        {
            s.append("  Prisoners: B ");
            s.append(board.getCapturedB());
            s.append("  W ");
            s.append(board.getCapturedW());
        }
        else
        {
            int n = board.getMoveNumber() - yIndex - 1;
            if (n >= 0)
            {
                Move move = board.getMove(n);
                s.append("  ");
                s.append(n + 1);
                s.append(" ");
                s.append(move.getColor() == Color.BLACK ? "B " : "W ");
                s.append(Point.toString(move.getPoint()));
            }
        }
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
            s.append(" ");
        }
        s.append("\n");
    }

    private static void printYCoord(int y, StringBuffer s)
    {
        String string = Integer.toString(y + 1);
        s.append(string);
        if (string.length() == 1)
            s.append(" ");
    }
}

//----------------------------------------------------------------------------
