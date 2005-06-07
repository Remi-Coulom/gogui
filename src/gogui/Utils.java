//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import game.Node;
import go.Move;
import gui.Clock;

//----------------------------------------------------------------------------

/** Static utility functions. */
public class Utils
{
    /** Create a new node with a move and append it to current node.
        Also adds time information from clock, if not null and initialized.
        The clock must not be running.
    */
    public static Node createNode(Node currentNode, Move move, Clock clock)
    {
        Node node = new Node(move);
        if (clock != null && clock.isInitialized())
        {
            assert(! clock.isRunning());
            go.Color color = move.getColor();
            // Round time to seconds
            double timeLeft = clock.getTimeLeft(color) / 1000;
            if (color == go.Color.BLACK)
            {
                node.setTimeLeftBlack(timeLeft);
                if (clock.isInByoyomi(color))
                    node.setMovesLeftBlack(clock.getMovesLeft(color));
            }
            else
            {
                assert(color == go.Color.WHITE);
                node.setTimeLeftWhite(timeLeft);
                if (clock.isInByoyomi(color))
                    node.setMovesLeftWhite(clock.getMovesLeft(color));
            }
        }
        currentNode.append(node);
        return node;
    }
}

//----------------------------------------------------------------------------
