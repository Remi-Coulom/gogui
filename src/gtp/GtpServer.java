//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtp;

import java.io.*;
import go.*;
import utils.StringUtils;

//----------------------------------------------------------------------------

class Command
{
    public boolean m_hasId;
    
    public int m_id;
    
    public String m_command;

    public boolean isQuit()
    {
        return m_command.trim().toLowerCase().equals("quit");
    }
}

class ReadThread extends Thread
{
    public ReadThread(GtpServer gtpServer, InputStream in, boolean log)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
        m_gtpServer = gtpServer;
        m_log = log;
    }

    public boolean endOfFile()
    {
        return m_endOfFile;
    }

    public Command getCommand()
    {
        synchronized (this)
        {
            assert(! m_waitCommand);
            m_waitCommand = true;
            notifyAll();
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                System.err.println("Interrupted");
            }
            assert(m_endOfFile || ! m_waitCommand);
            Command result = m_command;
            m_command = null;
            return result;
        }
    }

    public void run()
    {
        try
        {            
            while (true)
            {
                String line = m_in.readLine();
                if (line == null)
                {
                    m_endOfFile = true;
                }
                else
                {
                    if (m_log)
                        m_gtpServer.log(line);
                    line = line.trim();
                    if (line.equals("# interrupt"))
                    {
                        m_gtpServer.interruptCommand();
                    }
                    if (line.equals("") || line.charAt(0) == '#')
                        continue;
                }
                synchronized (this)
                {
                    while (! m_waitCommand)
                    {
                        wait();
                    }
                    if (line == null)
                        m_command = null;
                    else
                        m_command = parseLine(line);
                    notifyAll();
                    m_waitCommand = false;
                    if (m_command == null || m_command.isQuit())
                        return;
                }
            }
        }
        catch (Exception e)
        {
            System.err.println(StringUtils.formatException(e));
        }
    }

    private boolean m_endOfFile = false;

    private boolean m_log;

    private boolean m_waitCommand = false;

    private BufferedReader m_in;

    private Command m_command;

    private GtpServer m_gtpServer;

    private Command parseLine(String line)
    {
        assert(! line.trim().equals(""));
        int len = line.length();
        StringBuffer buffer = new StringBuffer(len);
        boolean wasLastSpace = false;
        for (int i = 0; i < len; ++i)
        {
            char c = line.charAt(i);
            if (Character.isISOControl(c))
                continue;
            if (Character.isWhitespace(c))
            {
                if (! wasLastSpace)
                {
                    buffer.append(' ');
                    wasLastSpace = true;
                }
            }
            else
            {
                buffer.append(c);
                wasLastSpace = false;
            }
        }
        String[] array = StringUtils.tokenize(buffer.toString());
        assert(array.length > 0);
        String command = buffer.toString();
        Command result = new Command();
        result.m_hasId = false;
        result.m_command = command;
        try
        {
            result.m_hasId = true;
            result.m_id = Integer.parseInt(array[0]);
            result.m_command = buffer.substring(array[0].length());
        }
        catch (NumberFormatException e)
        {
            result.m_hasId = false;
        }
        return result;
    }
}

public abstract class GtpServer
{
    public static class ColorArgument
    {
        public Color m_color;
    }

    public static class ColorPointArgument
    {
        public Color m_color;

        public Point m_point;
    }

    public static class DoubleArgument
    {
        public double m_double;
    }

    public static class IntegerArgument
    {
        public int m_integer;
    }

    public static class PointArgument
    {
        public Point m_point;
    }

    public GtpServer(InputStream in, OutputStream out, PrintStream log)
    {
        m_out = new PrintStream(out);
        m_in = in;
        m_log = log;
    }

    /** Callback for interrupting commands.
        This callback will be invoked if the special comment line
        "# interrupt" is received. It will be invoked from a different thread.
    */
    public abstract void interruptCommand();

    public abstract boolean handleCommand(String command,
                                          StringBuffer response);

    public synchronized void log(String line)
    {
        assert(m_log != null);
        m_log.println(line);
    }

    public void mainLoop() throws IOException
    {
        ReadThread readThread = new ReadThread(this, m_in, m_log != null);
        readThread.start();
        while (true)
        {
            Command command = readThread.getCommand();
            if (command == null)
                return;
            sendResponse(command);
            if (command.isQuit())
                return;
        }
    }

    /** Utility function for parsing a color argument.
        @param cmdArray Command line split into words.
        @param response Empty string buffer filled with GTP error message
        if parsing fails.
        @return Color argument or null if parsing fails.
    */
    public static ColorArgument parseColorArgument(String[] cmdArray,
                                                   StringBuffer response)
    {
        if (cmdArray.length != 2)
        {
            response.append("Missing color argument");
            return null;
        }
        ColorArgument argument = new ColorArgument();
        String arg1 = cmdArray[1].toLowerCase();
        if (arg1.equals("w") || arg1.equals("white"))
            argument.m_color = Color.WHITE;
        else if (arg1.equals("b") || arg1.equals("black"))
            argument.m_color = Color.BLACK;
        else
        {
            response.append("Invalid color argument");
            return null;
        }
        return argument;
    }

