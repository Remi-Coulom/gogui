//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import go.Color;
import go.Point;
import go.Move;
import utils.StringUtils;
import utils.MessageQueue;

//----------------------------------------------------------------------------

/** Interface to a Go program that uses GTP over the standard I/O streams.
    This class is final because it starts a thread in its constructor which
    might conflict with subclassing because the subclass constructor will
    be called after the thread is started.
*/
public final class Gtp
{
    /** Exception indication the failure of a GTP command. */
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    public interface TimeoutCallback
    {
        public boolean askContinue();
    }

    /** Callback interface for logging or displaying the GTP stream. */
    public interface IOCallback
    {
        public void receivedInvalidResponse(String s);

        public void receivedResponse(boolean error, String s);

        public void receivedStdErr(String s);

        public void sentCommand(String s);
    }    

    public Gtp(String program, boolean log, IOCallback callback)
        throws Gtp.Error
    {
        m_log = log;
        m_program = program;
        m_callback = callback;
        if (m_program.indexOf("%SRAND") >= 0)
        {
            // RAND_MAX in stdlib.h ist at least 32767
            final int RAND_MAX = 32767;
            int rand = (int)(Math.random() * (RAND_MAX + 1));
            m_program =
                m_program.replaceAll("%SRAND", Integer.toString(rand));
        }
        Runtime runtime = Runtime.getRuntime();
        try
        {
            // Create command array with StringUtils::tokenize
            // because Runtime.exec(String) uses a default StringTokenizer
            // which does not respect ".
            m_process = runtime.exec(StringUtils.tokenize(program));
        }
        catch (IOException e)
        {
            throw new Gtp.Error("Could not create " + program + ":\n" +
                                e.getMessage());
        }
        Reader reader = new InputStreamReader(m_process.getInputStream());
        m_in = new BufferedReader(reader);
        m_out = new PrintWriter(m_process.getOutputStream());
        m_illegalState = false;
        m_timedOut = false;
        m_isProgramDead = false;
        
        m_queue = new MessageQueue();
        m_inputThread = new InputThread(m_process.getInputStream(), m_queue);
        m_errorThread = new ErrorThread(m_process.getErrorStream(), m_queue);
        m_inputThread.start();
        m_errorThread.start();
    }
    
    public void close()
    {
        m_out.close();
    }

    public void destroyProcess()
    {
        m_process.destroy();
    }

    public String getResponse()
    {
        return m_response;
    }

    public String getCommandBoardsize(int size)
    {
        if (m_protocolVersion == 2)
            return ("boardsize " + size);
        else
            return null;
    }

    public String getCommandClearBoard(int size)
    {
        if (m_protocolVersion == 1)
            return "boardsize " + size;
        else
            return "clear_board";
    }

    public String getCommandGenmove(Color color)
    {
        String c = color.toString();
        if (m_protocolVersion == 1)
            return "genmove_" + c;
        else
            return "genmove " + c;
    }

    public String getCommandPlay(Color color)
    {
        
        String command = "";
        if (m_protocolVersion == 2)
            command = "play ";
        command = command + color.toString();
        return command;
    }

    public String getCommandPlay(Move move)
    {
        
        String command = getCommandPlay(move.getColor());
        go.Point p = move.getPoint();
        if (p == null)
            command = command + " pass";
        else
            command = command + " " + p.toString();
        return command;
    }

    public double getCpuTime() throws Gtp.Error
    {
        try
        {
            return Double.parseDouble(sendCommand("cputime"));
        }
        catch (NumberFormatException e)
        {
            throw new Gtp.Error("Invalid response to cputime command");
        }
    }

    /** Get full response including status and ID and last command. */
    public String getFullResponse()
    {
        return m_fullResponse;
    }

    public String getProgramCommand()
    {
        return m_program;
    }

    /** Get protocol version.
        You have to call queryProtocolVersion() first, otherwise this method
        will always return 1.
    */
    public int getProtocolVersion()
    {
        return m_protocolVersion;
    }

    public Vector getSupportedCommands()
    {
        Vector result = new Vector(128, 128);
        if (m_supportedCommands != null)
            for (int i = 0; i < m_supportedCommands.length; ++i)
                result.add(m_supportedCommands[i]);
        return result;
    }

    public boolean isCommandSupported(String command)
    {
        if (m_supportedCommands == null)
            return false;
        for (int i = 0; i < m_supportedCommands.length; ++i)
            if (m_supportedCommands[i].equals(command))
                return true;
        return false;
    }

    public boolean isCpuTimeSupported()
    {
        return isCommandSupported("cputime");
    }

    public boolean isInterruptSupported()
    {
        return (m_isInterruptCommentSupported || m_pid != null);
    }

    /** Check if program is dead or in illegal state. */
    public boolean isProgramDead()
    {
        return (m_isProgramDead || m_illegalState || m_timedOut);
    }

