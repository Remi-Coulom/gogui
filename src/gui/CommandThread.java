//=============================================================================
// $Id$
// $Source$
//=============================================================================

package gui;

import java.util.*;
import javax.swing.*;
import go.*;
import gtp.*;

//=============================================================================

class CommandThread
    extends Thread
{
    public CommandThread(Gtp gtp, GtpShell gtpShell)
    {
        m_gtp = gtp;
        m_gtpShell = gtpShell;
    }

    public void close()
    {
        if (! isProgramDead())
        {
            m_gtp.close();
            m_gtp.waitForExit();
        }
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

    public Vector getSupportedCommands()
    {
        assert(SwingUtilities.isEventDispatchThread());
        return m_gtp.getSupportedCommands();
    }

    public boolean isCommandSupported(String command)
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_gtp.isCommandSupported(command);
    }

    public boolean isProgramDead()
    {
        assert(SwingUtilities.isEventDispatchThread());
        return m_gtp.isProgramDead();
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
            while (true)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    System.err.println("Interrupted.");
                }
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

    /** Send special comment line for interrupting commands.
        Only allowed while no command or command with callback is running.
     */
    public void sendInterrupt()
    {
        m_gtp.sendInterrupt();
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
        String response = m_gtp.sendCommand(command);
        return response;
    }

    public String sendCommand(String command, long timeout) throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_gtp.sendCommand(command, timeout);
    }

    public void sendCommandsClearBoard(int size) throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        m_gtp.sendCommandsClearBoard(size);
    }

    public String sendCommandPlay(Move move) throws Gtp.Error
    {
        assert(SwingUtilities.isEventDispatchThread());
        assert(! m_commandInProgress);
        return m_gtp.sendCommandPlay(move);
    }

    private boolean m_commandInProgress;

    private Gtp m_gtp;

    private Gtp.Error m_exception;

    private GtpShell m_gtpShell;

    private static JFrame m_mainFrame;

    private Runnable m_callback;

    private String m_command;

    private String m_response;

    private Thread m_commandThread;
}

//=============================================================================
