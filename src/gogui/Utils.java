//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.Component;
import game.Node;
import go.Move;
import gtp.GtpError;
import gui.Clock;
import gui.CommandThread;
import gui.SimpleDialogs;
import utils.StringUtils;

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

    /** Set komi.
        Sends the komi command if the CommandThread is not null and
        it supports the command.
        Errors are shown to the user.
    */
    public static void sendKomi(Component parent, double komi,
                                CommandThread thread)
    {
        if (thread == null)
            return;
        try
        {
            if (thread.isCommandSupported("komi"))
                thread.sendCommand("komi " + komi);
        }
        catch (GtpError e)
        {
            showError(parent, e);
        }
    }

    /** Set rules using the scoring_system command.
        Sends the scoring_system command if rules are not
        go.Board.RULES_UNKNOWN, the CommandThread is not null and
        it supports the command.
        Errors are ignored.
    */
    public static void sendRules(int rules, CommandThread thread)
    {
        if (thread == null
            || rules == go.Board.RULES_UNKNOWN
            || ! thread.isCommandSupported("scoring_system"))
            return;
        try
        {
            String s =
                (rules == go.Board.RULES_JAPANESE ? "territory" : "area");
            thread.sendCommand("scoring_system " + s);
        }
        catch (GtpError e)
        {
        }
    }

    public static void showError(Component parent, GtpError error)
    {        
        String message = error.getMessage().trim();
        if (message.length() == 0)
            message = "Command failed";
        else
            message = StringUtils.capitalize(message);
        SimpleDialogs.showError(parent, message);
    }
}

//----------------------------------------------------------------------------
