//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.PrefUtils;

//----------------------------------------------------------------------------

/** Link to a position in a game file. */
public final class Bookmark
{
    public Bookmark(Bookmark bookmark)
    {
        copyFrom(bookmark);
    }

    public Bookmark(String name, File file, int move, String variation)
    {
        init(name, file, move, variation);
    }

    public Bookmark(File file, int move, String variation)
    {
        String name = "";
        if (file != null)            
        {
            File fileNoExt = new File(FileUtils.removeExtension(file, "sgf"));
            name = fileNoExt.getName();
            boolean hasVariation = ! variation.trim().equals("");
            if (move > 0 || hasVariation)
            {
                name = name + " (";
                if (hasVariation)
                {
                    name = name + variation;
                    if (move > 0)
                        name = name + "/";
                }
                if (move > 0)
                    name = name + move;
                name = name + ")";
            }
            file = file.getAbsoluteFile();
        }
        init(name, file, move, variation);
    }

    public void copyFrom(Bookmark bookmark)
    {
        init(bookmark.m_name, bookmark.m_file, bookmark.m_move,
             bookmark.m_variation);
    }

    public static ArrayList load()
    {
        ArrayList bookmarks = new ArrayList();
        Preferences prefs = PrefUtils.getNode(Bookmark.class, "bookmark");
        if (prefs == null)
            return bookmarks;
        int size = prefs.getInt("size", 0);
        for (int i = 0; i < size; ++i)
        {
            prefs = PrefUtils.getNode(Bookmark.class, "bookmark/" + i);
            if (prefs == null)
                break;
            String name = prefs.get("name", null);
            if (name == null)
                break;
            String fileName = prefs.get("file", "");
            int move = prefs.getInt("move", 0);
            String variation = prefs.get("variation", "");
            Bookmark b = new Bookmark(name, new File(fileName), move,
                                      variation);
            bookmarks.add(b);
        }
        return bookmarks;
    }

    public static void save(ArrayList bookmarks)
    {
        Preferences prefs = PrefUtils.createNode(Bookmark.class, "bookmark");
        if (prefs == null)
            return;
        prefs.putInt("size", bookmarks.size());
        for (int i = 0; i < bookmarks.size(); ++i)
        {
            prefs = PrefUtils.createNode(Bookmark.class, "bookmark/" + i);
            if (prefs == null)
                break;
            Bookmark b = (Bookmark)bookmarks.get(i);
            prefs.put("name", b.m_name);
            prefs.put("file", b.m_file.toString());
            prefs.put("move", Integer.toString(b.m_move));
            prefs.put("variation" , b.m_variation);
        }
    }

    public int m_move;

    public File m_file;

    public String m_name;

    public String m_variation;

    private void init(String name, File file, int move, String variation)
    {
        assert(move >= 0);
        m_file = file;
        m_move = move;
        m_variation = variation.trim();
        m_name = name;
    }
}

//----------------------------------------------------------------------------
