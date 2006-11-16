//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gmp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class GmpTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(GmpTest.class);
    }

    public void testBasics() throws IOException
    {
        createGmp();            
        receiveNewGame(false, true);
        sendMove(true, true, true, 4, 4);
        sendUndo(true, false);
        sendTalk("Hello");
        sendNewGame(true, true);
        receiveMove(true, false, true, -1, -1);
        closeGmp();
    }

    private static final boolean VERBOSE = true;

    private static final int OK = 0;

    private static final int NEWGAME = 2;

    private static final int MOVE = 5;

    private static final int UNDO = 6;

    private Gmp m_gmp;

    private InputStream m_in;

    private OutputStream m_out;

    private static byte checksum(byte[] packet)
    {
        int sum = (int)packet[0] + (int)packet[2] + (int)packet[3];
        return (byte)(sum & 0x7F);
    }

    private void closeGmp() throws IOException
    {
        m_out.close();
        m_in.close();
        m_gmp = null;
        m_in = null;
        m_out = null;
    }

    private void createGmp() throws IOException
    {
        assert(m_gmp == null);
        assert(m_in == null);
        assert(m_out == null);
        PipedInputStream gmpInput = new PipedInputStream();
        m_out = new PipedOutputStream(gmpInput);
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream gmpOutput = new PipedOutputStream(in);
        m_in = in;
        final boolean simple = false;
        m_gmp = new Gmp(gmpInput, gmpOutput, 19, 2, simple, VERBOSE);
    }

    private static int getMoveVal(boolean isBlack, int x, int y)
    {
        if (x == -1 && y == -1)
            return 0;
        assert(x >= 0 && y >= 0);
        int val = y * 19 + x + 1;
        if (! isBlack)
            val |= 0x200;
        return val;
    }

    private byte[] getPacket(boolean hisSeq, boolean mySeq, int cmd, int val)
    {
        byte[] packet = new byte[4];
        packet[0] = (byte)0x00;
        packet[1] = (byte)0x80;
        packet[2] = (byte)0x80;
        packet[3] = (byte)0x80;
        if (hisSeq)
            packet[0] |= 0x02;
        if (mySeq)
            packet[0] |= 0x01;
        packet[2] |= (cmd << 4);
        packet[2] |= ((val >> 7) & 0x07);
        packet[3] |= (val & 0x7F);
        packet[1] |= checksum(packet);
        return packet;
    }

    private byte[] read(int nuBytes) throws IOException
    {
        byte[] byteArray = new byte[nuBytes];
        int len = 0;
        while (len < nuBytes)
        {
            int n = m_in.read(byteArray, len, nuBytes - len);
            if (n < 0)
                fail();
            len += n;
        }
        return byteArray;
    }

    private void receive(boolean hisSeq, boolean mySeq, int cmd, int val)
        throws IOException
    {
        byte[] packet = read(4);
        verifyPacket(packet, hisSeq, mySeq, cmd, val);
    }

    private void receiveTalk(String talk) throws IOException
    {
        byte[] byteArray = read(talk.length());
        assertEquals(new String(byteArray), talk);
    }

    private void receiveNewGame(boolean hisSeq, boolean mySeq)
        throws IOException
    {
        send(hisSeq, mySeq, NEWGAME, 0);
        receive(mySeq, hisSeq, OK, 0);
        StringBuffer message = new StringBuffer();
        boolean result = m_gmp.waitNewGame(19, message);
        assertTrue(message.toString(), result);
    }

    private void receiveMove(boolean hisSeq, boolean mySeq, boolean isBlack,
                             int x, int y) throws IOException
    {
        send(hisSeq, mySeq, MOVE, getMoveVal(isBlack, x, y));
        receive(mySeq, hisSeq, OK, 0);
        StringBuffer message = new StringBuffer();
        Gmp.Move move = m_gmp.waitMove(isBlack, message);
        assertTrue(message.toString(), move != null);
        assertTrue(move.m_isBlack);
        assertTrue(move.m_x == x);
        assertTrue(move.m_y == y);
    }

    private void send(boolean hisSeq, boolean mySeq, int cmd, int val)
        throws IOException
    {
        byte[] packet = getPacket(hisSeq, mySeq, cmd, val);
        m_out.write(packet);
        m_out.flush();
    }

    private void sendMove(boolean hisSeq, boolean mySeq,
                          final boolean isBlack, final int x, final int y)
        throws IOException
    {
        Thread thread = new Thread()
            {
                public void run()
                {
                    StringBuffer message = new StringBuffer();
                    boolean result = m_gmp.play(isBlack, x, y, message);
                    assertTrue(message.toString(), result);
                }
            };
        thread.start();
        receive(hisSeq, mySeq, MOVE, getMoveVal(isBlack, x, y));
        send(mySeq, hisSeq, OK, 0);
        waitThread(thread);
    }

    private void sendNewGame(boolean hisSeq, boolean mySeq)
        throws IOException
    {
        Thread thread = new Thread()
            {
                public void run()
                {
                    StringBuffer message = new StringBuffer();
                    boolean result = m_gmp.newGame(19, message);
                    assertTrue(message.toString(), result);
                }
            };
        thread.start();
        receive(hisSeq, mySeq, NEWGAME, 0);
        send(mySeq, hisSeq, OK, 0);
        waitThread(thread);
    }

    private void sendTalk(final String talk) throws IOException
    {
        Thread thread = new Thread()
            {
                public void run()
                {
                    boolean result = m_gmp.sendTalk(talk);
                    assertTrue(result);
                }
            };
        thread.start();
        receiveTalk(talk);
        waitThread(thread);
    }

    private void sendUndo(boolean hisSeq, boolean mySeq) throws IOException
    {
        Thread thread = new Thread()
            {
                public void run()
                {
                    StringBuffer message = new StringBuffer();
                    boolean result = m_gmp.undo(message);
                    assertTrue(message.toString(), result);
                }
            };
        thread.start();
        receive(hisSeq, mySeq, UNDO, 1);
        send(mySeq, hisSeq, OK, 0);
        waitThread(thread);
    }

    private static void verifyPacket(byte[] packet, boolean hisSeq,
                                     boolean mySeq, int cmd, int val)
    {
        assertTrue(packet.length == 4);
        assertTrue("Invalid start byte", (packet[0] & 0xFC) == 0);
        for (int i = 1; i < 4; ++i)
            assertTrue("Invalid packet byte", (packet[i] & 0x80) != 0);
        int s = (packet[1] & 0x7F);
        assertTrue("Invalid checksum", s == checksum(packet));
        assertTrue("Reserved bit set", (packet[2] & 0x08) == 0);
        assertTrue("Wrong sequence bit", ((packet[0] & 0x02) != 0) == hisSeq);
        assertTrue("Wrong sequence bit", ((packet[0] & 0x01) != 0) == mySeq);
        assertTrue("Wrong command", ((packet[2] >> 4) & 0x07) == cmd);
        int v = (((packet[2] & 0x07) << 7) | (packet[3] & 0x7F));
        assertTrue("Wrong value: " + v + " (should be " + val + ")",
                   v == val);
    }

    private void waitThread(Thread thread)
    {
        try
        {
            thread.join();
        }
        catch (InterruptedException e)
        {
        }
    }
}

