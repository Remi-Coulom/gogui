//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class FileUtils
{
    public static String getExtension(File f)
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');       
        if (i > 0 &&  i < s.length() - 1)
            ext = s.substring(i + 1);
        return ext;
    }

    /** Check for extension (case-insensitive) */
    public static boolean hasExtension(File f, String extension)
    {
        String ext = getExtension(f);
        if (ext == null)
            return false;
        return (ext.toLowerCase().equals(extension.toLowerCase()));
    }

    /** Replace extension in file name.
        If the file does not have the extension oldExtension,
        the extension will not be replaced but the new extension will be
        appended.
    */
    public static String replaceExtension(File file, String oldExtension,
                                          String newExtension)
    {
        String name = file.toString();
        if (hasExtension(file, oldExtension))
        {
            int index = name.lastIndexOf(".");
            assert(index >= 0);
            return name.substring(0, index) + "." + newExtension;
        }
        return name + "." + newExtension;
    }
}

//-----------------------------------------------------------------------------
