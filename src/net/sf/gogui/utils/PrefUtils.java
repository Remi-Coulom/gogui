//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

//----------------------------------------------------------------------------

/** Utils for using java.util.prefs */
public class PrefUtils
{
    /** Get node for package and path, create if not already existing.
        @param c A class in the package.
        @param path The path name of the node relative to the package.
        @return The node 
    */
    public static Preferences createNode(Class c, String path)
    {
        Preferences prefs = Preferences.userNodeForPackage(c);
        return prefs.node(path);
    }

    /** Get node for package and path, return null if not already existing.
        @param c A class in the package.
        @param path The path name of the node relative to the package.
        @return The node or null, if node does not exist or failure in the
        backing store.
    */
    public static Preferences getNode(Class c, String path)
    {
        Preferences prefs = Preferences.userNodeForPackage(c);
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


    /** Make constructor unavailable; class is for namespace only. */
    private PrefUtils()
    {
    }
}

//----------------------------------------------------------------------------
