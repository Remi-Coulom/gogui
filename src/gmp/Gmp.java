//-----------------------------------------------------------------------------
/*
  Go Modem Protocol

  Go Modem Protocol Specification
  http://www.britgo.org/tech/gmp.html
  
  Simple version of the protocol:
  Appendix A in Call For Participation to the FJK Computer Go Tournament 2000
  http://www.brl.ntt.co.jp/sig-gi/fjk2k-go/cfp-english.txt
  
  $Id$
  $Source$
*/
//-----------------------------------------------------------------------------

package gmp;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

class Util
{
	public static String format(int i)
	{
        StringBuffer s = new StringBuffer(8);
		for (int k = 0; k < 8; ++k)
		{
            if (i % 2 != 0)
                s.append('1');
			else
                s.append('0');
			i >>= 1;
		}
        s.reverse();
		return s.toString();
	}

    public static void log(String line)
    {
        System.err.println("gmp: " + line);
        System.err.flush();
    }

}

/** Gmp command. */
class Cmd
{
    public static final int OK = 0;

    public static final int DENY = 1;

    public static final int NEWGAME = 2;

    public static final int QUERY = 3;

    public static final int ANSWER = 4;

    public static final int MOVE = 5;

    public static final int UNDO = 6;

    public static final int EXTENDED = 7;

    public static final int MASK_MOVE_COLOR = 0x200;

    public static final int MASK_MOVE_POINT = 0x1ff;

    public static final int QUERY_GAME = 0;

    public static final int QUERY_BUFSIZE = 1;

    public static final int QUERY_VERSION = 2;

    public static final int QUERY_NUMSTONES = 3;

    public static final int QUERY_TIMEBLACK = 4;

    public static final int QUERY_TIMEWHITE = 5;

    public static final int QUERY_CHARSET = 6;

    public static final int QUERY_RULES = 7;

    public static final int QUERY_HANDICAP = 8;

    public static final int QUERY_SIZE = 9;

    public static final int QUERY_TIMELIMIT = 10;

    public static final int QUERY_COLOR = 11;

    public static final int QUERY_WHO = 12;

    public int m_cmd;

    public int m_val;

    public Cmd(int cmd, int val)
    {
        m_cmd = cmd;
        m_val = val;
    }

    public static String answerValToString(int val, int query)
    {
        boolean zeroMeansUnknown = true;
        switch (query)
        {
        case QUERY_GAME:
            if (val == 1)
                return "GO";
            if (val == 2)
                return "CHESS";
            if (val == 3)
                return "OTHELLO";
            break;
        case QUERY_BUFSIZE:
            return Integer.toString(4 + val * 16) + " BYTES";
        case QUERY_VERSION:
            zeroMeansUnknown = false;
            break;
        case QUERY_CHARSET:
            if (val == 1)
                return "ASCII";
            if (val == 2)
                return "JAPANESE";
            break;
        case QUERY_RULES:
            if (val == 1)
                return "JAPANESE";
            if (val == 2)
                return "CHINESE (SST)";
            break;
        case QUERY_HANDICAP:
            if (val == 1)
                return "NONE";
            break;
        case QUERY_COLOR:
            if (val == 1)
                return "WHITE";
            if (val == 2)
                return "BLACK";
            break;
        case QUERY_WHO:
            if (val == 1)
                return "NEMESIS";
            if (val == 2)
                return "MANY FACES OF GO";
            if (val == 3)
                return "SMART GO BOARD";
            if (val == 4)
                return "GOLIATH";
            if (val == 5)
                return "GO INTELLECT";
            if (val == 6)
                return "STAR OF POLAND";
            break;
        }
        if (val == 0 && zeroMeansUnknown)
            return "UNKNOWN";
        return Integer.toString(val);
    }

