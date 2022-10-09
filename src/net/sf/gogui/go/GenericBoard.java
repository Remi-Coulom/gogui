//GenericBoard.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

import java.util.ArrayList;

import net.sf.gogui.game.ConstNode;

import static net.sf.gogui.go.GoColor.EMPTY;

import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpError;

/**
 * final class containing the methods used if a gtp gameRuler is attached
 * used by Board.java
 * @author fretel
 *
 */
public final class GenericBoard {

    public static GoColor getSideToMove(GtpClientBase gameRuler, Move move) throws GtpError {
        if (! gameRuler.isSupported("gogui-rules_side_to_move"))
            return move.getColor().otherColor();
        String color = gameRuler.send("gogui-rules_side_to_move");
        char c = color.charAt(0);
        GoColor sideToMove;
        if (c == 'b' || c == 'B')
            sideToMove = GoColor.BLACK;
        else
            sideToMove = GoColor.WHITE;
        return sideToMove;
    }

    public static boolean isGameOver(GtpClientBase gameRuler) throws GtpError {
        if (! gameRuler.isSupported("gogui-rules_legal_moves"))
            return false;
        return getLegalMoves(gameRuler).equals("");
    }

    /**
     * "pass" at the end if pass move is possible
     */
    public static boolean isLegalMove(GtpClientBase gameRuler, Move move) throws GtpError
    {
        if (! gameRuler.isSupported("gogui-rules_legal_moves"))
            return false;
        String legalMoves = getLegalMoves(gameRuler);
        return (move.getColor().equals(GenericBoard.getSideToMove(gameRuler, move)) && (legalMoves.contains(move.getPoint().toString())
                || legalMoves.contains("pass") && move.getPoint() == null));
    }

    public static String getLegalMoves(GtpClientBase gameRuler) throws GtpError
    {
        return gameRuler.send("gogui-rules_legal_moves");
    }

    /**
     * Supported pass char sequences in the gtp-rules_legal_moves command
     * are "pass" and "PASS"
     */
    public static boolean isPassLegal(GtpClientBase gameRuler) throws GtpError
    {
        if (gameRuler == null)
            return true;
        if (! gameRuler.isSupported("gogui-rules_legal_moves"))
            return false;
        String legalMoves = getLegalMoves(gameRuler);
        return legalMoves.contains("pass") || legalMoves.contains("PASS");
    }

    /**
     * Send a move to play in the game ruler and synchronizes the board
     */
    public static void sendPlay(GtpClientBase gameRuler, Board board, Move move)
    {
        try {
            gameRuler.sendPlay(move);
            GenericBoard.copyRulerBoardState(gameRuler, board);
            GenericBoard.setToMove(gameRuler, board, move);
        } catch (GtpError e) {
        }
    }

    /**
     * Forces the side to move from the game ruler to the board for a better synchronization.
     */
    public static void setToMove(GtpClientBase gameRuler, Board board, Move move)
    {
        try {
            Move rightColor = Move.get(GenericBoard.getSideToMove(gameRuler, move), move.getPoint());
            GoColor toMove = GenericBoard.getSideToMove(gameRuler, rightColor);
            board.setToMove(toMove);
        } catch (GtpError e) {
            board.setToMove(board.getToMove().otherColor());
        }
    }
    
    public static int getBoardSize(GtpClientBase gameRuler) throws GtpError
    {
        if (!gameRuler.isSupported("gogui-rules_board_size"))
            return -1;
        return Integer.parseInt(gameRuler.send("gogui-rules_board_size"));
    }
    
    public static String getGameId(GtpClientBase gameRuler) throws GtpError
    {
        if (!gameRuler.isSupported("gogui-rules_game_id"))
            return "";
        return gameRuler.send("gogui-rules_game_id");
    }

    /**
     * Forces the position from the game ruler to the board for a better synchronization.
     */
    public static void copyRulerBoardState(GtpClientBase gameRuler, Board board) {
        if (!gameRuler.isSupported("gogui-rules_board"))
            return;
        String rulerBoardState = "";
        try {
            rulerBoardState = gameRuler.send("gogui-rules_board");
        } catch (GtpError e) {
            return;
        }
        if (rulerBoardState.equals("")) return;
        int size = 0;
        try {
            size = GenericBoard.getBoardSize(gameRuler);
        } catch (GtpError e) {
            return;
        }
        GenericBoard.setup(rulerBoardState, board, size);

        if (gameRuler.isSupported("gogui-rules_captured_count"))
        {
           try
           {
               String captured = gameRuler.send("gogui-rules_captured_count");
               String[] numbers = captured.split(" ");
               if (numbers.length > 0)
                   board.setCaptured(GoColor.WHITE, Integer.parseInt(numbers[0]));
               if (numbers.length > 1)
                   board.setCaptured(GoColor.BLACK, Integer.parseInt(numbers[1]));
           }
           catch (GtpError e)
           {
           }
        }
    }

    private static void setup(String position, Board board, int size)
    {
        try
        {
            int nbChar = 0;
            PointList blacksSetup = new PointList();
            PointList whitesSetup = new PointList();
            PointList emptySetup = new PointList();
            for (int i = 0; i < size; i++) {
                int j = -1;
                char c = ' ';
                do {
                    do
                    {
                        c = position.charAt(nbChar);
                        nbChar++;
                    }
                    while (c != 'X'&& c != 'O' && c != '.' && c != '\n');

                    if (c != '\n')
                    {
                        j++;
                    }

                    if ( c == 'X')
                    {
                        GoPoint black = GoPoint.get(j, size-i-1);
                        if (board.getColor(black) != BLACK)
                        {
                            blacksSetup.add(black);
                        }
                    }
                    else if (c == 'O')
                    {
                        GoPoint white = GoPoint.get(j, size-i-1);
                        if (board.getColor(white) != WHITE)
                        {
                            whitesSetup.add(white);
                        }
                    }
                    else if (c == '.')
                    {
                        GoPoint empty = GoPoint.get(j, size-i-1);
                        if (board.getColor(empty) != EMPTY) {
                            emptySetup.add(empty);
                        }
                    }
                } while (j < size-1);
            }
            board.setPoints(blacksSetup, BLACK);
            board.setPoints(whitesSetup, WHITE);
            board.setPoints(emptySetup, EMPTY);
        }
        catch (Exception e)
        {
        }
    }

    public static boolean isSetupPossible(GtpClientBase gameRuler)
    {
        return gameRuler != null && gameRuler.isSupported("gogui-rules_setup");
    }

    /**
     * Clears the ruler board and plays moves from the beginning.
     * Then copy the ruler board changes to the board.
     */
    public static void copyBoardState(GtpClientBase gameRuler, ConstNode node, Board board)
    {
        ArrayList<Move> moves = new ArrayList<Move>();
        while (node.hasFather())
        {
            moves.add(node.getMove());
            node = node.getFatherConst();
        }
        try {
            GenericBoard.playFromBeginning(gameRuler, moves, board);
        } catch (GtpError e) {
        }
    }

    public static void playFromBeginning(GtpClientBase gameRuler, ArrayList<Move> moves, Board board) throws GtpError {
        gameRuler.sendClearBoard(board.getSize());
        for (int i = moves.size() - 1; i >= 0; i--)
        {
            gameRuler.sendPlay(moves.get(i));
        }
        GenericBoard.copyRulerBoardState(gameRuler, board);
    }

    //Makes the constructor unavailable.
    private GenericBoard()
    {
    }
    
}
