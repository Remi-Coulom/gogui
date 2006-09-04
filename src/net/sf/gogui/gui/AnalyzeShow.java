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
import java.util.Locale;
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
    /** Parse analyze command response and display it on the board.
        @return Text for status bar (from gfx TEXT) or null
    */
    public static String show(AnalyzeCommand command, GuiBoard guiBoard,
                              Board board, String response) throws GtpError
    {
        GoPoint pointArg = command.getPointArg();
        ArrayList pointListArg = command.getPointListArg();
        guiBoard.clearAllSelect();
        GuiBoardUtils.setSelect(guiBoard, pointListArg, true);
        if (pointArg != null)
            guiBoard.setSelect(pointArg, true);
        int type = command.getType();
        int size = board.getSize();
        String statusText = null;
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
                statusText = showGfx(response, guiBoard);
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
        return statusText;
    }

    /** Parse gfx analyze command response and display it on the board.
        @return Text for status bar (from gfx TEXT) or null
    */
    public static String showGfx(String response, GuiBoard guiBoard)
    {
        BufferedReader reader
            = new BufferedReader(new StringReader(response));
        String statusText = null;
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
            String text = showGfxLine(line, guiBoard);
            if (text != null)
                statusText = text;
        }
        return statusText;
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
    
    /** Parse gfx analyze command response line and display it on the board.
        @return Text for status bar (from gfx TEXT) or null
    */
    public static String showGfxLine(String line, GuiBoard guiBoard)
    {
        String[] arg = StringUtils.splitArguments(line);
        if (arg.length == 0)
            return null;
        String statusText = null;
        String cmd = arg[0].toUpperCase(Locale.ENGLISH);
        if (cmd.equals("BLACK"))
            showGfxTerritory(arg, GoColor.BLACK, guiBoard);
        else if (cmd.equals("CIRCLE"))
            showGfxCircle(arg, guiBoard);
        else if (cmd.equals("CLEAR"))
            guiBoard.clearAll();
        else if (cmd.equals("COLOR"))
            showGfxColor(arg, guiBoard);
        else if (cmd.equals("INFLUENCE"))
            showGfxInfluence(arg, guiBoard);
        else if (cmd.equals("LABEL"))
            showGfxLabel(arg, guiBoard);
        else if (cmd.equals("MARK"))
            showGfxMark(arg, guiBoard);
        else if (cmd.equals("SQUARE"))
            showGfxSquare(arg, guiBoard);
        else if (cmd.equals("TEXT"))
        {
            line = line.trim();
            int pos = line.indexOf(' ');
            if (pos > 0)
                statusText = line.substring(pos + 1);
        }
        else if (cmd.equals("TRIANGLE"))
            showGfxTriangle(arg, guiBoard);
        else if (cmd.equals("VAR"))
            showGfxVariation(arg, guiBoard);
        else if (cmd.equals("WHITE"))
            showGfxTerritory(arg, GoColor.WHITE, guiBoard);
        return statusText;
    }

    public static void showGfxMark(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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

    public static void showGfxSquare(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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

    public static void showGfxTriangle(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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
                                        GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
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

    public static void showGfxVariation(String[] arg, GuiBoard guiBoard)
    {
        int size = guiBoard.getBoardSize();
        int n = 0;
        for (int i = 1; i < arg.length; i += 2)
        {
            try
            {
                GoColor color;
                if (arg[i].equalsIgnoreCase("b"))
                    color = GoColor.BLACK;
                else if (arg[i].equalsIgnoreCase("w"))
                    color = GoColor.WHITE;
                else
                    break;
                if (i + 1 >= arg.length)
                    break;
                GoPoint point = GoPoint.parsePoint(arg[i + 1], size);
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

    private static void showVariation(GuiBoard guiBoard, String response,
                                      GoColor color)
    {
        int size = guiBoard.getBoardSize();
        Move moves[] = GtpUtils.parseVariation(response, color, size);
        GuiBoardUtils.showVariation(guiBoard, moves);
    }
}

//----------------------------------------------------------------------------
