// GtpClient.java

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.StringUtil;
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
    </p> */
public final class GtpClient
    extends GtpClientBase
{
    /** Exception thrown if executing a GTP engine failed. */
    public static class ExecFailed
        extends GtpError
    {
        public String m_program;

        public ExecFailed(String program, String message)
        {
            super(message);
            m_program = program;
        }

        public ExecFailed(String program, IOException e)
        {
            this(program, e.getMessage());
        }
    }

    /** Callback if a timeout occured. */
    public interface TimeoutCallback
    {
        /** Ask for continuation.
            If this function returns true, Gtp.send will wait for another
            timeout period, if it returns false, the program will be killed. */
        boolean askContinue();
    }

    /** Callback if an invalid response occured.
        Can be used to display invalid responses (without a status character)
        immediately, because send will not abort on an invalid response
        but continue to wait for a valid response line.
        This is necessary for some Go programs with broken GTP implementation
        which write debug data to standard output (e.g. Wallyplus 0.1.2). */
    public interface InvalidResponseCallback
    {
        void show(String line);
    }

    /** Callback interface for logging or displaying the GTP stream.
        Note that some of the callback functions are called from different
        threads. */
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
        @param callback Callback for external display of the streams. */
    public GtpClient(String program, File workingDirectory, boolean log,
                     IOCallback callback)
        throws GtpClient.ExecFailed
    {
        if (workingDirectory != null && ! workingDirectory.isDirectory())
            throw new ExecFailed(program,
                                 "Invalid working directory \""
                                 + workingDirectory + "\"");
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
            throw new ExecFailed(program,
                                 "Command for invoking Go program must be"
                                 + " not empty.");
        Runtime runtime = Runtime.getRuntime();
        try
        {
            // Create command array with StringUtil::splitArguments
            // because Runtime.exec(String) uses a default StringTokenizer
            // which does not respect ".
            String[] cmdArray = StringUtil.splitArguments(program);
            // Make file name absolute, if working directory is not current
            // directory. With Java 1.5, it seems that Runtime.exec succeeds
            // if the relative path is valid from the current, but not from
            // the given working directory, but the process is not usable
            // (reading from its input stream immediately returns
            // end-of-stream)
            if (cmdArray.length > 0)
            {
                File file = new File(cmdArray[0]);
                // Only replace if executable is a path to a file, not
                // an executable in the exec-path
                if (file.exists())
                    cmdArray[0] = file.getAbsolutePath();
            }
            m_process = runtime.exec(cmdArray, null, workingDirectory);
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

    /** Close the output stream to the program.
        Some engines don't handle closing the command stream without an
        explicit quit command well, so the preferred way to terminate a
        connection is to send a quit command. Closing the output stream after
        a quit is not strictly necessary, but may improve compatibility with
        engines that read the input stream in a different thread */
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

    /** Did the engine ever send a valid response to a command? */
    public boolean getAnyCommandsResponded()
    {
        return m_anyCommandsResponded;
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
        @return The command line that was given to the constructor. */
    public String getProgramCommand()
    {
        return m_program;
    }

    /** Check if program is dead. */
    public boolean isProgramDead()
    {
        return m_isProgramDead;
    }

    /** Send a command.
        @return The response text of the successful response not including
        the status character.
        @throws GtpError containing the response if the command fails. */
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
        @see TimeoutCallback */
    public String send(String command, long timeout,
                       TimeoutCallback timeoutCallback) throws GtpError
    {
        assert ! command.trim().equals("");
        assert ! command.trim().startsWith("#");
        m_timeoutCallback = timeoutCallback;
        m_fullResponse = "";
        m_response = "";
        ++m_commandNumber;
        if (m_autoNumber)
            command = Integer.toString(m_commandNumber) + " " + command;
        if (m_log)
            logOut(command);
        m_out.println(command);
        m_out.flush();
        try
        {
            if (m_out.checkError())
            {
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
        @param comment comment line (must start with '#'). */
    public void sendComment(String comment)
    {
        assert comment.trim().startsWith("#");
        if (m_log)
            logOut(comment);
        if (m_callback != null)
            m_callback.sentCommand(comment);
        m_out.println(comment);
        m_out.flush();
    }

    /** Enable auto-numbering commands.
        Every command will be prepended by an integer as defined in the GTP
        standard, the integer is incremented after each command. */
    public void setAutoNumber(boolean enable)
    {
        m_autoNumber = enable;
    }

    /** Set the callback for invalid responses.
        @see InvalidResponseCallback */
    public void setInvalidResponseCallback(InvalidResponseCallback callback)
    {
        m_invalidResponseCallback = callback;
    }

    public void setIOCallback(GtpClient.IOCallback callback)
    {
        m_callback = callback;
    }

    /** Set a prefix for logging to standard error.
        Only used if logging was enabled in the constructor. */
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
            printInterrupted();
        }
    }

    /** More sophisticated version of waitFor with timeout. */
    public void waitForExit(int timeout, TimeoutCallback timeoutCallback)
    {
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
            printInterrupted();
        }
    }

    /** Was program forcefully terminated by calling destroyProcess() */
    public boolean wasKilled()
    {
        return m_wasKilled;
    }

    private static final class Message
    {
        public Message(String text)
        {
            m_text = text;
        }

        public String m_text;
    }

    private class InputThread
        extends Thread
    {
        InputThread(InputStream in, BlockingQueue<Message> queue)
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

        private final BlockingQueue<Message> m_queue;

        private final StringBuilder m_buffer = new StringBuilder(1024);

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
                    putMessage(null);
                    return;
                }
                appendBuffer(line);
                if (! isResponseStart(line))
                {
                    if (! line.trim().equals(""))
                    {
                        if (m_callback != null)
                            m_callback.receivedInvalidResponse(line);
                        if (m_invalidResponseCallback != null)
                            m_invalidResponseCallback.show(line);
                    }
                    m_buffer.setLength(0);
                    continue;
                }
                while (true)
                {
                    line = readLine();
                    appendBuffer(line);
                    if (line == null)
                    {
                        putMessage(null);
                        return;
                    }
                    if (line.equals(""))
                    {
                        putMessage();
                        break;
                    }
                }
            }
        }

        private void putMessage()
        {
            // Calling Thread.yield increases the probability that the IO
            // callbacks for stderr and stdout are called in the right order
            // for the typical use case of a program writing to stderr
            // before writing the response. The yield costs some performance
            // however and could have a negative effect, if the program
            // writes to stderr immediately after the response (e.g. logging
            // output during pondering).
            Thread.yield();
            putMessage(m_buffer.toString());
            m_buffer.setLength(0);
        }

        private void putMessage(String text)
        {
            try
            {
                m_queue.put(new Message(text));
            }
            catch (InterruptedException e)
            {
                printInterrupted();
            }
        }

        private String readLine()
        {
            try
            {
                String line = m_in.readLine();
                if (m_log && line != null)
                    logIn(line);
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
        public ErrorThread(InputStream in, BlockingQueue<Message> queue)
        {
            m_in = new InputStreamReader(in);
            m_queue = queue;
        }

        public void run()
        {
            try
            {
                char[] buffer = new char[4096];
                while (true)
                {
                    int n;
                    try
                    {
                        n = m_in.read(buffer);
                    }
                    catch (IOException e)
                    {
                        return;
                    }
                    if (n <= 0)
                        return;
                    String text = new String(buffer, 0, n);
                    if (m_callback != null)
                        m_callback.receivedStdErr(text);
                    if (m_log)
                        logError(text);
                }
            }
            catch (Throwable t)
            {
                StringUtil.printException(t);
            }
        }

        private final Reader m_in;
    }

    private InvalidResponseCallback m_invalidResponseCallback;

    private boolean m_autoNumber;

    private boolean m_anyCommandsResponded;

    private boolean m_isProgramDead;

    private boolean m_wasKilled;

    private final boolean m_log;

    private int m_commandNumber;

    private IOCallback m_callback;

    private PrintWriter m_out;

    private Process m_process;

    private String m_fullResponse;

    private String m_response;

    private String m_logPrefix;

    private final String m_program;

    private BlockingQueue<Message> m_queue;

    private TimeoutCallback m_timeoutCallback;

    private InputThread m_inputThread;

    private ErrorThread m_errorThread;

    private void init(InputStream in, OutputStream out, InputStream err)
    {
        m_out = new PrintWriter(out);
        m_isProgramDead = false;
        m_queue = new ArrayBlockingQueue<Message>(10);
        m_inputThread = new InputThread(in, m_queue);
        if (err != null)
        {
            m_errorThread = new ErrorThread(err, m_queue);
            m_errorThread.start();
        }
        m_inputThread.start();
    }

    private synchronized void logError(String text)
    {
        System.err.print(text);
    }

    private synchronized void logIn(String msg)
    {
        if (m_logPrefix != null)
            System.err.print(m_logPrefix);
        System.err.print("<< ");
        System.err.println(msg);
    }

    private synchronized void logOut(String msg)
    {
        if (m_logPrefix != null)
            System.err.print(m_logPrefix);
        System.err.print(">> ");
        System.err.println(msg);
    }

    /** Print information about occurence of InterruptedException.
        An InterruptedException should never happen, because we don't call
        Thread.interrupt */
    private void printInterrupted()
    {
        System.err.println("GtpClient: InterruptedException");
        Thread.dumpStack();
    }

    private String readResponse(long timeout) throws GtpError
    {
        while (true)
        {
            Message message = waitForMessage(timeout);
            String response = message.m_text;
            if (response == null)
            {
                m_isProgramDead = true;
                throwProgramDied();
            }
            m_anyCommandsResponded = true;
            boolean error = (response.charAt(0) != '=');
            m_fullResponse = response;
            if (m_callback != null)
                m_callback.receivedResponse(error, m_fullResponse);
            assert response.length() >= 3;
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

    private void throwProgramDied() throws GtpError
    {
        m_isProgramDead = true;
        String name = m_name;
        if (name == null)
            name = "The Go program";
        if (m_wasKilled)
            throw new GtpError(name + " terminated.");
        else
            throw new GtpError(name + " terminated unexpectedly.");
    }

    private Message waitForMessage(long timeout) throws GtpError
    {
        Message message = null;
        if (timeout < 0)
        {
            try
            {
                message = m_queue.take();
            }
            catch (InterruptedException e)
            {
                printInterrupted();
                destroyProcess();
                throwProgramDied();
            }
        }
        else
        {
            message = null;
            while (message == null)
            {
                try
                {
                    message = m_queue.poll(timeout, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e)
                {
                    printInterrupted();
                }
                if (message == null)
                {
                    assert m_timeoutCallback != null;
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
