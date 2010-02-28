// ThumbnailReader.java

package net.sf.gogui.thumbnail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.sf.gogui.util.StringUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Thumbnail reader. */
public final class ThumbnailReader
{
    /** Information about the original file stored in a thumbnail. */
    public static class MetaData
    {
        public URI m_uri;

        public long m_lastModified;

        public String m_mimeType;

        public String m_description;

        public String m_software;
    }

    public static void main(String argv[]) throws IOException
    {
        for (String arg : argv)
        {
            System.out.println(arg);
            MetaData metaData = read(new File(arg));
            System.out.println("URI: " + metaData.m_uri);
            System.out.println("MTime: " + metaData.m_lastModified);
            System.out.println("MimeType: " + metaData.m_mimeType);
            System.out.println("Description: " + metaData.m_description);
            System.out.println("Software: " + metaData.m_software);
            System.out.println();
        }
    }

    public static MetaData read(File file) throws IOException
    {
        MetaData metaData = new MetaData();
        ImageInputStream stream = ImageIO.createImageInputStream(file);
        if (stream == null)
            return metaData;
        Iterator iter = ImageIO.getImageReaders(stream);
        ImageReader reader = (ImageReader)iter.next();
        reader.setInput(stream, true);
        IIOMetadata metadata;
        try
        {
            metadata = reader.getImageMetadata(0);
        }
        catch (Throwable t)
        {
            // Some PNGs generate a NegativeArraySizeException in
            // com.sun.imageio.plugins.png.PNGImageReader.readMetadata
            // with Java 1.5. Ignore these PNGs until the problem is
            // understood.
            StringUtil.printException(t);
            throw new IOException("Internal error reading PNG meta data");
        }
        String formatName = "javax_imageio_1.0";
        Node root = metadata.getAsTree(formatName);
        String uri = getMeta(root, "Thumb::URI");
        try
        {
            if (uri == null)
                warning(file, "no Thumb::URI");
            else
                metaData.m_uri = new URI(uri);
        }
        catch (URISyntaxException e)
        {
            warning(file, "invalid Thumb::URI " + uri);
        }
        String lastModified = getMeta(root, "Thumb::MTime");
        try
        {
            if (lastModified == null)
                warning(file, "no Thumb::MTime");
            else
                metaData.m_lastModified =
                    Long.parseLong(getMeta(root, "Thumb::MTime"));
        }
        catch (NumberFormatException e)
        {
            warning(file, "invalid Thumb::MTime " + lastModified);
        }
        metaData.m_mimeType = getMeta(root, "Thumb::Mimetype");
        metaData.m_description = getMeta(root, "Description");
        metaData.m_software = getMeta(root, "Software");
        return metaData;
    }

    /** Get meta data.
        @param node the (root) node of the meta data tree.
        @param key the key for the meta data.
        @return value or empty string if meta data does not exist. */
    private static String getMeta(Node node, String key)
    {
        if ("TextEntry".equalsIgnoreCase(node.getNodeName()))
        {
            String keyword = null;
            String value = null;
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); ++i)
            {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().equals("keyword"))
                    keyword = attribute.getNodeValue();
                else if (attribute.getNodeName().equals("value"))
                    value = attribute.getNodeValue();
            }
            if (key.equals(keyword))
                return value;
        }
        for (Node child = node.getFirstChild(); child != null;
             child = child.getNextSibling())
        {
            String value = getMeta(child, key);
            if (value != null)
                return value;
        }
        return null;
    }

    private static void warning(File file, String message)
    {
        System.err.println(file + ": " + message);
    }
}
