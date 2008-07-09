// ThumbnailReaderTest.java

package net.sf.gogui.thumbnail;

import java.io.File;
import java.io.IOException;

public final class ThumbnailReaderTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(ThumbnailReaderTest.class);
    }

    public void testRead() throws IOException
    {
        ThumbnailReader.MetaData metaData =
            getMetaData("gnome-thumbnail.png");
        assertEquals("file:///compsci/brule9/cshome/emarkus/gogui/sgf"
                     + "/openings/9x9/007.sgf",
                     metaData.m_uri.toString());
        assertEquals(1168622250, metaData.m_lastModified);
        assertEquals("GNOME::ThumbnailFactory", metaData.m_software);
        assertNull(metaData.m_description);
        assertNull(metaData.m_mimeType);
    }

    private ThumbnailReader.MetaData getMetaData(String fileName)
        throws IOException
    {
        File file = new File(getClass().getResource(fileName).getPath());
        return ThumbnailReader.read(file);
    }
}
