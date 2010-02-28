// GtpClientBase.java

package net.sf.gogui.gtp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.StringUtil;

/** Interface to a Go program that uses GTP.
    This class implements most of the functionality of a connection to a GTP
    command apart from how commands are actually sent to the program.
    Subclasses need to implement the abstract function send() and a few
    functions related to querying and using the ability to interrupt
    commands. */
public abstract class GtpClientBase
{
    /** Close output connection.
        Should do nothing if the concrete class does not communicate through
        streams. */
    public abstract void close();

    /** Get command for setting the board size.
        Note: call queryProtocolVersion first
        @return The boardsize command for GTP version 2 programs,
        otherwise null. */
    public String getCommandBoardsize(int size)
    {
        if (m_protocolVersion == 2)
        {
            m_buffer.setLength(0);
            m_buffer.append("boardsize ");
            m_buffer.append(size);
            return m_buffer.toString();
        }
        else
            return null;
    }

    /** Get command for starting a new game.
        Note: call queryProtocolVersion first
        @return The boardsize command for GTP version 1 programs,
        otherwise the clear_board command. */
    public String getCommandClearBoard(int size)
    {
        if (m_protocolVersion == 1)
        {
            m_buffer.setLength(0);
            m_buffer.append("boardsize ");
            m_buffer.append(size);
            return m_buffer.toString();
        }
        else
            return "clear_board";
    }

    /** Get command for generating a move.
        Note: call queryProtocolVersion first
        @param color GoColor::BLACK or GoColor::WHITE
        @return The right command depending on the GTP version. */
    public String getCommandGenmove(GoColor color)
    {
        assert color.isBlackWhite();
        if (m_protocolVersion == 1)
        {
            if (color == BLACK)
                return "genmove_black";
            else
                return "genmove_white";
        }
        if (color == BLACK)
            return "genmove b";
        else
            return "genmove w";
    }

    /** Get command for playing a move.
        Note: call queryProtocolVersion first
        @return The right command depending on the GTP version. */
    public String getCommandPlay(Move move)
    {
        m_buffer.setLength(0);
        if (m_protocolVersion == 1)
        {
            GoColor color = move.getColor();
            String point = GoPoint.toString(move.getPoint());
            if (color == BLACK)
               m_buffer.append("black ");
            else if (color == WHITE)
                m_buffer.append("white ");
            m_buffer.append(point);
        }
        else
        {
            m_buffer.append("play ");
            m_buffer.append(move);
        }
        if (m_lowerCase)
            return m_buffer.toString().toLowerCase(Locale.ENGLISH);
        return m_buffer.toString();
    }

    /** Send cputime command and convert the result to double.
        @throws GtpError if command fails or does not return a floating point
        number. */
    public double getCpuTime() throws GtpError
    {
        try
        {
            return Double.parseDouble(send("cputime"));
        }
        catch (NumberFormatException e)
        {
            throw new GtpError("Invalid response to cputime command");
        }
    }

    /** Get program name or "Unknown Program" if unknown.
        If queryName() was not called or the name command failed, the
        string "Unknown Program" is returned. */
    public String getLabel()
    {
        return (m_name == null ? "Unknown Program" : m_name);
    }

    /** Get program name.
        If queryName() was not called or the name command failed, the
        string "Unknown Program" is returned. */
    public String getName()
    {
        return m_name;
    }

    /** Get protocol version.
        You have to call queryProtocolVersion() first, otherwise this method
        will always return 2. */
    public int getProtocolVersion()
    {
        return m_protocolVersion;
    }

    /** Get the supported commands.
        Note: call querySupportedCommands() first.
        @return A vector of strings with the supported commands. */
    public ArrayList<String> getSupportedCommands()
    {
        ArrayList<String> result = new ArrayList<String>(128);
        if (m_supportedCommands != null)
            for (int i = 0; i < m_supportedCommands.length; ++i)
                result.add(m_supportedCommands[i]);
        return result;
    }

    /** Is the program in a state, that all subsequent commands will fail.
        Returns false, but can be reimplemented in a subclass. */
    public boolean isProgramDead()
    {
        return false;
    }

    /** Check if a command is supported.
        Note: call querySupportedCommands() first. */
    public boolean isSupported(String command)
    {
        if (m_supportedCommands == null)
            return false;
        for (int i = 0; i < m_supportedCommands.length; ++i)
            if (m_supportedCommands[i].equals(command))
                return true;
        return false;
    }

    /** Check if cputime command is supported.
        Note: call querySupportedCommands() first. */
    public boolean isCpuTimeSupported()
    {
        return isSupported("cputime");
    }

