//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package go;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class BoardUtils
{
    public static void print(Board board, PrintStream out)
    {
        int size = board.getSize();
        printXCoords(size, out);
        for (int y = size - 1; y >= 0; --y)
        {
            printYCoord(y, out);
            out.print(" ");
            for (int x = 0; x < size; ++x)
            {
                Point point = board.getPoint(x, y);
                Color color = board.getColor(point);
                if (color == Color.BLACK)
                    out.print("@ ");
                else if (color == Color.WHITE)
                    out.print("O ");
                else
                {
                    if (board.isHandicap(point))
                        out.print("+ ");
                    else
                        out.print(". ");
                }
            }
            printYCoord(y, out);
            printGameInfo(board, out, y);
            out.println();
        }
        printXCoords(size, out);
    }

    private static void printGameInfo(Board board, PrintStream out, int yIndex)
    {
        int size = board.getSize();
        if (yIndex == size - 1)
        {
            out.print("  ");
            out.print(board.getToMove() == Color.BLACK ? "Black" : "White");
            out.print(" to move.");
        }
        else if (yIndex == size - 2)
        {
            out.print("  Prisoners: B ");
            out.print(board.getCapturedB());
            out.print("  W ");
            out.print(board.getCapturedW());
        }
        else
        {
            int n = board.getMoveNumber() - yIndex - 1;
            if (n >= 0)
            {
                Move move = board.getMove(n);
                out.print("  ");
                out.print(n + 1);
                out.print(" ");
                out.print(move.getColor() == Color.BLACK ? "B " : "W ");
                out.print(Point.toString(move.getPoint()));
            }
        }
    }

    private static void printXCoords(int size, PrintStream out)
    {
        out.print("   ");
        int x;
        char c;
        for (x = 0, c = 'A'; x < size; ++x, ++c)
        {
            if (c == 'I')
                ++c;
            out.print(c);
            out.print(" ");
        }
        out.println();
    }

    private static void printYCoord(int y, PrintStream out)
    {
        String string = Integer.toString(y + 1);
        out.print(string);
        if (string.length() == 1)
            out.print(" ");
    }
}

//-----------------------------------------------------------------------------