    public void queryInterruptSupport()
    {
        try
        {
            if (isCommandSupported("gogui_interrupt"))
            {
                sendCommand("gogui_interrupt");
                m_isInterruptCommentSupported = true;
            }
            else if (isCommandSupported("gogui_sigint"))
                m_pid = sendCommand("gogui_sigint").trim();
        }
        catch (Gtp.Error e)
        {
        }
    }

    /** Queries the name.
        @return Name or "Unknown Program" if name command not supported
    */
    public String queryName()
    {
        try
        {
            return sendCommand("name");
        }
        catch (Error e)
        {
            return "Unknown Program";
        }
    }

    public void queryProtocolVersion() throws Gtp.Error
    {
        try
        {            
            String response;
            try
            {
                response = sendCommand("protocol_version");
            }
            catch (Error e)
            {
                m_protocolVersion = 1;
                return;
            }
            int v = Integer.parseInt(response);
            if (v < 1 || v > 2)
                throw new Error("Unknown protocol version: " + v);
            m_protocolVersion = v;
        }
        catch (NumberFormatException e)
        {
            throw new Error("Invalid protocol version");
        }
    }

    public void querySupportedCommands() throws Gtp.Error
    {
        String command = (m_protocolVersion == 1 ? "help" : "list_commands");
        String response = sendCommand(command);
        m_supportedCommands = StringUtils.tokenize(response);
        for (int i = 0; i < m_supportedCommands.length; ++i)
            m_supportedCommands[i] = m_supportedCommands[i].trim();
    }

    public String queryVersion()
    {
        try
        {
            return sendCommand("version");
        }
        catch (Error e)
        {
            return "";
        }
    }

    public String sendCommand(String command) throws Gtp.Error
    {
        return sendCommand(command, -1, null);
    }

    public String sendCommand(String command, long timeout,
                              TimeoutCallback timeoutCallback)
        throws Gtp.Error
    {
        assert(! command.trim().equals(""));
        assert(! command.trim().startsWith("#"));
        m_timeoutCallback = timeoutCallback;
        m_fullResponse = "";
        m_response = "";
        if (m_isProgramDead)
            throw new Error("Program is dead");
        if (m_timedOut)
            throw new Error("Program timed out");
        if (m_illegalState)
            throw new Error("Program sent invalid response");
        if (m_autoNumber)
        {
            ++m_commandNumber;
            command = Integer.toString(m_commandNumber) + " " + command;
        }
        log(">> " + command);
        m_out.println(command);
        m_out.flush();
        if (m_out.checkError())
        {
            m_isProgramDead = true;
            throw new Error("Go program died");
        }
        if (m_callback != null)
            m_callback.sentCommand(command);
        readResponse(timeout);
        return m_response;
    }

    public void sendCommandBoardsize(int size) throws Gtp.Error
    {
        String command = getCommandBoardsize(size);
        if (command != null)
            sendCommand(command);
    }

    public String sendCommandClearBoard(int size) throws Gtp.Error
    {
        return sendCommand(getCommandClearBoard(size));
    }

    public String sendCommandPlay(Move move) throws Gtp.Error
    {
        return sendCommand(getCommandPlay(move));
    }

    /** Send comment.
        @param comment comment line (must start with '#').
    */
    public void sendComment(String comment)
    {
        assert(comment.trim().startsWith("#"));
        log(">> " + comment);
        if (m_callback != null)
            m_callback.sentCommand(comment);
        m_out.println(comment);
        m_out.flush();
    }

    public void sendInterrupt() throws Gtp.Error
    {
        if (m_isInterruptCommentSupported)
            sendComment("# interrupt");
        else if (m_pid != null)
        {
            String command = "kill -INT " + m_pid;
            log(" " + command);
            Runtime runtime = Runtime.getRuntime();
            try
            {
                Process process = runtime.exec(command);
                int result = process.waitFor();
                if (result != 0)
                    throw new Gtp.Error("Command \"" + command
                                        + "\" returned " + result + ".");
            }
            catch (IOException e)
            {
                throw new Gtp.Error("Could not run command " + command +
                                    ":\n" + e);
            }
            catch (InterruptedException e)
            {
            }
        }
        else
            throw new Gtp.Error("Interrupt not supported");
    }

    public void enableAutoNumber()
    {
        m_autoNumber = true;
    }

    public void setLogPrefix(String prefix)
    {
        synchronized (this)
        {
            m_logPrefix = prefix;
        }
    }

    public void waitForExit()
    {
        try
        {
            m_process.waitFor();
        }
        catch (InterruptedException e)
        {
            System.err.println("Interrupted");
        }
    }

    private static class ReadMessage
    {
        public ReadMessage(boolean isError, String text)
        {
            m_isError = isError;
            m_text = text;
        }

        public boolean m_isError;

        public String m_text;
    }
    
