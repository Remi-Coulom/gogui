// GenericBoardTest.java

package net.sf.gogui.go;

import java.io.File;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;

import net.sf.gogui.game.Game;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClient.ExecFailed;
import net.sf.gogui.gtp.GtpError;


public final class GenericBoardTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GenericBoardTest.class);
    }
    
    GtpClient gameRuler;
    Board board;
    GameTree gameTree;
    Game game;
    
    private void gameRuler() {
        try {
                gameRuler = new GtpClient("/home/fretel/crazy_zero/gomoku/gomoku_gtp gogui.gtp", new File("/home/fretel/crazy_zero/gomoku"), false, null);
                gameRuler.querySupportedCommands();
            
        } catch (ExecFailed e) {
            e.printStackTrace();
        } catch (GtpError e) {
            e.printStackTrace();
        }
        board = new Board(19);
        gameTree = new GameTree();
        game = new Game(gameTree);
    }
    
    public void testGomokuRulerLegalMoves()
    {
        gameRuler();
        try 
        {
             assertFalse(GenericBoard.isPassLegal(gameRuler));
             assertFalse(GenericBoard.isLegalMove(gameRuler, Move.get(WHITE, GoPoint.get(10, 10))));
             assertTrue(GenericBoard.isLegalMove(gameRuler, Move.get(BLACK, GoPoint.get(10, 10))));
             gameRuler.sendPlay(Move.get(BLACK, GoPoint.get(10, 10)));
             assertFalse(GenericBoard.isLegalMove(gameRuler, Move.get(WHITE, GoPoint.get(10, 10))));
             assertFalse(GenericBoard.isLegalMove(gameRuler, Move.get(BLACK, GoPoint.get(9, 10))));
             
        } catch (GtpError e) {
            e.printStackTrace();
        }
    }
    
    public void testSynchroRulerWithBoard()
    {
        gameRuler();
        try
        {
            Move m = Move.get(BLACK, GoPoint.get(18, 18));
            gameRuler.sendPlay(m);
            m = Move.get(WHITE, GoPoint.get(15, 13));
            gameRuler.sendPlay(m);
            GenericBoard.copyRulerBoardState(gameRuler, board);
            assertTrue(board.getColor(GoPoint.get(18, 18)).equals(BLACK));
            assertTrue(board.getColor(GoPoint.get(15, 13)).equals(WHITE));
            assertFalse(board.getColor(GoPoint.get(13, 15)).equals(WHITE));
            gameRuler.sendPlay(m);
        } catch (GtpError e) {
            e.printStackTrace();
        }
    }
    
    public void testAutoSynchro()
    {
        gameRuler();
        Move m = Move.get(BLACK, GoPoint.get(18, 18));
        GenericBoard.sendPlay(gameRuler, board, m);
        assertTrue(board.getColor(GoPoint.get(18,18)).equals(BLACK));
    }
    
    public void testSynchroSideToMove()
    {
        gameRuler();
        assertEquals(board.getToMove(), BLACK);
        Move m = Move.get(BLACK, GoPoint.get(18, 18));
        GenericBoard.sendPlay(gameRuler, board,  m);
        assertEquals(board.getToMove(), WHITE);
        GenericBoard.sendPlay(gameRuler, board, m);
        assertEquals(board.getToMove(), BLACK);
    }
}
