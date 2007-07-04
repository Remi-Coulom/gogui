//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;

public final class BoardUpdaterTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(BoardUpdaterTest.class);
    }

    public void setUp()
    {
        m_updater = new BoardUpdater();
    }

    public void testSetup()
    {
        GameTree tree = new GameTree();
        Node root = tree.getRoot();
        root.addStone(BLACK, GoPoint.get(1, 1));
        root.addStone(BLACK, GoPoint.get(1, 2));
        root.addStone(BLACK, GoPoint.get(1, 3));
        root.addStone(BLACK, GoPoint.get(1, 4));
        root.addStone(BLACK, GoPoint.get(1, 5));
        Node child = new Node();
        child.addStone(WHITE, GoPoint.get(2, 1));
        child.addStone(WHITE, GoPoint.get(2, 2));
        child.addStone(WHITE, GoPoint.get(2, 3));
        child.addStone(WHITE, GoPoint.get(2, 4));
        child.addStone(WHITE, GoPoint.get(2, 5));
        child.addStone(BLACK, GoPoint.get(3, 1));
        root.append(child);
        Board board = new Board(19);
        m_updater.update(tree, child, board);
        assertEquals(BLACK, board.getColor(GoPoint.get(1, 1)));
        assertEquals(BLACK, board.getColor(GoPoint.get(1, 2)));
        assertEquals(BLACK, board.getColor(GoPoint.get(1, 3)));
        assertEquals(BLACK, board.getColor(GoPoint.get(1, 4)));
        assertEquals(BLACK, board.getColor(GoPoint.get(1, 5)));
        assertEquals(WHITE, board.getColor(GoPoint.get(2, 1)));
        assertEquals(WHITE, board.getColor(GoPoint.get(2, 2)));
        assertEquals(WHITE, board.getColor(GoPoint.get(2, 3)));
        assertEquals(WHITE, board.getColor(GoPoint.get(2, 4)));
        assertEquals(WHITE, board.getColor(GoPoint.get(2, 5)));
        assertEquals(BLACK, board.getColor(GoPoint.get(3, 1)));
    }

    private BoardUpdater m_updater;
}
