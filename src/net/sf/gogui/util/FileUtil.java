// FileUtil.java

package net.sf.gogui.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/** Static file utility functions. */
public final class FileUtil
{
    /** Return the file extension of a file name.
        @param file The file.
        @return File extension or null if file name has no extension. */
    public static String getExtension(File file)
    {
        String ext = null;
        String s = file.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 &&  i < s.length() - 1)
            ext = s.substring(i + 1);
        return ext;
    }

    /** Returns relative URI between to files.
        Can be used instead of URI.relativize(), which  does not compute
        relative URI's correctly, if toFile is not a subdirectory of fromFile
        (Sun's Java 1.5.0).
        @todo Handle special charcters and file names containing slashes.
        @param fromFile File to compute the URI relative to.
        @param toFile Target file or directory.
        @return Relative URI. */
    public static String getRelativeURI(File fromFile, File toFile)
    {
        assert ! fromFile.exists() || ! fromFile.isDirectory();
        fromFile = fromFile.getAbsoluteFile().getParentFile();
        assert fromFile != null;
        ArrayList<String> fromList = splitFile(fromFile);
        ArrayList<String> toList = splitFile(toFile);
        int fromSize = fromList.size();
        int toSize = toList.size();
        int i = 0;
        while (i < fromSize && i < toSize
               && fromList.get(i).equals(toList.get(i)))
            ++i;
        StringBuilder result = new StringBuilder();
        for (int j = i; j < fromSize; ++j)
            result.append("../");
        for (int j = i; j < toSize; ++j)
        {
            result.append(toList.get(j));
            if (j < toSize - 1)
                result.append('/');
        }
        return result.toString();
    }

    /** Return URI for file.
        Replacement for File.toURI() with defined (empty) authority. */
    public static URI getURI(File file)
    {
        try
        {
            return new URI("file", "", file.toURI().getPath(), null, null);
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }

    /** Check for extension (case-insensitive). */
    public static boolean hasExtension(File f, String extension)
    {
        String ext = getExtension(f);
        if (ext == null)
            return false;
        return ext.equalsIgnoreCase(extension);
    }

    /** Read a list of strings from a file.
        The file is expected to contain one string per line; leading and
        trailing whitespaces are removed. Empty lines or lines beginning
        with the comment character '#' are ignored. */
    public static ArrayList<String> readStringListFromFile(File file)
        throws IOException
    {
        ArrayList<String> result = new ArrayList<String>();
        FileReader reader = new FileReader(file);
            BufferedReader in = new BufferedReader(reader);
        try
        {
            while (true)
            {
                String line = in.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (! line.equals("") && ! line.startsWith("#"))
                    result.add(line);
            }
            return result;
        }
        finally
        {
            in.close();
        }
    }

    /** Remove extension in file name.
        If the file does not have the extension oldExtension,
        the extension will not be removed. */
    public static String removeExtension(File file, String oldExtension)
    {
        String name = file.toString();
        if (hasExtension(file, oldExtension))
        {
            int index = name.lastIndexOf('.');
            assert index >= 0;
            return name.substring(0, index);
        }
        return name;
    }

    /** Replace extension in file name.
        If the file does not have the extension oldExtension,
        the extension will not be replaced but the new extension will be
        appended. */
    public static String replaceExtension(File file, String oldExtension,
                                          String newExtension)
    {
        String name = file.toString();
        if (hasExtension(file, oldExtension))
        {
            int index = name.lastIndexOf('.');
            assert index >= 0;
            return name.substring(0, index) + "." + newExtension;
        }
        return name + "." + newExtension;
    }

    public static String replaceExtension(String file, String oldExtension,
                                          String newExtension)
    {
        return replaceExtension(new File(file), oldExtension, newExtension);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private FileUtil()
    {
    }

    private static ArrayList<String> splitFile(File file)
    {
        ArrayList<String> list = new ArrayList<String>();
        file = file.getAbsoluteFile();
        try
        {
            file = file.getCanonicalFile();
        }
        catch (IOException e)
        {
        }
        while (file != null)
        {
            list.add(0, file.getName());
            file = file.getParentFile();
        }
        return list;
    }
}
