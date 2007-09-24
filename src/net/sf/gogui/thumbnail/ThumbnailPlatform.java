//----------------------------------------------------------------------------
// ThumbnailPlatform.java
//----------------------------------------------------------------------------

package net.sf.gogui.thumbnail;

import java.io.File;
import net.sf.gogui.util.Platform;

/** Thumbnail platform settings. */
public final class ThumbnailPlatform
{
    public static boolean checkThumbnailSupport()
    {
        File dir = getNormalDir();
        // On Windows we try to create the directory, not in other platforms,
        // because we cannot create it with the right permissions from Java
        if (! dir.exists() && Platform.isWindows())
            dir.mkdirs();
        return dir.exists();
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
