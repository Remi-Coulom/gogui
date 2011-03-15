// TextParserTest.java

package net.sf.gogui.text;

import java.io.Reader;
import java.io.StringReader;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;

public final class TextParserTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TextParserTest.class);
    }

    public void testParse()
    {
        parse(" +  +  O  +  O  O  #  #  + \n" +
              " +  +  O  +  O  O  #  #  + \n" +
              " +  #  O  O  O  O  O  #  # \n" +
              " +  #  +  O  O  #  #  #  + \n" +
              " +  #  O  O  #  +  #  O  + \n" +
              " +  +  O  #  #  +  #  +  + \n" +
              " +  O  O  O  #  +  +  #  + \n" +
              " #  O  O  #  #  #  #  +  + \n" +
              " +  O  O  O  #  #  #  +  + \n");
        checkSize(9);
        checkColor(0, 0, EMPTY);
        checkColor(0, 1, BLACK);
        checkColor(0, 2, EMPTY);
        checkColor(1, 0, WHITE);
        checkColor(1, 1, WHITE);
        checkColor(1, 2, WHITE);
        checkColor(2, 0, WHITE);
        checkColor(2, 1, WHITE);
        checkColor(2, 2, WHITE);
        checkColor(3, 0, WHITE);
        checkColor(3, 1, BLACK);
        checkColor(3, 2, WHITE);
        checkColor(3, 3, BLACK);
    }

    public void testHeightGreaterWidth()
    {
        parse(". X . .\n" +
              "O . . .\n" +
              ". . . .\n" +
              ". . . .\n" +
              ". . . .\n" +
              ". . . .\n");
        checkSize(6);
        checkColor(0, 4, WHITE);
        checkColor(1, 5, BLACK);
    }

    public void testWidthGreaterHeight()
    {
        parse(". X . . . .\n" +
              "O . . . . .\n");
        checkSize(6);
        checkColor(0, 0, WHITE);
        checkColor(1, 1, BLACK);
    }

    /** Test example from GNU Go documentation width row width increasing. */
    public void testWidthIncreasing()
    {
        parse("@@@@@\n" +
              "@OOO@\n" +
              "@O.OO@\n" +
              "@@..O@\n" +
              "@@OO@@\n" +
              "@@@@@\n");
        checkSize(6);
        checkColor(0, 5, BLACK);
        checkColor(5, 5, EMPTY);
    }

    public void testParseGnuGo()
    {
        parse("   A B C D E F G H J K L M N O P Q R S T\n" +
              "19 . . . . . . O O X . . . . . . . . . . 19\n" +
              "18 . O O . . O O X . X . X O . O X . . . 18\n" +
              "17 . X X O O . X X . . X . O . . X . . . 17\n" +
              "16 . X . X . O . . X + . . . . X + . . . 16\n" +
              "15 . O X . O . . O . . X . O . . . X . . 15\n" +
              "14 . O . X O . . . . . . . . . . . . . . 14\n" +
              "13 . . . X O . . O . . X O O . X . . . . 13\n" +
              "12 . . O X . . . . . . X X . O X . . . . 12\n" +
              "11 . . O X . O . O . . . X O O O X . . . 11     WHITE (O) has captured 1 stones\n" +
              "10 . . O X . . . . O + . X X X O + . . . 10     BLACK (X) has captured 2 stones\n" +
              " 9 . . O X . . . . . . . X O . . . X . . 9\n" +
              " 8 . O O X . . . . X X X X O O . X . . . 8\n" +
              " 7 . X X X . . X . . O O X . O X . X . . 7\n" +
              " 6 . . . . . . . O . . . O X X . X X X . 6\n" +
              " 5 X X X X . . O . . . O O X X X O O X X 5\n" +
              " 4 O X . O . . . . . + . X O X O + . O O 4\n" +
              " 3 O O O . . . O . . O . . O O . O . . . 3\n" +
              " 2 . . . . . . . . . . . . . . O . . . . 2\n" +
              " 1 . . . . . . . . . . . . . . . . . . . 1\n" +
              "   A B C D E F G H J K L M N O P Q R S T\n");
        checkSize(19);
        checkColor(0, 0, EMPTY);
        checkColor(0, 1, EMPTY);
        checkColor(0, 2, WHITE);
        checkColor(0, 3, WHITE);
        checkColor(0, 4, BLACK);
        checkColor(0, 5, EMPTY);
    }

    /** Test parsing if long lines were wrapped. */
    public void testParseGnuGoLineWrap()
    {
        parse("   A B C D E F G H J K L M N O P Q R S T\n" +
              "19 . . . . . . O O X . . . . . . . . . . 19\n" +
              "18 . O O . . O O X . X . X O . O X . . . 18\n" +
              "17 . X X O O . X X . . X . O . . X . . . 17\n" +
              "16 . X . X . O . . X + . . . . X + . . . 16\n" +
              "15 . O X . O . . O . . X . O . . . X . . 15\n" +
              "14 . O . X O . . . . . . . . . . . . . . 14\n" +
              "13 . . . X O . . O . . X O O . X . . . . 13\n" +
              "12 . . O X . . . . . . X X . O X . . . . 12\n" +
              "11 . . O X . O . O . . . X O O O X . . . 11     WHITE (O) has captured 1\n" +
              "stones\n" +
              "10 . . O X . . . . O + . X X X O + . . . 10     BLACK (X) has captured 2\n" +
              "stones\n" +
              " 9 . . O X . . . . . . . X O . . . X . . 9\n" +
              " 8 . O O X . . . . X X X X O O . X . . . 8\n" +
              " 7 . X X X . . X . . O O X . O X . X . . 7\n" +
              " 6 . . . . . . . O . . . O X X . X X X . 6\n" +
              " 5 X X X X . . O . . . O O X X X O O X X 5\n" +
              " 4 O X . O . . . . . + . X O X O + . O O 4\n" +
              " 3 O O O . . . O . . O . . O O . O . . . 3\n" +
              " 2 . . . . . . . . . . . . . . O . . . . 2\n" +
              " 1 . . . . . . . . . . . . . . . . . . . 1\n" +
              "   A B C D E F G H J K L M N O P Q R S T\n");
        checkSize(19);
        checkColor(0, 0, EMPTY);
        checkColor(0, 1, EMPTY);
        checkColor(0, 2, WHITE);
        checkColor(0, 3, WHITE);
        checkColor(0, 4, BLACK);
        checkColor(0, 5, EMPTY);
    }

    /** Test that parsing succeeds if non-breaking spaces are used.
        The clipboard can contain non-breaking spaces if the text is copied
        from other programs. */
    public void testNonBreakingSpace()
    {
        parse(" + " + '\u00A0' + "+ O + O O # # + \n" +
              " + + O + O O # # + \n" +
              " + # O O O O O # # \n" +
              " + # + O O # # # + \n" +
              " + # O O # + # O + \n" +
              " + + O # # + # + + \n" +
              " + O O O # + + # + \n" +
              " # O O # # # # + + \n" +
              " + O O O # # # + + \n");
        checkSize(9);
    }

    /** Test that parsing succeeds if leading &gt; characters exist as
        in quotatations in emails. */
    public void testMailQuotations()
    {
        parse(">" +
              ">      . . . . . ." +
              ">      O O . . . ." +
              ">      O O X X X X" +
              ">      . X O O X ." +
              ">      . X O . O X" +
              ">      . X O O . X");
        checkSize(6);
    }

    private Board m_board;

    private void checkColor(int x, int y, GoColor c)
    {
        assertEquals(c, m_board.getColor(GoPoint.get(x, y)));
    }

    private void checkSize(int size)
    {
        assertEquals(size, m_board.getSize());
    }

    public void parse(String s)
    {
        TextParser parser = new TextParser();
        try
        {
            Reader reader = new StringReader(s);
            parser.parse(reader);
            m_board = parser.getBoard();
        }
        catch (ParseError e)
        {
            fail(e.getMessage());
        }
    }
}
