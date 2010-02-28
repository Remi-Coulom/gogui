// ThumbnailUtil.java

package net.sf.gogui.thumbnail;

import java.io.File;
import java.io.IOException;
import net.sf.gogui.game.BoardUpdater;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.Board;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.util.FileUtil;

/** Untility functions for managing the thumbnail directory. */
public final class ThumbnailUtil
{
    /** Expire all thumbnails older than a certain age.
        NOTE: This function is still experimental, it may not work yet */
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
        NOTE: This function is still experimental, it may not work yet */
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
                if (! file.delete())
                    System.err.println("  Could not delete file");
            }
        }
        else
            System.err.println("  Not expiring");
        System.err.println();
    }

    /** Get a node from the tree to be used for the thumbnail.
        The position selected is the last position in the main variation or the
        first position in the main variation that contains both black and
        white setup stones. The rationale for this is that one usually wants
        to see the last position of a game, which may contain black handicap
        setup stones before the moves, but the thumbnail shouldn't show the
        solution of files containing Go problems. */
    public static ConstNode getNode(ConstGameTree tree)
    {
        ConstNode node = tree.getRootConst();
        while (node.hasChildren()
               && ! (node.getSetup(BLACK).size() > 0
                     && node.getSetup(WHITE).size() > 0))
            node = node.getChildConst();
        return node;
    }

    /** Get a position from the tree to be used for the thumbnail.
        @see #getPosition() */
    public static ConstBoard getPosition(ConstGameTree tree)
    {
        Board board = new Board(tree.getBoardSize());
        new BoardUpdater().update(tree, getNode(tree), board);
        return board;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private ThumbnailUtil()
    {
    }
}
