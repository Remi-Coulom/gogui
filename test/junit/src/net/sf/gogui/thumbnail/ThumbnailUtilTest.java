// ThumbnailUtilTest.java

package net.sf.gogui.thumbnail;

import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.NodeUtil;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;

public final class ThumbnailUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(ThumbnailUtilTest.class);
    }

    /** Test that getNode() return the last position in a game. */
    public void testGetNodeGame()
    {
        Game game = new Game(19);
        game.play(Move.get(BLACK, 15, 15));
        game.play(Move.get(WHITE, 3, 3));
        ConstNode root = game.getTree().getRootConst();
        ConstNode node = ThumbnailUtil.getNode(game.getTree());
        assertEquals(NodeUtil.getLast(root), node);
    }

    /** Test that getNode() return the last position in a game with
        handicap. */
    public void testGetNodeGameWithHandicap()
    {
        PointList handicap = new PointList();
        handicap.add(GoPoint.get(3, 3));
        handicap.add(GoPoint.get(15, 15));
        Game game = new Game(19, null, handicap, "", null);
        game.play(Move.get(WHITE, 2, 5));
        game.play(Move.get(BLACK, 5, 2));
        ConstNode root = game.getTree().getRootConst();
        ConstNode node = ThumbnailUtil.getNode(game.getTree());
        assertEquals(NodeUtil.getLast(root), node);
    }

    /** Test that getNode() return the first position that contains both
        black and white setup stones. */
    public void testGetNodeSetup()
    {
        Game game = new Game(19);
        game.setup(GoPoint.get(0, 0), BLACK);
        game.setup(GoPoint.get(1, 0), WHITE);
        game.play(Move.get(BLACK, 1, 0));
        game.play(Move.get(WHITE, 1, 1));
        ConstNode root = game.getTree().getRootConst();
        ConstNode node = ThumbnailUtil.getNode(game.getTree());
        assertEquals(root, node);
    }
}
