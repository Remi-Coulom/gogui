// TexWriterTest.java

package net.sf.gogui.tex;

import java.io.ByteArrayOutputStream;
import net.sf.gogui.game.Game;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.Move;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;

public final class TexWriterTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TexWriterTest.class);
    }

    public void testMarkup()
    {
        int size = 19;
        Board board = new Board(size);
        String[][] markLabel = new String[size][size];
        boolean[][] mark = new boolean[size][size];
        board.play(BLACK, GoPoint.get(0, 0));
        mark[0][0] = true;
        mark[0][1] = true;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new TexWriter(null, out, board, markLabel, mark, null, null, null,
                      null);
        String s = out.toString();
        //System.err.println(s);
        assertTrue(s.indexOf("\\stone[\\markma]{black}{a}{1}") >= 0);
        assertTrue(s.indexOf("\\markpos{\\markma}{a}{2}") >= 0);
    }

    /** Test that a comment is appended for skipped second move on same
        point. */
    public void testTwoMovesOnPoint()
    {
        int size = 2;
        Game game = new Game(size);
        game.play(Move.get(BLACK, 0, 1));
        game.play(Move.get(WHITE, 1, 1));
        game.play(Move.get(BLACK, 0, 0));
        game.play(Move.get(WHITE, 1, 0));
        game.play(Move.get(BLACK, 0, 1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new TexWriter(null, out, game.getTree());
        String s = out.toString();
        //System.err.println(s);
        assertTrue(s.indexOf("\\stone[5]{black}~at~\\stone[1]{black}") >= 0);
    }
}
