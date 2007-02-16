//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
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
        root.addStone(GoColor.BLACK, GoPoint.get(1, 1));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 2));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 3));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 4));
        root.addStone(GoColor.BLACK, GoPoint.get(1, 5));
        Node child = new Node();
        child.addStone(GoColor.WHITE, GoPoint.get(2, 1));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 2));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 3));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 4));
        child.addStone(GoColor.WHITE, GoPoint.get(2, 5));
        child.addStone(GoColor.BLACK, GoPoint.get(3, 1));
        root.append(child);
        Board board = new Board(19);
        m_updater.update(tree, child, board);
        assertEquals(2, board.getNumberActions());
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 1)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 2)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 3)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 4)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(1, 5)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 1)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 2)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 3)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 4)));
        assertEquals(GoColor.WHITE, board.getColor(GoPoint.get(2, 5)));
        assertEquals(GoColor.BLACK, board.getColor(GoPoint.get(3, 1)));
    }

    private BoardUpdater m_updater;
}

