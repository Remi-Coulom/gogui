//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.util.MessageQueue;
import net.sf.gogui.util.ProcessUtil;

/** Interface to a Go program that uses GTP over the standard I/O streams.
    <p>
    This class is final because it starts a thread in its constructor which
    might conflict with subclassing because the subclass constructor will
    be called after the thread is started.
    </p>
    <p>
    Callbacks can be registered to monitor the input, output and error stream
    and to handle timeout and invalid responses.
    All callbacks are only called in the send functions and from the caller's
    thread.
    </p>
    <p>
    Internally the class reads the output and error streams from different
    threads and puts them on a message queue, so that the callbacks should
    be in the same order as the received data.
    It was not possible to use java.nio.Selector, because the streams in
    class Process are not SelectableChannels (Java 1.5.0).
    </p>
*/
public final class GtpClient
    extends GtpClientBase
{
    public static class ExecFailed
        extends GtpError
    {
        public String m_program;

        public ExecFailed(String program, IOException e)
        {
            super(e.getMessage());
            m_program = program;
        }        

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    /** Callback if a timeout occured. */
    public interface TimeoutCallback
    {
        /** Ask for continuation.
            If this function returns true, Gtp.send will wait for another
            timeout period, if it returns false, the program will be killed.
        */
        boolean askContinue();
    }

    /** Callback if an invalid response occured.
        Can be used to display invalid responses (without a status character)
        immediately, because send will not abort on an invalid response
        but continue to wait for a valid response line.
        This is necessary for some Go programs with broken GTP implementation
        which write debug data to standard output (e.g. Wallyplus 0.1.2).
    */
    public interface InvalidResponseCallback
    {
        void show(String line);
    }

    /** Callback interface for logging or displaying the GTP stream. */
    public interface IOCallback
    {
        void receivedInvalidResponse(String s);

        void receivedResponse(boolean error, String s);

        void receivedStdErr(String s);

        void sentCommand(String s);
    }    

    /** Constructor.
        @param program Command line for program.
        Will be split into words with respect to " as in StringUtil.tokenize.
        If the command line contains the string "%SRAND", it will be replaced
        by a random seed. This is useful if the random seed can be set by
        a command line option to produce deterministic randomness (the
        command returned by getProgramCommand() will contain the actual
        random seed used).
        @param workingDirectory The working directory to run the program in or
        null for the current directory
        @param log Log input, output and error stream to standard error.
        @param callback Callback for external display of the streams.
    */
    public GtpClient(String program, String workingDirectory, boolean log,
                     IOCallback callback)
        throws GtpError
    {
        m_log = log;
        m_callback = callback;
        m_wasKilled = false;
        if (program.indexOf("%SRAND") >= 0)
        {
            // RAND_MAX in stdlib.h ist at least 32767
            int randMax = 32767;
            int rand = (int)(Math.random() * (randMax + 1));
            program =
                program.replaceAll("%SRAND", Integer.toString(rand));
        }
        m_program = program;
        if (StringUtil.isEmpty(program))
            throw new GtpError("Command for invoking Go program must be"
                               + " not empty.");
        Runtime runtime = Runtime.getRuntime();
        try
        {
            File dir = null;
            if (! StringUtil.isEmpty(workingDirectory))
                dir = new File(workingDirectory);
            // Create command array with StringUtil::splitArguments
            // because Runtime.exec(String) uses a default StringTokenizer
            // which does not respect ".
            m_process = runtime.exec(StringUtil.splitArguments(program),
                                     null, dir);
        }
        catch (IOException e)
        {
            throw new ExecFailed(program, e);
        }
        init(m_process.getInputStream(), m_process.getOutputStream(),
             m_process.getErrorStream());
    }
    
    /** Constructor for given input and output streams. */
    public GtpClient(InputStream in, OutputStream out, boolean log,
                     IOCallback callback)
        throws GtpError
    {
        m_log = log;
        m_callback = callback;
        m_program = "-";
        m_process = null;
        init(in, out, null);
    }

    /** Close the output stream to the program. */
    public void close()
    {
        m_out.close();
    }

    /** Kill the Go program. */
    public void destroyProcess()
    {
        if (m_process != null)
        {
            m_wasKilled = true;
            m_process.destroy();
        }
    }

    /** Get response to last command sent. */
    public String getResponse()
    {
        return m_response;
    }

    /** Get full response including status and ID and last command. */
    public String getFullResponse()
    {
        return m_fullResponse;
    }

    /** Get the command line that was used for invoking the Go program.
        @return The command line that was given to the constructor.
    */
    public String getProgramCommand()
    {
        return m_program;
    }

    /** Check if interrupting a command is supported.
        Interrupting can supported by ANSI C signals or the special
        comment line "# interrupt" as described in the GoGui documentation
        chapter "Interrupting commands".
        Note: call queryInterruptSupport() first.
    */
    public boolean isInterruptSupported()
    {
        return (m_isInterruptCommentSupported || m_pid != null);
    }

    /** Check if program is dead. */
    public boolean isProgramDead()
    {
        return m_isProgramDead;
    }

    /** Query if interrupting is supported.
        @see GtpClient#isInterruptSupported
    */
    public void queryInterruptSupport()
    {
        try
        {
            if (isSupported("gogui-interrupt"))
            {
                send("gogui-interrupt");
                m_isInterruptCommentSupported = true;
            }
            else if (isSupported("gogui_interrupt"))
            {
                send("gogui_interrupt");
                m_isInterruptCommentSupported = true;
            }
            else if (isSupported("gogui-sigint"))
                m_pid = send("gogui-sigint").trim();
            else if (isSupported("gogui_sigint"))
                m_pid = send("gogui_sigint").trim();
        }
        catch (GtpError e)
        {
        }
    }

    /** Send a command.
        @return The response text of the successful response not including
        the status character.
        @throws GtpError containing the response if the command fails.
    */
    public String send(String command) throws GtpError
    {
        return send(command, -1, null);
    }

    public void queryName(long timeout, TimeoutCallback timeoutCallback)
        throws GtpError
    {
        m_name = send("name", timeout, timeoutCallback);
    }

    /** Send a command with timeout.
        @param command The command to send
        @param timeout Timeout in milliseconds or -1, if no timeout
        @param timeoutCallback Timeout callback or null if no timeout.
        @return The response text of the successful response not including
        the status character.
        @throws GtpError containing the response if the command fails.
        @see TimeoutCallback
    */
    public String send(String command, long timeout,
                       TimeoutCallback timeoutCallback) throws GtpError
    {
        assert(! command.trim().equals(""));
        assert(! command.trim().startsWith("#"));
        m_timeoutCallback = timeoutCallback;
        m_fullResponse = "";
        m_response = "";
        ++m_commandNumber;
        if (m_autoNumber)
            command = Integer.toString(m_commandNumber) + " " + command;
        log(">> " + command);
        m_out.println(command);
        m_out.flush();
        try
        {
            if (m_out.checkError())
            {
                readRemainingErrorMessages();
                throwProgramDied();
            }
            if (m_callback != null)
                m_callback.sentCommand(command);
            readResponse(timeout);
            return m_response;
        }
        catch (GtpError e)
        {
            e.setCommand(command);
            throw e;
        }
    }

    public void sendPlay(Move move, long timeout,
                         TimeoutCallback timeoutCallback) throws GtpError
    {
        send(getCommandPlay(move), timeout, timeoutCallback);
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

    /** Interrupt current command.
        Can be called from a different thread during a send.
        Note: call queryInterruptSupport first
        @see GtpClient#isInterruptSupported
        @throws GtpError if interrupting commands is not supported.
    */
    public void sendInterrupt() throws GtpError
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
                    throw new GtpError("Command \"" + command
                                        + "\" returned " + result);
            }
            catch (IOException e)
            {
                throw new GtpError("Could not run command " + command +
                                    ":\n" + e);
            }
            catch (InterruptedException e)
            {
            }
        }
        else
            throw new GtpError("Interrupt not supported");
    }

    /** Enable auto-numbering commands.
        Every command will be prepended by an integer as defined in the GTP
        standard, the integer is incremented after each command.
    */
    public void setAutoNumber(boolean enable)
    {
        m_autoNumber = enable;
    }

    /** Set the callback for invalid responses.
        @see InvalidResponseCallback
    */
    public void setInvalidResponseCallback(InvalidResponseCallback callback)
    {
        m_invalidResponseCallback = callback;
    }

    /** Set a prefix for logging to standard error.
        Only used if logging was enabled in the constructor.
    */
    public void setLogPrefix(String prefix)
    {
        synchronized (this)
        {
            m_logPrefix = prefix;
        }
    }

    /** Wait until the process of the program exits. */
    public void waitForExit()
    {
        if (m_process == null)
            return;
        try
        {
            m_process.waitFor();
            m_errorThread.join();
            m_inputThread.join();
        }
        catch (InterruptedException e)
        {
            System.err.println("GtpClient: InterruptedException");
        }        
    }

    /** More sophisticated version of waitFor with timeout. */
    public void waitForExit(int timeout, TimeoutCallback timeoutCallback)
    {
        setExitInProgress(true);
        if (m_process == null)
            return;
        while (true)
        {
            if (ProcessUtil.waitForExit(m_process, timeout))
                break;
            if (! timeoutCallback.askContinue())
            {
                m_process.destroy();
                return;
            }
        }
        try
        {
            m_errorThread.join(timeout);
            m_inputThread.join(timeout);
        }
        catch (InterruptedException e)
        {
            System.err.println("GtpClient: InterruptedException");
        }        
    }

    private static final class Message
    {
        public Message(int type, String text)
        {
            assert(type == RESPONSE || type == INVALID || type == ERROR);
            m_type = type;
            m_text = text;
        }

        public static final int RESPONSE = 0;

        public static final int INVALID = 1;

        public static final int ERROR = 2;

        public int m_type;

        public String m_text;
    }
    
    private class InputThread
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
                StringUtil.printException(t);
            }
        }

        private final BufferedReader m_in;

        private final MessageQueue m_queue;

        private final StringBuffer m_buffer = new StringBuffer(1024);

        private void appendBuffer(String line)
        {
            m_buffer.append(line);
            m_buffer.append('\n');
        }

        private boolean isResponseStart(String line)
        {
            if (line.length() < 1)
                return false;
            char c = line.charAt(0);
            return (c == '=' || c == '?');
        }

        private void mainLoop() throws InterruptedException
        {
            while (true)
            {
                String line = readLine();
                if (line == null)
                {
                    putMessage(Message.RESPONSE, null);
                    return;
                }
                appendBuffer(line);
                if (! isResponseStart(line))
                {
                    putMessage(Message.INVALID);
                    continue;
                }
                while (true)
                {
                    line = readLine();
                    appendBuffer(line);
                    if (line == null)
                    {
                        putMessage(Message.RESPONSE, null);
                        return;
                    }
                    if (line.equals(""))
                    {
                        // Give ErrorThread a chance to read first
                        Thread.yield();
                        putMessage(Message.RESPONSE);
                        break;
                    }
                }
                // Avoid programs flooding stderr or stdout after trying
                // to exit (see unlimited MessageQueue capacity bug)
                if (getExitInProgress())
                    Thread.sleep(m_queue.getSize() / 10);
            }
        }

        private void putMessage(int type)
        {
            putMessage(type, m_buffer.toString());
            m_buffer.setLength(0);
        }

        private void putMessage(int type, String text)
        {
            m_queue.put(new Message(type, text));
        }

        private String readLine()
        {
            try
            {
                String line = m_in.readLine();
                if (line != null)
                    log("<< " + line);
                return line;
            }
            catch (IOException e)
            {
                return null;
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
                StringUtil.printException(t);
            }
        }

        private final Reader m_in;

        private final MessageQueue m_queue;

        private void mainLoop() throws InterruptedException
        {
            int size = 1024;
            char[] buffer = new char[size];
            while (true)
            {                
                int n;
                try
                {
                    n = m_in.read(buffer, 0, size);
                }
                catch (IOException e)
                {
                    n = -1;
                }
                String text = null;
                if (n > 0)
                    text = new String(buffer, 0, n);
                m_queue.put(new Message(Message.ERROR, text));
                if (text == null)
                    return;
                logError(text);
                // Avoid programs flooding stderr or stdout after trying
                // to exit (see unlimited MessageQueue capacity bug)
                if (getExitInProgress())
                    Thread.sleep(m_queue.getSize() / 10);
            }
        }
    }

    private InvalidResponseCallback m_invalidResponseCallback;

    private boolean m_autoNumber;

    private boolean m_exitInProgress;

    private boolean m_isInterruptCommentSupported;

    private boolean m_isProgramDead;

    private boolean m_wasKilled;

    private final boolean m_log;

    private int m_commandNumber;

    private final IOCallback m_callback;

    private PrintWriter m_out;

    private Process m_process;

    private String m_fullResponse;

    private String m_response;

    private String m_logPrefix;

    private String m_pid;

    private final String m_program;

    private MessageQueue m_queue;

    private TimeoutCallback m_timeoutCallback;

    private InputThread m_inputThread;

    private ErrorThread m_errorThread;

    private synchronized boolean getExitInProgress()
    {
        return m_exitInProgress;
    }

    private void handleErrorStream(String text)
    {
        if (text == null)
            return;
        if (m_callback != null)
            m_callback.receivedStdErr(text);
    }

    private void init(InputStream in, OutputStream out, InputStream err)
    {
        m_out = new PrintWriter(out);
        m_isProgramDead = false;        
        m_queue = new MessageQueue();
        m_inputThread = new InputThread(in, m_queue);
        if (err != null)
        {
            m_errorThread = new ErrorThread(err, m_queue);
            m_errorThread.start();
        }
        m_inputThread.start();
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

    private synchronized void logError(String text)
    {
        if (m_log)
            System.err.print(text);
    }

    private void mergeErrorMessages(Message message)
    {
        assert(message.m_type == Message.ERROR);
        StringBuffer buffer = new StringBuffer(2048);
        while (message != null)
        {
            if (message.m_text != null)
                buffer.append(message.m_text);
            synchronized (m_queue.getMutex())
            {
                message = (Message)m_queue.unsynchronizedPeek();
                if (message != null && message.m_type != Message.ERROR)
                    message = null;
            }
            if (message != null)
            {
                message = (Message)m_queue.getIfAvaliable();
            }
        }
        handleErrorStream(buffer.toString());
    }

    private void readRemainingErrorMessages()
    {
        Message message;
        while (! m_queue.isEmpty())
        {
            message = (Message)m_queue.waitFor();
            if (message.m_type == Message.ERROR && message.m_text != null)
                handleErrorStream(message.m_text);
        }
    }

    private String readResponse(long timeout) throws GtpError
    {
        while (true)
        {            
            Message message = waitForMessage(timeout);
            if (message.m_type == Message.ERROR)
                mergeErrorMessages(message);
            else if (message.m_type == Message.INVALID)
            {
                m_fullResponse = message.m_text;
                if (m_callback != null)
                    m_callback.receivedInvalidResponse(m_fullResponse);
                if (m_invalidResponseCallback != null)
                    m_invalidResponseCallback.show(m_fullResponse);
            }
            else
            {
                assert(message.m_type == Message.RESPONSE);
                String response = message.m_text;
                if (response == null)
                {
                    m_isProgramDead = true;
                    readRemainingErrorMessages();
                    throwProgramDied();
                }
                boolean error = (response.charAt(0) != '=');
                m_fullResponse = response;
                if (m_callback != null)
                    m_callback.receivedResponse(error, m_fullResponse);
                assert(response.length() >= 3);            
                int index = response.indexOf(' ');
                int length = response.length();
                if (index < 0)
                    m_response = response.substring(1, length - 2);
                else
                    m_response = response.substring(index + 1, length - 2);
                if (error)
                    throw new GtpError(m_response);
                return m_response;
            }
        }
    }

    private synchronized void setExitInProgress(boolean exitInProgress)
    {
        m_exitInProgress = exitInProgress;
    }

    private void throwProgramDied() throws GtpError
    {
        m_isProgramDead = true;
        if (m_wasKilled)
            throw new GtpError(getName() + " terminated.");
        else
            throw new GtpError(getName() + " terminated unexpectedly.");
    }

    private Message waitForMessage(long timeout) throws GtpError
    {
        Message message;
        if (timeout < 0)
            message = (Message)m_queue.waitFor();
        else
        {
            message = null;
            while (message == null)
            {
                message = (Message)m_queue.waitFor(timeout);
                if (message == null)
                {
                    assert(m_timeoutCallback != null);
                    if (! m_timeoutCallback.askContinue())
                    {
                        destroyProcess();
                        throwProgramDied();
                    }
                }
            }
        }
        return message;
    }
}

