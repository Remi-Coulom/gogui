// Compare.java

package net.sf.gogui.tools.twogtp;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtil;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK_WHITE_EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.sgf.SgfReader;

/** Find duplicates in games. */
public final class Compare
{
    public static final class Placement
    {
        public boolean m_isSetup;

        public GoColor m_color;

        public GoPoint m_point;

        public Placement(boolean isSetup, GoColor color, GoPoint point)
        {
            m_isSetup = isSetup;
            m_color = color;
            m_point = point;
        }

        public Placement(Move move)
        {
            this(false, move.getColor(), move.getPoint());
        }
    }

    /** Check if game already exists in game collection.
        All games must have the same board size.
        Also finds rotated duplicates.
        @param board Board with the correct size (only used for
        Board.rotate).
        @param moves Moves of game to check.
        @param games Games in collection. The key is the game number, the value
        is the sequence of moves.
        @param useAlternate If true, assume that players are exchanged every
        second game. Only check games where player played the same color.
        @param isAlternated If useAlternate, indicate if game to check
        had players exchanged.
        @return String containing number of first identical game in
        collection or "-" if no duplicate was found. If a nearly identical
        game is found (&lt;= 20% identical moves comparing moves by number),
        the game number is returned with a question mark appended. */
    public static String checkDuplicate(ConstBoard board,
                                      ArrayList<Placement> moves,
                                      Map<Integer, ArrayList<Placement>> games,
                                      boolean useAlternate,
                                      boolean isAlternated)
    {
        String result = "-";
        int size = board.getSize();
        for (Map.Entry<Integer, ArrayList<Placement>> entry : games.entrySet())
        {
            int numberGame = entry.getKey();
            if (useAlternate && ((numberGame % 2 != 0) != isAlternated))
                continue;
            ArrayList<Placement> gameMoves = entry.getValue();
            for (int rot = 0; rot < BoardUtil.NUMBER_ROTATIONS; ++rot)
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
                    Placement move = moves.get(i);
                    Placement gameMove = gameMoves.get(i);
                    GoPoint gameRotatedPoint =
                        BoardUtil.rotate(rot, gameMove.m_point, size);
                    if (move.m_isSetup != gameMove.m_isSetup
                        || ! move.m_color.equals(gameMove.m_color)
                        || ! GoPoint.equals(move.m_point, gameRotatedPoint))
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

    /** Compare a set of SGF files.
        Prints the results to standard output, one line per game
        with the filename and the duplicate information as returned by
        Compare.checkDuplicate.
        @param filenames List of filenames
        @throws Exception If reading one of the files fails. */
    public static void compare(ArrayList<String> filenames) throws Exception
    {
        Board board = null;
        Map<Integer, ArrayList<Placement>> games =
            new TreeMap<Integer, ArrayList<Placement>>();
        for (int gameNumber = 0; gameNumber < filenames.size(); ++gameNumber)
        {
            String filename = filenames.get(gameNumber);
            File file = new File(filename);
            FileInputStream fileStream = new FileInputStream(file);
            SgfReader reader = new SgfReader(fileStream, file, null, 0);
            GameTree tree = reader.getTree();
            int size = tree.getBoardSize();
            if (board == null)
                board = new Board(size);
            else if (size != board.getSize())
                throw new Exception("Board size in " + filename +
                                    " does not match other games");
            ArrayList<Placement> moves = getPlacements(tree.getRoot());
            String duplicate =
                checkDuplicate(board, moves, games, false, false);
            System.out.println(Integer.toString(gameNumber) + " " +
                               filename + " " + duplicate);
            games.put(gameNumber, moves);
        }
    }

    public static ArrayList<Placement> getPlacements(ConstNode node)
    {
        ArrayList<Placement> result = new ArrayList<Placement>(512);
        while (node != null)
        {
            for (GoColor c : BLACK_WHITE_EMPTY)
            {
                PointList list = new PointList(node.getSetup(c));
                Collections.sort(list);
                for (GoPoint p : list)
                    result.add(new Placement(true, c, p));
            }
            Move move = node.getMove();
            if (move != null)
                result.add(new Placement(move));
            node = node.getChildConst();
        }
        return result;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Compare()
    {
    }
}
