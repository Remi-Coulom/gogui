// GameTest.java

package net.sf.gogui.game;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
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

    public static void testSetComment()
    {
        Game game = new Game(19);
        String comment = "foo";
        game.setComment(comment);
        assertTrue(game.isModified());
        assertEquals(comment, game.getCurrentNode().getComment());
        // Check for bug that cleared the modified flag after setting the same
        // comment again
        game.setComment(comment);
        assertTrue(game.isModified());
    }

    public static void testSetLabel()
    {
        Game game = new Game(19);
        GoPoint p = GoPoint.get(0, 0);
        String label = "foo";
        game.setLabel(p, label);
        assertTrue(game.isModified());
        assertEquals(label, game.getCurrentNode().getLabel(p));
        // Check for bug that cleared the modified flag after setting the same
        // label again
        game.setLabel(p, label);
        assertTrue(game.isModified());
    }

    public static void testSetToMove()
    {
        Game game = new Game(19);
        game.setToMove(WHITE);
        assertTrue(game.isModified());
        assertEquals(WHITE, game.getCurrentNode().getPlayer());
    }

    /** Test removing a stone in the root node.
        It should just remove the setup stone from the node, but not add a
        removal (empty setup stone). */
    public static void testSetupEmptyInRoot()
    {
        Game game = new Game(19);
        ConstNode node = game.getCurrentNode();
        GoPoint p = GoPoint.get(0, 0);
        game.setup(p, BLACK);
        assertEquals(1, node.getSetup(BLACK).size());
        assertEquals(0, node.getSetup(WHITE).size());
        assertEquals(0, node.getSetup(EMPTY).size());
        game.setup(p, EMPTY);
        assertEquals(0, node.getSetup(BLACK).size());
        assertEquals(0, node.getSetup(WHITE).size());
        assertEquals(0, node.getSetup(EMPTY).size());
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
        game.play(Move.get(BLACK, null));
        assertNotSame(root, game.getCurrentNode());
        ConstGameInfo oldInfo = game.getGameInfo(root);
        GameInfo newInfo = new GameInfo(oldInfo);
        TimeSettings newTimeSettings = new TimeSettings(300000);
        assertTrue(! newTimeSettings.equals(timeSettings));
        newInfo.setTimeSettings(newTimeSettings);
        game.setGameInfo(newInfo, root);
        assertEquals(newTimeSettings, game.getClock().getTimeSettings());
    }
}
