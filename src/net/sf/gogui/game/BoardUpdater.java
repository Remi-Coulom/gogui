// BoardUpdater.java

package net.sf.gogui.game;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;

/** Updates a go.Board to a node in a GameTree. */
public class BoardUpdater
{
    public BoardUpdater()
    {
        m_nodes = new ArrayList<ConstNode>(400);
    }

    public void update(ConstGameTree tree, ConstNode currentNode, Board board)
    {
        board.init(tree.getBoardSize());
        int handicap = tree.getGameInfoConst(currentNode).getHandicap();
        NodeUtil.getPathToRoot(currentNode, m_nodes);
        int nuMoves = 0;
        boolean isFirstPlacement = true;
        boolean isHandicapSetupDone = false;
        boolean isInInitialBlackMoveSequence = true;
        for (int i = m_nodes.size() - 1; i >= 0; --i)
        {
            ConstNode node = m_nodes.get(i);
            GoColor player = node.getPlayer();
            if (node.hasSetup())
            {
                ConstPointList setupBlack = node.getSetup(BLACK);
                ConstPointList setupWhite = node.getSetup(WHITE);
                ConstPointList setupEmpty = node.getSetup(EMPTY);
                if (handicap > 0 && isFirstPlacement
                    && setupBlack.size() == handicap && setupWhite.isEmpty()
                    && setupEmpty.isEmpty())
                {
                    board.setupHandicap(setupBlack);
                    isHandicapSetupDone = true;
                }
                else
                    newSetup(board, setupBlack, setupWhite, setupEmpty, player);
                isFirstPlacement = false;
                isInInitialBlackMoveSequence = false;
            }
            else if (player != null)
                board.setToMove(player);
            Move move = node.getMove();
            if (move != null)
            {
                board.play(move);
                ++nuMoves;
                isFirstPlacement = false;
                if (move.getColor() != BLACK)
                    isInInitialBlackMoveSequence = false;
                // Files from the KGS Go server with Chines rules store
                // handicap stones as moves, not as setup as specified by SGF
                if (handicap > 0 && ! isHandicapSetupDone &&
                    isInInitialBlackMoveSequence && nuMoves == handicap)
                {
                    setupMovesAsHandicap(board);
                    isHandicapSetupDone = true;
                }
            }
        }
    }

    /** Local variable used in update.
        Member variable for avoiding frequent new memory allocations. */
    private final ArrayList<ConstNode> m_nodes;

    /** Initialize board with new setup from merging the current position
        with the setup properties from a node. */
    private void newSetup(Board board, ConstPointList setupBlack,
                          ConstPointList setupWhite, ConstPointList setupEmpty,
                          GoColor player)
    {
        PointList black = new PointList();
        PointList white = new PointList();
        for (GoPoint p : board)
        {
            GoColor c = board.getColor(p);
            if (c == BLACK)
                black.add(p);
            else if (c == WHITE)
                white.add(p);
        }
        for (GoPoint p : setupBlack)
        {
            white.remove(p);
            if (! black.contains(p))
                black.add(p);
        }
        for (GoPoint p : setupWhite)
        {
            black.remove(p);
            if (! white.contains(p))
                white.add(p);
        }
        for (GoPoint p : setupEmpty)
        {
            black.remove(p);
            white.remove(p);
        }
        board.setup(black, white, player);
    }

    void setupMovesAsHandicap(Board board)
    {
        PointList black = new PointList();
        for (GoPoint p : board)
        {
            GoColor c = board.getColor(p);
            assert c != WHITE;
            if (c == BLACK)
                black.add(p);
        }
        board.setupHandicap(black);
    }
}
