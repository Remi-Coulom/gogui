//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpEngineClient;
import net.sf.gogui.gtp.GtpEngineConnection;
import net.sf.gogui.gtp.GtpExpectEngine;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.StreamCopy;

public final class GtpAdapterTest
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

    public void setUp() throws IOException, GtpError
    {
        m_expect = new GtpExpectEngine(null);
        expect("protocol_version", "2");
        expect("list_commands", "");
        final boolean useEngineConnection = false;
        if (useEngineConnection)
        {
            // Much slower than using GtpEngineClient
            GtpEngineConnection expectConnection
                = new GtpEngineConnection(m_expect);
            m_adapter = new GtpAdapter(expectConnection.getGtpClient(), null,
                                       false, false, false);
            GtpEngineConnection adapterConnection
                = new GtpEngineConnection(m_adapter);
            m_gtp = adapterConnection.getGtpClient();
        }
        else
        {
            m_adapter = new GtpAdapter(new GtpEngineClient(m_expect), null,
                                       false, false, false);
            m_gtp = new GtpEngineClient(m_adapter);
        }
    }

    public void testLoadSgf() throws ErrorMessage, IOException, GtpError
    {
        expect("boardsize 19", "");
        expect("clear_board", "");
        expect("play b D4", "");
        expect("play w Q16", "");
        send("loadsgf " + getTmpFile("test.sgf").toString());
        assertExpectQueueEmpty();
    }

    public void testName() throws ErrorMessage, IOException, GtpError
    {
        expect("name", "Foo");
        assertEquals("Foo", send("name"));
        assertExpectQueueEmpty();
    }

    public void testName2() throws ErrorMessage, IOException, GtpError
    {
        m_adapter.setName("Bar");
        assertEquals("Bar", send("name"));
        assertExpectQueueEmpty();
    }

    private GtpAdapter m_adapter;

    private GtpExpectEngine m_expect;

    private GtpClientBase m_gtp;

    private void assertExpectQueueEmpty()
    {
        assertTrue(m_expect.isExpectQueueEmpty());
    }

    private void expect(String command, String response)
    {
        m_expect.expect(command, response);
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

    private String send(String command) throws GtpError
    {
        return m_gtp.send(command);
    }
}

