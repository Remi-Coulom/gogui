//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;

/** Updates a go.Board to a node in a GameTree. */
public class BoardUpdater
{
    public BoardUpdater()
    {
        m_nodes = new ArrayList(400);
    }

    public void update(GameTree tree, Node currentNode, Board board)
    {
        int size = tree.getGameInformation().m_boardSize;
        assert(board.getSize() == size);
        m_nodes.clear();
        NodeUtil.getPathToRoot(currentNode, m_nodes);
        board.init(size);
        for (int i = m_nodes.size() - 1; i >= 0; --i)
        {
            Node node = (Node)m_nodes.get(i);
            for (int j = 0; j < node.getNumberAddBlack(); ++j)
                board.setup(node.getAddBlack(j), GoColor.BLACK);
            for (int j = 0; j < node.getNumberAddWhite(); ++j)
                board.setup(node.getAddWhite(j), GoColor.WHITE);
            for (int j = 0; j < node.getNumberAddEmpty(); ++j)
                board.setup(node.getAddEmpty(j), GoColor.EMPTY);
            Move move = node.getMove();
            if (move != null)
                board.play(move);
            GoColor toMove = node.getToMove();
            if (toMove != GoColor.EMPTY)
                board.setToMove(toMove);
        }
    }

    /** Local variable used in update.
        Member variable for avoiding frequent new memory allocations.
    */
    private ArrayList m_nodes;
}

