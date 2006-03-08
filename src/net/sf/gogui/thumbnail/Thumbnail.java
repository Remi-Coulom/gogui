//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.thumbnail;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
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
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gui.GuiField;
import net.sf.gogui.gui.GuiBoardDrawer;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** Thumbnail creator.
    Creates thumbnails according to the freedesktop.org standard.
    @todo Save to temp file and rename as required by the standard.
*/
public final class Thumbnail
{
    public Thumbnail(boolean verbose)
    {
        m_verbose = verbose;
        m_drawer = new GuiBoardDrawer(false);
    }

    public static boolean checkThumbnailSupport()
    {
        return getNormalDir().exists();
    }

    /** Create thumbnail.
        Creates a small thumbnail; only if .thumbnails/normal directory
        already exists in home directory.
    */
    public boolean create(File file)
    {
        m_lastNormalThumbnail = null;
        try
        {
            if (! checkThumbnailSupport())
            {
                // We cannot create it with the right permissions from Java
                log("Thumbnail directory does not exist: " + file);
                return false;
            }
            log("File: " + file);
            long lastModified = file.lastModified() / 1000L;
            if (lastModified == 0L)
            {
                log("Could not get last modification time: " + file);
                return false;
            }
            URI uri = FileUtils.getURI(file);
            log("URI: " + uri);
            String md5 = getMD5(uri.toString());
            if (m_verbose)
                log("MD5: " + md5);
            ArrayList moves = new ArrayList();
            Board board = getBoard(file, moves);
            for (int i = 0; i < moves.size(); ++i)
                board.play((Move)moves.get(i));
            int size = board.getSize();
            GuiField[][] field = new GuiField[size][size];
            for (int x = 0; x < size; ++x)
                for (int y = 0; y < size; ++y)
                {
                    field[x][y] = new GuiField();
                    GoColor color = board.getColor(GoPoint.create(x, y));
                    field[x][y].setColor(color);
                }
            BufferedImage image = getImage(field, 256, 256);

            /* Don't write the large image yet, takes too much space
                writeImage(image, new File(largeDir, md5 + ".png"), uri,
                           lastModified);
            */

            BufferedImage normalImage = createImage(128, 128);
            Graphics2D graphics = normalImage.createGraphics();
            Image scaledInstance
                = image.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            graphics.drawImage(scaledInstance, 0, 0, null);
            writeImage(normalImage, new File(getNormalDir(), md5 + ".png"),
                       uri, lastModified);

            return true;
        }
        catch (FileNotFoundException e)
        {
            log("File not found: " + file);
            return false;
        }
        catch (IOException e)
        {
            log(e.getMessage());
            return false;
        }
        catch (NoSuchAlgorithmException e)
        {
            log("No MD5 message digest found");
            return false;
        }
        catch (SgfReader.SgfError e)
        {
            log("SGF error: " + file);
            return false;
        }
    }

    public static File getNormalDir()
    {
        String home = System.getProperty("user.home", "");
        return new File(new File(home, ".thumbnails"), "normal");
    }

    public File getLastNormalThumbnail()
    {
        return m_lastNormalThumbnail;
    }

    public static void main(String[] arg)
    {
        Thumbnail thumbnail = new Thumbnail(true);
        for (int i = 0; i < arg.length; ++i)
            thumbnail.create(new File(arg[i]));
    }

    private final boolean m_verbose;

    private File m_lastNormalThumbnail;

    private final GuiBoardDrawer m_drawer;

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
        textEntry.setAttribute("encoding", "ISO-8859-1");
        textEntry.setAttribute("language", "en");
        textEntry.setAttribute("compression", "none");
        text.appendChild(textEntry);
        node.appendChild(text);
    }

    private Board getBoard(File file, ArrayList moves)
        throws FileNotFoundException, SgfReader.SgfError
    {
        FileInputStream in = new FileInputStream(file);
        SgfReader reader = new SgfReader(in, file.toString(), null, 0);
        try
        {
            in.close();
        }
        catch (IOException e)
        {
            log(e.getMessage());
        }
        GameTree tree = reader.getGameTree();
        int size = tree.getGameInformation().m_boardSize;
        Board board = new Board(size);
        net.sf.gogui.game.Node node = tree.getRoot();
        while (node != null)
        {
            moves.addAll(NodeUtils.getAllAsMoves(node));
            node = node.getChild();
        }
        //if (m_verbose)
        //    BoardUtils.print(board, System.err);
        return board;
    }

    private BufferedImage getImage(GuiField[][] field, int width, int height)
    {
        BufferedImage image = createImage(width, height);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);
        m_drawer.draw(graphics, field, width, false, false);
        graphics.dispose();
        return image;
    }

    private static String getMD5(String string)
        throws NoSuchAlgorithmException, UnsupportedEncodingException
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

    private void log(String line)
    {
        if (! m_verbose)
            return;
        System.err.println(line);
    }

    private void writeImage(BufferedImage image, File file, URI uri,
                            long lastModified) throws IOException
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
        addMeta(node, "Software", "GoGui " + Version.get());
        try
        {
            meta.mergeTree(formatName, node);
        }
        catch (IIOInvalidTreeException e)
        {
            assert(false);
            return;
        }
        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, meta), null);
        m_lastNormalThumbnail = file;
    }
}

//----------------------------------------------------------------------------
