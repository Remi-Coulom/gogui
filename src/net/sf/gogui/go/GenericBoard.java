//GenericBoard.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

import net.sf.gogui.gogui.GoGui;

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
            Move rightColor = Move.get(GenericBoard.getSideToMove(gameRuler, move), move.getPoint());
            gameRuler.sendPlay(move);
            GenericBoard.copyRulerBoardState(gameRuler, board);
            GenericBoard.setToMove(gameRuler, board, rightColor);
        } catch (GtpError e) {
            System.out.println("not done");
        }
    }

    /**
     * Forces the side to move from the game ruler to the board for a better synchronization.
     */
    public static void setToMove(GtpClientBase gameRuler, Board board, Move move)
    {
        try {
            GoColor toMove = GenericBoard.getSideToMove(gameRuler, move);
            System.out.println("ici " + toMove);
            board.setToMove(toMove);
        } catch (GtpError e) {
        }
    }
    /**
     * Forces the position from the game ruler to the board for a better synchronization.
     */
    public static void copyRulerBoardState(GtpClientBase gameRuler, Board board) {
        if (!gameRuler.isSupported("gogui-rules_board"))
            return;
        String rulerBoardState = "";
        try {
            rulerBoardState = gameRuler.send("gogui-rules_board");;
        } catch (GtpError e) {
        }
        if (rulerBoardState.equals("")) {
            board.clear();
            return;
        }
        int size = 0;
        try {
           System.out.println( gameRuler.send("gogui-rules_board"));
        } catch (GtpError e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            size = GenericBoard.getBoardSize(gameRuler);
        } catch (GtpError e) {
            e.printStackTrace();
            return;
        }
        int nbChar = 0;
        boolean setup = false;
        PointList blacks = new PointList();
        PointList whites = new PointList();
        Move playOneMove = null;
        for (int i = 0; i < size; i++) {
            int j = -1;
            char c = ' ';
            do {
                do {
                    c = rulerBoardState.charAt(nbChar);
                    nbChar++;
                }while (c != 'X'&& c != 'O' && c != '.' && c != '\n');
                if (c != '\n')
                {
                    j++;
                }
                if ( c == 'X')
                {
                    GoPoint black = GoPoint.get(j, size-i-1);
                    if (board.getColor(black) != BLACK)
                    {
                        System.out.println(playOneMove);
                            blacks.add(black);
                        if (playOneMove == null)
                            playOneMove = Move.get(BLACK, black);
                        else if (!setup)
                            setup = true;
                    }
                }
                else if (c == 'O')
                {
                    GoPoint white = GoPoint.get(j, size-i-1);
                    if (board.getColor(white) != WHITE)
                    {
                        whites.add(white);
                        if (playOneMove == null)
                            playOneMove = Move.get(WHITE, white);
                        else if (!setup)
                            setup = true;
                    }
                }
                else if (c == '.')
                {
                    GoPoint empty = GoPoint.get(j, size-i-1);
                    if (!board.getColor(empty).equals(EMPTY)) {
                        setup = true;
                    }
                }
            } while (j < size-1);
        }
        if (setup || blacks.size() + whites.size() >1)
            try {
                System.out.println(blacks.size()+" " +whites.size());
                board.setup(blacks, whites, GenericBoard.getSideToMove(gameRuler, null));
            } catch (GtpError e) {
        }
        else {
            if (playOneMove != null)
            {
                board.playGameMove(playOneMove);
            }
        }
    }
    
    public static int getBoardSize(GtpClientBase gameRuler) throws GtpError
    {
        if (!gameRuler.isSupported("gogui-rules_board_size"))
            return 0;
        return Integer.parseInt(gameRuler.send("gogui-rules_board_size"));
    }
    
    //Makes the constructor unavailable.
    private GenericBoard()
    {
    }

    public static void copyBoardState(GtpClientBase gameRuler, Board board) {
        try {
            gameRuler.sendClearBoard(board.getSize());
            ConstPointList b = board.getSetup(BLACK);
            for (GoPoint p : b) {
                gameRuler.sendPlay(Move.get(BLACK, p));
            }
            b = board.getSetup(BLACK);
            for (GoPoint p : b) {
                gameRuler.sendPlay(Move.get(WHITE, p));
            }
            board.getSetup(WHITE);
            gameRuler.send("showboard");
        } catch (GtpError e) {
        }
    }
    
}
