//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.util.*;
import javax.swing.*;
import go.*;
import gtp.*;

//----------------------------------------------------------------------------

public class CommandThread
    extends Thread
{
    public CommandThread(Gtp gtp)
    {
        m_gtp = gtp;
    }

    public void close()
    {
        if (! isProgramDead())
        {
            m_gtp.close();
            m_gtp.waitForExit();
        }
    }

    public void destroyGtp()
    {
        m_gtp.destroyProcess();
    }
    
    /** Get response to asynchronous command.
        You must call getException() first.
    */
    public String getResponse()
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
    public Gtp.Error getException()
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

    public void sendInterrupt() throws Gtp.Error
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

    public void queryProtocolVersion() throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        m_gtp.queryProtocolVersion();
    }

    public void querySupportedCommands() throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        m_gtp.querySupportedCommands();
    }

    public String queryVersion() throws Gtp.Error
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
                catch (Gtp.Error e)
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
    
    public String sendCommand(String command) throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        TimeoutCallback timeoutCallback = new TimeoutCallback(command);
        String response = m_gtp.sendCommand(command, 5000, timeoutCallback);
        return response;
    }

    public void sendCommandBoardsize(int size) throws Gtp.Error
    {
        m_gtp.sendCommandBoardsize(size);
    }

    public void sendCommandClearBoard(int size) throws Gtp.Error
    {
        m_gtp.sendCommandClearBoard(size);
    }

    public String sendCommandPlay(Move move) throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_gtp.sendCommandPlay(move);
    }

    private static class TimeoutCallback
        implements Gtp.TimeoutCallback
    {
        TimeoutCallback(String command)
        {
            m_command = command;
        }

        public boolean askContinue()
        {
            String message = "Program did not respond to command"
                + " '" + m_command + "'\n" +
                "Abort waiting?";
            return ! SimpleDialogs.showQuestion(null, message);
        }

        private String m_command;
    };

    private boolean m_commandInProgress;

    private Gtp m_gtp;

    private Gtp.Error m_exception;

    private Runnable m_callback;

    private String m_command;

    private String m_response;
}

//----------------------------------------------------------------------------
