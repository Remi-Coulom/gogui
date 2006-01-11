//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.File;

//----------------------------------------------------------------------------

public class FileUtilsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(FileUtilsTest.class);
    }

    public void testGetExtension()
    {
        assertEquals("bar", FileUtils.getExtension(new File("foo.bar")));
        assertEquals(null, FileUtils.getExtension(new File("foo")));
    }

    public void testGetRelativeURI()
    {
        final char sep = File.separatorChar;
        File from = new File("file1");
        File to = new File("file2");
        assertEquals("file2", FileUtils.getRelativeURI(from, to));
        from = new File("dir1" + sep + "file1");
        to = new File("dir1" + sep + "file2");
        assertEquals("file2", FileUtils.getRelativeURI(from, to));
        from = new File("dir1" + sep + "file1");
        to = new File("dir2" + sep + "file2");
        assertEquals("../dir2/file2", FileUtils.getRelativeURI(from, to));
    }

    public void testHasExtension()
    {
        assertTrue(FileUtils.hasExtension(new File("foo.bar"), "bar"));
        assertTrue(FileUtils.hasExtension(new File("foo.BAR"), "bar"));
        assertTrue(FileUtils.hasExtension(new File("foo.bar"), "BAR"));
        assertFalse(FileUtils.hasExtension(new File("bar.foo"), "bar"));
    }
}

//----------------------------------------------------------------------------