    /** Utility function for parsing a color and point argument.
        @param cmdArray Command line split into words.
        @param response Empty string buffer filled with GTP error message
        if parsing fails.
        @param boardSize Board size is needed for parsing the point
        @return ColorPoint argument or null if parsing fails.
    */
    public static ColorPointArgument
        parseColorPointArgument(String[] cmdArray, StringBuffer response,
                                int boardSize)
    {
        if (cmdArray.length != 3)
        {
            response.append("Missing color and vertex argument");
            return null;
        }
        ColorPointArgument argument = new ColorPointArgument();
        String arg1 = cmdArray[1].toLowerCase();
        if (arg1.equals("w") || arg1.equals("white"))
            argument.m_color = Color.WHITE;
        else if (arg1.equals("b") || arg1.equals("black"))
            argument.m_color = Color.BLACK;
        else
        {
            response.append("Invalid color argument");
            return null;
        }
        try
        {
            Point point = Gtp.parsePoint(cmdArray[2], boardSize);
            argument.m_point = point;
            return argument;
        }
        catch (Gtp.Error e)
        {
            response.append("Invalid vertex argument");
            return null;
        }
    }

    /** Utility function for parsing an integer argument.
        @param cmdArray Command line split into words.
        @param response Empty string buffer filled with GTP error message
        if parsing fails.
        @return Double argument or null if parsing fails.
    */
    public static DoubleArgument parseDoubleArgument(String[] cmdArray,
                                                     StringBuffer response)
    {
        if (cmdArray.length != 2)
        {
            response.append("Missing float argument");
            return null;
        }
        try
        {
            double f = Double.parseDouble(cmdArray[1]);
            DoubleArgument doubleArgument = new DoubleArgument();
            doubleArgument.m_double = f;
            return doubleArgument;
        }
        catch (NumberFormatException e)
        {
            response.append("Invalid float argument");
            return null;
        }
    }

    /** Utility function for parsing an integer argument.
        @param cmdArray Command line split into words.
        @param response Empty string buffer filled with GTP error message
        if parsing fails.
        @return Integer argument or null if parsing fails.
    */
    public static IntegerArgument parseIntegerArgument(String[] cmdArray,
                                                       StringBuffer response)
    {
        if (cmdArray.length != 2)
        {
            response.append("Missing integer argument");
            return null;
        }
        try
        {
            int integer = Integer.parseInt(cmdArray[1]);
            IntegerArgument integerArgument = new IntegerArgument();
            integerArgument.m_integer = integer;
            return integerArgument;
        }
        catch (NumberFormatException e)
        {
            response.append("Invalid integer argument");
            return null;
        }
    }

    /** Utility function for parsing an point argument.
        @param cmdArray Command line split into words.
        @param response Empty string buffer filled with GTP error message
        if parsing fails.
        @param boardSize Board size is needed for parsing the point
        @return Point argument or null if parsing fails.
    */
    public static PointArgument parsePointArgument(String[] cmdArray,
                                                   StringBuffer response,
                                                   int boardSize)
    {
        if (cmdArray.length != 2)
        {
            response.append("Missing vertex argument");
            return null;
        }
        try
        {
            Point point = Gtp.parsePoint(cmdArray[1], boardSize);
            PointArgument argument = new PointArgument();
            argument.m_point = point;
            return argument;
        }
        catch (Gtp.Error e)
        {
            response.append("Invalid vertex argument");
            return null;
        }
    }

    public void respond(boolean status, boolean hasId, int id,
                        String response)
    {
        StringBuffer fullResponse = new StringBuffer(256);
        if (status)
            fullResponse.append('=');
        else
            fullResponse.append('?');
        if (hasId)
            fullResponse.append(id);
        fullResponse.append(' ');
        fullResponse.append(response);
        if (response.length() == 0
            || response.charAt(response.length() - 1) != '\n')
            fullResponse.append('\n');
        m_out.println(fullResponse);
        if (m_log != null)
            m_log.println(fullResponse);
    }

    private InputStream m_in;

    private PrintStream m_log;

    private PrintStream m_out;

    private void sendResponse(Command cmd)
    {
        StringBuffer response = new StringBuffer();
        boolean status = handleCommand(cmd.m_command.trim(), response);
        respond(status, cmd.m_hasId, cmd.m_id, response.toString());
    }
}

//----------------------------------------------------------------------------
