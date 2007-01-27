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

    /** Test clear_board and boardsize commands.
        The GtpSynchronizer used by GtpAdapter should always send a boardsize
        and clear_board command after receiving a boardsize command to avoid
        not knowing the state after the boardsize command.
    */
    public void testClearBoard() throws ErrorMessage, IOException, GtpError
    {
        initAdapter();
        send("boardsize 19");
        // Nothing to do for the GtpSynchronizer
        assertExpectQueueEmpty();
        send("clear_board");
        // Nothing to do for the GtpSynchronizer
        assertExpectQueueEmpty();
    }

    /** Test that place_free_handicap used if supported by the engine.
        GtpSynchronizer will still transmit them as play commands until
        it also supports place_free_handicap.
    */
    public void testFreeHandicap()
        throws ErrorMessage, IOException, GtpError
    {
        initAdapter(false, "place_free_handicap");
        expect("place_free_handicap 2", "A1 A2");
        expect("play b A1", "");
        expect("play b A2", "");
        assertEquals("A1 A2", send("place_free_handicap 2"));
        assertExpectQueueEmpty();
    }

    /** Test that place_free_handicap is emulated if not supported by the
        engine.
    */
    public void testFreeHandicapEmu()
        throws ErrorMessage, IOException, GtpError
    {
        initAdapter();
        expect("play b D4", "");
        expect("play b Q16", "");
        assertEquals("D4 Q16", send("place_free_handicap 2"));
        assertExpectQueueEmpty();
    }

    public void testLoadSgf() throws ErrorMessage, IOException, GtpError
    {
        initAdapter();
        expect("play b D4", "");
        expect("play w Q16", "");
        send("loadsgf " + getTmpFile("test.sgf"));
        assertExpectQueueEmpty();
    }

    public void testLowerCase() throws ErrorMessage, IOException, GtpError
    {
        initAdapter(true, "");
        expect("play b d4", "");
        expect("play w pass", "");
        send("play b D4");
        send("play w PASS");
        assertExpectQueueEmpty();
    }

    /** Test that adapter returns the program's name. */
    public void testName() throws ErrorMessage, IOException, GtpError
    {
        initAdapter(false, "name");
        expect("name", "Foo");
        assertEquals("Foo", send("name"));
        assertExpectQueueEmpty();
    }

    /** Test that adapter returns its own name if if was set with setName(). */
    public void testName2() throws ErrorMessage, IOException, GtpError
    {
        initAdapter();
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

    public void initAdapter() throws IOException, GtpError
    {
        initAdapter(false, "");
    }

    public void initAdapter(boolean lowerCase, String supportedCommands)
        throws IOException, GtpError
    {
        m_expect = new GtpExpectEngine(null);
        expect("protocol_version", "2");
        expect("list_commands", supportedCommands);
        expect("boardsize 19", "");
        expect("clear_board", "");
        final boolean useEngineConnection = false;
        if (useEngineConnection)
        {
            // Much slower than using GtpEngineClient
            GtpEngineConnection expectConnection
                = new GtpEngineConnection(m_expect);
            m_adapter = new GtpAdapter(expectConnection.getGtpClient(), null,
                                       false, false, lowerCase, 19);
            GtpEngineConnection adapterConnection
                = new GtpEngineConnection(m_adapter);
            m_gtp = adapterConnection.getGtpClient();
        }
        else
        {
            m_adapter = new GtpAdapter(new GtpEngineClient(m_expect), null,
                                       false, false, lowerCase, 19);
            m_gtp = new GtpEngineClient(m_adapter);
        }
        assertExpectQueueEmpty();
    }

    private String send(String command) throws GtpError
    {
        return m_gtp.send(command);
    }
}