    private static class InputThread
        extends Thread
    {
        InputThread(InputStream in, MessageQueue queue)
        {
            m_in = new BufferedReader(new InputStreamReader(in));
            m_queue = queue;
        }

        public void run()
        {
            try
            {
                mainLoop();
            }
            catch (Throwable t)
            {
                StringUtils.printException(t);
            }
        }

        private BufferedReader m_in;

        private MessageQueue m_queue;

        private void mainLoop() throws IOException, InterruptedException
        {
            while (true)
            {
                String line = m_in.readLine();
                Thread.yield(); // Give ErrorThread a chance to read first
                m_queue.put(new ReadMessage(false, line));
                if (line == null)
                    return;
            }
        }
    }

    private class ErrorThread
        extends Thread
    {
        public ErrorThread(InputStream in, MessageQueue queue)
        {
            m_in = new InputStreamReader(in);
            m_queue = queue;
        }

        public void run()
        {
            try
            {
                mainLoop();
            }
            catch (Throwable t)
            {
                StringUtils.printException(t);
            }
        }

        private Reader m_in;

        private MessageQueue m_queue;

        private void mainLoop() throws IOException, InterruptedException
        {
            int size = 1024;
            char[] buffer = new char[size];
            while (true)
            {
                int n = m_in.read(buffer, 0, size);
                String text = null;
                if (n > 0)
                    text = new String(buffer, 0, n);
                m_queue.put(new ReadMessage(true, text));
                if (text == null)
                    return;
            }
        }
    }

    private boolean m_autoNumber;

    private boolean m_isInterruptCommentSupported;

    private boolean m_isProgramDead;

    private boolean m_illegalState;

    private boolean m_log;

    private boolean m_timedOut;

    private int m_protocolVersion = 1;

    private int m_commandNumber;

    private BufferedReader m_in;

    private IOCallback m_callback;

    private PrintWriter m_out;

    private Process m_process;

    private String m_fullResponse;

    private String m_response;

    private String m_logPrefix;

    private String m_pid;

    private String m_program;

    private String[] m_supportedCommands;

    private ErrorThread m_errorThread;

    private InputThread m_inputThread;

    private MessageQueue m_queue;

    private TimeoutCallback m_timeoutCallback;

    private void handleErrorStream(String text)
    {
        if (text == null)
            return;
        if (m_log)
            System.err.print(text);
        if (m_callback != null)
            m_callback.receivedStdErr(text);
    }

    private static boolean isResponseLine(String line)
    {
        if (line.length() < 2)
            return false;
        char c = line.charAt(0);
        return (c == '=' || c == '?');
    }

    private synchronized void log(String msg)
    {
        if (m_log)
        {
            if (m_logPrefix != null)
                System.err.print(m_logPrefix);
            System.err.println(msg);
        }
    }

    private String readLine(long timeout) throws Gtp.Error
    {
        while (true)
        {            
            ReadMessage message;
            if (timeout < 0)
                message = (ReadMessage)m_queue.waitFor();
            else
            {
                message = null;
                while (message == null)
                {
                    message = (ReadMessage)m_queue.waitFor(timeout);
                    if (message == null)
                    {
                        assert(m_timeoutCallback != null);
                        if (! m_timeoutCallback.askContinue())
                        {
                            m_timedOut = true;
                            throw new Gtp.Error("Program timed out");
                        }
                    }
                }
            }
            if (! message.m_isError)
            {
                String line = message.m_text;
                if (line == null)
                {
                    m_isProgramDead = true;
                    while (! m_queue.isEmpty())
                    {
                        message = (ReadMessage)m_queue.waitFor();
                        assert(message.m_isError);
                        handleErrorStream(message.m_text);
                    }
                    throw new Error("Go program died");
                }
                log("<< " + line);
                return line;
            }
            else
            {
                handleErrorStream(message.m_text);
            }
        }
    }

    private void readResponse(long timeout) throws Gtp.Error
    {
        String line = "";
        while (line.trim().equals(""))
            line = readLine(timeout);
        StringBuffer response = new StringBuffer(line);
        response.append("\n");
        if (! isResponseLine(line))
        {
            m_fullResponse = response.toString();
            m_callback.receivedInvalidResponse(response.toString());
            m_illegalState = true;
            throw new Error("Invalid response:\n" + line);
        }
        boolean error = (line.charAt(0) != '=');
        boolean done = false;
        while (! done)
        {
            line = readLine(timeout);
            done = line.equals("");
            response.append(line);
            response.append("\n");
        }
        if (m_callback != null)
            m_callback.receivedResponse(error, response.toString());
        m_fullResponse = response.toString();
        assert(response.length() >= 4);            
        int index = response.indexOf(" ");
        if (index < 0)
            m_response = response.substring(0, response.length() - 2);
        else
            m_response =
                response.substring(index + 1, response.length() - 2);
        if (error)
        {
            String message = m_response.trim();
            if (message.equals(""))
                message = "GTP command failed";
            throw new Error(message);
        }
    }
}

//----------------------------------------------------------------------------
