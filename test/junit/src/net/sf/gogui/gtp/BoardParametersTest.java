package net.sf.gogui.gtp;

import net.sf.gogui.gtp.BoardParameters;

public class BoardParametersTest
        extends junit.framework.TestCase {

    public static void main(String[] args) { junit.textui.TestRunner.run(suite()); }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(BoardParametersTest.class);
    }

    public void testTestEquals() {
        BoardParameters boardParameters = new BoardParameters(19);
        BoardParameters boardParameters2 = new BoardParameters(13);
        BoardParameters boardParameters3 = new BoardParameters(19, 19, "rect");
        BoardParameters boardParameters4 = new BoardParameters(13, 13, "hex");
        assertFalse(boardParameters.equals(boardParameters2));
        assertEquals(boardParameters, boardParameters3);
        assertFalse(boardParameters2.equals(boardParameters4));
    }

    public void testGet() {
        String input = "13";
        BoardParameters boardParameters = BoardParameters.get(input);
        assertEquals(new BoardParameters(13), boardParameters);

        input = "13 13";
        boardParameters = BoardParameters.get(input);
        assertEquals(new BoardParameters(13, 13, "rect"), boardParameters);

        input = "13 hex";
        boardParameters = BoardParameters.get(input);
        assertEquals(new BoardParameters(13, 13, "hex"), boardParameters);

        input = "13 13 hex";
        boardParameters = BoardParameters.get(input);
        assertEquals(new BoardParameters(13, 13, "hex"), boardParameters);
    }


}