    public static String cmdToString(int cmd)
    {
        switch (cmd)
        {
        case OK:
            return "OK";
        case DENY:
            return "DENY";
        case NEWGAME:
            return "NEWGAME";
        case QUERY:
            return "QUERY";
        case ANSWER:
            return "ANSWER";
        case MOVE:
            return "MOVE";
        case UNDO:
            return "UNDO";
        case EXTENDED:
            return "EXTENDED";
        default:
            return Integer.toString(cmd);
        }
    }

    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        if (object instanceof Cmd)
        {
            Cmd cmd = (Cmd)object;
            return (cmd.m_cmd == m_cmd && cmd.m_val == m_val);
        }
        return false;
    }

    public int hashCode()
    {
        return (m_cmd << 16) | m_val;
    }

    public static String moveValToString(int val, int size)
    {
        StringBuffer result = new StringBuffer(16);
        if ((val & MASK_MOVE_COLOR) != 0)
            result.append("W ");
        else
            result.append("B ");
        if (size == 0)
            result.append(val & MASK_MOVE_POINT);
        else
        {
            Gmp.Move move = parseMove(val, size);
            if (move.m_x < 0 || move.m_y < 0)
                result.append("PASS");
            else
            {
                int x = 'A' + move.m_x;
                if (x >= 'I')
                    ++x;
                char xChar = (char)(x);
                result.append(xChar);
                result.append(move.m_y + 1);
            }
        }
        return result.toString();
    }

    public static Gmp.Move parseMove(int val, int size)
    {
        Gmp.Move move = new Gmp.Move();
        move.m_isBlack = ((val & MASK_MOVE_COLOR) == 0);
        val &= MASK_MOVE_POINT;
        if (val == 0)
        {
            move.m_x = -1;
            move.m_y = -1;
        }
        else
        {
            val -= 1;
            move.m_x = val % size;
            move.m_y = val / size;
        }
        return move;
    }

    public static String queryValToString(int val)
    {
        switch (val)
        {
        case QUERY_GAME:
            return "GAME";
        case QUERY_BUFSIZE:
            return "BUFSIZE";
        case QUERY_VERSION:
            return "VERSION";
        case QUERY_NUMSTONES:
            return "NUMSTONES";
        case QUERY_TIMEBLACK:
            return "TIMEBLACK";
        case QUERY_TIMEWHITE:
            return "TIMEWHITE";
        case QUERY_CHARSET:
            return "CHARSET";
        case QUERY_RULES:
            return "RULES";
        case QUERY_HANDICAP:
            return "HANDICAP";
        case QUERY_SIZE:
            return "SIZE";
        case QUERY_TIMELIMIT:
            return "TIMELIMIT";
        case QUERY_COLOR:
            return "COLOR";
        case QUERY_WHO:
            return "WHO";
        default:
            return "? (" + Integer.toString(val) + ")";
        }
    }

    public String toString(int size, int lastQuery)
    {
        StringBuffer buffer = new StringBuffer(32);
        buffer.append(Cmd.cmdToString(m_cmd));
        switch (m_cmd)
        {
        case OK:
        case DENY:
        case NEWGAME:
            break;
        case QUERY:
            buffer.append(' ');
            buffer.append(Cmd.queryValToString(m_val));
            break;
        case ANSWER:
            buffer.append(' ');
            buffer.append(Cmd.answerValToString(m_val, lastQuery));
            break;
        case MOVE:
            buffer.append(' ');
            buffer.append(Cmd.moveValToString(m_val, size));
            break;
        default:
            buffer.append(' ');
            buffer.append(m_val);
            break;
        }
        return buffer.toString();
    }
};

class WriteThread extends Thread
{
    public WriteThread(OutputStream out)
    {
        m_out = out;
    }

    public void run()
    {
        try
        {
            synchronized (this)
            {
                Random random = new Random();
                while (true)
                {
                    if (m_sendInProgress)
                    {
                        long timeout =
                            20000 + (long)(random.nextFloat() * 10000);
                        wait(timeout);
                    }
                    else
                        wait();
                    if (m_sendInProgress)
                        writePacket();
                }
            }
        }
        catch (InterruptedException e)
        {
            return;
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            System.err.println(msg);
        }
    }

    public synchronized void resend()
    {
        notifyAll();
    }

    public synchronized void sendPacket(byte[] packet, boolean onlyOnce)
    {
        m_packet = packet;
        if (onlyOnce)
        {
            writePacket();
            return;
        }
        m_sendInProgress = true;
        notifyAll();
    }

