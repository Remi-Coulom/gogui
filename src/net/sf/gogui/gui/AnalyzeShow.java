//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.util.StringUtil;

/** Show response to an AnalyzeCommand in the GUI. */
public final class AnalyzeShow
{
    /** Parse analyze command response and display it on the board. */
    public static void show(AnalyzeCommand command, GuiBoard guiBoard,
                            StatusBar statusBar, ConstBoard board,
                            String response)
        throws GtpError
    {
        GoPoint pointArg = command.getPointArg();
        PointList pointListArg = command.getPointListArg();
        guiBoard.clearAllSelect();
        GuiBoardUtil.setSelect(guiBoard, pointListArg, true);
        if (pointArg != null)
            guiBoard.setSelect(pointArg, true);
        int type = command.getType();
        int size = board.getSize();
        switch (type)
        {
        case AnalyzeCommand.BWBOARD:
            {
                String b[][] = GtpUtil.parseStringBoard(response, size);
                GuiBoardUtil.showBWBoard(guiBoard, b);
            }
            break;
        case AnalyzeCommand.CBOARD:
            {
                String colors[][] = GtpUtil.parseStringBoard(response, size);
                GuiBoardUtil.showColorBoard(guiBoard, colors);
            }
            break;
        case AnalyzeCommand.DBOARD:
            {
                double b[][] = GtpUtil.parseDoubleBoard(response, size);
                GuiBoardUtil.showDoubleBoard(guiBoard, b);
            }
            break;
        case AnalyzeCommand.GFX:
            {
                showGfx(response, guiBoard, statusBar);
            }
            break;
        case AnalyzeCommand.PLIST:
            {
                PointList points = GtpUtil.parsePointList(response, size);
                GuiBoardUtil.showPointList(guiBoard, points);
            }
            break;
        case AnalyzeCommand.HPSTRING:
        case AnalyzeCommand.PSTRING:
            {
                PointList points = GtpUtil.parsePointString(response, size);
                GuiBoardUtil.showPointList(guiBoard, points);
            }
            break;
        case AnalyzeCommand.PSPAIRS:
            {
                PointList pointList = new PointList(32);
                ArrayList stringList = new ArrayList(32);
                GtpUtil.parsePointStringList(response, pointList, stringList,
                                              size);
                GuiBoardUtil.showPointStringList(guiBoard, pointList,
                                                  stringList);
            }
            break;
        case AnalyzeCommand.SBOARD:
            {
                String b[][] = GtpUtil.parseStringBoard(response, size);
                GuiBoardUtil.showStringBoard(guiBoard, b);
            }
            break;
        case AnalyzeCommand.VAR:
            {                    
                showVariation(guiBoard, response, board.getToMove());
            }
            break;
        case AnalyzeCommand.VARB:
            {
                showVariation(guiBoard, response, GoColor.BLACK);
            }
            break;
        case AnalyzeCommand.VARC:
            {
                showVariation(guiBoard, response, command.getColorArg());
            }
            break;
        case AnalyzeCommand.VARW:
            {
                showVariation(guiBoard, response, GoColor.WHITE);
            }
            break;
        case AnalyzeCommand.VARP:
            {
                GoColor c = getColor(board, pointArg, pointListArg);
                if (c != GoColor.EMPTY)
                    showVariation(guiBoard, response, c);
            }
            break;
        case AnalyzeCommand.VARPO:
            {
                GoColor c = getColor(board, pointArg, pointListArg);
                if (c != GoColor.EMPTY)
                    showVariation(guiBoard, response, c.otherColor());
            }
            break;
        default:
            break;
        }
    }

