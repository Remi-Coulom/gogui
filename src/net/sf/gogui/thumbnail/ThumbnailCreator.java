//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.thumbnail;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Iterator;
import java.util.Locale;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import net.sf.gogui.boardpainter.GuiField;
import net.sf.gogui.boardpainter.BoardPainter;
import net.sf.gogui.game.BoardUpdater;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.version.Version;

/** Thumbnail creator.
    Creates thumbnails according to the freedesktop.org standard.
*/
public final class ThumbnailCreator
{
    public static class Error
        extends ErrorMessage
    {
        public Error(String message)
        {
            super(message);
        }

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sf.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    public ThumbnailCreator(boolean verbose)
    {
        m_verbose = verbose;
        m_painter = new BoardPainter();
    }

    /** Create thumbnail at standard location.
        Does not create the thumnbail if an up-to-date thumbnail already
        exists.
    */
    public void create(File input) throws Error
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
        create(input, null, 128, true);
    }

    /** Create thumbnail.
        @param input The SGF file
        @param output The output thumbnail. Null for standard filename in
        .thumbnails/normal
        @param thumbnailSize The image size of the thumbnail.
        @param scale If true thumbnailSize will be scaled down for boards
        smaller than 19.
    */
    public void create(File input, File output, int thumbnailSize,
                       boolean scale) throws Error
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
            GuiField[][] field = new GuiField[size][size];
            for (int x = 0; x < size; ++x)
                for (int y = 0; y < size; ++y)
                {
                    field[x][y] = new GuiField();
                    GoColor color = board.getColor(GoPoint.get(x, y));
                    field[x][y].setColor(color);
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
                image = getImage(field, 2 * imageSize, 2 * imageSize);
                BufferedImage newImage = createImage(imageSize, imageSize);
                Graphics2D graphics = newImage.createGraphics();
                Image scaledInstance
                    = image.getScaledInstance(imageSize, imageSize,
                                              Image.SCALE_SMOOTH);
                graphics.drawImage(scaledInstance, 0, 0, null);
                image = newImage;
            }
            else
                image = getImage(field, imageSize, imageSize);
            if (output == null)
                output = getThumbnailFileNormalSize(input);
            long lastModified = getLastModified(input);
            writeImage(image, output, uri, lastModified);
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

    private BufferedImage createImage(int width, int height)
    {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private void addMeta(org.w3c.dom.Node node, String keyword, String value)
    {
        IIOMetadataNode text = new IIOMetadataNode("Text");
        IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
        textEntry.setAttribute("value", value);
        textEntry.setAttribute("keyword", keyword);
        textEntry.setAttribute("encoding", Locale.getDefault().toString());
        textEntry.setAttribute("language", "en");
        textEntry.setAttribute("compression", "none");
        text.appendChild(textEntry);
        node.appendChild(text);
    }

    private ConstBoard readFile(File file)
        throws FileNotFoundException, SgfError
    {
        FileInputStream in = new FileInputStream(file);
        SgfReader reader;
        try
        {
            reader = new SgfReader(in, file, null, 0);
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                log(e.getMessage());
            }
        }
        GameTree tree = reader.getTree();
        GameInformation info = tree.getGameInformation(tree.getRoot());
        int size = tree.getBoardSize();
        m_description = info.suggestGameName();
        if (m_description == null)
            m_description = "";
        Board board = new Board(size);
        net.sf.gogui.game.ConstNode node = tree.getRoot();
        while (node.hasChildren() && ! node.hasSetup())
            node = node.getChildConst();
        new BoardUpdater().update(tree, node, board);
        //System.err.print(net.sf.gogui.go.BoardUtil.toString(board));
        return board;
    }

    private BufferedImage getImage(GuiField[][] field, int width, int height)
    {
        BufferedImage image = createImage(width, height);
        Graphics2D graphics = image.createGraphics();
        m_painter.draw(graphics, field, width, false);
        graphics.dispose();
        return image;
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
            byte[] md5 = digest.digest(string.getBytes("US-ASCII"));
            StringBuffer buffer = new StringBuffer();
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
        catch (UnsupportedEncodingException e)
        {
            throw new Error("MD5: unsupported encoding");
        }
    }

    private File getThumbnailFileNormalSize(File file) throws Error
    {
        URI uri = getURI(file);
        String md5 = getMD5(uri.toString());
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

    private void writeImage(BufferedImage image, File file, URI uri,
                            long lastModified)
        throws IOException, Error
    {
        Iterator iter = ImageIO.getImageWritersBySuffix("png");
        ImageWriter writer = (ImageWriter)iter.next();
        ImageTypeSpecifier specifier = new ImageTypeSpecifier(image);
        IIOMetadata meta = writer.getDefaultImageMetadata(specifier, null);
        String formatName = "javax_imageio_1.0";
        org.w3c.dom.Node node = meta.getAsTree(formatName);
        addMeta(node, "Thumb::URI", uri.toString());
        addMeta(node, "Thumb::MTime", Long.toString(lastModified));
        addMeta(node, "Thumb::Mimetype", "application/x-go-sgf");
        if (! m_description.equals(""))
            addMeta(node, "Description", m_description);
        addMeta(node, "Software", "GoGui " + Version.get());
        try
        {
            meta.mergeTree(formatName, node);
        }
        catch (IIOInvalidTreeException e)
        {
            assert false;
            return;
        }
        // Renaming a temporary file as required bu the standard does
        // not work, because File.renameTo may fail on some platforms
        //File tempFile = File.createTempFile("gogui-thumbnail", ".png");
        //tempFile.deleteOnExit();
        //ImageOutputStream ios = ImageIO.createImageOutputStream(tempFile);
        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, meta), null);
        //if (! tempFile.renameTo(file))
        //    throw new Error("Could not rename " + tempFile + " to " + file);
        m_lastThumbnail = file;
    }
}

