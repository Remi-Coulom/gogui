// PrefUtil.java

package net.sf.gogui.util;

import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/** Utils for using java.util.prefs package. */
public final class PrefUtil
{
    /** Get node path, create if not already existing.
        @param path The absolute path name of the node.
        @return The node */
    public static Preferences createNode(String path)
    {
        assert ! path.startsWith("/");
        return Preferences.userRoot().node(path);
    }

    /** Get a list of strings from preferences.
        The list is stored as a size property end element_N properties with
        N being the element index.
        @param path The absolute path name of the node.
        @return The list of strings. */
    public static ArrayList<String> getList(String path)
    {
        Preferences prefs = getNode(path);
        if (prefs == null)
            return new ArrayList<String>();
        int size = prefs.getInt("size", 0);
        if (size <= 0)
            return new ArrayList<String>();
        ArrayList<String> result = new ArrayList<String>(size);
        for (int i = 0; i < size; ++i)
        {
            String element = prefs.get("element_" + i, null);
            if (element == null)
                // Should not happen
                break;
            result.add(element);
        }
        return result;
    }

    /** Get node for package and path, return null if not already existing.
        @param path The absolute path name of the node.
        @return The node or null, if node does not exist or failure in the
        backing store. */
    public static Preferences getNode(String path)
    {
        assert ! path.startsWith("/");
        Preferences prefs = Preferences.userRoot();
        try
        {
            if (! prefs.nodeExists(path))
                return null;
        }
        catch (BackingStoreException e)
        {
            return null;
        }
        return prefs.node(path);
    }

    /** Put a list of strings to preferences.
        The list is stored as a size property end element_N properties with
        N being the element index.
        @param path The absolute path name of the node.
        @param list The list of strings. */
    public static void putList(String path, ArrayList<String> list)
    {
        Preferences prefs = createNode(path);
        if (prefs == null)
            return;
        prefs.putInt("size", list.size());
        for (int i = 0; i < list.size(); ++i)
            prefs.put("element_" + i, list.get(i));
    }

    /** Make constructor unavailable; class is for namespace only. */
    private PrefUtil()
    {
    }
}
