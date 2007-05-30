//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
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
                ConstPointList addBlack = node.getAddStones(GoColor.BLACK);
                ConstPointList addWhite = node.getAddStones(GoColor.WHITE);
                ConstPointList addEmpty = node.getAddStones(GoColor.EMPTY);
                if (isFirstPlacement && addBlack.size() == handicap
                    && addWhite.size() == 0 && addEmpty.size() == 0)
                    board.setupHandicap(addBlack);
                else
                {
                    PointList black = new PointList();
                    PointList white = new PointList();
                    for (int j = 0; j < board.getPoints().size(); ++j)
                    {
                        GoPoint p = board.getPoints().get(j);
                        if (board.getColor(p) == GoColor.BLACK)
                            black.add(p);
                        else if (board.getColor(p) == GoColor.WHITE)
                            white.add(p);
                    }
                    for (int j = 0; j < addBlack.size(); ++j)
                        if (! black.contains(addBlack.get(j)))
                            black.add(addBlack.get(j));
                    for (int j = 0; j < addWhite.size(); ++j)
                        if (! white.contains(addWhite.get(j)))
                            white.add(addWhite.get(j));
                    for (int j = 0; j < addEmpty.size(); ++j)
                    {
                        black.remove(addEmpty.get(j));
                        white.remove(addEmpty.get(j));
                    }
                    board.setup(black, white, player);
                }
                isFirstPlacement = false;
            }
            else if (player != null)
                board.setToMove(player);
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
    private final ArrayList m_nodes;
}
