//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gmp;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

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

    public int m_cmd;

    public int m_val;

    public Cmd(int cmd, int val)
    {
        m_cmd = cmd;
        m_val = val;
    }

    public boolean equals(Cmd cmd)
    {
        return (cmd.m_cmd == m_cmd && cmd.m_val == m_val);
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
        case 0:
            return "GAME";
        case 1:
            return "BUFFER";
        case 2:
            return "VERSION";
        case 3:
            return "STONES";
        case 4:
            return "TIMEBLACK";
        case 5:
            return "TIMEWHITE";
        case 6:
            return "CHARSET";
        case 7:
            return "RULES";
        case 8:
            return "HANDICAP";
        case 9:
            return "SIZE";
        case 10:
            return "TIMELIMIT";
        case 11:
            return "COLOR";
        case 12:
            return "WHO";
        default:
            return Integer.toString(val);
        }
    }

    public String toString(int size)
    {
        StringBuffer buffer = new StringBuffer(32);
        switch (m_cmd)
        {
        case OK:
            buffer.append("OK");
            break;
        case DENY:
            buffer.append("DENY");
            break;
        case NEWGAME:
            buffer.append("NEWGAME");
            break;
        case QUERY:
            buffer.append("QUERY");
            buffer.append(' ');
            buffer.append(Cmd.queryValToString(m_val));
            break;
        case ANSWER:
            buffer.append("ANSWER");
            buffer.append(' ');
            buffer.append(m_val);
            break;
        case MOVE:
            buffer.append("MOVE");
            buffer.append(' ');
            buffer.append(Cmd.moveValToString(m_val, size));
            break;
        case UNDO:
            buffer.append("UNDO");
            buffer.append(' ');
            buffer.append(m_val);
            break;
        case EXTENDED:
            buffer.append("EXTENDED");
            buffer.append(' ');
            buffer.append(m_val);
            break;
        default:
            buffer.append(m_cmd);
            break;
        }
        return buffer.toString();
    }
};