    public synchronized void sendTalk(String talk)
    {
        int size = talk.length();
        byte buffer[] = new byte[size + 1];
        for (int i = 0; i < size; ++i)
        {
            byte b = (byte)talk.charAt(i);
            buffer[i] = ((b > 3 && b < 127) ? b : (byte)'?');            
        }
        buffer[size] = (byte)'\n';
        try
        {
            m_out.write(buffer);
            m_out.flush();
        }
        catch (IOException e)
        {
        }
    }

    public synchronized void stopSend()
    {
        m_sendInProgress = false;
        notifyAll();
    }

    private boolean m_sendInProgress = false;

    private byte[] m_packet;

    private OutputStream m_out;

    private void writePacket()
    {
        Util.log("send "
                 + Util.format(m_packet[0]) + " "
                 + Util.format(m_packet[1]) + " "
                 + Util.format(m_packet[2]) + " "
                 + Util.format(m_packet[3]));
        try
        {
            m_out.write(m_packet);
            m_out.flush();
        }
        catch (IOException e)
        {
        }
    }
}

class ReadThread extends Thread
{
    public static class WaitResult
    {
        public boolean m_success;

        public String m_response;

        public int m_val;
    }

    public ReadThread(InputStream in, OutputStream out, int size,
                      int colorIndex, boolean simple)
    {
        assert(size >= 1);
        assert(size <= 22);
        assert(colorIndex >= 0);
        assert(colorIndex <= 2);
        m_in = in;
        m_size = size;
        m_colorIndex = colorIndex;
        m_simple = simple;
        m_writeThread = new WriteThread(out);
        m_writeThread.start();
    }

    public void interruptCommand()
    {
        synchronized (this)
        {
            m_state = STATE_INTERRUPTED;
            notifyAll();
        }
    }

    public synchronized boolean queue(StringBuffer response)
    {
        int size = m_cmdQueue.size();
        for (int i = 0; i < size; ++i)
        {
            Cmd cmd = (Cmd)m_cmdQueue.get(i);
            response.append(cmd.toString(m_size, m_lastQuery));
            response.append('\n');
        }
        return true;
    }

