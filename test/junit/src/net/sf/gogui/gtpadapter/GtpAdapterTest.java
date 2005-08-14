//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.IOException;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpEngineConnection;
import net.sf.gogui.gtp.GtpExpectEngine;

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

    public void testLoadSgf() throws IOException, GtpError
    {
        GtpExpectEngine expect = new GtpExpectEngine(null);
        GtpEngineConnection expectConnection
            = new GtpEngineConnection(expect);
        expect.expect("protocol_version", "2");
        expect.expect("list_commands", "");
        GtpAdapter adapter
            = new GtpAdapter(expectConnection.getGtp(), null, false);
        adapter.setEmuLoadSgf();
        GtpEngineConnection adapterConnection
            = new GtpEngineConnection(adapter);
        Gtp gtp = adapterConnection.getGtp();
        expect.expect("boardsize 19", "");
        expect.expect("clear_board", "");
        expect.expect("play B D4", "");
        expect.expect("play W Q16", "");
        gtp.sendCommand("loadsgf test.sgf");
        assertTrue(expect.isExpectQueueEmpty());
    }
}

//----------------------------------------------------------------------------