class ReadThread
    extends Thread
{
    public static class WaitResult
    {
        public boolean m_success;

        public String m_response;

        public int m_val;
    }

    public ReadThread(InputStream input, OutputStream output,
                      int size, int colorIndex)
    {
        assert(size >= 1);
        assert(size <= 22);
        assert(colorIndex >= 0);
        assert(colorIndex <= 2);
        m_in = input;
        m_out = output;
        m_size = size;
        m_colorIndex = colorIndex;
    }

    public void interruptCommand()
    {
        synchronized (this)
        {
            if (m_state == STATE_IDLE)
            {
                m_state = STATE_INTERRUPTED;
                notifyAll();
            }
        }
    }

    public void run()
    {
        try
        {
            while (true)
            {
                int b = m_in.read();
                if (b < 0)
                {
                    log("input stream was closed");
                    m_state = STATE_DISCONNECTED;
                    break;
                }
                log("recv " + format(b));
                handleByte(b);
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            System.err.println(msg);
        }
    }

    public synchronized boolean send(Cmd cmd, StringBuffer response)
    {
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
        if (m_cmdStack.size() > 0)
        {
            Cmd stackCmd = (Cmd)m_cmdStack.get(0);
            if (! stackCmd.equals(cmd))
            {
                response.append("Received " +
                                stackCmd.toString(m_size));
                return false;
            }
            m_cmdStack.remove(0);
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
            case STATE_CONFLICT:
                response.append("Conflict.");
                m_state = STATE_IDLE;
                return false;
            case STATE_WAIT_OK:
                continue;
            default:
                return false;
            }
        }
    }

    public boolean sendText(String text)
    {
        int size = text.length();
        byte buffer[] = new byte[size + 1];
        for (int i = 0; i < size; ++i)
        {
            byte b = (byte)text.charAt(i);
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
            return false;
        }
        return true;
    }

    public synchronized WaitResult waitCmd(int cmd, int valMask,
                                           int valCondition)
    {
        WaitResult result = new WaitResult();
        result.m_success = false;
        while (true)
        {
            if (m_cmdStack.size() > 0)
            {
                Cmd stackCmd = (Cmd)m_cmdStack.get(0);
                if (stackCmd.m_cmd != cmd
                    || ((stackCmd.m_val & valMask) != valCondition))
                {
                    result.m_response = ("Received " +
                                         stackCmd.toString(m_size));
                    return result;
                }
                result.m_success = true;
                result.m_val = stackCmd.m_val;
                m_cmdStack.remove(0);
                return result;
            }
            if (m_state == STATE_DISCONNECTED)
            {
                result.m_response = "GMP connection broken";
                return result;
            }
            try
            {
                assert(m_state == STATE_IDLE);
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

    private static final int QUERY_HANDICAP = 8;

    private static final int QUERY_SIZE = 9;

    private static final int QUERY_COLOR = 11;

    private static final int STATE_IDLE = 0;

    private static final int STATE_DISCONNECTED = 1;

    private static final int STATE_WAIT_OK = 2;

    private static final int STATE_CONFLICT = 3;

    private static final int STATE_DENY = 4;

    private static final int STATE_INTERRUPTED = 5;

    private boolean m_hisLastSeq = false;

    private boolean m_myLastSeq = false;

    private byte[] m_outputBuffer = new byte[4];

    private int m_state = STATE_IDLE;

    private int m_colorIndex = 0;

    private int m_pending = 0;

    private int m_size = 0;

    private int[] m_inBuffer = new int[4];

    private InputStream m_in;

    private OutputStream m_out;

    private StringBuffer m_talkLine = new StringBuffer();

    private Vector m_cmdStack = new Vector(32, 32);

    private void answerQuery(int val)
    {
        int answer = 0;
        if (val == QUERY_COLOR)
            answer = m_colorIndex;
        else if (val == QUERY_SIZE)
            answer = m_size;
        else if (val == QUERY_HANDICAP)
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

	private static String format(int i)
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

    private synchronized void handleByte(int b)
    {
        // Talk character
        if (b > 3 && b < 128)
        {
            char c = (char)b;
            if (c == '\r')
            {
                log("talk char '\\r'");
                return;
            }
            if (c == '\n')
            {
                log("talk char '\\n'");
                log("talk: " + m_talkLine);
                m_talkLine.setLength(0);
                return;
            }
            log("talk '" + c + "'");
            m_talkLine.append(c);
            return;
        }
        // Start byte
        if (b < 4)
        {
            if (m_pending > 0)
                log("new start byte. discarding old bytes.");
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
                    log("bad checksum");
                    return;
                }
                handlePacket();
            }
            return;
        }
        log("discarding command byte.");
    }

    private void handleCmd(Cmd cmd)
    {
        log("received " + cmd.toString(m_size));
        if (cmd.m_cmd == Cmd.QUERY)
            answerQuery(cmd.m_val);
        else
        {
            m_cmdStack.add(cmd);
            sendOk();
            m_state = STATE_IDLE;
        }
    }

    private void handlePacket()
    {
        Cmd cmd = getCmd();
        boolean seq = getSeq();
        boolean ack = getAck();
        if (m_state == STATE_WAIT_OK)
        {
            if (cmd.m_cmd == Cmd.OK)
            {
                if (ack != m_myLastSeq)
                {
                    log("sequence error");
                    return;
                }
                log("received OK");
                m_state = STATE_IDLE;
                return;
            }
            if (seq == m_hisLastSeq)
            {
                log("old cmd. resending OK");
                sendOk();
                return;
            }
            if (ack == m_myLastSeq)
            {
                m_state = STATE_IDLE;
                m_hisLastSeq = seq;
                handleCmd(cmd);
                notifyAll();
                return;
            }
            log("conflict");
            m_state = STATE_CONFLICT;
            m_myLastSeq = ! m_myLastSeq;
            notifyAll();
        }
        else
        {
            assert(m_state == STATE_IDLE);
            if (cmd.m_cmd == Cmd.OK)
            {
                log("ignoring unexpected OK");
                return;
            }
            if (ack != m_myLastSeq)
            {
                log("ignoring old cmd");
                return;
            }
            if (seq == m_hisLastSeq)
            {
                log("old cmd. resending OK");
                sendOk();
                return;
            }
            m_hisLastSeq = seq;
            handleCmd(cmd);
            notifyAll();
        }
    }

    private void log(String line)
    {
        System.err.println("gmp: " + line);
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
        log("send " + (new Cmd(cmd, val)).toString(m_size));
        try
        {
            if (cmd != Cmd.OK)
                m_myLastSeq = ! m_myLastSeq;
            m_outputBuffer[0] = (byte)(m_myLastSeq ? 1 : 0);
            m_outputBuffer[0] |= (byte)(m_hisLastSeq ? 2 : 0);
            m_outputBuffer[2] = makeCmdByte1(cmd, val);
            m_outputBuffer[3] = makeCmdByte2(val);
            setChecksum();
            writeOutputBuffer();
            if (cmd != Cmd.OK)
                m_state = STATE_WAIT_OK;
            return true;
        }
        catch (IOException e)
        {
            log("write error");
            return false;
        }
    }

    private boolean sendOk()
    {
        return sendCmd(Cmd.OK, 0);
    }

    private void setChecksum()
    {
        int b0 = m_outputBuffer[0];
        int b2 = m_outputBuffer[2];
        int b3 = m_outputBuffer[3];
        int checksum = getChecksum(b0, b2, b3);
        m_outputBuffer[1] = (byte)(0x0080 | checksum);
    }

    private void writeOutputBuffer() throws IOException
    {
        log("send "
            + format(m_outputBuffer[0]) + " "
            + format(m_outputBuffer[1]) + " "
            + format(m_outputBuffer[2]) + " "
            + format(m_outputBuffer[3]));
        m_out.write(m_outputBuffer);
        m_out.flush();
    }
}

public class Gmp
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
               int colorIndex)
    {
        m_size = size;
        m_readThread = new ReadThread(input, output, size, colorIndex);
        log("starting read thread");
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

    public boolean sendText(String text)
    {
        return m_readThread.sendText(text);
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

    private int m_size;

    private ReadThread m_readThread;

    private String m_lastError;


    private void log(String line)
    {
        System.err.println("gmp: " + line);
    }
}

//-----------------------------------------------------------------------------
