//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.io.File;
import java.io.IOException;

//----------------------------------------------------------------------------

/** Static file utility functions. */
public class FileUtils
{
    /** Return the file extension of a file name.
        @return File extension or null if file name has no extension.
    */
    public static String getExtension(File f)
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');       
        if (i > 0 &&  i < s.length() - 1)
            ext = s.substring(i + 1);
        return ext;
    }

    /** Relative path from file1 to file2. */
    public static String getRelativePath(File file1, File file2)
        throws IOException
    {
        file1 = file1.getCanonicalFile();
        file2 = file2.getCanonicalFile();
        if (file1.isFile())
            file1 = file1.getParentFile();
        if (file2.isFile())
            file2 = file2.getParentFile();
        char sep = File.separatorChar;
        String[] dir1 = StringUtils.split(file1.toString(), sep);
        String[] dir2 = StringUtils.split(file2.toString(), sep);
        int i = 0;
        while (i < dir1.length && i < dir2.length && dir1[i].equals(dir2[i]))
            ++i;
        StringBuffer result = new StringBuffer();
        for (int j = i; j < dir1.length; ++j)
        {
            result.append("..");
            result.append(sep);
        }
        for (int j = i; j < dir2.length; ++j)
        {
            result.append(dir2[i]);
            result.append(sep);
        }
        return result.toString();
    }

    /** Check for extension (case-insensitive). */
    public static boolean hasExtension(File f, String extension)
    {
        String ext = getExtension(f);
        if (ext == null)
            return false;
        return (ext.toLowerCase().equals(extension.toLowerCase()));
    }

    /** Remove extension in file name.
        If the file does not have the extension oldExtension,
        the extension will not be removed.
    */
    public static String removeExtension(File file, String oldExtension)
    {
        String name = file.toString();
        if (hasExtension(file, oldExtension))
        {
            int index = name.lastIndexOf(".");
            assert(index >= 0);
            return name.substring(0, index);
        }
        return name;
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

//----------------------------------------------------------------------------
