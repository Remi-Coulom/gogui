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
import java.util.Properties;
import net.sf.gogui.utils.FileUtils;

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
        }
        file = file.getAbsoluteFile();
        init(name, file, move, variation);
    }

    public void copyFrom(Bookmark bookmark)
    {
        init(bookmark.m_name, bookmark.m_file, bookmark.m_move,
             bookmark.m_variation);
    }

    public static ArrayList load(File file)
    {
        ArrayList bookmarks = new ArrayList();
        Properties props = new Properties();
        try
        {
            props.load(new FileInputStream(file));
        }
        catch (IOException e)
        {
            return bookmarks;
        }
        for (int i = 0; ; ++i)
        {
            String name = props.getProperty("name_" + i);
            if (name == null)
                break;
            String fileName = props.getProperty("file_" + i, "");
            int move;
            try
            {
                move = Integer.parseInt(props.getProperty("move_" + i, ""));
            }
            catch (NumberFormatException e)
            {
                move = 0;
            }
            String variation = props.getProperty("variation_" + i, "");
            Bookmark b = new Bookmark(name, new File(fileName), move,
                                      variation);
            bookmarks.add(b);
        }
        return bookmarks;
    }

    public static void save(ArrayList bookmarks, File file)
    {
        Properties props = new Properties();
        for (int i = 0; i < bookmarks.size(); ++i)
        {
            Bookmark b = (Bookmark)bookmarks.get(i);
            props.setProperty("name_" + i, b.m_name);
            props.setProperty("file_" + i, b.m_file.toString());
            props.setProperty("move_" + i, Integer.toString(b.m_move));
            props.setProperty("variation_" + i , b.m_variation);
        }
        try
        {
            FileOutputStream out = new FileOutputStream(file);
            props.store(out, null);
            out.close();
        }
        catch (IOException e)
        {
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
