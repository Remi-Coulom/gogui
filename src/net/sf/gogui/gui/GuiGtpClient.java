//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.SwingUtilities;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpSynchronizer;

/** Wrapper around gtp.GtpClient to be used in a GUI environment.
    Allows to send fast commands immediately in the event dispatch thread
    and potentially slow commands in a separate thread with a callback
    in the event thread after the command finished.
    Fast commands are ones that the Go engine is supposed to answer quickly
    (like boardsize, play and undo), however they have a timeout to
    prevent the GUI to hang, if the program does not respond.
    After the timeout a dialog is opened that allows to kill the program or
    continue to wait.
*/
public class GuiGtpClient
{
    public GuiGtpClient(GtpClient gtp, Component owner,
                        GtpSynchronizer.Listener listener,
                        MessageDialogs messageDialogs)
    {
        m_gtp = gtp;
        m_owner = owner;
        m_messageDialogs = messageDialogs;
        m_gtpSynchronizer = new GtpSynchronizer(gtp, listener, false);
        Thread thread = new Thread() {
                public void run() {
                    synchronized (m_mutex)
                    {
                        boolean firstWait = true;
                        while (true)
                        {
                            try
                            {
                                if (m_command == null || ! firstWait)
                                    m_mutex.wait();
                            }
                            catch (InterruptedException e)
                            {
                                System.err.println("Interrupted");
                            }
                            firstWait = false;
                            m_response = null;
                            m_exception = null;
                            try
                            {
                                m_response = m_gtp.send(m_command);
                            }
                            catch (GtpError e)
                            {
                                m_exception = e;
                            }
                            SwingUtilities.invokeLater(m_callback);
                        }
                    }
                }
            };
        thread.start();
    }

    public void close()
    {
        if (! isProgramDead())
        {
            m_gtp.close();
            TimeoutCallback timeoutCallback = new TimeoutCallback(null);
            m_gtp.waitForExit(TIMEOUT, timeoutCallback);
        }
    }

    public void destroyGtp()
    {
        m_gtp.destroyProcess();
    }

    public void setAutoNumber(boolean enable)
    {
        m_gtp.setAutoNumber(enable);
    }

    /** Query analyze commands configuration from the program.
        @return The response to gogui-analyze_commands or null, if this
        command is not supported.
    */
    public String getAnalyzeCommands()
    {
        String command;
        if (isSupported("gogui-analyze_commands"))
            command = "gogui-analyze_commands";
        else if (isSupported("gogui_analyze_commands"))
            // Used by old versions of GoGui
            command = "gogui_analyze_commands";
        else
            return null;
        try
        {
            return send(command);
        }
        catch (GtpError e)
        {
            return null;
        }
    }

    public boolean getAnyCommandsResponded()
    {
        return m_gtp.getAnyCommandsResponded();
    }

    public String getCommandClearBoard(int size)
    {
        return m_gtp.getCommandClearBoard(size);
    }

    public String getCommandGenmove(GoColor color)
    {
        return m_gtp.getCommandGenmove(color);
    }

    public String getCommandPlay(Move move)
    {
        return m_gtp.getCommandPlay(move);
    }

    /** Get exception of asynchronous command.
        You must call this before you are allowed to send new a command.
    */
    public GtpError getException()
    {
        synchronized (m_mutex)
        {
            assert SwingUtilities.isEventDispatchThread();
            assert m_commandInProgress;
            m_commandInProgress = false;
            return m_exception;
        }
    }

