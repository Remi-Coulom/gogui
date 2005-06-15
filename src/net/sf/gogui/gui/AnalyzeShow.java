//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.util.Vector;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;

//----------------------------------------------------------------------------

/** Show response to an AnalyzeCommand in the GUI. */
public class AnalyzeShow
{
    public static void show(AnalyzeCommand command, GuiBoard guiBoard,
                            Board board, String response) throws GtpError
    {
        GoPoint pointArg = command.getPointArg();
        Vector pointListArg = command.getPointListArg();
        guiBoard.clearAllSelect();
        for (int i = 0; i < pointListArg.size(); ++i)
            guiBoard.setSelect((GoPoint)pointListArg.get(i), true);
        if (pointArg != null)
            guiBoard.setSelect(pointArg, true);
        int type = command.getType();
        String title = command.getTitle();
        int size = board.getSize();
        switch (type)
        {
        case AnalyzeCommand.BWBOARD:
            {
                String b[][] =
                    GtpUtils.parseStringBoard(response, title, size);
                guiBoard.showBWBoard(b);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.CBOARD:
            {
                String b[][] =
                    GtpUtils.parseStringBoard(response, title, size);
                guiBoard.showColorBoard(b);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.DBOARD:
            {
                double b[][] =
                    GtpUtils.parseDoubleBoard(response, title, size);
                guiBoard.showDoubleBoard(b, command.getScale());
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.PLIST:
            {
                GoPoint list[] =
                    GtpUtils.parsePointList(response, size);
                guiBoard.showPointList(list);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.HPSTRING:
        case AnalyzeCommand.PSTRING:
            {
                GoPoint list[] = GtpUtils.parsePointString(response, size);
                guiBoard.showPointList(list);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.PSPAIRS:
            {
                Vector pointList = new Vector(32, 32);
                Vector stringList = new Vector(32, 32);
                GtpUtils.parsePointStringList(response, pointList, stringList,
                                              size);
                guiBoard.showPointStringList(pointList, stringList);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.SBOARD:
            {
                String b[][] =
                    GtpUtils.parseStringBoard(response, title, size);
                guiBoard.showStringBoard(b);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.VAR:
            {                    
                showVariation(guiBoard, response, board.getToMove(), size);
            }
            break;
        case AnalyzeCommand.VARB:
            {
                showVariation(guiBoard, response, GoColor.BLACK, size);
            }
            break;
        case AnalyzeCommand.VARC:
            {
                showVariation(guiBoard, response, command.getColorArg(),
                              size);
            }
            break;
        case AnalyzeCommand.VARW:
            {
                showVariation(guiBoard, response, GoColor.WHITE, size);
            }
            break;
        case AnalyzeCommand.VARP:
            {
                GoColor c = getColor(board, pointArg, pointListArg);
                if (c != GoColor.EMPTY)
                    showVariation(guiBoard, response, c, size);
            }
            break;
        case AnalyzeCommand.VARPO:
            {
                GoColor c = getColor(board, pointArg, pointListArg);
                if (c != GoColor.EMPTY)
                    showVariation(guiBoard, response, c.otherColor(), size);
            }
            break;
        default:
            break;
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private AnalyzeShow()
    {
    }

    private static GoColor getColor(Board board, GoPoint pointArg,
                                    Vector pointListArg)
    {
        GoColor color = GoColor.EMPTY;
        if (pointArg != null)
            color = board.getColor(pointArg);
        if (color != GoColor.EMPTY)
            return color;
        for (int i = 0; i < pointListArg.size(); ++i)
        {
            GoPoint point = (GoPoint)pointListArg.get(i);
            color = board.getColor(point);
            if (color != GoColor.EMPTY)
                break;
        }
        return color;
    }

    private static void showVariation(GuiBoard guiBoard, String response,
                                      GoColor color, int size)
    {
        Move moves[] = GtpUtils.parseVariation(response, color, size);
        guiBoard.showVariation(moves);
        guiBoard.repaint();
    }
}

//----------------------------------------------------------------------------
