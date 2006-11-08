//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Frame;
import java.awt.Point;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.gui.AnalyzeCommand;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtil;
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
            GoPoint list[] = GtpUtil.parsePointString(text);
            GuiBoardUtil.showPointList(m_guiBoard, list);
        }

        private GuiBoard m_guiBoard;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GoGuiUtil()
    {
    }
}