    public String getProgramLabel()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.getLabel();
    }

    public String getProgramName()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.getName();
    }

    public String getProgramCommand()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.getProgramCommand();
    }

    public int getProtocolVersion()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.getProtocolVersion();
    }

    /** Get response to asynchronous command.
        You must call getException() first.
    */
    public String getResponse()
    {
        synchronized (m_mutex)
        {
            assert SwingUtilities.isEventDispatchThread();
            assert ! m_commandInProgress;
            return m_response;
        }
    }

    public ArrayList<String> getSupportedCommands()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.getSupportedCommands();
    }

    public String getTitleFromProgram()
    {
        try
        {
            if (isSupported("gogui-title"))
                return send("gogui-title");
            else if (isSupported("gogui_title"))
                return send("gogui_title");
        }
        catch (GtpError e)
        {
        }
        return null;
    }

    public void sendInterrupt() throws GtpError
    {
        m_gtp.sendInterrupt();
    }

    public void initSynchronize(ConstBoard board, Komi komi,
                                TimeSettings timeSettings) throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        m_gtpSynchronizer.init(board, komi, timeSettings);
    }

    public boolean isCommandInProgress()
    {
        return m_commandInProgress;
    }

    public boolean isGenmoveSupported()
    {
        return m_gtp.isGenmoveSupported();
    }

    public boolean isSupported(String command)
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        return m_gtp.isSupported(command);
    }

    public boolean isInterruptSupported()
    {
        return m_gtp.isInterruptSupported();
    }

    public boolean isOutOfSync()
    {
        return m_gtpSynchronizer.isOutOfSync();
    }

    public boolean isProgramDead()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.isProgramDead();
    }

    public void queryInterruptSupport()
    {
        m_gtp.queryInterruptSupport();
    }

    public void queryName() throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        TimeoutCallback timeoutCallback = new TimeoutCallback("name");
        m_gtp.queryName(TIMEOUT, timeoutCallback);
    }

    public void queryProtocolVersion()
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        m_gtp.queryProtocolVersion();
    }

    public void querySupportedCommands() throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        m_gtp.querySupportedCommands();
    }

    public String queryVersion() throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        return m_gtp.queryVersion();
    }

    /** Send asynchronous command. */
    public void send(String command, Runnable callback)
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        synchronized (m_mutex)
        {
            m_command = command;
            m_callback = callback;
            m_commandInProgress = true;
            m_mutex.notifyAll();
        }
    }

    /** Send command in event dispatch thread. */
    public String send(String command) throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        TimeoutCallback timeoutCallback = new TimeoutCallback(command);
        return m_gtp.send(command, TIMEOUT, timeoutCallback);
    }

    public void sendBoardsize(int size) throws GtpError
    {
        m_gtp.sendBoardsize(size);
    }

    public void sendClearBoard(int size) throws GtpError
    {
        m_gtp.sendClearBoard(size);
    }

    public void sendPlay(Move move) throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        TimeoutCallback timeoutCallback = new TimeoutCallback("play");
        m_gtp.sendPlay(move, TIMEOUT, timeoutCallback);
    }

    public void synchronize(ConstBoard board, Komi komi,
                            TimeSettings timeSettings) throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        m_gtpSynchronizer.synchronize(board, komi, timeSettings);
    }

    public void updateAfterGenmove(ConstBoard board)
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        m_gtpSynchronizer.updateAfterGenmove(board);
    }

    public void updateHumanMove(ConstBoard board, Move move) throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        m_gtpSynchronizer.updateHumanMove(board, move);
    }

    public boolean wasKilled()
    {
        return m_gtp.wasKilled();
    }

    private class TimeoutCallback
        implements GtpClient.TimeoutCallback
    {
        TimeoutCallback(String command)
        {
            m_command = command;
        }

        public boolean askContinue()
        {
            String mainMessage = "Terminate Go program?";
            String optionalMessage;
            if (m_command == null)
                optionalMessage =
                    "The Go program did not quit after the command" +
                    " stream was closed.";
            else
                optionalMessage =
                    "The Go program did not respond to the command \""
                    + m_command + "\".";
            optionalMessage = optionalMessage +
                " Would you like to terminate the Go program now" +
                " or wait?";
            return ! m_messageDialogs.showQuestion(null, m_owner, mainMessage,
                                                   optionalMessage,
                                                   "Terminate Program", "Wait",
                                                   true);
        }

        private final String m_command;
    };

    /** The timeout for commands that are expected to be fast.
        GoGui 0.9.4 used 8 sec, but this was not enough on some machines
        when starting up engines like Aya in the Wine emulator.
    */
    private static final int TIMEOUT = 15000;

    private boolean m_commandInProgress;

    private final GtpClient m_gtp;

    private GtpError m_exception;

    private final GtpSynchronizer m_gtpSynchronizer;

    private final Component m_owner;

    private final MessageDialogs m_messageDialogs;

    private final Object m_mutex = new Object();

    private Runnable m_callback;

    private String m_command;

    private String m_response;
}