    /** Check if a genmove command is supported.
        If list_commands is not supported, it is assumed that genmove is
        supported.
        Note: call querySupportedCommands() first. */
    public boolean isGenmoveSupported()
    {
        if (m_protocolVersion == 1)
            return (! isSupported("help") ||
                    (isSupported("genmove_black")
                     && isSupported("genmove_white")));
        return (! isSupported("list_commands") || isSupported("genmove"));
    }

    /** Check if interrupting a command is supported.
        Interrupting can supported by ANSI C signals or the special
        comment line "# interrupt" as described in the GoGui documentation
        chapter "Interrupting commands".
        Note: call queryInterruptSupport() first. */
    public boolean isInterruptSupported()
    {
        return (m_isInterruptCommentSupported || m_pid != null);
    }

    /** Query if interrupting is supported.
        @see GtpClient#isInterruptSupported */
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

    /** Queries the name.
        @see #getName() */
    public void queryName()
    {
        try
        {
            m_name = send("name");
        }
        catch (GtpError e)
        {
        }
    }

    /** Query the protocol version.
        Assumes version 2 if the protocol_version command is not available,
        fails, or returns a version greater 2.
        @see GtpClientBase#getProtocolVersion */
    public void queryProtocolVersion()
    {
        m_protocolVersion = 2;
        try
        {
            String response = send("protocol_version");
            int v = Integer.parseInt(response);
            if (v == 1 || v == 2)
                m_protocolVersion = v;
        }
        catch (NumberFormatException e)
        {
        }
        catch (GtpError e)
        {
        }
    }

    /** Query the supported commands.
        @see GtpClientBase#getSupportedCommands
        @see GtpClientBase#isSupported */
    public void querySupportedCommands() throws GtpError
    {
        String command = (m_protocolVersion == 1 ? "help" : "list_commands");
        String response = send(command);
        m_supportedCommands = StringUtil.splitArguments(response);
        for (int i = 0; i < m_supportedCommands.length; ++i)
            m_supportedCommands[i] = m_supportedCommands[i].trim();
    }

    /** Queries the program version.
        @return The version or an empty string if the version command fails. */
    public String queryVersion()
    {
        try
        {
            return send("version");
        }
        catch (GtpError e)
        {
            return "";
        }
    }

    /** Send a command.
        @return The response text of the successful response not including
        the status character.
        @throws GtpError containing the response if the command fails. */
    public abstract String send(String command) throws GtpError;

    /** Send comment.
        @param comment comment line (must start with '#'). */
    public abstract void sendComment(String comment);

    /** Send command for setting the board size.
        Send the command if it exists in the GTP protocol version.
        Note: call queryProtocolVersion first
        @see GtpClientBase#getCommandBoardsize */
    public void sendBoardsize(int size) throws GtpError
    {
        String command = getCommandBoardsize(size);
        if (command != null)
            send(command);
    }

    /** Send command for staring a new game.
        Note: call queryProtocolVersion first
        @see GtpClientBase#getCommandClearBoard */
    public void sendClearBoard(int size) throws GtpError
    {
        send(getCommandClearBoard(size));
    }

    /** Send command for playing a move.
        Note: call queryProtocolVersion first */
    public void sendPlay(Move move) throws GtpError
    {
        send(getCommandPlay(move));
    }

    /** Interrupt current command.
        Can be called from a different thread during a send.
        Note: call queryInterruptSupport first
        @see GtpClient#isInterruptSupported
        @throws GtpError if interrupting commands is not supported. */
    public void sendInterrupt() throws GtpError
    {
        if (m_isInterruptCommentSupported)
            sendComment("# interrupt");
        else if (m_pid != null)
        {
            String command = "kill -INT " + m_pid;
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
                printInterrupted();
            }
        }
        else
            throw new GtpError("Interrupt not supported");
    }

    /** Enable lower case mode for play commands.
        For engines that don't implement GTP correctly and understand
        only lower case moves in the play command. */
    public void setLowerCase()
    {
        m_lowerCase = true;
    }

    /** Wait until the process of the program exits.
        Should do nothing if the concrete class does not create a process. */
    public abstract void waitForExit();

    private boolean m_isInterruptCommentSupported;

    protected String m_name;

    private String m_pid;

    /** Local variable in some functions, reused for efficiency. */
    private final StringBuilder m_buffer = new StringBuilder(128);

    private boolean m_lowerCase;

    private int m_protocolVersion = 2;

    private String[] m_supportedCommands;

    private void printInterrupted()
    {
        System.err.println("GtpClient: InterruptedException");
        Thread.dumpStack();
    }
}
