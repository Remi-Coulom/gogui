//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package twogtp;

import java.io.*;
import java.util.*;
import game.*;
import go.*;

//----------------------------------------------------------------------------

public class Compare
{
    public static String checkDuplicate(Board board, Vector moves,
                                        Vector games, boolean useAlternate,
                                        boolean isAlternated)
    {
        String result = "-";
        for (int numberGame = 0; numberGame < games.size(); ++numberGame)
        {
            if (useAlternate && ((numberGame % 2 != 0) != isAlternated))
                continue;
            Vector gameMoves = (Vector)games.get(numberGame);
            for (int rot = 0; rot < Board.NUMBER_ROTATIONS; ++rot)
            {
                int numberDifferent = 0;
                int moveNumber = moves.size();
                int maxDifferent = moveNumber / 5;
                if (gameMoves.size() != moveNumber)
                {
                    numberDifferent = Math.abs(gameMoves.size() - moveNumber);
                    moveNumber = Math.min(gameMoves.size(), moveNumber);
                }
                for (int i = 0;
                     numberDifferent <= maxDifferent && i < moveNumber; ++i)
                {
                    Move move = (Move)moves.get(i);
                    Point point = move.getPoint();
                    Color color = move.getColor();
                    Move gameMove = (Move)gameMoves.get(i);
                    Point gamePoint = board.rotate(rot, gameMove.getPoint());
                    Color gameColor = gameMove.getColor();                    
                    if (! color.equals(gameColor)
                        || ! Point.equals(point, gamePoint))
                        ++numberDifferent;
                }
                if (numberDifferent == 0)
                    return Integer.toString(numberGame);
                else if (numberDifferent < maxDifferent)
                    result = Integer.toString(numberGame) + "?";
            }
        }
        return result;
    }

    public static void compare(Vector filenames) throws Exception
    {
        Board board = null;
        Vector games = new Vector();
        for (int gameNumber = 0; gameNumber < filenames.size(); ++gameNumber)
        {
            String filename = (String)filenames.get(gameNumber);
            File file = new File(filename);
            FileInputStream fileStream = new FileInputStream(file);
            sgf.Reader reader
                = new sgf.Reader(fileStream, file.toString(), null, 0);
            GameTree gameTree = reader.getGameTree();
            GameInformation gameInformation = gameTree.getGameInformation();
            int size = gameInformation.m_boardSize;
            if (board == null)
                board = new Board(size);
            else if (size != board.getSize())
                throw new Exception("Board size in " + filename +
                                    " does not match other games");
            Vector moves = getAllAsMoves(gameTree.getRoot());
            String duplicate =
                checkDuplicate(board, moves, games, false, false);
            System.out.println(Integer.toString(gameNumber) + " " +
                               filename + " " + duplicate);
            games.add(moves);
        }
    }

    /** Return moves in main variation from node.
        All setup stones are translated to moves and passes are filled in
        to ensure that moves are alternating beginning with black.
    */
    public static Vector getAllAsMoves(Node node)
    {
        Vector moves = new Vector(128, 128);
        while (node != null)
        {
            moves.addAll(node.getAllAsMoves());
            node = node.getChild();
        }
        moves = Move.fillPasses(moves, Color.BLACK);
        return moves;
    }
}

//----------------------------------------------------------------------------
