//----------------------------------------------------------------------------
// $Id: GameTreeTest.java 3791 2007-01-17 19:42:34Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

public final class GameTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GameTest.class);
    }

    /** Test removing a stone in the root node.
        It should just remove the addStoen property, but not add an addEmpty
        property.
    */
    public static void testSetupEmptyInRoot()
    {
        Game game = new Game(19);
        ConstNode node = game.getCurrentNode();
        GoPoint p = GoPoint.get(0, 0);
        game.setup(p, GoColor.BLACK);
        assertEquals(1, node.getNumberAddBlack());
        assertEquals(0, node.getNumberAddWhite());
        assertEquals(0, node.getNumberAddEmpty());
        game.setup(p, GoColor.EMPTY);
        assertEquals(0, node.getNumberAddBlack());
        assertEquals(0, node.getNumberAddWhite());
        assertEquals(0, node.getNumberAddEmpty());
    }
}
