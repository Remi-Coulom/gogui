// FileUtilTest.java

package net.sf.gogui.util;

import java.io.File;

public final class FileUtilTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(FileUtilTest.class);
    }

    public void testGetExtension()
    {
        assertEquals("bar", FileUtil.getExtension(new File("foo.bar")));
        assertEquals(null, FileUtil.getExtension(new File("foo")));
    }

    public void testGetRelativeURI()
    {
        final char sep = File.separatorChar;
        File from = new File("file1");
        File to = new File("file2");
        assertEquals("file2", FileUtil.getRelativeURI(from, to));
        from = new File("dir1" + sep + "file1");
        to = new File("dir1" + sep + "file2");
        assertEquals("file2", FileUtil.getRelativeURI(from, to));
        from = new File("dir1" + sep + "file1");
        to = new File("dir2" + sep + "file2");
        assertEquals("../dir2/file2", FileUtil.getRelativeURI(from, to));
    }

    public void testHasExtension()
    {
        assertTrue(FileUtil.hasExtension(new File("foo.bar"), "bar"));
        assertTrue(FileUtil.hasExtension(new File("foo.BAR"), "bar"));
        assertTrue(FileUtil.hasExtension(new File("foo.bar"), "BAR"));
        assertFalse(FileUtil.hasExtension(new File("bar.foo"), "bar"));
    }
}
