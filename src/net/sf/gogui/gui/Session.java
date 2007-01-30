//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import net.sf.gogui.util.PrefUtil;

/** Utilities for saving and restoring windows between session.
    Window sizes and locations are saved separately for different Go board
    sizes.
*/
public final class Session
{
    /** Constructor.
        @param path Absolute path for saving the preferences using
        java.util.prefs.
    */
    public Session(String path)
    {
        m_path = path;
    }

    public boolean isVisible(String name)
    {
        Preferences prefs = getNode(name);
        if (prefs == null)
            return false;
        return prefs.getBoolean("show", false);
    }

    public void restoreLocation(Window window, String name)
    {
        Preferences prefs = getNode(name);
        if (prefs == null)
            return;
        int x = prefs.getInt("x", -1);
        int y = prefs.getInt("y", -1);
        if (x == -1 || y == -1)
            return;
        setLocationChecked(window, x, y);
    }

    public void restoreSize(Window window, String name)
    {
        Preferences prefs = getNode(name);
        if (prefs == null)
            return;
        int x = prefs.getInt("x", -1);
        int y = prefs.getInt("y", -1);
        int width = prefs.getInt("width", -1);
        int height = prefs.getInt("height", -1);
        if (x == -1 || y == -1 || width == -1 || height == -1)
            return;
        setSizeChecked(window, x, y, width, height);
    }

    public void restoreSize(Window window, Window owner, String name)
    {
        int x = -1;
        int y = -1;
        int width = -1;
        int height = -1;
        Preferences prefs = getNode(name);
        if (prefs != null)
        {
            x = prefs.getInt("x", -1);
            y = prefs.getInt("y", -1);
            width = prefs.getInt("width", -1);
            height = prefs.getInt("height", -1);
        }
        if (x == -1 || y == -1 || width == -1 || height == -1)
        {
            window.setLocationRelativeTo(owner);
            return;
        }
        Point ownerLocation = owner.getLocation();
        setSizeChecked(window, x + ownerLocation.x,  y + ownerLocation.y,
                       width, height);
    }

    public void saveLocation(Window window, String name)
    {
        if (isFrameSpecialMode(window))
            return;
        Preferences prefs = createNode(name);
        if (prefs == null)
            return;
        Point location = window.getLocation();
        prefs.putInt("x", location.x);
        prefs.putInt("y", location.y);
    }

    public void saveLocation(Window window, Window owner, String name)
    {
        if (isFrameSpecialMode(window))
            return;
        Preferences prefs = createNode(name);
        if (prefs == null)
            return;
        Point location = window.getLocation();
        Point ownerLocation = owner.getLocation();
        prefs.putInt("x", location.x - ownerLocation.x);
        prefs.putInt("y", location.y - ownerLocation.y);
    }

    public void saveSize(Window window, String name)
    {
        saveLocation(window, name);
        if (isFrameSpecialMode(window))
            return;
        saveWidthHeight(window, name);
    }

    public void saveSize(Window window, Window owner, String name)
    {
        saveLocation(window, owner, name);
        if (isFrameSpecialMode(window))
            return;
        saveWidthHeight(window, name);
    }

    public void saveSizeAndVisible(Window window, Window owner, String name)
    {
        if (window != null)
            saveSize(window, owner, name);
        saveVisible(window, name);
    }

    public void saveVisible(Window window, String name)
    {
        boolean isVisible = (window != null && window.isVisible());
        Preferences prefs = createNode(name);
        if (prefs == null)
            return;
        prefs.putBoolean("show", isVisible);
    }

    private final String m_path;

    private Preferences createNode(String name)
    {
        return PrefUtil.createNode(getPath(name));
    }

    private Preferences getNode(String name)
    {
        return PrefUtil.getNode(getPath(name));
    }

    private String getPath(String name)
    {
        if (m_path == "")
            return "windows/" + name;
        else
            return m_path + "/windows/" + name;
    }

    private static Dimension getScreenSize()
    {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    private static boolean isFrameSpecialMode(Window window)
    {
        return (window instanceof JFrame
                && ! GuiUtil.isNormalSizeMode((JFrame)window));
    }

    private void saveWidthHeight(Window window, String name)
    {
        Preferences prefs = createNode(name);
        if (prefs == null)
            return;
        Dimension size = window.getSize();
        prefs.putInt("width", size.width);
        prefs.putInt("height", size.height);
    }

    private void setLocationChecked(Window window, int x, int y)
    {
        Dimension screenSize = getScreenSize();
        x = Math.max(0, x);
        if (x > screenSize.width)
            x = 0;
        y = Math.max(0, y);
        if (y > screenSize.height)
            y = 0;
        window.setLocation(x, y);
    }

    private void setSizeChecked(Window window, int x, int y, int width,
                                int height)
    {
        Dimension screenSize = getScreenSize();
        x = Math.max(0, x);
        if (x > screenSize.width)
            x = 0;
        y = Math.max(0, y);
        if (y > screenSize.height)
            y = 0;
        width = Math.min(width, screenSize.width);
        height = Math.min(height, screenSize.height);
        if (window instanceof GtpShell)
            // Workaround, see GtpShell.setFinalSize()
            ((GtpShell)window).setFinalSize(x, y, width, height);
        else
        {
            window.setBounds(x, y, width, height);
            window.validate();
        }
    }
}

