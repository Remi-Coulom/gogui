//----------------------------------------------------------------------------
// $Id: Main.java 3544 2006-10-08 18:04:37Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.thumbnail;

import java.io.File;
import java.io.IOException;
import net.sf.gogui.util.FileUtil;

/** Untility functions for managing the thumbnail directory. */
public final class ThumbnailUtil
{
    /** Expire all thumbnails older than a certain age.
        NOTE: This function is still experimental, it may not work yet
    */
    public static void expire(int seconds, boolean checkOnly)
    {
        if (! ThumbnailPlatform.checkThumbnailSupport())
        {
            System.err.println("Thumbnails not supported on this platform.");
            return;
        }
        File dir = ThumbnailPlatform.getNormalDir();
        long currentTimeSeconds = System.currentTimeMillis() / 1000L;
        System.err.println("Expiring thumbnails. Time: "
                           + currentTimeSeconds);
        for (File file : dir.listFiles())
            expire(file, currentTimeSeconds, seconds, checkOnly);
    }

    /** Expire thumbnails if older than a certain age.
        NOTE: This function is still experimental, it may not work yet
    */
    public static void expire(File file, long currentTimeSeconds,
                              long seconds, boolean checkOnly)
    {
        if (! file.isFile())
        {
            System.err.println("Not a normal file: " + file);
            return;
        }
        if (! FileUtil.hasExtension(file, "png"))
        {
            System.err.println("Not a thumbnail: " + file);
            return;
        }
        ThumbnailReader.MetaData metaData;
        try
        {
            metaData = ThumbnailReader.read(file);
        }
        catch (IOException e)
        {
            System.err.println("Could not read meta data: " + file);
            return;
        }
        System.err.println("File: " + file);
        System.err.println("  URI: " + metaData.m_uri);
        System.err.println("  MTime: " + metaData.m_lastModified);
        System.err.println("  MimeType: " + metaData.m_mimeType);
        System.err.println("  Software: " + metaData.m_software);
        System.err.println("  Description: " + metaData.m_description);
        if (metaData.m_lastModified == 0)
        {
            System.err.println("  No MTime meta data");
            return;
        }
        long age = currentTimeSeconds - metaData.m_lastModified;
        if (age > seconds)
        {
            if (checkOnly)
                System.err.println("  Would expire");
            else
            {
                System.err.println("  Expiring");
                file.delete();
            }
        }
        else
            System.err.println("  Not expiring");
        System.err.println();
    }

    /** Make constructor unavailable; class is for namespace only. */
    private ThumbnailUtil()
    {
    }
}
