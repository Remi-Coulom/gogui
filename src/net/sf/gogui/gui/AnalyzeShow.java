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
public final class AnalyzeShow
{
    public static void show(AnalyzeCommand command, GuiBoard guiBoard,
                            Board board, String response) throws GtpError
    {
        GoPoint pointArg = command.getPointArg();
        ArrayList pointListArg = command.getPointListArg();
        guiBoard.clearAllSelect();
        GuiBoardUtils.updateFromGoBoard(guiBoard, board, false);
        GuiBoardUtils.setSelect(guiBoard, pointListArg, true);
        if (pointArg != null)
            guiBoard.setSelect(pointArg, true);
        int type = command.getType();
        int size = board.getSize();
        switch (type)
        {
        case AnalyzeCommand.BWBOARD:
            {
                String b[][] = GtpUtils.parseStringBoard(response, size);
                GuiBoardUtils.showBWBoard(guiBoard, b);
            }
            break;
        case AnalyzeCommand.CBOARD:
            {
                String colors[][] = GtpUtils.parseStringBoard(response, size);
                GuiBoardUtils.showColorBoard(guiBoard, colors);
            }
            break;
        case AnalyzeCommand.DBOARD:
            {
                double b[][] = GtpUtils.parseDoubleBoard(response, size);
                GuiBoardUtils.showDoubleBoard(guiBoard, b);
            }
            break;
        case AnalyzeCommand.GFX:
            {
                showGfx(response, guiBoard, board.getSize());
            }
            break;
        case AnalyzeCommand.PLIST:
            {
                GoPoint list[] = GtpUtils.parsePointList(response, size);
                GuiBoardUtils.showPointList(guiBoard, list);
            }
            break;
        case AnalyzeCommand.HPSTRING:
        case AnalyzeCommand.PSTRING:
            {
                GoPoint list[] = GtpUtils.parsePointString(response, size);
                GuiBoardUtils.showPointList(guiBoard, list);
            }
            break;
        case AnalyzeCommand.PSPAIRS:
            {
                ArrayList pointList = new ArrayList(32);
                ArrayList stringList = new ArrayList(32);
                GtpUtils.parsePointStringList(response, pointList, stringList,
                                              size);
                GuiBoardUtils.showPointStringList(guiBoard, pointList,
                                                  stringList);
            }
            break;
        case AnalyzeCommand.SBOARD:
            {
                String b[][] = GtpUtils.parseStringBoard(response, size);
                GuiBoardUtils.showStringBoard(guiBoard, b);
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
            showGfxLine(line, guiBoard, size);
        }
    }

    public static void showGfxCircle(String[] arg, GuiBoard guiBoard,
                                     int size)
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

    public static void showGfxColor(String[] arg, GuiBoard guiBoard, int size)
    {
        if (arg.length < 2)
            return;
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

    public static void showGfxInfluence(String[] arg, GuiBoard guiBoard,
                                        int size)
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
                double value = Double.parseDouble(arg[i + 1]);
                guiBoard.setInfluence(point, value);
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    public static void showGfxLabel(String[] arg, GuiBoard guiBoard, int size)
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
    
    public static void showGfxLine(String line, GuiBoard guiBoard, int size)
    {
        String[] arg = StringUtils.splitArguments(line);
        if (arg.length == 0)
            return;
        String cmd = arg[0].toUpperCase();
        if (cmd.equals("BLACK"))
            showGfxTerritory(arg, GoColor.BLACK, guiBoard, size);
        else if (cmd.equals("CIRCLE"))
            showGfxCircle(arg, guiBoard, size);
        else if (cmd.equals("COLOR"))
            showGfxColor(arg, guiBoard, size);
        else if (cmd.equals("INFLUENCE"))
            showGfxInfluence(arg, guiBoard, size);
        else if (cmd.equals("LABEL"))
            showGfxLabel(arg, guiBoard, size);
        else if (cmd.equals("MARK"))
            showGfxMark(arg, guiBoard, size);
        else if (cmd.equals("SQUARE"))
            showGfxSquare(arg, guiBoard, size);
        else if (cmd.equals("TRIANGLE"))
            showGfxTriangle(arg, guiBoard, size);
        else if (cmd.equals("WHITE"))
            showGfxTerritory(arg, GoColor.WHITE, guiBoard, size);
    }

    public static void showGfxMark(String[] arg, GuiBoard guiBoard, int size)
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

    public static void showGfxSquare(String[] arg, GuiBoard guiBoard,
                                     int size)
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

    public static void showGfxTriangle(String[] arg, GuiBoard guiBoard,
                                       int size)
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

    public static void showGfxTerritory(String[] arg, GoColor color,
                                        GuiBoard guiBoard, int size)
    {
        for (int i = 1; i < arg.length; ++i)
        {
            try
            {
                GoPoint point = GoPoint.parsePoint(arg[i], size);
                if (point == null)
                    continue;
                guiBoard.setTerritory(point, color);
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
        }
    }

    private static void showVariation(GuiBoard guiBoard, String response,
                                      GoColor color, int size)
    {
        Move moves[] = GtpUtils.parseVariation(response, color, size);
        GuiBoardUtils.showVariation(guiBoard, moves);
    }
}

//----------------------------------------------------------------------------
