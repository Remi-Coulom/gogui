//----------------------------------------------------------------------------
// $Id: GameTreeTest.java 3791 2007-01-17 19:42:34Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

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
        assertEquals(1, node.getAddStones(GoColor.BLACK).size());
        assertEquals(0, node.getAddStones(GoColor.WHITE).size());
        assertEquals(0, node.getAddStones(GoColor.EMPTY).size());
        game.setup(p, GoColor.EMPTY);
        assertEquals(0, node.getAddStones(GoColor.BLACK).size());
        assertEquals(0, node.getAddStones(GoColor.WHITE).size());
        assertEquals(0, node.getAddStones(GoColor.EMPTY).size());
    }

    /** Test that clock is initialized with time settings. */
    public static void testTimeSettingsInit()
    {
        TimeSettings timeSettings = new TimeSettings(600000);
        Game game = new Game(19, null, null, null, timeSettings);
        assertEquals(timeSettings, game.getClock().getTimeSettings());
    }

    /** Test that clock is updated after time settings changed. */
    public static void testTimeSettingsUpdate()
    {
        TimeSettings timeSettings = new TimeSettings(600000);
        Game game = new Game(19, null, null, null, timeSettings);
        ConstNode root = game.getRoot();
        game.play(Move.get(null, GoColor.BLACK));
        assertNotSame(root, game.getCurrentNode());
        ConstGameInformation oldInfo = game.getGameInformation(root);
        GameInformation newInfo = new GameInformation(oldInfo);
        TimeSettings newTimeSettings = new TimeSettings(300000);
        assertTrue(! newTimeSettings.equals(timeSettings));
        newInfo.setTimeSettings(newTimeSettings);
        game.setGameInformation(newInfo, root);
        assertEquals(newTimeSettings, game.getClock().getTimeSettings());
    }
}
