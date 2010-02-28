// GuiGtpClient.java

package net.sf.gogui.gui;

import java.awt.Component;
import java.text.MessageFormat;
import javax.swing.SwingUtilities;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClientBase;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpSynchronizer;
import static net.sf.gogui.gui.I18n.i18n;

/** Wrapper around gtp.GtpClient to be used in a GUI environment.
    Allows to send fast commands with the GtpClientBase.send() function
    immediately in the event dispatch thread and potentially slow commands in
    a separate thread with a callback in the event thread after the command
    finished.
    Fast commands are ones that the Go engine is supposed to answer quickly
    (like boardsize, play and undo), however they have a timeout to
    prevent the GUI to hang, if the program does not respond.
    After the timeout a dialog is opened that allows to kill the program or
    continue to wait.
    This class also contains a GtpSynchronizer. */
public class GuiGtpClient
    extends GtpClientBase
{
    public GuiGtpClient(GtpClient gtp, Component owner,
                        GtpSynchronizer.Listener listener,
                        MessageDialogs messageDialogs)
    {
        m_gtp = gtp;
        m_owner = owner;
        m_messageDialogs = messageDialogs;
        m_gtpSynchronizer = new GtpSynchronizer(this, listener, false);
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

    public boolean getAnyCommandsResponded()
    {
        return m_gtp.getAnyCommandsResponded();
    }

    /** Get exception of asynchronous command.
        You must call this before you are allowed to send new a command. */
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

    public String getProgramCommand()
    {
        return m_gtp.getProgramCommand();
    }

    /** Get response to asynchronous command.
        You must call getException() first. */
    public String getResponse()
    {
        synchronized (m_mutex)
        {
            assert SwingUtilities.isEventDispatchThread();
            assert ! m_commandInProgress;
            return m_response;
        }
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

    public boolean isOutOfSync()
    {
        return m_gtpSynchronizer.isOutOfSync();
    }

    public boolean isProgramDead()
    {
        assert SwingUtilities.isEventDispatchThread();
        return m_gtp.isProgramDead();
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

    public void sendComment(String comment)
    {
        m_gtp.sendComment(comment);
    }

    /** Send command in event dispatch thread. */
    public String send(String command) throws GtpError
    {
        assert SwingUtilities.isEventDispatchThread();
        assert ! m_commandInProgress;
        TimeoutCallback timeoutCallback = new TimeoutCallback(command);
        return m_gtp.send(command, TIMEOUT, timeoutCallback);
    }

    public void setAutoNumber(boolean enable)
    {
        m_gtp.setAutoNumber(enable);
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

    public void waitForExit()
    {
        m_gtp.waitForExit();
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
            String mainMessage = i18n("MSG_PROGRAM_NOT_RESPONDING");
            String optionalMessage;
            String destructiveOption;
            if (m_command == null)
            {
                optionalMessage = i18n("MSG_PROGRAM_NOT_RESPONDING_2");
                destructiveOption = i18n("LB_FORCE_QUIT_PROGRAM");
            }
            else
            {
                optionalMessage =
                    MessageFormat.format(i18n("MSG_PROGRAM_NOT_RESPONDING_3"),
                                         m_command);
                destructiveOption = i18n("LB_TERMINATE_PROGRAM");
            }
            return ! m_messageDialogs.showWarningQuestion(null, m_owner,
                                                          mainMessage,
                                                          optionalMessage,
                                                          destructiveOption,
                                                          i18n("LB_WAIT"),
                                                          true);
        }

        private final String m_command;
    };

    /** The timeout for commands that are expected to be fast.
        GoGui 0.9.4 used 8 sec, but this was not enough on some machines
        when starting up engines like Aya in the Wine emulator. */
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
