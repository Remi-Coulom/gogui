// RecentFileStorage.java

package net.sf.gogui.unfinished;

import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Recent file storage according to the Freedesktop standard.
    http://standards.freedesktop.org/recent-file-spec/0.2/
    @todo Implementation not finished yet. Don't use this class. */
public final class RecentFileStorage
{
    public static final int MAX_ITEMS = 500;

    public static void add(URI uri, String mimetype)
    {
        add(uri, mimetype, null, false);
    }

    /** @todo create empty document, if read document is invalid (e.g.
        no RecentFiles element)? */
    public static void add(URI uri, String mimetype, String group,
                           boolean isPrivate)
    {
        updateFromFile();
        NodeList list = s_document.getElementsByTagName("RecentFiles");
        int length = list.getLength();
        if (length == 0)
        {
            System.err.println("error: no tag RecentFiles");
            return;
        }
        if (length > 1)
            System.err.println("warning: multiple tags RecentFiles");
        Node recentFiles = list.item(0);
        Element recentItem = s_document.createElement("RecentItem");
        recentFiles.appendChild(recentItem);
        Element uriElement = s_document.createElement("URI");
        uriElement.appendChild(s_document.createTextNode(uri.toString()));
        recentItem.appendChild(uriElement);
        Element timestampElement = s_document.createElement("Timestamp");
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        timestampElement.appendChild(s_document.createTextNode(timestamp));
        recentItem.appendChild(timestampElement);
        writeFile();
    }

    public static ArrayList<String> getAllMimeType(String mimeType)
    {
        updateFromFile();
        ArrayList<String> result = new ArrayList<String>();
        NodeList nodeList = s_document.getElementsByTagName("RecentItem");
        for (int i = 0; i < nodeList.getLength(); ++i)
        {
            Node element = nodeList.item(i);
            NamedNodeMap attributes = element.getAttributes();
            Node nodeMimeType = attributes.getNamedItem("Mime-Type");
            if (nodeMimeType == null)
                continue;
            String value = nodeMimeType.getNodeValue();
            if (! value.equals(mimeType))
                continue;
            result.add(value);
        }
        return result;
    }

    /** @todo Implement */
    public static URI[] getAllGroup(String group)
    {
        return new URI[0];
    }

    /** Check if file was changed by another application.
        @return True if file was changed since the last invocation of one of
        the getAll methods or if it was not read yet. */
    public static boolean wasChanged()
    {
        if (s_document == null)
            return true;
        if (! s_file.exists())
            return true;
        long currentTime = System.currentTimeMillis();
        return s_file.lastModified() > currentTime;
    }

    /** For temporary testing.
        @todo Remove later */
    public static void main(String[] args)
    {
        ArrayList<String> uriList = getAllMimeType("application/x-go-sgf");
        for (int i = 0; i < uriList.size(); ++i)
            System.err.println(uriList.get(i));
        //add(FileUtil.getURI(new File("foobar")), "application/x-go-sgf");
    }

    //private static long m_timestamp;

    /** Content of the recent files file. */
    private static Document s_document;

    private static DocumentBuilder s_builder;

    private static File s_file
        = new File(System.getProperties().getProperty("user.home"),
                   ".recently-used");

    static
    {
        try
        {
            s_builder
                = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private RecentFileStorage()
    {
    }

    private static void updateFromFile()
    {
        if (! wasChanged())
            return;
        if (s_builder == null)
            return;
        //m_timestamp = System.currentTimeMillis();
        if (! s_file.exists())
        {
            createEmptyDocument();
            return;
        }
        try
        {
            s_document = s_builder.parse(s_file);
            return;
        }
        catch (SAXException saxe)
        {
            Exception e = saxe;
            if (saxe.getException() != null)
                e = saxe.getException();
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        createEmptyDocument();
    }

    private static void createEmptyDocument()
    {
        assert s_builder != null;
        s_document = s_builder.newDocument();
        Element recentFiles = s_document.createElement("RecentFiles");
        s_document.appendChild(recentFiles);
    }

    private static void writeFile()
    {
        try
        {
            s_document.normalize();
            Source source = new DOMSource(s_document);
            Result result = new StreamResult(s_file);
            Transformer transformer
                = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(source, result);
        }
        catch (TransformerConfigurationException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        catch (TransformerException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
