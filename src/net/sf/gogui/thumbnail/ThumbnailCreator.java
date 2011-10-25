// ThumbnailCreator.java

package net.sf.gogui.thumbnail;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.sf.gogui.boardpainter.BoardPainter;
import net.sf.gogui.boardpainter.BoardPainterUtil;
import net.sf.gogui.boardpainter.Field;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.gamefile.GameFile;
import net.sf.gogui.gamefile.GameReader;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.version.Version;

/** Thumbnail creator.
    Creates thumbnails according to the freedesktop.org standard. */
public final class ThumbnailCreator
{
    /** Error thrown if thumbnail creation fails. */
    public static class Error
        extends ErrorMessage
    {
        public Error(String message)
        {
            super(message);
        }
    }

    public ThumbnailCreator(boolean verbose)
    {
        m_verbose = verbose;
        m_painter = new BoardPainter();
    }

    /** Create thumbnail at standard location.
        Does not create the thumnbail if an up-to-date thumbnail already
        exists. */
    public void create(File input) throws ErrorMessage
    {
        File file = getThumbnailFileNormalSize(input);
        if (file.exists())
        {
            URI uri = getURI(input);
            long lastModified = getLastModified(input);
            try
            {
                ThumbnailReader.MetaData data = ThumbnailReader.read(file);
                if (uri.equals(data.m_uri)
                    && data.m_lastModified == lastModified)
                {
                    m_lastThumbnail = file;
                    m_description = data.m_description;
                    return;
                }
            }
            catch (IOException e)
            {
            }
        }
        create(input, null, 128, false);
    }

    /** Create thumbnail.
        @param input The SGF file
        @param output The output thumbnail. Null for standard filename in
        .thumbnails/normal
        @param thumbnailSize The image size of the thumbnail.
        @param scale If true thumbnailSize will be scaled down for boards
        smaller than 19. */
    public void create(File input, File output, int thumbnailSize,
                       boolean scale) throws ErrorMessage
    {
        assert thumbnailSize > 0;
        m_lastThumbnail = null;
        try
        {
            log("File: " + input);
            URI uri = getURI(input);
            log("URI: " + uri);
            m_description = "";
            ConstBoard board = readFile(input);
            int size = board.getSize();
            Field[][] fields = new Field[size][size];
            for (int x = 0; x < size; ++x)
                for (int y = 0; y < size; ++y)
                {
                    fields[x][y] = new Field();
                    GoColor color = board.getColor(GoPoint.get(x, y));
                    fields[x][y].setColor(color);
                }
            int imageSize = thumbnailSize;
            if (scale)
                imageSize = Math.min(thumbnailSize * size / 19,
                                     thumbnailSize);
            BufferedImage image;
            if (imageSize < 256)
            {
                // Create large image and scale down, looks better than
                // creating small image
                image = BoardPainterUtil.getImage(m_painter, fields,
                                                  2 * imageSize,
                                                  2 * imageSize);
                BufferedImage newImage
                    = BoardPainterUtil.createImage(imageSize, imageSize);
                Graphics2D graphics = newImage.createGraphics();
                Image scaledInstance
                    = image.getScaledInstance(imageSize, imageSize,
                                              Image.SCALE_SMOOTH);
                graphics.drawImage(scaledInstance, 0, 0, null);
                image = newImage;
            }
            else
                image = BoardPainterUtil.getImage(m_painter, fields,
                                                  imageSize, imageSize);
            if (output == null)
                output = getThumbnailFileNormalSize(input);
            long lastModified = getLastModified(input);
            Map<String,String> metaData = new TreeMap<String,String>();
            metaData.put("Thumb::URI", uri.toASCIIString());
            metaData.put("Thumb::MTime", Long.toString(lastModified));
            switch (m_gameFile.m_format)
            {
            case XML:
                metaData.put("Thumb::Mimetype", "application/x-go-sgf");
                break;
            case SGF:
                metaData.put("Thumb::Mimetype", "application/x-go+xml");
            }
            if (! m_description.equals(""))
                metaData.put("Description", m_description);
            metaData.put("Software", "GoGui " + Version.get());
            // Renaming a temporary file as required by the standard does
            // not work, because File.renameTo may fail on some platforms
            //File tempFile = File.createTempFile("gogui-thumbnail", ".png");
            //tempFile.deleteOnExit();
            //ImageOutputStream ios
            //    = ImageIO.createImageOutputStream(tempFile);
            BoardPainterUtil.writeImage(image, output, metaData);
            //if (! tempFile.renameTo(output))
            //    throw new Error("Could not rename " + tempFile + " to "
            //                    + output);
            m_lastThumbnail = output;
        }
        catch (FileNotFoundException e)
        {
            throw new Error("File not found: " + input);
        }
        catch (IOException e)
        {
            throw new Error(e.getMessage());
        }
        catch (SgfError e)
        {
            throw new Error(e.getMessage());
        }
    }

    public String getLastDescription()
    {
        return m_description;
    }

    public File getLastThumbnail()
    {
        return m_lastThumbnail;
    }

    private final boolean m_verbose;

    private String m_description;

    private File m_lastThumbnail;

    private final BoardPainter m_painter;

    private GameFile m_gameFile;

    /** Read a file and return a position to use for the thumbnail.
        The position is the first position in the main variation that contains
        setup stones (unless they are handicap stones) or, if no such position
        exists, the last position. */
    private ConstBoard readFile(File file) throws ErrorMessage
    {
        GameReader reader = new GameReader(file);
        m_gameFile = reader.getFile();
        ConstGameTree tree = reader.getTree();
        ConstGameInfo info = tree.getGameInfoConst(tree.getRootConst());
        m_description = info.suggestGameName();
        if (m_description == null)
            m_description = "";
        return ThumbnailUtil.getPosition(tree);
    }

    private long getLastModified(File file) throws Error
    {
        long lastModified = file.lastModified() / 1000L;
        if (lastModified == 0L)
            throw new Error("Could not get last modification time: " + file);
        return lastModified;
    }

    private String getMD5(String string) throws Error
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(string.getBytes());
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < md5.length; ++i)
            {
                buffer.append(Integer.toHexString((md5[i] >> 4) & 0x0F));
                buffer.append(Integer.toHexString(md5[i] & 0x0F));
            }
            return buffer.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new Error("No MD5 message digest found");
        }
    }

    private File getThumbnailFileNormalSize(File file) throws Error
    {
        URI uri = getURI(file);
        String md5 = getMD5(uri.toASCIIString());
        return new File(ThumbnailPlatform.getNormalDir(), md5 + ".png");
    }

    private URI getURI(File file) throws Error
    {
        URI uri = FileUtil.getURI(file);
        if (uri == null)
            throw new Error("Invalid file name");
        return uri;
    }

    private void log(String line)
    {
        if (! m_verbose)
            return;
        System.err.println(line);
    }
}
