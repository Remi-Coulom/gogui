//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gmp;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

class Command
{
    public static final int OK = 0;

    public static final int DENY = 1;

    public static final int NEWGAME = 2;

    public static final int QUERY = 3;

    public static final int ANSWER = 4;

    public static final int MOVE = 5;

    public static final int UNDO = 6;

    public static final int EXTENDED = 7;

    public int m_cmd;

    public int m_val;

    public Command(int cmd, int val)
    {
        m_cmd = cmd;
        m_val = val;
    }

    public boolean equals(Command cmd)
    {
        return (cmd.m_cmd == m_cmd && cmd.m_val == m_val);
    }

    public String toString()
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
            break;
        case ANSWER:
            buffer.append("ANSWER");
            break;
        case MOVE:
            buffer.append("MOVE");
            break;
        case UNDO:
            buffer.append("UNDO");
            break;
        case EXTENDED:
            buffer.append("EXTENDED");
            break;
        default:
            buffer.append(m_cmd);
            break;
        }
        buffer.append(' ');
        buffer.append(m_val);
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

        public int m_value;
    }

    public ReadThread(InputStream input, OutputStream output)
    {
        m_in = input;
        m_out = output;
    }

    public int getSize()
    {
        return 19;
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
                    m_status = STATUS_DISCONNECTED;
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

    public synchronized boolean send(Command cmd, StringBuffer response)
    {
        if (m_status == STATUS_WAIT_OK)
        {
            response.append("Command in progress");
            return false;
        }
        if (m_commandStack.size() > 0)
        {
            Command stackCommand = (Command)m_commandStack.get(0);
            if (! stackCommand.equals(cmd))
            {
                response.append("Received " + stackCommand.toString());
                return false;
            }
            m_commandStack.remove(0);
            return true;
        }
        sendCommand(cmd.m_cmd, cmd.m_val);
        try
        {
            wait();
        }
        catch (InterruptedException e)
        {
            System.err.println("Interrupted.");
        }
        switch (m_status)
        {
        case STATUS_IDLE:
            return true;
        case STATUS_DENY:
            response.append("Command denied.");
            m_status = STATUS_IDLE;
            return false;
        case STATUS_CONFLICT:
            response.append("Conflict.");
            m_status = STATUS_IDLE;
            return false;
        case STATUS_WAIT_OK:
            response.append("Command in progress.");
            return false;
        default:
            return false;
        }
    }

    public synchronized WaitResult waitCmd(int cmd, int valueMask,
                                           int valueCondition)
    {
        WaitResult result = new WaitResult();
        result.m_success = false;
        while (true)
        {
            if (m_commandStack.size() > 0)
            {
                Command stackCommand = (Command)m_commandStack.get(0);
                if (stackCommand.m_cmd != cmd
                    || ((stackCommand.m_cmd & valueMask) != valueCondition))
                {
                    result.m_response = ("Received " + stackCommand);
                    return result;
                }
                result.m_success = true;
                result.m_value = stackCommand.m_val;
                m_commandStack.remove(0);
                return result;
            }
            try
            {
                wait();
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

    private static final int STATUS_IDLE = 0;

    private static final int STATUS_DISCONNECTED = 1;

    private static final int STATUS_WAIT_OK = 2;

    private static final int STATUS_CONFLICT = 3;

    private static final int STATUS_DENY = 4;

    private boolean m_hisLastSeq = false;

    private boolean m_myLastSeq = false;

    private byte[] m_outputBuffer = new byte[4];

    private int m_status = STATUS_IDLE;

    private int m_pending = 0;

    private int[] m_inBuffer = new int[4];

    private InputStream m_in;

    private OutputStream m_out;

    private Vector m_commandStack = new Vector(32, 32);

    private void answerQuery(int val)
    {
        int answer = 0;
        if (val == QUERY_COLOR)
            answer = 1; // XXX
        else if (val == QUERY_SIZE)
            answer = getSize();
        else if (val == QUERY_HANDICAP)
            answer = 1;
        sendCommand(Command.ANSWER, answer);
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

    private Command getCommand()
    {
        int cmd = (m_inBuffer[2] >> 4) & 0x7;
        int val = ((m_inBuffer[2] & 0x03) << 7) | (m_inBuffer[3] & 0x7f);
        return new Command(cmd, val);
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
            log("talk '" + (char)b + "'");
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

    private void handleCommand(Command cmd)
    {
        log("received " + cmd);
        if (cmd.m_cmd == Command.QUERY)
            answerQuery(cmd.m_val);
        else
        {
            m_commandStack.add(cmd);
            sendOk();
            m_status = STATUS_IDLE;
        }
    }

    private void handlePacket()
    {
        Command cmd = getCommand();
        boolean seq = getSeq();
        boolean ack = getAck();
        if (m_status == STATUS_WAIT_OK)
        {
            if (cmd.m_cmd == Command.OK)
            {
                if (ack != m_myLastSeq)
                {
                    log("sequence error");
                    return;
                }
                m_status = STATUS_IDLE;
                return;
            }
            if (seq == m_hisLastSeq)
            {
                log("old command. resending OK");
                sendOk();
                return;
            }
            if (ack == m_myLastSeq)
            {
                m_status = STATUS_IDLE;
                m_hisLastSeq = seq;
                handleCommand(cmd);
                notifyAll();
                return;
            }
            m_status = STATUS_CONFLICT;
            m_myLastSeq = ! m_myLastSeq;
        }
        else
        {
            assert(m_status == STATUS_IDLE);
            if (cmd.m_cmd == Command.OK)
            {
                log("ignoring unexpected OK");
                return;
            }
            if (ack != m_myLastSeq)
            {
                log("ignoring old command");
                return;
            }
            if (seq == m_hisLastSeq)
            {
                log("old command. resending OK");
                sendOk();
                return;
            }
            m_hisLastSeq = seq;
            handleCommand(cmd);
            notifyAll();
        }
    }

    private void log(String line)
    {
        System.err.println("gmp: " + line);
    }

	private byte makeCommandByte1(int command, int value)
	{
        value = value & 0x000003FF;
		return (byte)(0x0080 | (command << 4) | (value >> 7));
	}
	
	private byte makeCommandByte2(int value)
	{
        return (byte)(0x0080 | (value & 0x0000007F));
	}	

    private boolean sendCommand(int cmd, int val)
    {
        log("send " + new Command(cmd, val));
        try
        {
            if (cmd != Command.OK)
                m_myLastSeq = ! m_myLastSeq;
            m_outputBuffer[0] = (byte)(m_myLastSeq ? 1 : 0);
            m_outputBuffer[0] |= (byte)(m_hisLastSeq ? 2 : 0);
            m_outputBuffer[2] = makeCommandByte1(cmd, val);
            m_outputBuffer[3] = makeCommandByte2(val);
            setChecksum();
            writeOutputBuffer();
            m_status = STATUS_WAIT_OK;
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
        return sendCommand(Command.OK, 0);
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

    public Gmp(InputStream input, OutputStream output)
    {
        m_readThread = new ReadThread(input, output);
        log("starting read thread");
        m_readThread.start();
    }

    public boolean newGame(int size, StringBuffer response)
    {
        if (size != m_readThread.getSize())
        {
            response.append("Board size must be " + m_readThread.getSize());
            return false;
        }
        return m_readThread.send(new Command(Command.NEWGAME, 0), response);
    }

    public boolean play(boolean isBlack, int x, int y, StringBuffer response)
    {
        int size = m_readThread.getSize();
        if (x >= size || y >= size)
        {
            response.append("Invalid coordinates");
            return false;
        }
        int val = (isBlack ? 0 : 0x200);
        if (x >= 0 && y >= 0)
        val &= (1 + x + y * size);
        return m_readThread.send(new Command(Command.MOVE, val), response);
    }

    public Move waitMove(boolean isBlack, StringBuffer response)
    {
        ReadThread.WaitResult result;
        int valCondition = (isBlack ? 0 : 0x200);
        result = m_readThread.waitCmd(Command.MOVE, 0x200, valCondition);
        if (result.m_response != null)
            response.append(result.m_response);
        if (! result.m_success)
            return null;
        return parseMove(result.m_value);
    }

    private ReadThread m_readThread;

    private String m_lastError;


    private void log(String line)
    {
        System.err.println("gmp: " + line);
    }

    private Move parseMove(int value)
    {
        Move move = new Move();
        move.m_isBlack = ((value & 0x200) == 0);
        value &= 0x1ff;
        if (value == 0)
        {
            move.m_x = -1;
            move.m_y = -1;
        }
        else
        {
            value -= 1;
            move.m_x = value % m_readThread.getSize();
            move.m_y = value / m_readThread.getSize();
        }
        return move;
    }

}

//-----------------------------------------------------------------------------