    public void run()
    {
        try
        {
            byte buffer[] = new byte[16];
            while (true)
            {
                int n = m_in.read(buffer);
                synchronized (this)
                {
                    if (n < 0)
                        break;
                    for (int i = 0; i < n; ++i)
                    {
                        int b = buffer[i];
                        if (b < 0)
                            b += 256;
                        Util.log("recv " + Util.format(b));
                        handleByte(b);
                    }
                }
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            System.err.println(msg);
        }
        synchronized (this)
        {
            m_state = STATE_DISCONNECTED;
        }
        Util.log("input stream was closed");
        m_writeThread.interrupt();
    }

    public synchronized boolean send(Cmd cmd, StringBuffer response)
    {
        while (m_state == STATE_WAIT_ANSWER_OK || m_state == STATE_WAIT_ANSWER)
        {
            try
            {
                sleep(1000);
            }
            catch (InterruptedException ignored)
            {
            }
        }
        if (m_state == STATE_WAIT_OK)
        {
            response.append("Command in progress");
            return false;
        }
        if (m_state == STATE_DISCONNECTED)
        {
            response.append("GMP connection broken");
            return false;
        }
        if (m_cmdQueue.size() > 0)
        {
            Cmd stackCmd = (Cmd)m_cmdQueue.get(0);
            if (! stackCmd.equals(cmd))
            {
                response.append("Received " +
                                stackCmd.toString(m_size, m_lastQuery));
                return false;
            }
            m_cmdQueue.remove(0);
            return true;
        }
        sendCmd(cmd.m_cmd, cmd.m_val);
        while (true)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted.");
            }
            switch (m_state)
            {
            case STATE_IDLE:
                return true;
            case STATE_DENY:
                response.append("Command denied.");
                m_state = STATE_IDLE;
                return false;
            case STATE_WAIT_OK:
                continue;
            case STATE_INTERRUPTED:
                // GMP connection cannot be used anymore after sending
                // was interrupted
                response.append("GMP connection closed");
                m_state = STATE_DISCONNECTED;
                return false;
            case STATE_DISCONNECTED:
                response.append("GMP connection broken.");
                return false;
            default:
                return false;
            }
        }
    }

    public void sendTalk(String text)
    {
        m_writeThread.sendTalk(text);
    }

    public synchronized WaitResult waitCmd(int cmd, int valMask,
                                           int valCondition)
    {
        WaitResult result = new WaitResult();
        result.m_success = false;
        while (true)
        {
            if (m_cmdQueue.size() > 0)
            {
                Cmd stackCmd = (Cmd)m_cmdQueue.get(0);
                if (stackCmd.m_cmd != cmd
                    || ((stackCmd.m_val & valMask) != valCondition))
                {
                    result.m_response =
                        ("Received " +
                         stackCmd.toString(m_size, m_lastQuery));
                    return result;
                }
                result.m_success = true;
                result.m_val = stackCmd.m_val;
                m_cmdQueue.remove(0);
                return result;
            }
            if (m_state == STATE_DISCONNECTED)
            {
                result.m_response = "GMP connection broken";
                return result;
            }
            try
            {
                Util.log("Waiting for " + Cmd.cmdToString(cmd) + " ...");
                wait();
                if (m_state == STATE_INTERRUPTED)
                {
                    result.m_response = "Interrupted";
                    return result;
                }
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted.");
            }
        }
    }

    private static final int STATE_IDLE = 0;

    private static final int STATE_DISCONNECTED = 1;

    private static final int STATE_WAIT_OK = 2;

    private static final int STATE_DENY = 3;

    private static final int STATE_INTERRUPTED = 4;

    private static final int STATE_WAIT_ANSWER_OK = 5;

    private static final int STATE_WAIT_ANSWER = 6;

    private boolean m_hisLastSeq = false;

    private boolean m_myLastSeq = false;

    private boolean m_simple;

    private int m_lastQuery = 0;

    private int m_state = STATE_IDLE;

    private int m_colorIndex = 0;

    private int m_pending = 0;

    private int m_size = 0;

    private int m_queryCount;

    private final static int m_queries[] = {
        Cmd.QUERY_COLOR, Cmd.QUERY_HANDICAP };

    private int[] m_inBuffer = new int[4];

    private InputStream m_in;

    private StringBuffer m_talkLine = new StringBuffer();

    private Vector m_cmdQueue = new Vector(32, 32);

    private WriteThread m_writeThread;

    private void answerQuery(int val)
    {
        int answer = 0;
        m_lastQuery = val;
        if (val == Cmd.QUERY_COLOR)
            answer = m_colorIndex;
        else if (val == Cmd.QUERY_SIZE)
            answer = m_size;
        else if (val == Cmd.QUERY_HANDICAP)
            answer = 1;
        sendCmd(Cmd.ANSWER, answer);
    }

    private boolean checkChecksum()
    {
        int b0 = m_inBuffer[0];
        int b2 = m_inBuffer[2];
        int b3 = m_inBuffer[3];
        int checksum = getChecksum(b0, b2, b3);
        return (checksum == m_inBuffer[1]);
    }

    private int getChecksum(int b0, int b2, int b3)
    {
        int checksum = ((b0 + b2 + b3) | 0x80) & 0xff;
        return checksum;
    }

    private boolean getAck()
    {
        int ack = (m_inBuffer[0] & 2);
        return (ack != 0);
    }

    private Cmd getCmd()
    {
        int cmd = (m_inBuffer[2] >> 4) & 0x7;
        int val = ((m_inBuffer[2] & 0x07) << 7) | (m_inBuffer[3] & 0x7f);
        return new Cmd(cmd, val);
    }

    private boolean getSeq()
    {
        int seq = (m_inBuffer[0] & 1);
        return (seq != 0);
    }

    private void handleByte(int b)
    {
        // Talk character
        if (b > 3 && b < 128)
        {
            char c = (char)b;
            if (c == '\r')
            {
                Util.log("talk char '\\r'");
                return;
            }
            if (c == '\n')
            {
                Util.log("talk char '\\n'");
                Util.log("talk: " + m_talkLine);
                m_talkLine.setLength(0);
                return;
            }
            Util.log("talk '" + c + "'");
            m_talkLine.append(c);
            return;
        }
        // Start byte
        if (b < 4)
        {
            if (m_pending > 0)
                Util.log("new start byte. discarding old bytes.");
            m_inBuffer[0] = b;
            m_pending = 3;
            return;
        }
        // Other command byte
        if (m_pending > 0)
        {
            int index = 4 - m_pending;
            assert(index > 0);
            assert(index < 4);
            m_inBuffer[index] = b;
            --m_pending;
            if (m_pending == 0)
            {
                if (! checkChecksum())
                {
                    Util.log("bad checksum");
                    return;
                }
                handlePacket();
            }
            return;
        }
        Util.log("discarding command byte.");
    }

    private void handleCmd(Cmd cmd)
    {
        Util.log("received " + cmd.toString(m_size, m_lastQuery));
        if (cmd.m_cmd == Cmd.QUERY)
            answerQuery(cmd.m_val);
        else if (cmd.m_cmd == Cmd.ANSWER)
        {
            if (m_queryCount < m_queries.length - 1)
            {
                ++m_queryCount;
                int val = m_queries[m_queryCount];
                m_lastQuery = val;
                sendCmd(Cmd.QUERY, val);
            }
            else
            {
                sendOk();
                m_state = STATE_IDLE;
                notifyAll();
            }
        }
        else if (cmd.m_cmd == Cmd.NEWGAME && m_simple)
        {
            m_cmdQueue.add(cmd);
            m_queryCount = 0;
            int val = m_queries[m_queryCount];
            m_lastQuery = val;
            sendCmd(Cmd.QUERY, val);
            notifyAll();
        }
        else
        {
            sendOk();
            if (cmd.m_cmd == Cmd.DENY)
            {
                if (m_state == STATE_WAIT_ANSWER_OK)
                    m_state = STATE_IDLE;
                else
                {
                    m_state = STATE_DENY;
                    notifyAll();
                }
            }
            else
            {
                m_cmdQueue.add(cmd);
                m_state = STATE_IDLE;
                notifyAll();
            }
        }
    }

    private void handlePacket()
    {
        Cmd cmd = getCmd();
        boolean seq = getSeq();
        boolean ack = getAck();
        if (m_state == STATE_WAIT_OK
            || m_state == STATE_WAIT_ANSWER_OK
            || m_state == STATE_WAIT_ANSWER)
        {
            if (cmd.m_cmd == Cmd.OK)
            {
                if (ack != m_myLastSeq)
                {
                    Util.log("sequence error");
                    return;
                }
                Util.log("received OK");
                m_state = STATE_IDLE;
                m_writeThread.stopSend();
                notifyAll();
                return;
            }
            if (seq == m_hisLastSeq)
            {
                Util.log("old cmd. resending OK");
                sendOk();
                return;
            }
            if (ack == m_myLastSeq)
            {
                m_state = STATE_IDLE;
                m_writeThread.stopSend();
                m_hisLastSeq = seq;
                handleCmd(cmd);
                return;
            }
            /* Actually GMP requires to abandon command on conflict,
               but since we might have sent it out repeatedly already,
               the opponent could have detected the conflict and accepted
               the resent command, so it's easier to ignore the conflict.
            */
            Util.log("ignore conflict");
            m_writeThread.resend();
        }
        else
        {
            assert(m_state == STATE_IDLE);
            if (cmd.m_cmd == Cmd.OK)
            {
                Util.log("ignoring unexpected OK");
                return;
            }
            if (ack != m_myLastSeq)
            {
                Util.log("ignoring old cmd");
                return;
            }
            if (seq == m_hisLastSeq)
            {
                Util.log("old cmd. resending OK");
                sendOk();
                return;
            }
            m_hisLastSeq = seq;
            handleCmd(cmd);
        }
    }

	private byte makeCmdByte1(int cmd, int val)
	{
        val = val & 0x000003FF;
		return (byte)(0x0080 | (cmd << 4) | (val >> 7));
	}
	
	private byte makeCmdByte2(int val)
	{
        return (byte)(0x0080 | (val & 0x0000007F));
	}	

    private boolean sendCmd(int cmd, int val)
    {
        Util.log("send " + (new Cmd(cmd, val)).toString(m_size, m_lastQuery));
        boolean isOkCmd = (cmd == Cmd.OK);
        if (! isOkCmd)
            m_myLastSeq = ! m_myLastSeq;
        byte packet[] = new byte[4];
        packet[0] = (byte)(m_myLastSeq ? 1 : 0);
        packet[0] |= (byte)(m_hisLastSeq ? 2 : 0);
        packet[2] = makeCmdByte1(cmd, val);
        packet[3] = makeCmdByte2(val);
        setChecksum(packet);
        m_writeThread.sendPacket(packet, isOkCmd);
        if (! isOkCmd)
        {
            if (cmd == Cmd.ANSWER)
                m_state = STATE_WAIT_ANSWER_OK;
            else if (cmd == Cmd.QUERY)
                m_state = STATE_WAIT_ANSWER;
            else
                m_state = STATE_WAIT_OK;
        }
        return true;
    }

    private boolean sendOk()
    {
        return sendCmd(Cmd.OK, 0);
    }

    private void setChecksum(byte[] packet)
    {
        int b0 = packet[0];
        int b2 = packet[2];
        int b3 = packet[3];
        int checksum = getChecksum(b0, b2, b3);
        packet[1] = (byte)(0x0080 | checksum);
    }
}

