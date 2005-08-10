//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Show response to an AnalyzeCommand in the GUI. */
public class AnalyzeShow
{
    public static void show(AnalyzeCommand command, GuiBoard guiBoard,
                            Board board, String response) throws GtpError
    {
        GoPoint pointArg = command.getPointArg();
        ArrayList pointListArg = command.getPointListArg();
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
                String colors[][] =
                    GtpUtils.parseStringBoard(response, title, size);
                GuiBoardUtils.showColorBoard(guiBoard, colors, board);
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
        case AnalyzeCommand.GFX:
            {
                showGfx(response, guiBoard, board.getSize());
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
                ArrayList pointList = new ArrayList(32);
                ArrayList stringList = new ArrayList(32);
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
                                    ArrayList pointListArg)
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

    public static void showGfx(String response, GuiBoard guiBoard, int size)
        throws GtpError
    {
        BufferedReader reader
            = new BufferedReader(new StringReader(response));
        while (true)
        {
            String line;
            try
            {
                line = reader.readLine();
            }
            catch (IOException e)
            {
                assert(false);
                break;
            }
            if (line == null)
                break;
            String[] arg = StringUtils.tokenize(line);
            if (arg.length == 0)
                continue;
            String cmd = arg[0].toUpperCase();
            if (cmd.equals("BLACK"))
            {
                for (int i = 1; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setTerritory(point, GoColor.BLACK);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("CIRCLE"))
            {
                for (int i = 1; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setMarkCircle(point, true);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("COLOR"))
            {
                if (arg.length < 2)
                    continue;
                Color color = GuiBoardUtils.getColor(arg[1]);
                for (int i = 2; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setFieldBackground(point, color);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("LABEL"))
            {
                for (int i = 1; i < arg.length; i += 2)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (i + 1 >= arg.length)
                            break;
                        if (point == null)
                            continue;
                        guiBoard.setLabel(point, arg[i + 1]);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("MARK"))
            {
                for (int i = 1; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setMark(point, true);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("SQUARE"))
            {
                for (int i = 1; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setMarkSquare(point, true);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("TRIANGLE"))
            {
                for (int i = 1; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setMarkTriangle(point, true);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
            else if (cmd.equals("WHITE"))
            {
                for (int i = 1; i < arg.length; ++i)
                {
                    try
                    {
                        GoPoint point = GoPoint.parsePoint(arg[i], size);
                        if (point == null)
                            continue;
                        guiBoard.setTerritory(point, GoColor.WHITE);
                    }
                    catch (GoPoint.InvalidPoint e)
                    {
                    }
                }
            }
        }
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
