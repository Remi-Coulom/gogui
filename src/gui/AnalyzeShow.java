//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.util.Vector;
import gtp.GtpError;
import gtp.GtpUtils;

//----------------------------------------------------------------------------

public class AnalyzeShow
{
    public static void show(AnalyzeCommand command, gui.Board guiBoard,
                            go.Board board, String response) throws GtpError
    {
        go.Point pointArg = command.getPointArg();
        Vector pointListArg = command.getPointListArg();
        guiBoard.clearAllSelect();
        for (int i = 0; i < pointListArg.size(); ++i)
            guiBoard.setSelect((go.Point)pointListArg.get(i), true);
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
                go.Point list[] =
                    GtpUtils.parsePointList(response, size);
                guiBoard.showPointList(list);
                guiBoard.repaint();
            }
            break;
        case AnalyzeCommand.HPSTRING:
        case AnalyzeCommand.PSTRING:
            {
                go.Point list[] = GtpUtils.parsePointString(response, size);
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
                showVariation(guiBoard, response, go.Color.BLACK, size);
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
                showVariation(guiBoard, response, go.Color.WHITE, size);
            }
            break;
        case AnalyzeCommand.VARP:
            {
                go.Color c = getColor(board, pointArg, pointListArg);
                if (c != go.Color.EMPTY)
                    showVariation(guiBoard, response, c, size);
            }
            break;
        case AnalyzeCommand.VARPO:
            {
                go.Color c = getColor(board, pointArg, pointListArg);
                if (c != go.Color.EMPTY)
                if (c != go.Color.EMPTY)
                    showVariation(guiBoard, response, c.otherColor(), size);
            }
            break;
        }
    }

    private static go.Color getColor(go.Board board, go.Point pointArg,
                                     Vector pointListArg)
    {
        go.Color color = go.Color.EMPTY;
        if (pointArg != null)
            color = board.getColor(pointArg);
        if (color != go.Color.EMPTY)
            return color;
        for (int i = 0; i < pointListArg.size(); ++i)
        {
            go.Point point = (go.Point)pointListArg.get(i);
            color = board.getColor(point);
            if (color != go.Color.EMPTY)
                break;
        }
        return color;
    }

    private static void showVariation(gui.Board guiBoard, String response,
                                      go.Color color, int size)
    {
        go.Move moves[] = GtpUtils.parseVariation(response, color, size);
        guiBoard.showVariation(moves);
        guiBoard.repaint();
    }
}

//----------------------------------------------------------------------------
