//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;

//----------------------------------------------------------------------------

/** Updates a go.Board to a node in a GameTree. */
public class BoardUpdater
{
    public BoardUpdater()
    {
        m_nodes = new ArrayList(400);
        m_moves = new ArrayList(400);
    }

    public void update(GameTree tree, Node node, Board board)
    {
        int size = tree.getGameInformation().m_boardSize;
        assert(board.getSize() == size);
        m_nodes.clear();
        NodeUtil.getPathToRoot(node, m_nodes);
        board.init(size);
        for (int i = m_nodes.size() - 1; i >= 0; --i)
        {            
            NodeUtil.getAllAsMoves((Node)m_nodes.get(i), m_moves);
            for (int j = 0; j < m_moves.size(); ++j)
                board.play((Move)m_moves.get(j));
        }
        GoColor toMove = node.getToMove();
        if (toMove != GoColor.EMPTY)
            board.setToMove(toMove);
    }

    /** Local variable used in update.
        Member variable for avoiding frequent new memory allocations.
    */
    private ArrayList m_moves;

    /** Local variable used in update.
        Member variable for avoiding frequent new memory allocations.
    */
    private ArrayList m_nodes;
}

//----------------------------------------------------------------------------
