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
}

//-----------------------------------------------------------------------------
