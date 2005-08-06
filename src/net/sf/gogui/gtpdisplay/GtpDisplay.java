//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpdisplay;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiField;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.SquareLayout;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** GTP adapter showing the current board in a window. */
public class GtpDisplay
    extends GtpEngine
{
    public GtpDisplay(InputStream in, OutputStream out, String program,
                      boolean verbose, boolean fastPaint)
        throws Exception
    {
        super(in, out, null);
        if (! (program == null || program.equals("")))
        {
            m_gtp = new Gtp(program, verbose, null);
            m_gtp.queryProtocolVersion();
            m_gtp.querySupportedCommands();
        }
        else
            m_gtp = null;
        m_size = 19;
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
        m_guiBoard = new GuiBoard(m_board, fastPaint);
        if (m_gtp != null)
            m_guiBoard.setShowCursor(false);
        m_guiBoard.setListener(new GuiBoard.Listener()
            {
                public void contextMenu(GoPoint point, GuiField field)
                {
                }

                public void fieldClicked(GoPoint point,
                                         boolean modifiedSelect)
                {
                    cbFieldClicked(point, modifiedSelect);
                }
            });
        m_squareLayout = new SquareLayout();
        m_squareLayout.setPreferMultipleOf(m_size + 2);
        JPanel panel = new JPanel(m_squareLayout);
        panel.add(m_guiBoard);
        contentPane.add(panel);
        contentPane.add(createStatusBar(), BorderLayout.SOUTH);
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
        StringBuffer error = new StringBuffer();
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

    public void handleCommand(String cmdLine, StringBuffer response)
        throws GtpError
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        if (cmd.equals("black"))
            cmdPlay(GoColor.BLACK, cmdArray);
        else if (cmd.equals("boardsize"))
            cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board"))
            cmdClearBoard(response);
        else if (cmd.equals("genmove"))
            cmdGenmove(cmdArray, response);
        else if (cmd.equals("genmove_black"))
            cmdUnknown();
        else if (cmd.equals("genmove_white"))
            cmdUnknown();
        else if (cmd.equals("help"))
            cmdListCommands(response);
        else if (cmd.equals("list_commands"))
            cmdListCommands(response);
        else if (cmd.equals("loadsgf"))
            cmdUnknown();
        else if (cmd.equals("name"))
            cmdName(response);
        else if (cmd.equals("place_free_handicap"))
            cmdPlaceFreeHandicap(cmdArray, response);
        else if (cmd.equals("play"))
            cmdPlay(cmdArray);
        else if (cmd.equals("protocol_version"))
            response.append("2");
        else if (cmd.equals("set_free_handicap"))
            cmdSetFreeHandicap(cmdArray);
        else if (cmd.equals("version"))
            cmdVersion();
        else if (cmd.equals("undo"))
            cmdUndo(cmdArray);
        else if (cmd.equals("white"))
            cmdPlay(GoColor.WHITE, cmdArray);
        else if (cmd.equals("quit"))
            cmdQuit(response);
        else
        {
            if (m_gtp == null)
                throw new GtpError("unknown command");
            send(cmdLine, response);
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

    private final Gtp m_gtp;

    private Move m_move;

    private JFrame m_frame;

    private JLabel m_statusLabel;

    private final String m_name;

    private final SquareLayout m_squareLayout;

    private void cbFieldClicked(GoPoint point, boolean modifiedSelect)
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (m_board.getColor(point) != GoColor.EMPTY)
            return;
        synchronized (this)
        {
            if (modifiedSelect)
                m_fieldClicked = null;
            else
                m_fieldClicked = point;
            notifyAll();
        }
    }

    private void clearStatus()
    {
        showStatus(" ");
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

    private void cmdBoardsize(String cmdArray[], StringBuffer response)
        throws GtpError
    {
        int size = parseIntegerArgument(cmdArray);
        if (size < 1)
            throw new GtpError("Invalid board size");
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
                    m_squareLayout.setPreferMultipleOf(m_size + 2);
                    m_frame.pack();
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(null);
                }
            });
    }

    private void cmdClearBoard(StringBuffer response) throws GtpError
    {
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
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(null);
                }
            });
    }

    private void cmdGenmove(String cmdArray[], StringBuffer response)
        throws GtpError
    {
        GoColor color = parseColorArgument(cmdArray);
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
            synchronized (this)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    System.err.println("InterruptedException");
                }
                point = m_fieldClicked;
            }
            response.append(GoPoint.toString(point));
        }
        else
        {
            String command = m_gtp.getCommandGenmove(color);
            send(command, response);
            point = GtpUtils.parsePoint(response.toString(), m_size);
        }
        m_move = Move.create(point, color);
        invokeAndWait(new Runnable()
            {
                public void run()
                {                    
                    m_board.play(m_move);
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(m_move.getPoint());
                    clearStatus();
                }
            });
    }

    private void cmdListCommands(StringBuffer response) throws GtpError
    {
        if (m_gtp != null)
        {
            Vector commands = m_gtp.getSupportedCommands();
            for (int i = 0; i < commands.size(); ++i)
            {
                String cmd = (String)commands.get(i);
                if (cmd.equals("boardsize")
                    || cmd.equals("black")
                    || cmd.equals("clear_board")
                    || cmd.equals("genmove")
                    || cmd.equals("genmove_black")
                    || cmd.equals("genmove_white")
                    || cmd.equals("help")
                    || cmd.equals("list_commands")
                    || cmd.equals("play")
                    || cmd.equals("protocol_version")
                    || cmd.equals("quit")
                    || cmd.equals("white"))
                    continue;
                response.append(cmd);
                response.append("\n");
            }
        }
        response.append("boardsize\n");
        response.append("clear_board\n");        
        response.append("genmove\n");
        response.append("list_commands\n");
        response.append("play\n");
        response.append("protocol_version\n");
        response.append("quit\n");
    }

    private void cmdName(StringBuffer response)
    {
        if (m_gtp == null)
            response.append("GtpDisplay");
        else            
            response.append(m_name);
    }

    private void cmdPlaceFreeHandicap(String cmdArray[],
                                      StringBuffer response) throws GtpError
    {
        int n = parseIntegerArgument(cmdArray);
        Vector stones = Board.getHandicapStones(m_size, n);
        if  (stones == null)
            throw new GtpError("Invalid number of handicap stones");
        StringBuffer pointList = new StringBuffer(128);
        for (int i = 0; i < stones.size(); ++i)
        {
            GoPoint point = (GoPoint)stones.get(i);
            play(GoColor.BLACK, point);
            if (pointList.length() > 0)
                pointList.append(" ");
            pointList.append(point);
        }
        response.append(pointList);
    }

    private void cmdPlay(String cmdArray[]) throws GtpError
    {
        ColorPointArgument argument
            = parseColorPointArgument(cmdArray, m_size);
        play(argument.m_color, argument.m_point);
    }

    private void cmdPlay(GoColor color, String cmdArray[]) throws GtpError
    {
        GoPoint point = parsePointArgument(cmdArray, m_size);
        play(color, point);
    }

    private void cmdQuit(StringBuffer response) throws GtpError
    {
        if (m_gtp != null)
            send("quit", response);
    }

    private void cmdSetFreeHandicap(String cmdArray[]) throws GtpError
    {
        for (int i = 1; i < cmdArray.length; ++i)
        {
            GoPoint point = GtpUtils.parsePoint(cmdArray[i], m_size);
            play(GoColor.BLACK, point);
        }
    }

    private void cmdUndo(String cmdArray[]) throws GtpError
    {
        if (cmdArray.length != 1)
            throw new GtpError("No arguments allowed");
        undo();
    }

    private void cmdUnknown() throws GtpError
    {
        throw new GtpError("Unknown command");
    }

    private void cmdVersion()
    {
    }

    private JComponent createStatusBar()
    {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(1, 0));
        outerPanel.add(panel, BorderLayout.CENTER);
        // Workaround for Java 1.4.1 on Mac OS X: add some empty space
        // so that status bar does not overlap the window resize widget
        if (Platform.isMac())
        {
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            outerPanel.add(filler, BorderLayout.EAST);
        }
        JLabel label = new JLabel(" ");
        label.setBorder(BorderFactory.createLoweredBevelBorder());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        m_statusLabel = label;
        return outerPanel;
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
            String command = m_gtp.getCommandPlay(Move.create(point, color));
            send(command);
        }
        m_move = Move.create(point, color);
        invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.play(m_move);
                    m_guiBoard.markLastMove(m_move.getPoint());
                    m_guiBoard.updateFromGoBoard();
                }
            });
    }

    private void send(String cmd, StringBuffer response) throws GtpError
    {
        response.append(m_gtp.sendCommand(cmd));
    }

    private void send(String cmd) throws GtpError
    {
        m_gtp.sendCommand(cmd);
    }

    private void showStatus(String text)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_statusLabel.setText(text);
        m_statusLabel.repaint();
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
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(null);
                }
            });
    }
}

//----------------------------------------------------------------------------
