//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpEngineConnection;
import net.sf.gogui.gtp.GtpExpectEngine;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.StreamCopy;

//----------------------------------------------------------------------------

public class GtpAdapterTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GtpAdapterTest.class);
    }

    public void testLoadSgf() throws ErrorMessage, IOException, GtpError
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpEngineConnection expectConnection
            = new GtpEngineConnection(expect);
        expect.expect("protocol_version", "2");
        expect.expect("list_commands", "");
        GtpAdapter adapter
            = new GtpAdapter(expectConnection.getGtpClient(), null, false);
        adapter.setEmuLoadSgf();
        GtpEngineConnection adapterConnection
            = new GtpEngineConnection(adapter);
        GtpClient gtp = adapterConnection.getGtpClient();
        expect.expect("boardsize 19", "");
        expect.expect("clear_board", "");
        expect.expect("play b D4", "");
        expect.expect("play w Q16", "");
        gtp.send("loadsgf " + getTmpFile("test.sgf").toString());
        assertTrue(expect.isExpectQueueEmpty());
    }

    public void testName() throws ErrorMessage, IOException, GtpError
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpEngineConnection expectConnection
            = new GtpEngineConnection(expect);
        expect.expect("protocol_version", "2");
        expect.expect("list_commands", "");
        GtpAdapter adapter
            = new GtpAdapter(expectConnection.getGtpClient(), null, false);
        GtpEngineConnection adapterConnection
            = new GtpEngineConnection(adapter);
        GtpClient gtp = adapterConnection.getGtpClient();
        expect.expect("name", "Foo");
        assertEquals("Foo", gtp.send("name"));
        assertTrue(expect.isExpectQueueEmpty());
    }

    public void testName2() throws ErrorMessage, IOException, GtpError
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpEngineConnection expectConnection
            = new GtpEngineConnection(expect);
        expect.expect("protocol_version", "2");
        expect.expect("list_commands", "");
        GtpAdapter adapter
            = new GtpAdapter(expectConnection.getGtpClient(), null, false);
        adapter.setName("Bar");
        GtpEngineConnection adapterConnection
            = new GtpEngineConnection(adapter);
        GtpClient gtp = adapterConnection.getGtpClient();
        assertEquals("Bar", gtp.send("name"));
        assertTrue(expect.isExpectQueueEmpty());
    }

    private File getTmpFile(String name) throws ErrorMessage, IOException
    {
        InputStream in = getClass().getResourceAsStream(name);
        if (in == null)
            throw new ErrorMessage("Resource " + name + " not found");
        File file = File.createTempFile("gogui", null);
        file.deleteOnExit();
        StreamCopy copy
            = new StreamCopy(false, in, new FileOutputStream(file), true);
        copy.run();
        return file;
    }
}

//----------------------------------------------------------------------------
