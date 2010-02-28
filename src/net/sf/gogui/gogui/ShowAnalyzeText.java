// ShowAnalyeText.java

package net.sf.gogui.gogui;

import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;
import net.sf.gogui.gtp.AnalyzeType;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtil;
import net.sf.gogui.gui.TextViewer;

/** Show multi-line text output from analyze command.
    Optionally can reuse window of last output. */
public final class ShowAnalyzeText
{
    public ShowAnalyzeText(Frame owner, GuiBoard guiBoard)
    {
        m_owner = owner;
        m_guiBoard = guiBoard;
    }

    public void show(AnalyzeType type, GoPoint pointArg, String title,
                     String response, boolean reuseWindow)
    {
        boolean highlight = (type == AnalyzeType.HSTRING
                             || type == AnalyzeType.HPSTRING);
        TextViewer.Listener listener = null;
        if (type == AnalyzeType.PSTRING || type == AnalyzeType.HPSTRING)
            listener = new PointSelectionMarker(m_guiBoard);
        // Remove first line, if empty (formatted responses frequently start
        // with an empty line to avoid text on the line with the status
        // character)
        response = response.replaceAll("\\A *\n", "");
        if (reuseWindow && m_textViewer != null)
            m_textViewer.setText(title, response, highlight);
        else
        {
            m_textViewer = new TextViewer(m_owner, title, response, highlight,
                                          listener);
            m_textViewer.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        m_textViewer = null;
                    }
                });
            if (pointArg == null)
                m_textViewer.setLocationByPlatform(true);
            else
            {
                Point location = m_guiBoard.getLocationOnScreen(pointArg);
                m_textViewer.setLocation(location);
            }
            m_textViewer.setVisible(true);
        }
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

    private Frame m_owner;

    private GuiBoard m_guiBoard;

    private TextViewer m_textViewer;
}
