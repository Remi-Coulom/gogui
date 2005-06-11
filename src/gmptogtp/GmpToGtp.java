//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gmptogtp;

import java.io.InputStream;
import java.io.OutputStream;
import gmp.Gmp;
import go.Color;
import go.Point;
import gtp.GtpEngine;
import utils.StringUtils;
import version.Version;

//----------------------------------------------------------------------------

/** GTP to GMP adapter. */
public class GmpToGtp
    extends GtpEngine
{
    public GmpToGtp(String title, InputStream in, OutputStream out,
                    boolean verbose, int size, int colorIndex, boolean wait,
                    boolean simple)
    {
        super(System.in, System.out, null);
        m_simple = simple;
        m_colorIndex = colorIndex;
        m_wait = wait;
        m_gmp = new Gmp(in, out, size, colorIndex, simple, verbose);
        m_title = title;
        m_size = size;
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (cmd.equals("boardsize"))
            return cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board"))
            return cmdClearBoard(response);
        else if (cmd.equals("genmove"))
            return cmdGenmove(cmdArray, response);
        else if (cmd.equals("gmp_queue"))
            return queue(response);
        else if (cmd.equals("gmp_text"))
            return sendTalk(cmdLine, response);
        else if (cmd.equals("gogui_interrupt"))
            ;
        else if (cmd.equals("gogui_title"))
            response.append(m_title);
        else if (cmd.equals("list_commands"))
            response.append("boardsize\n" +
                            "clear_board\n" +
                            "genmove\n" +
                            "gmp_text\n" +
                            "gmp_queue\n" +
                            "gogui_interrupt\n" +
                            "gogui_title\n" +
                            "list_commands\n" +
                            "name\n" +
                            "play\n" +
                            "undo\n" +
                            "version\n" +
                            "quit\n");
        else if (cmd.equals("name"))
            response.append("GmpToGtp");
        else if (cmd.equals("play"))
            return cmdPlay(cmdArray, response);
        else if (cmd.equals("protocol_version"))
            response.append("2");
        else if (cmd.equals("quit"))
            return true;
        else if (cmd.equals("undo"))
            return undo(response);
        else if (cmd.equals("version"))
            response.append(Version.get());
        else
        {
            response.append("unknown command");
            status = false;
        }
        return status;
    }

    public void interruptCommand()
    {
        m_gmp.interruptCommand();
    }

    private boolean m_firstGame = true;

    private final boolean m_simple;

    private final boolean m_wait;

    private final int m_colorIndex;

    private final int m_size;

    private final String m_title;

    private final Gmp m_gmp;

    private boolean cmdBoardsize(String[] cmdArray, StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (argument.m_integer != m_size)
        {
            response.append("size must be " + m_size);
            return false;
        }
        return true;
    }

    private boolean cmdClearBoard(StringBuffer response)
    {
        if (! (m_wait && m_firstGame) && ! (m_simple && m_colorIndex == 1))
            return m_gmp.newGame(m_size, response);
        m_firstGame = false;
        return m_gmp.waitNewGame(m_size, response);
    }

    private boolean cmdGenmove(String[] cmdArray, StringBuffer response)
    {
        ColorArgument argument = parseColorArgument(cmdArray, response);
        if (argument == null)
            return false;
        boolean isBlack = (argument.m_color == Color.BLACK);
        Gmp.Move move = m_gmp.waitMove(isBlack, response);
        if (move == null)
            return false;
        if (move.m_x < 0)
        {
            response.append("PASS");
            return true;
        }
        int x = 'A' + move.m_x;
        if (x >= 'I')
            ++x;
        response.append((char)(x));
        response.append(move.m_y + 1);
        return true;
    }

    private boolean cmdPlay(String[] cmdArray, StringBuffer response)
    {
        ColorPointArgument argument =
            parseColorPointArgument(cmdArray, response, m_size);
        if (argument == null)
            return false;
        int x = -1;
        int y = -1;
        Point point = argument.m_point;
        if (point != null)
        {
            x = point.getX();
            y = point.getY();
            
        }
        return m_gmp.play(argument.m_color == Color.BLACK, x, y, response);
    }

    private boolean queue(StringBuffer response)
    {
        return m_gmp.queue(response);
    }

    private boolean sendTalk(String command, StringBuffer response)
    {
        int index = command.indexOf(' ');
        if (index > 0)
        {
            if (! m_gmp.sendTalk(command.substring(index + 1)))
            {
                response.append("Write error");
                return false;
            }                
        }
        return true;
    }

    private boolean undo(StringBuffer response)
    {
        return m_gmp.undo(response);
    }
}

//----------------------------------------------------------------------------
