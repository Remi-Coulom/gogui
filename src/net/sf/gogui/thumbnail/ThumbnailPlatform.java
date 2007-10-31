// ThumbnailPlatform.java

package net.sf.gogui.thumbnail;

import java.io.File;
import net.sf.gogui.util.Platform;

/** Thumbnail platform settings. */
public final class ThumbnailPlatform
{
    public static boolean checkThumbnailSupport()
    {
        if (Platform.isWindows())
            return false;
        // Don't try to create the directory, because we cannot create it with
        // the right permissions from Java
        return getNormalDir().exists();
    }

    /** Get directory for normal size thumbnails. */
    public static File getNormalDir()
    {
        String home = System.getProperty("user.home", "");
        return new File(new File(home, ".thumbnails"), "normal");
    }

    /** Make constructor unavailable; class is for namespace only. */
    private ThumbnailPlatform()
    {
    }
}
