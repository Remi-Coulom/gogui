//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gmptogtp;

import java.io.InputStream;
import java.io.OutputStream;
import net.sf.gogui.gmp.Gmp;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

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

    public void handleCommand(String cmdLine, StringBuffer response)
        throws GtpError
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        if (cmd.equals("boardsize"))
            cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board"))
            cmdClearBoard(response);
        else if (cmd.equals("genmove"))
            cmdGenmove(cmdArray, response);
        else if (cmd.equals("gmp_queue"))
            cmdQueue(response);
        else if (cmd.equals("gmp_talk"))
            cmdTalk(cmdLine, response);
        else if (cmd.equals("gogui_interrupt"))
            ;
        else if (cmd.equals("gogui_title"))
            response.append(m_title);
        else if (cmd.equals("list_commands"))
            response.append("boardsize\n" +
                            "clear_board\n" +
                            "genmove\n" +
                            "gmp_talk\n" +
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
            cmdPlay(cmdArray, response);
        else if (cmd.equals("protocol_version"))
            response.append("2");
        else if (cmd.equals("quit"))
            ;
        else if (cmd.equals("undo"))
            cmdUndo(response);
        else if (cmd.equals("version"))
            response.append(Version.get());
        else
            throw new GtpError("unknown command");
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

    private void cmdBoardsize(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        if (parseIntegerArgument(cmdArray) != m_size)
            throw new GtpError("size must be " + m_size);
    }

    private void cmdClearBoard(StringBuffer response) throws GtpError
    {
        if (! (m_wait && m_firstGame) && ! (m_simple && m_colorIndex == 1))
        {
            if (! m_gmp.newGame(m_size, response))
                throw new GtpError(response.toString());
            return;
        }
        m_firstGame = false;
        if (! m_gmp.waitNewGame(m_size, response))
            throw new GtpError(response.toString());
    }

    private void cmdGenmove(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        boolean isBlack = (parseColorArgument(cmdArray) == GoColor.BLACK);
        Gmp.Move move = m_gmp.waitMove(isBlack, response);
        if (move == null)
            throw new GtpError(response.toString());
        if (move.m_x < 0)
        {
            response.append("PASS");
            return;
        }
        int x = 'A' + move.m_x;
        if (x >= 'I')
            ++x;
        response.append((char)(x));
        response.append(move.m_y + 1);
    }

    private void cmdPlay(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        ColorPointArgument argument
            = parseColorPointArgument(cmdArray, m_size);
        int x = -1;
        int y = -1;
        GoPoint point = argument.m_point;
        if (point != null)
        {
            x = point.getX();
            y = point.getY();
            
        }
        if (! m_gmp.play(argument.m_color == GoColor.BLACK, x, y, response))
            throw new GtpError(response.toString());
    }

    private void cmdQueue(StringBuffer response) throws GtpError
    {
        if (! m_gmp.queue(response))
            throw new GtpError(response.toString());
    }

    private void cmdTalk(String command, StringBuffer response)
        throws GtpError
    {
        int index = command.indexOf(' ');
        if (index > 0 && ! command.substring(index + 1).trim().equals(""))
        {
            if (! m_gmp.sendTalk(command.substring(index + 1)))
                throw new GtpError("Write error");
        }
        else
            response.append(m_gmp.getTalk());
    }

    private void cmdUndo(StringBuffer response) throws GtpError
    {
        if (! m_gmp.undo(response))
            throw new GtpError(response.toString());
    }
}

//----------------------------------------------------------------------------
