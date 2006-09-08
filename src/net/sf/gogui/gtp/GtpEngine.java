//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.StringUtil;

//----------------------------------------------------------------------------

/** Base class for Go programs and tools implementing GTP. */
public abstract class GtpEngine
{
    public GtpEngine(PrintStream log)
    {
        m_log = log;
        register("known_command", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdKnownCommand(cmd); } });
        register("list_commands", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdListCommands(cmd); } });
        register("name", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdName(cmd); } });
        register("protocol_version", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdProtocolVersion(cmd); } });
        register("quit", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdQuit(cmd); } });
        register("version", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdVersion(cmd); } });
    }

    public void cmdKnownCommand(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        String name = cmd.getCommand();
        cmd.setResponse(m_commands.containsKey(name) ? "true" : "false");
    }

    public void cmdListCommands(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        StringBuffer response = cmd.getResponse();
        Iterator it = m_commands.keySet().iterator();
        while (it.hasNext())
        {
            response.append(it.next());
            response.append('\n');
        }
    }

    public void cmdName(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        cmd.setResponse(m_name);
    }

    public void cmdProtocolVersion(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        cmd.setResponse("2");
    }

    public void cmdQuit(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        setQuit();
    }

    public void cmdVersion(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        cmd.setResponse(m_version);
    }

    /** Callback for interrupting commands.
        This callback will be invoked if the special comment line
        "# interrupt" is received. It will be invoked from a different thread.
    */
    public abstract void interruptCommand();

    /** Handle command.
        The default implementation looks up the command within the registered
        commands and calls the registered callback.
    */
    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        String name = cmd.getCommand();
        GtpCallback callback = (GtpCallback)m_commands.get(name);
        if (callback == null)
            throw new GtpError("unknown command: " + name);
        callback.run(cmd);
    }

    public synchronized void log(String line)
    {
        assert(m_log != null);
        m_log.println(line);
    }

    /** Main command loop.
        Reads commands and calls GtpEngine.handleCommand until the end of
        the input stream or the quit command is reached.
    */
    public void mainLoop(InputStream in, OutputStream out) throws IOException
    {
        m_out = new PrintStream(out);
        m_in = in;
        ReadThread readThread = new ReadThread(this, m_in, m_log != null);
        readThread.start();
        while (true)
        {
            GtpCommand cmd = readThread.getCommand();
            if (cmd == null)
                return;
            boolean status = true;
            String response;
            try
            {
                handleCommand(cmd);
                response = cmd.getResponse().toString();
            }
            catch (GtpError e)
            {
                response = e.getMessage();
                status = false;
            }
            String sanitizedResponse = response.replaceAll("\\n\\n", "\n \n");
            respond(status, cmd.hasId(), cmd.getId(), sanitizedResponse);
            // TODO: Use only quit flaf not GtpCommand.isQuit once all
            // subclasses use the new registered quit command
            if (m_quit || cmd.isQuit())
                return;
        }
    }

    /** Utility function for parsing an point argument.
        @param cmdArray Command line split into words.
        @param boardSize Board size is needed for parsing the point
        @return GoPoint argument
    */
    public static GoPoint parsePointArgument(String[] cmdArray, int boardSize)
        throws GtpError
    {
        if (cmdArray.length != 2)
            throw new GtpError("Missing point argument");
        return GtpUtil.parsePoint(cmdArray[1], boardSize);
    }

    /** Utility function for parsing an point list argument.
        @param cmdArray Command line split into words.
        @param boardSize Board size is needed for parsing the points
        @return Point list argument
    */
    public static ArrayList parsePointListArgument(String[] cmdArray,
                                                   int boardSize)
        throws GtpError
    {
        int length = cmdArray.length;
        assert(length >= 1);
        ArrayList pointList = new ArrayList();
        for (int i = 1; i < length; ++i)
        {
            GoPoint point = GtpUtil.parsePoint(cmdArray[i], boardSize);
            pointList.add(point);
        }
        return pointList;
    }

    /** Print invalid response directly to output stream.
        Should only be used for simulationg broken GTP implementations
        like used in GtpDummy's dummy_invalid command.
        @param text Text to print to output stream.
        No newline will be appended.
    */
    public void printInvalidResponse(String text)
    {
        m_out.print(text);
    }

    /** Register new command.
        If a command was already registered with the same name,
        it will be replaced by the new command.
    */
    public void register(String command, GtpCallback callback)
    {
        if (m_commands.containsKey(command))
            m_commands.remove(command);
        m_commands.put(command, callback);
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

    /** Set quit flag for terminating command loop. */
    public void setQuit()
    {
        m_quit = true;
    }

    /** Set name for name command. */
    public void setName(String name)
    {
        m_name = name;
    }

    /** Set version for version command. */
    public void setVersion(String version)
    {
        m_version = version;
    }

    private boolean m_quit;

    private String m_name = "Unknown";

    /** Engine version.
        The GTP standard says to return empty string, if no meaningful reponse
        is available.
    */
    private String m_version;

    /** Mapping from command (String) to GtpCallback. */
    private final TreeMap m_commands = new TreeMap();

    private InputStream m_in;

    private final PrintStream m_log;

    private PrintStream m_out;
}

//----------------------------------------------------------------------------

/** Thread reading the command stream.
    Reading is done in a seperate thread to allow the notification
    of GtpServer about an asynchronous interrupt received using
    the special comment line '# interrupt'.
*/
class ReadThread
    extends Thread
{
    public ReadThread(GtpEngine gtpServer, InputStream in, boolean log)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
        m_gtpServer = gtpServer;
        m_log = log;
    }

    public synchronized boolean endOfFile()
    {
        return m_endOfFile;
    }

    public GtpCommand getCommand()
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
            GtpCommand result = m_command;
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
                    synchronized (this)
                    {
                        m_endOfFile = true;
                    }
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
                        m_command = new GtpCommand(line);
                    notifyAll();
                    m_waitCommand = false;
                    if (m_command == null || m_command.isQuit())
                        return;
                }
            }
        }
        catch (Throwable e)
        {
            StringUtil.printException(e);
        }
    }

    private boolean m_endOfFile;

    private final boolean m_log;

    private boolean m_waitCommand;

    private final BufferedReader m_in;

    private GtpCommand m_command;

    private final GtpEngine m_gtpServer;
}

//----------------------------------------------------------------------------
