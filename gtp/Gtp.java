//=============================================================================
// $Id$
// $Source$
//=============================================================================

package gtp;

import java.io.*;
import java.util.*;
import board.*;
import utils.StringUtils;

//=============================================================================

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
        public void commandSent(String s);

        public void receivedLine(String s);

        public void receivedStdErr(String s);
    }    

    public Gtp(String program, boolean log) throws Error
    {
        m_log = log;
        m_program = program;
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
            m_process = runtime.exec(program);
        }
        catch (IOException e)
        {
            throw new Gtp.Error(e.getMessage());
        }
        Reader reader;
        reader = new InputStreamReader(m_process.getInputStream());
        m_in = new BufferedReader(reader);
        m_out = new PrintStream(m_process.getOutputStream());
        m_isProgramDead = false;
        Thread stdErrThread = new StdErrThread(m_process);
        stdErrThread.start();
    }

    public String getAnswer()
    {
        return m_answer;
    }

    public String getProgramCommand()
    {
        return m_program;
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
            Gtp gtp = new Gtp(program, true);
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

    public static double[][] parseDoubleBoard(String answer, String title,
                                              int boardSize) throws Error
    {
        try
        {
            double result[][] = new double[boardSize][boardSize];
            String s[][] = parseStringBoard(answer, title, boardSize);
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
            throw new Error("Invalid move.");
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
            throw new Error("Invalid move.");
        }
        return new Point(x, y);
    }
    
    public static Point[] parsePointList(String s) throws Error
    {
        Vector vector = new Vector(32, 32);
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

    public String sendCommand(String command, long timeout) throws Error
    {
        assert(! command.trim().equals(""));
        assert(! command.trim().startsWith("#"));
        log(">> " + command);
        if (m_isProgramDead)
            throw new Error("Program is dead.");
        if (m_callback != null)
            m_callback.commandSent(command);
        m_answer = "";
        m_out.println(command);
        m_out.flush();
        if (m_out.checkError())
        {
            m_isProgramDead = true;
            throw new Error("Go program died.");
        }
        java.util.Timer timer = null;
        if (timeout > 0)
        {
            timer = new java.util.Timer();
            timer.schedule(new Interrupt(Thread.currentThread()), timeout);
        }
        readAnswer();
        if (timer != null)
            timer.cancel();
        return m_answer;
    }

    public String sendCommand(String command) throws Error
    {
        return sendCommand(command, 0);
    }

    public void setIOCallback(IOCallback callback)
    {
        m_callback = callback;
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
                char buf[] = new char[32768];
                while (true)
                {
                    int len = m_err.read(buf);
                    if (len < 0)
                        return;
                    if (len > 0)
                    {
                        char c[] = new char[len];
                        for (int i = 0; i < len; ++i)
                            c[i] = buf[i];
                        log(c);
                        if (m_callback != null)
                            m_callback.receivedStdErr(new String(c));
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

    private boolean m_isProgramDead;
    private boolean m_log;
    private BufferedReader m_in;
    private IOCallback m_callback;
    private PrintStream m_out;
    private Process m_process;
    private String m_answer;
    private String m_program;

    private void readAnswer() throws Error
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
                    throw new Error("Go program died.");
                }
                log("<< " + line);
                if (m_callback != null)
                    m_callback.receivedLine(line);
            }
            if (! isAnswerLine(line))
                throw new Error("Invalid response:\n\"" + line + "\"");
            boolean success = (line.charAt(0) == '=');
            m_answer = line.substring(2);
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
                if (m_callback != null)
                    m_callback.receivedLine(line);
                done = line.equals("");
                if (! done)
                    m_answer = m_answer + "\n" + line;
            }
            if (! success)
                throw new Error(m_answer);
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

    private static boolean isAnswerLine(String line)
    {
        if (line.length() < 2)
            return false;
        char c = line.charAt(0);
        return (c == '=' || c == '?');
    }

    private synchronized void log(String msg)
    {
        if (m_log)
            System.err.println(msg);
    }

    private synchronized void log(char[] c)
    {
        if (m_log)
            System.err.print(c);
    }
}

//=============================================================================
