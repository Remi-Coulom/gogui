//-----------------------------------------------------------------------------
/* @file Gtp.java
   Interface to a GTP Go program.

   @todo Should use java.nio to avoid thread switching with Thread.yield()
   to keep order between stderr and stout of program.

   $Id$
   $Source$
*/
//-----------------------------------------------------------------------------

package gtp;

import java.io.*;
import java.util.*;
import go.*;
import utils.StringUtils;

//-----------------------------------------------------------------------------

public class Gtp
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    public interface IOCallback
    {
        public void receivedResponse(boolean error, String s);

        public void receivedStdErr(String s);

        public void sentCommand(String s);
    }    

    public Gtp(String program, boolean log, IOCallback callback) throws Error
    {
        m_log = log;
        m_program = program;
        m_callback = callback;
        if (m_program.indexOf("%SRAND") >= 0)
        {
            // RAND_MAX in stdlib.h ist at least 32767
            final int RAND_MAX = 32767;
            int rand = (int)(Math.random() * (RAND_MAX + 1));
            m_program = StringUtils.replace(m_program, "%SRAND",
                                            Integer.toString(rand));
        }
        Runtime runtime = Runtime.getRuntime();
        try
        {
            // Create command array with StringUtils::getCmdArray
            // because Runtime.exec(String) uses a default StringTokenizer
            // which does not respect ".
            m_process = runtime.exec(StringUtils.getCmdArray(program));
        }
        catch (IOException e)
        {
            throw new Gtp.Error("Could not create " + program + ":\n" +
                                e.getMessage());
        }
        Reader reader = new InputStreamReader(m_process.getInputStream());
        m_in = new BufferedReader(reader);
        m_out = new PrintStream(m_process.getOutputStream());
        m_isProgramDead = false;
        Thread stdErrThread = new StdErrThread(m_process);
        stdErrThread.start();
        if (! m_fastUpdate)
            // Give StdErrThread a chance to start first        
            Thread.currentThread().yield();
    }

    public void close()
    {
        m_out.close();
    }

    public String getResponse()
    {
        return m_response;
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

    public double getCpuTime() throws Error
    {
        try
        {
            return Double.parseDouble(sendCommand("cputime"));
        }
        catch (NumberFormatException e)
        {
            throw new Error("Invalid response to cputime command");
        }
    }

    /** Get fulle response including status and ID and last command. */
    public String getFullResponse()
    {
        return m_fullResponse;
    }

    public String getProgramCommand()
    {
        return m_program;
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

    public boolean isProgramDead()
    {
        return m_isProgramDead;
    }

    public static void main(String[] args)
    {
        try
        {
            String program;
            if (args.length > 0)
                program = args[0];
            else
                program = "gnugo --mode gtp";
            Gtp gtp = new Gtp(program, true, null);
            gtp.sendCommand("name");
            gtp.sendCommand("version");
            gtp.sendCommand("quit");
            System.exit(0);
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public static double[][] parseDoubleBoard(String response, String title,
                                              int boardSize) throws Error
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
            throw new Error("Floating point number expected.");
        }
    }

    public static Point parsePoint(String s) throws Error
    {
        s = s.trim().toUpperCase();
        if (s.equals("PASS"))
            return null;
        if (s.length() < 2)
            throw new Error("Invalid point or move.");
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
            throw new Error("Invalid point or move.");
        }
        return new Point(x, y);
    }
    
    public static Point[] parsePointList(String s) throws Error
    {
        Vector vector = new Vector(32, 32);
        s = StringUtils.replace(s, "\n", " ");
        String p[] = StringUtils.split(s, ' ');
        for (int i = 0; i < p.length; ++i)
            if (! p[i].equals(""))
                vector.add(parsePoint(p[i]));
        Point result[] = new Point[vector.size()];
        for (int i = 0; i < result.length; ++i)
            result[i] = (Point)vector.get(i);
        return result;
    }

    public static String[][] parseStringBoard(String s, String title,
                                              int boardSize) throws Error
    {
        String result[][] = new String[boardSize][boardSize];
        try
        {
            Reader reader = new StringReader(s);
            StreamTokenizer tokenizer = new StreamTokenizer(reader);
            tokenizer.ordinaryChars('0', '9');
            tokenizer.ordinaryChar('-');
            tokenizer.ordinaryChar('.');
            tokenizer.wordChars('0', '9');
            tokenizer.wordChars('-', '-');
            tokenizer.wordChars('.', '.');
            tokenizer.wordChars('?', '?');
            tokenizer.wordChars('!', '!');
            tokenizer.wordChars('*', '*');
            tokenizer.wordChars('"', '"');
            if (title != null)
            {
                boolean foundTitle = false;
                while (! foundTitle)
                {
                    switch (tokenizer.nextToken())
                    {
                    case StreamTokenizer.TT_WORD:
                        if (tokenizer.sval.equals(title))
                        {
                            int ttype = tokenizer.nextToken();
                            if (ttype == ':')
                                foundTitle = true;
                            else if (ttype == StreamTokenizer.TT_EOF)
                                throw new Error(title + " not found.");
                        }
                        break;
                    case StreamTokenizer.TT_EOF:
                        throw new Error(title + " not found.");
                    }
                }
            }
            for (int y = boardSize - 1; y >= 0; --y)
                for (int x = 0; x < boardSize; ++x)
                {
                    int ttype = tokenizer.nextToken();
                    if (ttype != StreamTokenizer.TT_WORD)
                        throw new Error("Word expected.");
                    if (tokenizer.sval.equals("\"\""))
                        result[x][y] = "";
                    else
                        result[x][y] = tokenizer.sval;
                }
        }
        catch (IOException e)
        {
            throw new Error("I/O error.");
        }
        return result;
    }

    public void queryProtocolVersion() throws Error
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
            throw new Error(e.getMessage());
        }
    }

    public void querySupportedCommands() throws Error
    {
        String command = (m_protocolVersion == 1 ? "help" : "list_commands");
        String response = sendCommand(command);
        m_supportedCommands = StringUtils.split(response, '\n');
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

    public String sendCommand(String command, long timeout) throws Error
    {
        assert(! command.trim().equals(""));
        assert(! command.trim().startsWith("#"));
        if (m_isProgramDead)
            throw new Error("Program is dead.");
        log(">> " + command);
        m_response = "";
        m_out.println(command);
        m_out.flush();
        if (m_out.checkError())
        {
            m_isProgramDead = true;
            throw new Error("Go program died.");
        }
        if (m_callback != null)
            m_callback.sentCommand(command);
        java.util.Timer timer = null;
        if (timeout > 0)
        {
            timer = new java.util.Timer();
            timer.schedule(new Interrupt(Thread.currentThread()), timeout);
        }
        readResponse();
        if (timer != null)
            timer.cancel();
        return m_response;
    }

    public String sendCommand(String command) throws Error
    {
        return sendCommand(command, 0);
    }

    public void sendCommandBoardsize(int size) throws Error
    {
        if (m_protocolVersion == 2)
            sendCommand("boardsize " + size);
    }

    public String sendCommandClearBoard(int size) throws Error
    {
        return sendCommand(getCommandClearBoard(size));
    }

    public String sendCommandPlay(Move move) throws Error
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

    /** Send interrupt comment. */
    public void sendInterrupt()
    {
        sendComment("# interrupt");
    }

    /** Don't try to keep stdin/stderr callbacks in correct order.
        Increases the probablitiy of changing the order of
        stderr/stdout.
     */
    public void setFastUpdate(boolean fastUpdate)
    {
        m_fastUpdate = fastUpdate;
    }

    public void setLogPrefix(String prefix)
    {
        m_logPrefix = prefix;
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
                int size = 1024;
                char[] buffer = new char[size];
                StringBuffer stringBuffer = new StringBuffer();
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
            catch (RuntimeException e)
            {
                String msg = e.getMessage();
                if (msg != null)
                    System.err.println(msg);
                e.printStackTrace();
            }
            catch (Throwable t)
            {
                String msg = t.getMessage();
                if (msg == null)
                    msg = t.getClass().getName();
                System.err.println(msg);
            }
        }

        private Reader m_err;
    }

    private static class Interrupt extends TimerTask
    {
        public Interrupt(Thread thread)
        {
            m_thread = thread;
        }

        public void run()
        {
            m_thread.interrupt();
        }

        private Thread m_thread;
    }

    private boolean m_fastUpdate;

    private boolean m_isProgramDead;

    private boolean m_log;

    private int m_protocolVersion = 1;

    private BufferedReader m_in;

    private IOCallback m_callback;

    private PrintStream m_out;

    private Process m_process;

    private String m_fullResponse;

    private String m_response;

    private String m_logPrefix;

    private String m_program;

    private String[] m_supportedCommands;

    private void readResponse() throws Error
    {
        if (! m_fastUpdate)
            // Give StdErrThread a chance to read standard error output of the
            // program first
            Thread.currentThread().yield();
        try
        {
            String line = "";
            while (line.trim().equals(""))
            {
                line = m_in.readLine();
                if (line == null)
                {
                    m_isProgramDead = true;
                    throw new Error("Go program died.");
                }
                log("<< " + line);
            }
            StringBuffer response = new StringBuffer(line);
            response.append("\n");
            if (! isResponseLine(line))
                throw new Error("Invalid response:\n\"" + line + "\"");
            boolean error = (line.charAt(0) != '=');
            boolean done = false;
            while (! done)
            {
                line = m_in.readLine();
                if (line == null)
                {
                    m_isProgramDead = true;
                    throw new Error("Go program died.");
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
            throw new Error("Timeout while waiting for program.");
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

//-----------------------------------------------------------------------------
