// BoardPainterUtil.java

package net.sf.gogui.boardpainter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Utility functions for users of class BoardPainter. */
public final class BoardPainterUtil
{
    public static BufferedImage createImage(int width, int height)
    {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /** Use a painter to paint the board in a buffered image.
        The image can be written to a file with writeImage(). */
    public static BufferedImage getImage(BoardPainter painter,
                                         ConstField[][] field, int width,
                                         int height)
    {
        BufferedImage image = createImage(width, height);
        Graphics2D graphics = image.createGraphics();
        painter.draw(graphics, field, width, false);
        graphics.dispose();
        return image;
    }

    /** Write an image in PNG format to a file.
        @param metaData Optional PNG meta data (or null) */
    public static void writeImage(BufferedImage image, File file,
                                  Map<String,String> metaData)
        throws IOException
    {
        Iterator iter = ImageIO.getImageWritersBySuffix("png");
        ImageWriter writer = (ImageWriter)iter.next();
        IIOMetadata meta = null;
        if (metaData != null)
        {
            ImageTypeSpecifier specifier = new ImageTypeSpecifier(image);
            meta = writer.getDefaultImageMetadata(specifier, null);
            String formatName = "javax_imageio_1.0";
            org.w3c.dom.Node node = meta.getAsTree(formatName);
            for (Map.Entry<String,String> entry : metaData.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();
                addMeta(node, key, value);
            }
            try
            {
                meta.mergeTree(formatName, node);
            }
            catch (IIOInvalidTreeException e)
            {
                assert false;
                return;
            }
        }
        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        try
        {
            writer.write(null, new IIOImage(image, null, meta), null);
        }
        catch (IllegalStateException e)
        {
            // ImageWriter on Linux Java 1.5 throws an  IllegalStateException
            // instead of an IOException, if it has no write permissions
            throw new IOException("Could not write to file " + file);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private BoardPainterUtil()
    {
    }

    private static void addMeta(org.w3c.dom.Node node, String keyword,
                                String value)
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
}
