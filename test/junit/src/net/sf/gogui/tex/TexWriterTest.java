// TexWriterTest.java

package net.sf.gogui.tex;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import net.sf.gogui.game.Game;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.Move;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.BoardParameters;

public final class TexWriterTest
    extends junit.framework.TestCase
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TexWriterTest.class);
    }

    public void testMarkup()
    {
        Board board = new Board(new BoardParameters(19));
        Dimension dimension = board.getDimension();
        String[][] markLabel = new String[dimension.width][dimension.height];
        boolean[][] mark = new boolean[dimension.width][dimension.height];
        board.play(BLACK, GoPoint.get(0, 0));
        mark[0][0] = true;
        mark[0][1] = true;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new TexWriter(null, out, board, markLabel, mark, null, null, null,
                      null);
        String s = out.toString();
        //System.err.println(s);
        assertTrue(s.contains("\\stone[\\markma]{black}{a}{1}"));
        assertTrue(s.contains("\\markpos{\\markma}{a}{2}"));
    }

    /** Test that a comment is appended for skipped second move on same
        point. */
    public void testTwoMovesOnPoint()
    {
        Game game = new Game(new BoardParameters(2));
        game.play(Move.get(BLACK, 0, 1));
        game.play(Move.get(WHITE, 1, 1));
        game.play(Move.get(BLACK, 0, 0));
        game.play(Move.get(WHITE, 1, 0));
        game.play(Move.get(BLACK, 0, 1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new TexWriter(null, out, game.getTree());
        String s = out.toString();
        //System.err.println(s);
        assertTrue(s.contains("\\stone[5]{black}~at~\\stone[1]{black}"));
    }
}
