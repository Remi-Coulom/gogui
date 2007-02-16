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
        ConstGameInformation info = tree.getGameInformationConst(currentNode);
        int size = tree.getBoardSize();
        int handicap = info.getHandicap();
        m_nodes.clear();
        NodeUtil.getPathToRoot(currentNode, m_nodes);
        board.init(size);
        boolean isFirstPlacement = true;
        for (int i = m_nodes.size() - 1; i >= 0; --i)
        {
            ConstNode node = (ConstNode)m_nodes.get(i);
            GoColor player = node.getPlayer();
            if (node.hasSetup())
            {
                if (isFirstPlacement
                    && node.getAddStones(GoColor.BLACK).size() == handicap
                    && node.getAddStones(GoColor.WHITE).size() == 0
                    && node.getAddStones(GoColor.EMPTY).size() == 0)
                    doSetupHandicap(node, board);
                else
                    doSetup(node, board);
                isFirstPlacement = false;
            }
            else if (player != null && ! board.getToMove().equals(player))
                doSetup(node, board);
            Move move = node.getMove();
            if (move != null)
            {
                board.play(move);
                isFirstPlacement = false;
            }
        }
    }

    /** Local variable used in update.
        Member variable for avoiding frequent new memory allocations.
    */
    private ArrayList m_nodes;

    private void doSetup(ConstNode node, Board board)
    {
        board.setup(node.getAddStones(GoColor.BLACK),
                    node.getAddStones(GoColor.WHITE),
                    node.getAddStones(GoColor.EMPTY), node.getPlayer());
    }

    private void doSetupHandicap(ConstNode node, Board board)
    {
        board.setupHandicap(node.getAddStones(GoColor.BLACK));
    }
}

