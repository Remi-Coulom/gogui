//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtpdisplay;

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
import go.Move;
import gtp.Gtp;
import gtp.GtpEngine;
import gtp.GtpError;
import gtp.GtpUtils;
import gui.GuiUtils;
import gui.SimpleDialogs;
import utils.Platform;
import utils.SquareLayout;
import utils.StringUtils;

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
        m_size = 19;
        m_board = new go.Board(m_size);
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
        if (m_gtp != null)
        {
            m_name = m_gtp.queryName();
            String title = "GtpDisplay: " + m_name;
            m_frame.setTitle(title);
        }
        else
            m_frame.setTitle("GtpDisplay");
        Container contentPane = m_frame.getContentPane();
        m_guiBoard = new gui.Board(m_board, fastPaint);
        if (m_gtp != null)
            m_guiBoard.setShowCursor(false);
        m_guiBoard.setListener(new gui.Board.Listener()
            {
                public void fieldClicked(go.Point point,
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
        if (! invokeAndWait(new Runnable()
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
            }, error))
            System.err.println(error);
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (cmd.equals("black"))
            status = cmdPlay(go.Color.BLACK, cmdArray, response);
        else if (cmd.equals("boardsize"))
            status = cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board"))
            status = cmdClearBoard(response);
        else if (cmd.equals("genmove"))
            status = cmdGenmove(cmdArray, response);
        else if (cmd.equals("genmove_black"))
            status = cmdUnknown(response);
        else if (cmd.equals("genmove_white"))
            status = cmdUnknown(response);
        else if (cmd.equals("help"))
            status = cmdListCommands(response);
        else if (cmd.equals("list_commands"))
            status = cmdListCommands(response);
        else if (cmd.equals("loadsgf"))
            status = cmdUnknown(response);
        else if (cmd.equals("name"))
            status = cmdName(response);
        else if (cmd.equals("place_free_handicap"))
            status = cmdPlaceFreeHandicap(cmdArray, response);
        else if (cmd.equals("play"))
            status = cmdPlay(cmdArray, response);
        else if (cmd.equals("protocol_version"))
            response.append("2");
        else if (cmd.equals("set_free_handicap"))
            status = cmdSetFreeHandicap(cmdArray, response);
        else if (cmd.equals("version"))
            status = cmdVersion(response);
        else if (cmd.equals("undo"))
            status = cmdUndo(cmdArray, response);
        else if (cmd.equals("white"))
            status = cmdPlay(go.Color.WHITE, cmdArray, response);
        else if (cmd.equals("quit"))
            status = cmdQuit(response);
        else
        {
            if (m_gtp == null)
            {
                response.append("unknown command");
                status = false;
            }
            else
                status = send(cmdLine, response);
        }
        return status;
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

    private go.Board m_board;

    private go.Color m_color;

    private gui.Board m_guiBoard;

    private go.Point m_fieldClicked;

    private Gtp m_gtp;

    private Move m_move;

    private JFrame m_frame;

    private JLabel m_statusLabel;

    private String m_name;

    private SquareLayout m_squareLayout;

    private void cbFieldClicked(go.Point point, boolean modifiedSelect)
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (m_board.getColor(point) != go.Color.EMPTY)
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

    private boolean cmdBoardsize(String cmdArray[], StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (argument.m_integer < 1)
        {
            response.append("Invalid board size");
            return false;
        }
        int size = argument.m_integer;
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandBoardsize(size);
            if (command != null)
            {
                if (! send(command, response))
                    return false;
            }
            command = m_gtp.getCommandClearBoard(size);
            if (! send(command, response))
                return false;
        }
        m_size = size;
        if (! invokeAndWait(new Runnable()
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
            }, response))
            return false;
        return true;
    }

    private boolean cmdClearBoard(StringBuffer response)
    {
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandClearBoard(m_size);
            if (! send(command, response))
                return false;
        }
        if (! invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.newGame();
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(null);
                }
            }, response))
            return false;
        return true;
    }

    private boolean cmdGenmove(String cmdArray[], StringBuffer response)
    {
        ColorArgument argument = parseColorArgument(cmdArray, response);
        if (argument == null)
            return false;
        go.Color color = argument.m_color;
        go.Point point;        
        if (m_gtp == null && m_frame != null)
        {
            m_color = color;
            if (! invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        showStatus("Input move for " + m_color
                                   + " (right click for pass)");
                    }
                }, response))
                return false;
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
            response.append(go.Point.toString(point));
        }
        else
        {
            String command = m_gtp.getCommandGenmove(color);
            if (! send(command, response))
                return false;
            try
            {
                point = GtpUtils.parsePoint(response.toString(), m_size);
            }
            catch (GtpError e)
            {
                response.append("Program played illegal move");
                return false;
            }
        }
        m_move = new Move(point, color);
        if (! invokeAndWait(new Runnable()
            {
                public void run()
                {                    
                    m_board.play(m_move);
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(m_move.getPoint());
                    clearStatus();
                }
            }, response))
            return false;
        return true;
    }

    private boolean cmdListCommands(StringBuffer response)
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
        return true;
    }

    private boolean cmdName(StringBuffer response)
    {
        if (m_gtp == null)
            response.append("GtpDisplay");
        else            
            response.append(m_name);
        return true;
    }

    private boolean cmdPlaceFreeHandicap(String cmdArray[],
                                         StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        Vector stones =
            go.Board.getHandicapStones(m_size, argument.m_integer);
        if  (stones == null)
        {
            response.append("Invalid number of handicap stones");
            return false;
        }
        StringBuffer pointList = new StringBuffer(128);
        for (int i = 0; i < stones.size(); ++i)
        {
            go.Point point = (go.Point)stones.get(i);
            if (! play(go.Color.BLACK, point, response))
                break;
            if (pointList.length() > 0)
                pointList.append(" ");
            pointList.append(point);
        }
        response.append(pointList);
        return true;
    }

    private boolean cmdPlay(String cmdArray[], StringBuffer response)
    {
        ColorPointArgument argument =
            parseColorPointArgument(cmdArray, response, m_size);
        if (argument == null)
            return false;
        return play(argument.m_color, argument.m_point, response);
    }

    private boolean cmdPlay(go.Color color, String cmdArray[],
                            StringBuffer response)
    {
        PointArgument argument =
            parsePointArgument(cmdArray, response, m_size);
        if (argument == null)
            return false;
        return play(color, argument.m_point, response);
    }

    private boolean cmdQuit(StringBuffer response)
    {
        if (m_gtp != null)
            return send("quit", response);
        return true;
    }

    private boolean cmdSetFreeHandicap(String cmdArray[],
                                       StringBuffer response)
    {
        for (int i = 1; i < cmdArray.length; ++i)
        {
            go.Point point;
            try
            {
                point = GtpUtils.parsePoint(cmdArray[i], m_size);
            }
            catch (GtpError e)
            {
                response.append("Invalid vertex");
                return false;
            }
            if (! play(go.Color.BLACK, point, response))
                return false;
        }
        return true;
    }

    private boolean cmdUndo(String cmdArray[], StringBuffer response)
    {
        if (cmdArray.length != 1)
        {
            response.append("No arguments allowed");
            return false;
        }
        return undo(response);
    }

    private boolean cmdUnknown(StringBuffer response)
    {
        response.append("Unknown command");
        return false;
    }

    private boolean cmdVersion(StringBuffer response)
    {
        return true;
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

    private boolean invokeAndWait(Runnable runnable, StringBuffer response)
    {
        try
        {
            SwingUtilities.invokeAndWait(runnable);
        }
        catch (InterruptedException e)
        {
            // Shouldn't happen
            response.append("InterruptedException");
            return false;
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            response.append("InvocationTargetException");
            return false;
        }
        return true;
    }

    private boolean play(go.Color color, go.Point point,
                         StringBuffer response)
    {
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandPlay(color) + " ";
            if (point == null)
                command = command + "PASS";
            else
                command = command + point;
            if (! send(command, response))
                return false;
        }
        m_move = new Move(point, color);
        if (! invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.play(m_move);
                    m_guiBoard.markLastMove(m_move.getPoint());
                    m_guiBoard.updateFromGoBoard();
                }
            }, response))
            return false;
        return true;
    }

    private boolean send(String cmd, StringBuffer response)
    {
        try
        {
            response.append(m_gtp.sendCommand(cmd));
            return true;
        }
        catch (GtpError error)
        {
            response.append(error.getMessage());
            return false;
        }
    }

    private void showStatus(String text)
    {
        assert(SwingUtilities.isEventDispatchThread());
        m_statusLabel.setText(text);
        m_statusLabel.repaint();
    }

    private boolean undo(StringBuffer response)
    {
        if (m_gtp != null)
        {
            if (! send("undo", response))
                return false;
        }
        if (! invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_board.undo();
                    m_guiBoard.updateFromGoBoard();
                    m_guiBoard.markLastMove(null);
                }
            }, response))
            return false;
        return true;
    }
}

//----------------------------------------------------------------------------
