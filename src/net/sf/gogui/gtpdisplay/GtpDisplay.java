//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpdisplay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtils;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.gui.StatusBar;

//----------------------------------------------------------------------------

/** GTP adapter showing the current board in a window. */
public class GtpDisplay
    extends GtpEngine
{
    public GtpDisplay(String program, boolean verbose, boolean fastPaint)
        throws Exception
    {
        super(null);
        if (! (program == null || program.equals("")))
        {
            m_gtp = new GtpClient(program, verbose, null);
            m_gtp.queryProtocolVersion();
            m_gtp.querySupportedCommands();
        }
        else
            m_gtp = null;
        m_size = GoPoint.DEFAULT_SIZE;
        m_board = new Board(m_size);
        m_frame = new JFrame();
        m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowAdapter windowAdapter = new WindowAdapter()
            {
                public void windowClosing(WindowEvent event)
                {
                    closeFrame();
                }
            };
        m_frame.addWindowListener(windowAdapter);
        if (m_gtp == null)
        {
            m_name = null;
            m_frame.setTitle("GtpDisplay");
        }
        else
        {
            m_name = m_gtp.queryName();
            String title = "GtpDisplay: " + m_name;
            m_frame.setTitle(title);
        }
        Container contentPane = m_frame.getContentPane();
        m_guiBoard = new GuiBoard(m_size, fastPaint);
        if (m_gtp != null)
            m_guiBoard.setShowCursor(false);
        m_guiBoard.setListener(new GuiBoard.Listener()
            {
                public void contextMenu(GoPoint point, Component invoker,
                                        int x, int y)
                {
                }

                public void fieldClicked(GoPoint point,
                                         boolean modifiedSelect)
                {
                    cbFieldClicked(point, modifiedSelect);
                }
            });
        contentPane.add(m_guiBoard);
        m_statusBar = new StatusBar();
        contentPane.add(m_statusBar, BorderLayout.SOUTH);
        GuiUtils.setGoIcon(m_frame);
        m_frame.pack();
        m_frame.setVisible(true);
    }

    public void close()
    {
        if (m_gtp != null)
        {
            m_gtp.close();
            m_gtp.waitForExit();
        }
        try
        {
            invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        if (m_frame != null)
                        {
                            SimpleDialogs.showInfo(m_frame,
                                                   "GTP stream was closed");
                            showStatus("GTP stream was closed");
                        }
                        else if (m_gtp == null)
                            System.exit(0);
                    }
                });
        }
        catch (GtpError e)
        {
            System.err.println(e.getMessage());
        }
    }

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        if (cmd.getCommand().equals("black"))
            cmdPlay(GoColor.BLACK, cmd);
        else if (cmd.getCommand().equals("boardsize"))
            cmdBoardsize(cmd);
        else if (cmd.getCommand().equals("clear_board"))
            cmdClearBoard(cmd);
        else if (cmd.getCommand().equals("genmove"))
            cmdGenmove(cmd);
        else if (cmd.getCommand().equals("genmove_black"))
            cmdUnknown();
        else if (cmd.getCommand().equals("genmove_white"))
            cmdUnknown();
        else if (cmd.getCommand().equals("help"))
            cmdListCommands(cmd);
        else if (cmd.getCommand().equals("komi"))
            cmdKomi(cmd);
        else if (cmd.getCommand().equals("list_commands"))
            cmdListCommands(cmd);
        else if (cmd.getCommand().equals("loadsgf"))
            cmdUnknown();
        else if (cmd.getCommand().equals("name"))
            cmdName(cmd);
        else if (cmd.getCommand().equals("place_free_handicap"))
            cmdPlaceFreeHandicap(cmd);
        else if (cmd.getCommand().equals("play"))
            cmdPlay(cmd);
        else if (cmd.getCommand().equals("protocol_version"))
            cmd.setResponse("2");
        else if (cmd.getCommand().equals("set_free_handicap"))
            cmdSetFreeHandicap(cmd);
        else if (cmd.getCommand().equals("version"))
            cmdVersion();
        else if (cmd.getCommand().equals("undo"))
            cmdUndo(cmd);
        else if (cmd.getCommand().equals("white"))
            cmdPlay(GoColor.WHITE, cmd);
        else if (cmd.getCommand().equals("quit"))
            cmdQuit(cmd);
        else
        {
            if (m_gtp == null)
                throw new GtpError("unknown command");
            send(cmd.getLine(), cmd.getResponse());
        }
    }

    public void interruptCommand()
    {
        if (m_gtp == null)
            return;
        try
        {
            if (m_gtp.isInterruptSupported())
                m_gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            System.err.println(e);
        }
    }

    /** Only accept this board size.
        A value of -1 means accept any size.
    */
    private int m_size;

    private final Board m_board;

    private GoColor m_color;

    private final GuiBoard m_guiBoard;

    private GoPoint m_fieldClicked;

    private final GtpClient m_gtp;

    private Move m_move;

    private final Object m_mutex = new Object();

    private JFrame m_frame;

    private StatusBar m_statusBar;

    private final String m_name;

    private void cbFieldClicked(GoPoint point, boolean modifiedSelect)
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (m_board.getColor(point) != GoColor.EMPTY)
            return;
        synchronized (m_mutex)
        {
            if (modifiedSelect)
                m_fieldClicked = null;
            else
                m_fieldClicked = point;
            m_mutex.notifyAll();
        }
    }

    private void clearStatus()
    {
        m_statusBar.clear();
    }

    private void closeFrame()
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (m_gtp == null)
        {
            if (! SimpleDialogs.showQuestion(m_frame,
                                             "Terminate GtpDisplay?"))
                return;
            System.exit(0);
        }
        m_frame.dispose();
        m_frame = null;
    }

    private void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAXSIZE);
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandBoardsize(size);
            if (command != null)
                send(command);
            command = m_gtp.getCommandClearBoard(size);
            send(command);
        }
        m_size = size;
        invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.initSize(m_size);
                    m_guiBoard.initSize(m_size);
                    m_frame.pack();
                    updateFromGoBoard();
                }
            });
    }

    private void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandClearBoard(m_size);
            send(command);
        }
        invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.newGame();
                    updateFromGoBoard();
                }
            });
    }

    private void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        GoColor color = cmd.getColorArg();
        GoPoint point;        
        if (m_gtp == null && m_frame != null)
        {
            m_color = color;
            invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        showStatus("Input move for " + m_color
                                   + " (right click for pass)");
                    }
                });
            synchronized (m_mutex)
            {
                try
                {
                    m_mutex.wait();
                }
                catch (InterruptedException e)
                {
                    System.err.println("InterruptedException");
                }
                point = m_fieldClicked;
            }
            cmd.setResponse(GoPoint.toString(point));
        }
        else
        {
            String command = m_gtp.getCommandGenmove(color);
            send(command, cmd.getResponse());
            point = GtpUtils.parsePoint(cmd.getResponse().toString(), m_size);
        }
        m_move = Move.get(point, color);
        invokeAndWait(new Runnable()
            {
                public void run()
                {                    
                    m_board.play(m_move);
                    updateFromGoBoard();
                    clearStatus();
                }
            });
    }

    private void cmdKomi(GtpCommand cmd) throws GtpError
    {
        if (m_gtp == null)
            return;
        send(cmd.getLine(), cmd.getResponse());
    }

    private void cmdListCommands(GtpCommand cmd) throws GtpError
    {
        if (m_gtp != null)
        {
            ArrayList commands = m_gtp.getSupportedCommands();
            for (int i = 0; i < commands.size(); ++i)
            {
                String c = (String)commands.get(i);
                if (c.equals("boardsize")
                    || c.equals("black")
                    || c.equals("clear_board")
                    || c.equals("genmove")
                    || c.equals("genmove_black")
                    || c.equals("genmove_white")
                    || c.equals("help")
                    || c.equals("komi")
                    || c.equals("list_commands")
                    || c.equals("play")
                    || c.equals("protocol_version")
                    || c.equals("quit")
                    || c.equals("white"))
                    continue;
                cmd.getResponse().append(c);
                cmd.getResponse().append("\n");
            }
        }
        cmd.getResponse().append("boardsize\n");
        cmd.getResponse().append("clear_board\n");        
        cmd.getResponse().append("genmove\n");
        cmd.getResponse().append("komi\n");
        cmd.getResponse().append("list_commands\n");
        cmd.getResponse().append("play\n");
        cmd.getResponse().append("protocol_version\n");
        cmd.getResponse().append("quit\n");
    }

    private void cmdName(GtpCommand cmd)
    {
        if (m_gtp == null)
            cmd.setResponse("GtpDisplay");
        else            
            cmd.setResponse(m_name);
    }

    private void cmdPlaceFreeHandicap(GtpCommand cmd) throws GtpError
    {
        int n = cmd.getIntArg();
        ArrayList stones = Board.getHandicapStones(m_size, n);
        if  (stones == null)
            throw new GtpError("Invalid number of handicap stones");
        StringBuffer pointList = new StringBuffer(128);
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint point = (GoPoint)stones.get(i);
            play(GoColor.BLACK, point);
            if (pointList.length() > 0)
                pointList.append(' ');
            pointList.append(point);
        }
        cmd.setResponse(pointList.toString());
    }

    private void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_size);
        play(color, point);
    }

    private void cmdPlay(GoColor color, GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        GoPoint point = cmd.getPointArg(0, m_size);
        play(color, point);
    }

    private void cmdQuit(GtpCommand cmd) throws GtpError
    {
        if (m_gtp != null)
            send("quit", cmd.getResponse());
    }

    private void cmdSetFreeHandicap(GtpCommand cmd) throws GtpError
    {
        for (int i = 0; i < cmd.getNuArg(); ++i)
            play(GoColor.BLACK, cmd.getPointArg(i, m_size));
    }

    private void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        undo();
    }

    private void cmdUnknown() throws GtpError
    {
        throw new GtpError("Unknown command");
    }

    private void cmdVersion()
    {
    }

    private void invokeAndWait(Runnable runnable) throws GtpError
    {
        try
        {
            SwingUtilities.invokeAndWait(runnable);
        }
        catch (InterruptedException e)
        {
            // Shouldn't happen
            throw new GtpError("InterruptedException");
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            throw new GtpError("InvocationTargetException");
        }
    }

    private void play(GoColor color, GoPoint point) throws GtpError
    {
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandPlay(Move.get(point, color));
            send(command);
        }
        m_move = Move.get(point, color);
        invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.play(m_move);
                    updateFromGoBoard();
                }
            });
    }

    private void send(String cmd, StringBuffer response) throws GtpError
    {
        response.append(m_gtp.send(cmd));
    }

    private void send(String cmd) throws GtpError
    {
        m_gtp.send(cmd);
    }

    private void showStatus(String text)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_statusBar.setText(text);
    }

    private void undo() throws GtpError
    {
        if (m_gtp != null)
            send("undo");
        invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.undo();
                    updateFromGoBoard();
                }
            });
    }

    private void updateFromGoBoard()
    {
        GuiBoardUtils.updateFromGoBoard(m_guiBoard, m_board, true);
    }
}

//----------------------------------------------------------------------------
