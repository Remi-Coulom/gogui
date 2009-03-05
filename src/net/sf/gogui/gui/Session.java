// Session.java

package net.sf.gogui.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import net.sf.gogui.util.PrefUtil;
import net.sf.gogui.util.StringUtil;

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
        assert ! StringUtil.isEmpty(path);
        m_path = path;
    }

    public boolean isVisible(String name)
    {
        Preferences prefs = getNode(null);
        if (prefs == null)
            return false;
        return prefs.getBoolean("show-" + name, false);
    }

    public void restoreLocation(Window window, String name)
    {
        Preferences prefs = getNode(name);
        if (prefs == null)
            return;
        int x = prefs.getInt("x", Integer.MIN_VALUE);
        int y = prefs.getInt("y", Integer.MIN_VALUE);
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
            return;
        setLocationChecked(window, x, y);
    }

    public void restoreLocation(Window window, Window owner, String name)
    {
        int x = Integer.MIN_VALUE;
        int y = Integer.MIN_VALUE;
        Preferences prefs = getNode(name);
        if (prefs != null)
        {
            x = prefs.getInt("x", Integer.MIN_VALUE);
            y = prefs.getInt("y", Integer.MIN_VALUE);
        }
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
        {
            if (! window.isVisible())
                // use a platform-dependent default (setLocationByPlatform can
                // only be used, if window not already visible)
                window.setLocationByPlatform(true);
            return;
        }
        Point ownerLocation = owner.getLocation();
        setLocationChecked(window, x + ownerLocation.x,  y + ownerLocation.y);
    }

    public void restoreSize(Window window, String name)
    {
        Preferences prefs = getNode(name);
        if (prefs == null)
            return;
        int x = prefs.getInt("x", Integer.MIN_VALUE);
        int y = prefs.getInt("y", Integer.MIN_VALUE);
        int width = prefs.getInt("width", Integer.MIN_VALUE);
        int height = prefs.getInt("height", Integer.MIN_VALUE);
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE
            || width == Integer.MIN_VALUE || height == Integer.MIN_VALUE)
            return;
        setSizeChecked(window, width, height);
        // Restore location after size, because some window managers move a
        // window, if the size changes in a way that it would not be fully
        // visible
        setLocationChecked(window, x, y);
    }

    public void restoreSize(Window window, Window owner, String name)
    {
        int width = Integer.MIN_VALUE;
        int height = Integer.MIN_VALUE;
        Preferences prefs = getNode(name);
        if (prefs != null)
        {
            width = prefs.getInt("width", Integer.MIN_VALUE);
            height = prefs.getInt("height", Integer.MIN_VALUE);
        }
        if (width == Integer.MIN_VALUE || height == Integer.MIN_VALUE)
            return;
        setSizeChecked(window, width, height);
        // Restore location after size, because some window managers move a
        // window, if the size changes in a way that it would not be fully
        // visible
        restoreLocation(window, owner, name);
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
        if (isFrameSpecialMode(window) || ! window.isVisible())
            return;
        saveLocation(window, name);
        saveWidthHeight(window, name);
    }

    public void saveSize(Window window, Window owner, String name)
    {
        if (isFrameSpecialMode(window) || ! window.isVisible())
            return;
        saveLocation(window, owner, name);
        saveWidthHeight(window, name);
    }

    public void saveVisible(Window window, String name)
    {
        boolean isVisible = (window != null && window.isVisible());
        Preferences prefs = createNode(null);
        if (prefs == null)
            return;
        prefs.putBoolean("show-" + name, isVisible);
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
        if (name == null)
            return m_path;
        else
            return m_path + "/" + name;
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

    private void setSizeChecked(Window window, int width, int height)
    {
        Dimension screenSize = getScreenSize();
        width = Math.min(width, screenSize.width);
        height = Math.min(height, screenSize.height);
        window.setSize(width, height);
        window.validate();
    }
}
