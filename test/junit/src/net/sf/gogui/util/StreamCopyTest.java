// StreamCopyTest.java

package net.sf.gogui.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamCopyTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(StreamCopyTest.class);
    }

    public void testBasic() throws ErrorMessage
    {
        String input = "This is a test text\n";
        InputStream src = new ByteArrayInputStream(input.getBytes());
        OutputStream dest = new ByteArrayOutputStream();
        StreamCopy copy = new StreamCopy(false, src, dest, true);
        copy.run();
        assertEquals(input, dest.toString());
    }
}