    /** Parse gfx analyze command response and display it on the board. */
    public static void showGfx(String response, GuiBoard guiBoard,
                               StatusBar statusBar)
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
            showGfxLine(line, guiBoard, statusBar);
        }
    }

    public static void showGfxCircle(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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

    public static void showGfxColor(String[] arg, GuiBoard guiBoard)
    {
        if (arg.length < 2)
            return;
        int size = guiBoard.getBoardSize();
        Color color = GuiBoardUtil.getColor(arg[1]);
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

    public static void showGfxInfluence(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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

    public static void showGfxLabel(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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
    
    /** Parse gfx analyze command response line and display it on the board. */
    public static void showGfxLine(String line, GuiBoard guiBoard,
                                   StatusBar statusBar)
    {
        String[] args = StringUtil.splitArguments(line);
        if (args.length == 0)
            return;
        String cmd = args[0].toUpperCase(Locale.ENGLISH);
        if (cmd.equals("BLACK"))
            showGfxTerritory(args, GoColor.BLACK, guiBoard);
        else if (cmd.equals("CIRCLE"))
            showGfxCircle(args, guiBoard);
        else if (cmd.equals("CLEAR"))
            guiBoard.clearAll();
        else if (cmd.equals("COLOR"))
            showGfxColor(args, guiBoard);
        else if (cmd.equals("INFLUENCE"))
            showGfxInfluence(args, guiBoard);
        else if (cmd.equals("LABEL"))
            showGfxLabel(args, guiBoard);
        else if (cmd.equals("MARK"))
            showGfxMark(args, guiBoard);
        else if (cmd.equals("PROGRESS"))
            showGfxProgress(args, statusBar);
        else if (cmd.equals("SQUARE"))
            showGfxSquare(args, guiBoard);
        else if (cmd.equals("TEXT"))
        {
            line = line.trim();
            int pos = line.indexOf(' ');
            if (pos > 0)
                statusBar.setText(line.substring(pos + 1));
        }
        else if (cmd.equals("TRIANGLE"))
            showGfxTriangle(args, guiBoard);
        else if (cmd.equals("VAR"))
            showGfxVariation(args, guiBoard);
        else if (cmd.equals("WHITE"))
            showGfxTerritory(args, GoColor.WHITE, guiBoard);
    }

    public static void showGfxMark(String[] args, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        for (int i = 1; i < args.length; ++i)
        {
            try
            {
                GoPoint point = GoPoint.parsePoint(args[i], size);
                if (point == null)
                    continue;
                guiBoard.setMark(point, true);
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
        }
    }

    public static void showGfxProgress(String[] args, StatusBar statusBar)
    {
        if (args.length < 1)
            return;
        try
        {
            int percent = Integer.parseInt(args[1]);
            if (percent < 0)
                percent = 0;
            if (percent > 100)
                percent = 100;
            statusBar.setProgress(percent);
        }
        catch (NumberFormatException e)
        {
        }
    }

    public static void showGfxSquare(String[] args, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        for (int i = 1; i < args.length; ++i)
        {
            try
            {
                GoPoint point = GoPoint.parsePoint(args[i], size);
                if (point == null)
                    continue;
                guiBoard.setMarkSquare(point, true);
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
        }
    }

    public static void showGfxTriangle(String[] args, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        for (int i = 1; i < args.length; ++i)
        {
            try
            {
                GoPoint point = GoPoint.parsePoint(args[i], size);
                if (point == null)
                    continue;
                guiBoard.setMarkTriangle(point, true);
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
        }
    }

    public static void showGfxTerritory(String[] args, GoColor color,
                                        GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        for (int i = 1; i < args.length; ++i)
        {
            try
            {
                GoPoint point = GoPoint.parsePoint(args[i], size);
                if (point == null)
                    continue;
                guiBoard.setTerritory(point, color);
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
        }
    }

    public static void showGfxVariation(String[] args, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        int n = 0;
        for (int i = 1; i < args.length; i += 2)
        {
            try
            {
                GoColor color;
                if (args[i].equalsIgnoreCase("b"))
                    color = GoColor.BLACK;
                else if (args[i].equalsIgnoreCase("w"))
                    color = GoColor.WHITE;
                else
                    break;
                if (i + 1 >= args.length)
                    break;
                GoPoint point = GoPoint.parsePoint(args[i + 1], size);
                ++n;
                if (point != null)
                {
                    guiBoard.setColor(point, color);
                    guiBoard.setLabel(point, Integer.toString(n));
                }
            }
            catch (GoPoint.InvalidPoint e)
            {
            }
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private AnalyzeShow()
    {
    }

    private static GoColor getColor(ConstBoard board, GoPoint pointArg,
                                    ConstPointList pointListArg)
    {
        GoColor color = GoColor.EMPTY;
        if (pointArg != null)
            color = board.getColor(pointArg);
        if (color != GoColor.EMPTY)
            return color;
        for (int i = 0; i < pointListArg.size(); ++i)
        {
            GoPoint point = pointListArg.get(i);
            color = board.getColor(point);
            if (color != GoColor.EMPTY)
                break;
        }
        return color;
    }

    private static void showVariation(GuiBoard guiBoard, String response,
                                      GoColor color)
    {
        int size = guiBoard.getBoardSize();
        Move moves[] = GtpUtil.parseVariation(response, color, size);
        GuiBoardUtil.showVariation(guiBoard, moves);
    }
}

