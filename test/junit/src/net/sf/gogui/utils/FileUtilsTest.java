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
}

//----------------------------------------------------------------------------
