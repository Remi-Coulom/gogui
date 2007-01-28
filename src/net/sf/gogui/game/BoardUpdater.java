//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;

/** Updates a go.Board to a node in a GameTree. */
public class BoardUpdater
{
    public BoardUpdater()
    {
        m_nodes = new ArrayList(400);
    }

    public void update(ConstGameTree tree, ConstNode currentNode, Board board)
    {
        int size = tree.getGameInformationConst().getBoardSize();
        m_nodes.clear();
        NodeUtil.getPathToRoot(currentNode, m_nodes);
        board.init(size);
        for (int i = m_nodes.size() - 1; i >= 0; --i)
        {
            Node node = (Node)m_nodes.get(i);
            if (node.hasSetup())
            {
                PointList setupBlack = new PointList();
                for (int j = 0; j < node.getNumberAddBlack(); ++j)
                    setupBlack.add(node.getAddBlack(j));
                PointList setupWhite = new PointList();
                for (int j = 0; j < node.getNumberAddWhite(); ++j)
                    setupWhite.add(node.getAddWhite(j));
                PointList setupEmpty = new PointList();
                for (int j = 0; j < node.getNumberAddEmpty(); ++j)
                    setupEmpty.add(node.getAddEmpty(j));
                board.setup(setupBlack, setupWhite, setupEmpty);
            }
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

