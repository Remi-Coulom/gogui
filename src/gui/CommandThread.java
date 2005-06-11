//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.Component;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import go.Move;
import gtp.Gtp;
import gtp.GtpError;

//----------------------------------------------------------------------------

/** Wrapper around gtp.Gtp to be used in a GUI environment.
    Allows to send fast commands immediately in the event dispatch thread
    and potentially slow commands in a separate thread with a callback
    in the event thread after the command finished.
    Fast commands are ones that the Go engine is supposed to answer quickly
    (like boardsize, play and undo), however they have a timeout (6 sec) to
    prevent the GUI to hang, if the program does not respond.
    After the timeout a dialog is opened that allows to kill the program or
    continue to wait.
*/
public class CommandThread
    extends Thread
{
    public CommandThread(Gtp gtp, Component owner)
    {
        m_gtp = gtp;
        m_owner = owner;
    }

    public void close()
    {
        if (! isProgramDead())
        {
            m_gtp.close();
            TimeoutCallback timeoutCallback = new TimeoutCallback(null);
            m_gtp.waitForExit(6000, timeoutCallback);
        }
    }

    public void destroyGtp()
    {
        m_gtp.destroyProcess();
    }
    
    /** Get response to asynchronous command.
        You must call getException() first.
    */
    public synchronized String getResponse()
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_response;
    }
    
    public String getCommandClearBoard(int size)
    {
        return m_gtp.getCommandClearBoard(size);
    }

    public String getCommandGenmove(go.Color color)
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
    public synchronized GtpError getException()
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(m_commandInProgress);
        m_commandInProgress = false;
        return m_exception;
    }
    
    public String getProgramCommand()
    {
        assert(SwingUtilities.isEventDispatchThread());
        return m_gtp.getProgramCommand();
    }

    public int getProtocolVersion()
    {
        assert(SwingUtilities.isEventDispatchThread());
        return m_gtp.getProtocolVersion();
    }

    public Vector getSupportedCommands()
    {
        assert(SwingUtilities.isEventDispatchThread());
        return m_gtp.getSupportedCommands();
    }

    public void sendInterrupt() throws GtpError
    {
        m_gtp.sendInterrupt();
    }

    public boolean isCommandInProgress()
    {
        return m_commandInProgress;
    }

    public boolean isCommandSupported(String command)
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_gtp.isCommandSupported(command);
    }

    public boolean isInterruptSupported()
    {
        return m_gtp.isInterruptSupported();
    }

    public boolean isProgramDead()
    {
        assert(SwingUtilities.isEventDispatchThread());
        return m_gtp.isProgramDead();
    }

    public void queryInterruptSupport()
    {
        m_gtp.queryInterruptSupport();
    }

    public void queryProtocolVersion() throws GtpError
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        m_gtp.queryProtocolVersion();
    }

    public void querySupportedCommands() throws GtpError
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        m_gtp.querySupportedCommands();
    }

    public String queryVersion() throws GtpError
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_gtp.queryVersion();
    }

    public void run()
    {
        synchronized (this)
        {
            boolean firstWait = true;
            while (true)
            {
                try
                {
                    if (m_command == null || ! firstWait)
                        wait();
                }
                catch (InterruptedException e)
                {
                    System.err.println("Interrupted.");
                }
                firstWait = false;
                m_response = null;
                m_exception = null;
                try
                {
                    m_response = m_gtp.sendCommand(m_command);
                }
                catch (GtpError e)
                {
                    m_exception = e;
                }
                SwingUtilities.invokeLater(m_callback);
            }
        }
    }

    /** Send asynchronous command. */
    public void sendCommand(String command, Runnable callback)
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        synchronized (this)
        {
            m_command = command;
            m_callback = callback;
            m_commandInProgress = true;
            notifyAll();
        }
    }
    
    /** Send command in event dispatch thread. */
    public String sendCommand(String command)
        throws GtpError
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        TimeoutCallback timeoutCallback = new TimeoutCallback(command);
        String response = m_gtp.sendCommand(command, 6000, timeoutCallback);
        return response;
    }

    public void sendCommandBoardsize(int size) throws GtpError
    {
        m_gtp.sendCommandBoardsize(size);
    }

    public void sendCommandClearBoard(int size) throws GtpError
    {
        m_gtp.sendCommandClearBoard(size);
    }

    public void sendCommandPlay(Move move) throws GtpError
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        m_gtp.sendCommandPlay(move);
    }

    private class TimeoutCallback
        implements Gtp.TimeoutCallback
    {
        TimeoutCallback(String command)
        {
            m_command = command;
        }

        public boolean askContinue()
        {
            String message;
            if (m_command == null)
                message = "Program did not terminate";
            else
                message = "Program did not respond to command"
                    + " '" + m_command + "'";
            message = message + "\nKill program?";
            String title = "Error";
            String options[] = { "Kill Program", "Wait" };
            int result =
                JOptionPane.showOptionDialog(m_owner, message, title,
                                             JOptionPane.YES_NO_OPTION,
                                             JOptionPane.ERROR_MESSAGE,
                                             null, options, options[1]);
            if (result == 0)
                return false;
            return true;
        }

        private final String m_command;
    };

    private boolean m_commandInProgress;

    private Gtp m_gtp;

    private GtpError m_exception;    

    private Component m_owner;

    private Runnable m_callback;

    private String m_command;

    private String m_response;
}

//----------------------------------------------------------------------------
