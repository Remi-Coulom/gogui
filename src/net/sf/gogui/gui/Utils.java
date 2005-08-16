//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.utils.StringUtils;

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
            GoColor color = move.getColor();
            // Round time to seconds
            long timeLeft = clock.getTimeLeft(color) / 1000L;
            if (color == GoColor.BLACK)
            {
                node.setTimeLeftBlack((double)timeLeft);
                if (clock.isInByoyomi(color))
                    node.setMovesLeftBlack(clock.getMovesLeft(color));
            }
            else
            {
                assert(color == GoColor.WHITE);
                node.setTimeLeftWhite((double)timeLeft);
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
                                String name, CommandThread thread)
    {
        if (thread == null)
            return;
        try
        {
            if (thread.isCommandSupported("komi"))
                thread.send("komi " + GameInformation.roundKomi(komi));
        }
        catch (GtpError e)
        {
            showError(parent, name, e);
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
            || rules == Board.RULES_UNKNOWN
            || ! thread.isCommandSupported("scoring_system"))
            return;
        try
        {
            String s =
                (rules == Board.RULES_JAPANESE ? "territory" : "area");
            thread.send("scoring_system " + s);
        }
        catch (GtpError e)
        {
        }
    }

    public static void showError(Component parent, String name,
                                 GtpError error)
    {        
        String message = error.getMessage().trim();
        if (message.length() == 0)
            message = "Command failed";
        else
            message = StringUtils.capitalize(message);
        SimpleDialogs.showError(parent, name + ": " + message);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Utils()
    {
    }
}

//----------------------------------------------------------------------------
