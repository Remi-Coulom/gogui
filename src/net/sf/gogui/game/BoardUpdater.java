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
        ConstGameInformation gameInformation = tree.getGameInformationConst();
        int size = gameInformation.getBoardSize();
        int handicap = gameInformation.getHandicap();
        m_nodes.clear();
        NodeUtil.getPathToRoot(currentNode, m_nodes);
        board.init(size);
        boolean isFirstPlacement = true;
        for (int i = m_nodes.size() - 1; i >= 0; --i)
        {
            ConstNode node = (ConstNode)m_nodes.get(i);
            if (node.hasSetup())
            {
                if (isFirstPlacement
                    && node.getNumberAddWhite() == 0
                    && node.getNumberAddEmpty() == 0
                    && node.getNumberAddBlack() == handicap)
                    doSetupHandicap(node, board);
                else
                    doSetup(node, board);
                isFirstPlacement = false;
            }
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
        PointList setupBlack = new PointList();
        for (int i = 0; i < node.getNumberAddBlack(); ++i)
            setupBlack.add(node.getAddBlack(i));
        PointList setupWhite = new PointList();
        for (int i = 0; i < node.getNumberAddWhite(); ++i)
            setupWhite.add(node.getAddWhite(i));
        PointList setupEmpty = new PointList();
        for (int i = 0; i < node.getNumberAddEmpty(); ++i)
            setupEmpty.add(node.getAddEmpty(i));
        board.setup(setupBlack, setupWhite, setupEmpty, node.getPlayer());
    }

    private void doSetupHandicap(ConstNode node, Board board)
    {
        PointList points = new PointList();
        for (int i = 0; i < node.getNumberAddBlack(); ++i)
            points.add(node.getAddBlack(i));
        board.setupHandicap(points);
    }
}