/**
   This class is final because it starts a thread in it's constructor which
   might conflict with subclassing because the subclass constructor will
   be called after the thread is started.
*/
public final class Gmp
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    public static class Move
    {
        public boolean m_isBlack;
        
        public int m_x;
        
        public int m_y;
    }

    /** Create a Gmp.
        @param size board size 1-22
        Gmp supports only sizes up to 22 (number of bits in MOVE cmd)
        @param colorIndex color computer color on your side
        0=unknown, 1=white, 2=black
    */
    public Gmp(InputStream input, OutputStream output, int size,
               int colorIndex, boolean simple)
    {
        m_size = size;
        m_readThread = new ReadThread(input, output, size, colorIndex, simple);
        m_readThread.start();
    }

    public void interruptCommand()
    {
        m_readThread.interruptCommand();
    }

    public boolean newGame(int size, StringBuffer response)
    {
        if (size != m_size)
        {
            response.append("Board size must be " + m_size);
            return false;
        }
        return m_readThread.send(new Cmd(Cmd.NEWGAME, 0), response);
    }

    public boolean play(boolean isBlack, int x, int y, StringBuffer response)
    {
        if (x >= m_size || y >= m_size)
        {
            response.append("Invalid coordinates");
            return false;
        }
        int val = (isBlack ? 0 : Cmd.MASK_MOVE_COLOR);
        if (x >= 0 && y >= 0)
            val |= (1 + x + y * m_size);
        return m_readThread.send(new Cmd(Cmd.MOVE, val), response);
    }

    public boolean queue(StringBuffer response)
    {
        return m_readThread.queue(response);
    }

    public void sendTalk(String text)
    {
        m_readThread.sendTalk(text);
    }

    public boolean undo(StringBuffer response)
    {
        return m_readThread.send(new Cmd(Cmd.UNDO, 1), response);
    }

    public Move waitMove(boolean isBlack, StringBuffer response)
    {
        ReadThread.WaitResult result;
        int valCondition = (isBlack ? 0 : Cmd.MASK_MOVE_COLOR);
        result = m_readThread.waitCmd(Cmd.MOVE, Cmd.MASK_MOVE_COLOR,
                                      valCondition);
        if (result.m_response != null)
            response.append(result.m_response);
        if (! result.m_success)
            return null;
        return Cmd.parseMove(result.m_val, m_size);
    }

    public boolean waitNewGame(int size, StringBuffer response)
    {
        if (size != m_size)
        {
            response.append("Board size must be " + m_size);
            return false;
        }
        ReadThread.WaitResult result;
        result = m_readThread.waitCmd(Cmd.NEWGAME, 0, 0);
        if (result.m_response != null)
            response.append(result.m_response);
        return result.m_success;
    }

    private int m_size;

    private ReadThread m_readThread;
}

//-----------------------------------------------------------------------------
