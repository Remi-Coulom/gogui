//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gmp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import junit.framework.TestCase;
import junit.framework.TestResult;

//----------------------------------------------------------------------------

final class GmpTestUtils
{
    public static byte[] clone(byte[] packet)
    {
        assert(packet.length == 4);
        byte[] result = new byte[4];
        for (int i = 0; i < 4; ++i)
            result[i] = packet[i];
        return result;
    }

    public static byte[] readPacket(InputStream in) throws IOException
    {
        byte[] packet = new byte[4];
        int len = 0;
        while (len < 4)
        {
            int n = in.read(packet, len, 4 - len);
            if (n < 0)
                return null;
            len += n;
        }
        return packet;
    }
}

//----------------------------------------------------------------------------

public class GmpTest
    extends TestCase
{
    public static void main(String args[])
    {
        GmpTest test = new GmpTest();
        test.testBasics();
    }

    public void testBasics()
    {
        try
        {
            createGmp();

            send(false, true, NEWGAME, 0);
            receive(true, false, OK, 0);
            waitNewGame();

            Thread thread = new Thread()
                {
                    public void run()
                    {
                        play(true, 4, 4);
                    }
                };
            thread.start();
            receive(true, true, MOVE, 0x200 & 62);
            send(true, true, OK, 0);
            waitThread(thread);

            closeGmp();
        }
        catch (IOException e)
        {
            fail("IOException");
        }
    }

    private final boolean m_verbose = false;

    private final int OK = 0;

    private final int NEWGAME = 2;

    private final int MOVE = 5;

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
        m_gmp = new Gmp(gmpInput, gmpOutput, 19, 2, simple, m_verbose);
    }

    private GmpTestResponder createResponder(byte[] answer)
    {
        return new GmpTestResponder(m_in, m_out, answer);
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

    private void play(boolean isBlack, int x, int y)
    {
        StringBuffer message = new StringBuffer();
        boolean result = m_gmp.play(isBlack, x, y, message);
        assertTrue(message.toString(), result);
    }

    private void send(boolean hisSeq, boolean mySeq, int cmd, int val)
        throws IOException
    {
        byte[] packet = getPacket(hisSeq, mySeq, cmd, val);
        m_out.write(packet);
        m_out.flush();
    }

    private void receive(boolean hisSeq, boolean mySeq, int cmd, int val)
        throws IOException
    {
        byte[] packet = GmpTestUtils.readPacket(m_in);
        verifyPacket(packet, hisSeq, mySeq, cmd, val);
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
        int v = ((packet[2] & 0x07) << 7) & (packet[3] & 0x7F);
        assertTrue("Wrong value", v == val);
    }

    private void waitNewGame()
    {
        StringBuffer message = new StringBuffer();
        boolean result = m_gmp.waitNewGame(19, message);
        assertTrue(message.toString(), result);
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

//----------------------------------------------------------------------------
