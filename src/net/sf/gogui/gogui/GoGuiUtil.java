//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Frame;
import java.awt.Point;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.gui.AnalyzeCommand;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtil;
import net.sf.gogui.gui.StatusBar;
import net.sf.gogui.gui.TextViewer;

/** Utility functions for class GoGui. */
public final class GoGuiUtil
{
    public static void showAnalyzeTextOutput(Frame owner, GuiBoard guiBoard,
                                             int type, GoPoint pointArg,
                                             String title, String response)
    {
        boolean highlight = (type == AnalyzeCommand.HSTRING
                             || type == AnalyzeCommand.HPSTRING);
        TextViewer.Listener listener = null;
        if (type == AnalyzeCommand.PSTRING || type == AnalyzeCommand.HPSTRING)
            listener = new PointSelectionMarker(guiBoard);
        // Remove first line, if empty (formatted responses frequently start
        // with an empty line to avoid text on the line with the status
        // character)
        response = response.replaceAll("\\A *\n", "");
        TextViewer textViewer = new TextViewer(owner, title, response,
                                               highlight, listener);
        if (pointArg == null)
            textViewer.setLocationRelativeTo(owner);
        else
        {
            Point location = guiBoard.getLocationOnScreen(pointArg);
            textViewer.setLocation(location);
        }
        textViewer.setVisible(true);
    }

    public static void updateMoveText(StatusBar statusBar, ConstGame game)
    {
        statusBar.setToPlay(game.getToMove());
        ConstNode node = game.getCurrentNode();
        int moveNumber = NodeUtil.getMoveNumber(node);
        int movesLeft = NodeUtil.getMovesLeft(node);
        Move move = node.getMove();
        String variation = NodeUtil.getVariationString(node);
        StringBuffer moveText = new StringBuffer(128);
        StringBuffer toolTip = new StringBuffer(128);
        if (moveNumber > 0)
        {
            moveText.append(moveNumber);
            moveText.append(' ');
        }
        if (movesLeft > 0)
        {
            moveText.append('(');
            moveText.append(moveNumber + movesLeft);
            moveText.append(") ");
        }

        if (move != null)
        {
            GoColor c = move.getColor();
            GoPoint p = move.getPoint();
            moveText.append(c == GoColor.BLACK ? "B " : "W ");
            moveText.append(GoPoint.toString(p));
            moveText.append(' ');
        }
        if (! "".equals(variation))
        {
            moveText.append('[');
            moveText.append(variation);
            moveText.append(']');
        }
        if (move == null)
        {
            toolTip.append(moveNumber);
            if (moveNumber == 1)
                toolTip.append(" move played ");
            else
                toolTip.append(" moves played ");
        }
        else
        {
            GoColor c = move.getColor();
            GoPoint p = move.getPoint();
            toolTip.append("Last move ");
            toolTip.append(moveNumber);
            toolTip.append(c == GoColor.BLACK ? " B " : " W ");
            toolTip.append(GoPoint.toString(p));
            toolTip.append(' ');
        }
        if (movesLeft > 0)
        {
            toolTip.append("of ");
            toolTip.append(moveNumber + movesLeft);
            if (moveNumber + movesLeft == 1)
                toolTip.append(" move ");
            else
                toolTip.append(" moves ");
        }
        if ("".equals(variation))
            toolTip.append("in main variation");
        else
        {
            toolTip.append("in variation ");
            toolTip.append(variation);
        }
        statusBar.setMoveText(moveText.toString(), toolTip.toString());
    }

    private static class PointSelectionMarker
        implements TextViewer.Listener
    {
        public PointSelectionMarker(GuiBoard guiBoard)
        {
            m_guiBoard = guiBoard;
        }

        public void textSelected(String text)
        {
            if (! m_guiBoard.isShowing())
                return;
            PointList points = GtpUtil.parsePointString(text);
            GuiBoardUtil.showPointList(m_guiBoard, points);
        }

        private final GuiBoard m_guiBoard;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GoGuiUtil()
    {
    }
}

