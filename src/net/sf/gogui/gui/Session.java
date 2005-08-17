//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import net.sf.gogui.utils.Preferences;
import net.sf.gogui.utils.StringUtils;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import javax.swing.JFrame;

//----------------------------------------------------------------------------

/** Utilities for saving and restoring windows between session.
    Window sizes and locations are saved separatly for different Go board
    sizes.
*/
public final class Session
{
    public static void restoreLocation(Window window, Preferences prefs,
                                       String name, int boardSize)
    {
        name = name + "-" + boardSize;
        if (! prefs.contains(name))
            return;
        String[] args = StringUtils.splitArguments(prefs.getString(name));
        if (args.length < 2)
            return;
        try
        {
            Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
            int x = Math.max(0, Integer.parseInt(args[0]));
            if (x > screenSize.width)
                x = 0;
            int y = Math.max(0, Integer.parseInt(args[1]));
            if (y > screenSize.height)
                y = 0;
            window.setLocation(x, y);
        }
        catch (NumberFormatException e)
        {
        }
    }

    public static void restoreSize(Window window, Preferences prefs,
                                   String name, int boardSize)
    {
        name = name + "-" + boardSize;
        if (! prefs.contains(name))
            return;
        String[] args = StringUtils.splitArguments(prefs.getString(name));
        if (args.length < 4)
            return;
        try
        {
            Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
            int x = Math.max(0, Integer.parseInt(args[0]));
            if (x > screenSize.width)
                x = 0;
            int y = Math.max(0, Integer.parseInt(args[1]));
            if (y > screenSize.height)
                y = 0;
            int width;
            int height;
            width = Integer.parseInt(args[2]);
            width = Math.min(width, screenSize.width);
            height = Integer.parseInt(args[3]);
            height = Math.min(height, screenSize.height);
            if (window instanceof GtpShell)
                ((GtpShell)window).setFinalSize(x, y, width, height);
            else
            {
                window.setBounds(x, y, width, height);
                window.validate();
            }
        }
        catch (NumberFormatException e)
        {
        }
    }

    public static void saveLocation(Window window, Preferences prefs,
                                    String name, int boardSize)
    {
        if (isFrameSpecialMode(window))
            return;
        name = name + "-" + boardSize;
        Point location = window.getLocation();
        String value = Integer.toString(location.x) + " " + location.y;
        prefs.setString(name, value);
    }

    public static void saveSize(Window window, Preferences prefs, String name,
                                int boardSize)
    {
        if (isFrameSpecialMode(window))
            return;
        name = name + "-" + boardSize;
        Point location = window.getLocation();
        Dimension size = window.getSize();
        String value = Integer.toString(location.x) + " " + location.y
            + " " + size.width + " " + size.height;
        prefs.setString(name, value);
    }

    public static void saveSizeAndVisible(Window window, Preferences prefs,
                                          String name, int boardSize)
    {
        if (window != null)
            saveSize(window, prefs, "window-" + name, boardSize);
        boolean isVisible = (window != null && window.isVisible());
        prefs.setBool("show-" + name, isVisible);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Session()
    {
    }

    private static boolean isFrameSpecialMode(Window window)
    {
        return (window instanceof JFrame
                && ! GuiUtils.isNormalSizeMode((JFrame)window));
        
    }
}

//----------------------------------------------------------------------------
