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
        expect.expect("play B D4", "");
        expect.expect("play W Q16", "");
        gtp.sendCommand("loadsgf " + getTmpFile("test.sgf").toString());
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
