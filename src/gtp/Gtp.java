//----------------------------------------------------------------------------
/* @file Gtp.java
   Interface to a GTP Go program.

   @note Should use java.nio to keep correct order between stderr and stout
   of program.
   But with java.nio in Java 1.4 it does not seem possible to get
   selectable channels for the streams of a process .

   $Id$
   $Source$
*/
//----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import go.*;
import utils.StringUtils;

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
        m_isProgramDead = false;
        m_stdErrThread = new StdErrThread(m_process);
        m_stdErrThread.start();
        // Give StdErrThread a chance to start first        
        Thread.yield();
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

    public boolean isProgramDead()
    {
        return m_isProgramDead;
    }

    public static double[][] parseDoubleBoard(String response, String title,
                                              int boardSize) throws Gtp.Error
    {
        try
        {
            double result[][] = new double[boardSize][boardSize];
            String s[][] = parseStringBoard(response, title, boardSize);
            for (int x = 0; x < boardSize; ++x)
                for (int y = 0; y < boardSize; ++y)
                    result[x][y] = Double.parseDouble(s[x][y]);
            return result;
        }
        catch (NumberFormatException e)
        {
            throw new Gtp.Error("Floating point number expected");
        }
    }

    public static Point parsePoint(String s, int boardSize) throws Gtp.Error
    {
        s = s.trim().toUpperCase();
        if (s.equals("PASS"))
            return null;
        if (s.length() < 2)
            throw new Error("Invalid point or move");
        char xChar = s.charAt(0);
        if (xChar >= 'J')
            --xChar;
        int x = xChar - 'A';
        int y;
        try
        {
            y = Integer.parseInt(s.substring(1)) - 1;
        }
        catch (NumberFormatException e)
        {
            throw new Gtp.Error("Invalid point or move");
        }
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
            throw new Gtp.Error("Invalid coordinates");
        return new Point(x, y);
    }
    
    public static Point[] parsePointList(String s, int boardSize)
        throws Gtp.Error
    {
        Vector vector = new Vector(32, 32);
        String p[] = StringUtils.tokenize(s);
        for (int i = 0; i < p.length; ++i)
            if (! p[i].equals(""))
                vector.add(parsePoint(p[i], boardSize));
        Point result[] = new Point[vector.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (Point)vector.get(i);
        return result;
    }

    /** Find all points contained in string. */
    public static Point[] parsePointString(String s, int boardSize)
    {
        Vector vector = new Vector(32, 32);
        String p[] = StringUtils.tokenize(s);
        for (int i = 0; i < p.length; ++i)
            if (! p[i].equals(""))
            {
                Point point;
                try
                {
                    point = parsePoint(p[i], boardSize);
                }
                catch (Error e)
                {
                    continue;
                }
                vector.add(point);
            }
        Point result[] = new Point[vector.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (Point)vector.get(i);
        return result;
    }

    public static void parsePointStringList(String s, Vector pointList,
                                            Vector stringList,
                                            int boardsize) throws Gtp.Error
    {
        pointList.clear();
        stringList.clear();
        String array[] = StringUtils.tokenize(s);
        boolean nextIsPoint = true;
        Point point = null;
        for (int i = 0; i < array.length; ++i)
            if (! array[i].equals(""))
            {
                if (nextIsPoint)
                {
                    point = parsePoint(array[i], boardsize);
                    nextIsPoint = false;
                }
                else
                {
                    nextIsPoint = true;
                    pointList.add(point);
                    stringList.add(array[i]);
                }
            }
        if (! nextIsPoint)
            throw new Gtp.Error("Missing string.");
    }

    public static String[][] parseStringBoard(String s, String title,
                                              int boardSize) throws Gtp.Error
    {
        String result[][] = new String[boardSize][boardSize];
        try
        {
            BufferedReader reader = new BufferedReader(new StringReader(s));
            if (title != null && ! title.trim().equals(""))
            {
                String pattern = title + ":";
                while (true)
                {
                    String line = reader.readLine();
                    if (line == null)
                        throw new Gtp.Error(title + " not found.");
                    if (line.trim().equals(pattern))
                        break;
                }
            }
            for (int y = boardSize - 1; y >= 0; --y)
            {
                String line = reader.readLine();
                if (line == null)
                    throw new Gtp.Error("Incomplete string board");
                if (line.trim().equals(""))
                {
                    ++y;
                    continue;
                }
                String[] tokens = StringUtils.tokenize(line);
                if (tokens.length < boardSize)
                    throw new Gtp.Error("Incomplete string board");
                for (int x = 0; x < boardSize; ++x)
                    result[x][y] = tokens[x];
            }
        }
        catch (IOException e)
        {
            throw new Gtp.Error("I/O error");
        }
        return result;
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
        assert(! command.trim().equals(""));
        assert(! command.trim().startsWith("#"));
        m_fullResponse = "";
        m_response = "";
        if (m_isProgramDead)
            throw new Error("Program is dead");
        if (m_illegalState)
            throw new Error("Program sent illegal response");
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
        readResponse();
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

    /** Handle standard error stream of the GTP Go program. */
    private class StdErrThread extends Thread
    {
        public StdErrThread(Process process)
        {
            m_err = new InputStreamReader(process.getErrorStream());
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

        private Reader m_err;

        private void mainLoop() throws java.io.IOException
        {
            int size = 1024;
            char[] buffer = new char[size];
            StringBuffer stringBuffer = new StringBuffer(1024);
            while (true)
            {
                int n = m_err.read(buffer, 0, size);
                if (n < 0)
                {
                    if (m_callback != null)
                        m_callback.receivedStdErr(stringBuffer.toString());
                    return;
                }
                if (m_log)
                {
                    System.err.print(new String(buffer, 0, n));
                    System.err.flush();
                }
                stringBuffer.append(buffer, 0, n);
                int index = stringBuffer.lastIndexOf("\n");
                if (index == -1)
                    continue;
                String s = stringBuffer.substring(0, index + 1);
                stringBuffer.delete(0, index + 1);
                if (m_callback != null)
                    m_callback.receivedStdErr(s);
            }
        }   
    }

    private boolean m_autoNumber;

    private boolean m_isInterruptCommentSupported;

    private boolean m_isProgramDead;

    private boolean m_illegalState;

    private boolean m_log;

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

    private StdErrThread m_stdErrThread;

    private void readResponse() throws Gtp.Error
    {
        try
        {
            String line = "";
            while (line.trim().equals(""))
            {
                line = m_in.readLine();
                if (line == null)
                {
                    m_isProgramDead = true;
                    throw new Error("Go program died");
                }
                // Give StdErrThread a chance to read stderr first
                if (m_callback != null)
                    Thread.yield();
                log("<< " + line);
            }
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
                line = m_in.readLine();
                if (line == null)
                {
                    m_isProgramDead = true;
                    throw new Error("Go program died");
                }
                log("<< " + line);
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
        catch (InterruptedIOException e)
        {
            m_isProgramDead = true;
            throw new Error("Timeout while waiting for program");
        }
        catch (IOException e)
        {
            m_isProgramDead = true;
            throw new Error(e.getMessage());
        }
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
            System.err.flush();
        }
    }
}

//----------------------------------------------------------------------------
