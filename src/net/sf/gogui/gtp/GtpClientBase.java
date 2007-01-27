//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.StringUtil;

/** Interface to a Go program that uses GTP over the standard I/O streams.
    Implements most of the functionality apart from that the commands are
    sent through and received from streams (to allow a more efficient testing
    implementaton of a GTP client that directly calls command callbacks at
    a GtpEngine).
*/
public abstract class GtpClientBase
{
    /** Close output connection.
        Should do nothing if the concrete class does not communicate through
        streams.
    */
    public abstract void close();

    /** Get command for setting the board size.
        Note: call queryProtocolVersion first
        @return The boardsize command for GTP version 2 programs,
        otherwise null.
    */
    public String getCommandBoardsize(int size)
    {
        if (m_protocolVersion == 2)
            return ("boardsize " + size);
        else
            return null;
    }

    /** Get command for starting a new game.
        Note: call queryProtocolVersion first
        @return The boardsize command for GTP version 1 programs,
        otherwise the clear_board command.
    */
    public String getCommandClearBoard(int size)
    {
        if (m_protocolVersion == 1)
            return "boardsize " + size;
        else
            return "clear_board";
    }

    /** Get command for generating a move.
        Note: call queryProtocolVersion first
        @param color GoColor::BLACK or GoColor::WHITE
        @return The right command depending on the GTP version.
    */
    public String getCommandGenmove(GoColor color)
    {
        assert(color == GoColor.BLACK || color == GoColor.WHITE);
        if (m_protocolVersion == 1)
        {
            if (color == GoColor.BLACK)
                return "genmove_black";
            else
                return "genmove_white";
        }
        if (color == GoColor.BLACK)
            return "genmove b";
        else
            return "genmove w";
    }

    /** Get command for playing a move.
        Note: call queryProtocolVersion first
        @param move Any color, including GoColor.EMPTY, this is
        non-standard GTP, but GoGui tries to transmit empty setup
        points this way, even if it is only to produce an error with the
        Go engine.
        @return The right command depending on the GTP version.
    */
    public String getCommandPlay(Move move)
    {
        String point = GoPoint.toString(move.getPoint());
        GoColor color = move.getColor();
        String command;
        if (m_protocolVersion == 1)
        {
            if (color == GoColor.BLACK)
               command = "black " + point;
            else if (color == GoColor.WHITE)
                command = "white " + point;
            else
                command = "empty " + point;
        }
        else
        {
            if (color == GoColor.BLACK)
                command = "play b " + point;
            else if (color == GoColor.WHITE)
                command = "play w " + point;
            else
                command = "play empty " + point;
        }
        if (m_lowerCase)
            command = command.toLowerCase(Locale.ENGLISH);
        return command;
    }

    /** Send cputime command and convert the result to double.
        @throws GtpError if command fails or does not return a floating point
        number.
    */
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

    /** Get protocol version.
        You have to call queryProtocolVersion() first, otherwise this method
        will always return 2.
    */
    public int getProtocolVersion()
    {
        return m_protocolVersion;
    }

    /** Get the supported commands.
        Note: call querySupportedCommands() first.
        @return A vector of strings with the supported commands.
    */
    public ArrayList getSupportedCommands()
    {
        ArrayList result = new ArrayList(128);
        if (m_supportedCommands != null)
            for (int i = 0; i < m_supportedCommands.length; ++i)
                result.add(m_supportedCommands[i]);
        return result;
    }

    /** Check if a command is supported.
        Note: call querySupportedCommands() first.
    */
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
        Note: call querySupportedCommands() first.
    */
    public boolean isCpuTimeSupported()
    {
        return isSupported("cputime");
    }

    /** Check if interrupting a command is supported. */
    public abstract boolean isInterruptSupported();

    /** Queries the name.
        @return Name or "Unknown Program" if name command not supported
    */
    public String queryName()
    {
        try
        {
            return send("name");
        }
        catch (GtpError e)
        {
            return "Unknown Program";
        }
    }

    /** Query the protocol version.
        Sets the protocol version to the response or to 2 if protocol_version
        command fails.
        @see GtpClientBase#getProtocolVersion
        @throws GtpError if the response to protocol_version is not 1 or 2.
    */
    public void queryProtocolVersion() throws GtpError
    {
        try
        {            
            String response;
            try
            {
                response = send("protocol_version");
            }
            catch (GtpError e)
            {
                m_protocolVersion = 2;
                return;
            }
            int v = Integer.parseInt(response);
            if (v < 1 || v > 2)
                throw new GtpError("Unknown protocol version: " + v);
            m_protocolVersion = v;
        }
        catch (NumberFormatException e)
        {
            throw new GtpError("Invalid protocol version");
        }
    }

    /** Query the supported commands.
        @see GtpClientBase#getSupportedCommands
        @see GtpClientBase#isSupported
    */
    public void querySupportedCommands() throws GtpError
    {
        String command = (m_protocolVersion == 1 ? "help" : "list_commands");
        String response = send(command);
        m_supportedCommands = StringUtil.splitArguments(response);
        for (int i = 0; i < m_supportedCommands.length; ++i)
            m_supportedCommands[i] = m_supportedCommands[i].trim();
    }

    /** Queries the program version.
        @return The version or an empty string if the version command fails.
    */
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
        @throws GtpError containing the response if the command fails.
    */
    public abstract String send(String command) throws GtpError;

    /** Send command for setting the board size.
        Send the command if it exists in the GTP protocol version.
        Note: call queryProtocolVersion first
        @see GtpClientBase#getCommandBoardsize
    */
    public void sendBoardsize(int size) throws GtpError
    {
        String command = getCommandBoardsize(size);
        if (command != null)
            send(command);
    }

    /** Send command for staring a new game.
        Note: call queryProtocolVersion first
        @see GtpClientBase#getCommandClearBoard
    */
    public void sendClearBoard(int size) throws GtpError
    {
        send(getCommandClearBoard(size));
    }

    /** Send command for playing a move.
        Note: call queryProtocolVersion first
    */
    public void sendPlay(Move move) throws GtpError
    {
        send(getCommandPlay(move));
    }

    /** Interrupt current command.
        Can be called from a different thread during a send.
        @throws GtpError if interrupting commands is not supported.
    */
    public abstract void sendInterrupt() throws GtpError;

    /** Enable lower case mode for play commands.
        For engines that don't implement GTP correctly and understand
        only lower case moves in the play command.
    */
    public void setLowerCase()
    {
        m_lowerCase = true;
    }

    /** Wait until the process of the program exits.
        Should do nothing if the concrete class does not create a process.
    */
    public abstract void waitForExit();

    private boolean m_lowerCase;

    private int m_protocolVersion = 2;

    private String[] m_supportedCommands;
}

