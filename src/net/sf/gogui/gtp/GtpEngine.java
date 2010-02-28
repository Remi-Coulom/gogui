// GtpEngine.java

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.StringUtil;

/** Base class for Go programs and tools implementing GTP. */
public class GtpEngine
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
        String name = cmd.getArg();
        cmd.setResponse(m_commands.containsKey(name) ? "true" : "false");
    }

    public void cmdListCommands(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        StringBuilder response = cmd.getResponse();
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

    public void cmdUnknown(GtpCommand cmd) throws GtpError
    {
        throw new GtpError("unknown command: " + cmd.getCommand());
    }

    public void cmdVersion(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        cmd.setResponse(m_version);
    }

    /** Callback for interrupting commands.
        This callback will be invoked if the special comment line
        "# interrupt" is received. It will be invoked from a different
        thread. */
    public void interruptCommand()
    {
        m_interrupted = true;
    }

    /** Handle command.
        The default implementation looks up the command within the registered
        commands and calls the registered callback. */
    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        m_interrupted = false;
        String name = cmd.getCommand();
        GtpCallback callback = (GtpCallback)m_commands.get(name);
        if (callback == null)
            cmdUnknown(cmd);
        else
            callback.run(cmd);
    }

    public boolean isRegistered(String command)
    {
        return m_commands.containsKey(command);
    }

    public synchronized void log(String line)
    {
        assert m_log != null;
        m_log.println(line);
    }

    /** Main command loop.
        Reads commands and calls GtpEngine.handleCommand until the end of
        the input stream or the quit command is reached. */
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
            // TODO: Use only quit flag not GtpCommand.isQuit once all
            // subclasses use the new registered quit command
            if (m_quit || cmd.isQuit())
                return;
        }
    }

    /** Utility function for parsing a point argument.
        @param cmdArray Command line split into words.
        @param boardSize Board size is needed for parsing the point
        @return GoPoint argument */
    public static GoPoint parsePointArgument(String[] cmdArray, int boardSize)
        throws GtpError
    {
        if (cmdArray.length != 2)
            throw new GtpError("Missing point argument");
        try
        {
            return GoPoint.parsePoint(cmdArray[1], boardSize);
        }
        catch (InvalidPointException e)
        {
            throw new GtpError(e.getMessage());
        }
    }

    /** Utility function for parsing an point list argument.
        @param cmdArray Command line split into words.
        @param boardSize Board size is needed for parsing the points
        @return Point list argument */
    public static PointList parsePointListArgument(String[] cmdArray,
                                                   int boardSize)
        throws GtpError
    {
        try
        {
            int length = cmdArray.length;
            assert length >= 1;
            PointList pointList = new PointList();
            for (int i = 1; i < length; ++i)
            {
                GoPoint p = GoPoint.parsePoint(cmdArray[i], boardSize);
                pointList.add(p);
            }
            return pointList;
        }
        catch (InvalidPointException e)
        {
            throw new GtpError(e.getMessage());
        }
    }

    /** Print invalid response directly to output stream.
        Should only be used for simulationg broken GTP implementations
        like used in the gogui-dummy_invalid command.
        @param text Text to print to output stream.
        No newline will be appended. */
    public void printInvalidResponse(String text)
    {
        m_out.print(text);
    }

    /** Register new command.
        If a command was already registered with the same name,
        it will be replaced by the new command. */
    public final void register(String command, GtpCallback callback)
    {
        unregister(command);
        m_commands.put(command, callback);
    }

    public void respond(boolean status, boolean hasId, int id,
                        String response)
    {
        StringBuilder fullResponse = new StringBuilder(256);
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

    public final void unregister(String command)
    {
        if (m_commands.containsKey(command))
            m_commands.remove(command);
    }

    protected boolean isInterrupted()
    {
        return m_interrupted;
    }

    private volatile boolean m_interrupted;

    private boolean m_quit;

    private String m_name = "Unknown";

    /** Engine version.
        The GTP standard says to return empty string, if no meaningful reponse
        is available. */
    private String m_version;

    /** Mapping from command to callback. */
    private final Map<String,GtpCallback> m_commands
        = new TreeMap<String,GtpCallback>();

    private InputStream m_in;

    private final PrintStream m_log;

    private PrintStream m_out;
}

/** Thread reading the command stream.
    Reading is done in a seperate thread to allow the notification
    of Server about an asynchronous interrupt received using
    the special comment line '# interrupt'. */
class ReadThread
    extends Thread
{
    public ReadThread(GtpEngine server, InputStream in, boolean log)
    {
        m_in = new BufferedReader(new InputStreamReader(in));
        m_server = server;
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
            assert ! m_waitCommand;
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
            assert m_endOfFile || ! m_waitCommand;
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
                        m_server.log(line);
                    line = line.trim();
                    if (line.equals("# interrupt"))
                    {
                        m_server.interruptCommand();
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

    private final GtpEngine m_server;
}
