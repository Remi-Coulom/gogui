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
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
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
        super(null);
        m_simple = simple;
        m_colorIndex = colorIndex;
        m_wait = wait;
        m_gmp = new Gmp(in, out, size, colorIndex, simple, verbose);
        m_title = title;
        m_size = size;
    }

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        if (cmd.getCommand().equals("boardsize"))
            cmdBoardsize(cmd);
        else if (cmd.getCommand().equals("clear_board"))
            cmdClearBoard(cmd);
        else if (cmd.getCommand().equals("genmove"))
            cmdGenmove(cmd);
        else if (cmd.getCommand().equals("gmp_queue"))
            cmdQueue(cmd);
        else if (cmd.getCommand().equals("gmp_talk"))
            cmdTalk(cmd);
        else if (cmd.getCommand().equals("gogui_interrupt"))
            ;
        else if (cmd.getCommand().equals("gogui_title"))
            cmd.getResponse().append(m_title);
        else if (cmd.getCommand().equals("list_commands"))
            cmd.getResponse().append("boardsize\n" +
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
        else if (cmd.getCommand().equals("name"))
            cmd.getResponse().append("GmpToGtp");
        else if (cmd.getCommand().equals("play"))
            cmdPlay(cmd);
        else if (cmd.getCommand().equals("protocol_version"))
            cmd.getResponse().append("2");
        else if (cmd.getCommand().equals("quit"))
            ;
        else if (cmd.getCommand().equals("undo"))
            cmdUndo(cmd);
        else if (cmd.getCommand().equals("version"))
            cmd.getResponse().append(Version.get());
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

    private void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        int size = cmd.getIntArg();
        if (size != m_size)
            throw new GtpError("size must be " + m_size);
    }

    private void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        StringBuffer message = new StringBuffer();
        if (! (m_wait && m_firstGame) && ! (m_simple && m_colorIndex == 1))
        {
            if (! m_gmp.newGame(m_size, message))
                throw new GtpError(message.toString());
            return;
        }
        m_firstGame = false;
        if (! m_gmp.waitNewGame(m_size, message))
            throw new GtpError(message.toString());
    }

    private void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        StringBuffer message = new StringBuffer();
        boolean isBlack = (cmd.getColorArg() == GoColor.BLACK);
        Gmp.Move move = m_gmp.waitMove(isBlack, message);
        if (move == null)
            throw new GtpError(message.toString());
        if (move.m_x < 0)
        {
            cmd.getResponse().append("PASS");
            return;
        }
        int x = 'A' + move.m_x;
        if (x >= 'I')
            ++x;
        cmd.getResponse().append((char)(x));
        cmd.getResponse().append(move.m_y + 1);
    }

    private void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_size);
        int x = -1;
        int y = -1;
        if (point != null)
        {
            x = point.getX();
            y = point.getY();            
        }
        StringBuffer message = new StringBuffer();
        if (! m_gmp.play(color == GoColor.BLACK, x, y, message))
            throw new GtpError(message.toString());
    }

    private void cmdQueue(GtpCommand cmd) throws GtpError
    {
        StringBuffer message = new StringBuffer();
        if (! m_gmp.queue(message))
            throw new GtpError(message.toString());
    }

    private void cmdTalk(GtpCommand cmd) throws GtpError
    {
        String line = cmd.getLine();
        int index = line.indexOf(' ');
        if (index > 0 && ! line.substring(index + 1).trim().equals(""))
        {
            if (! m_gmp.sendTalk(line.substring(index + 1)))
                throw new GtpError("Write error");
        }
        else
            cmd.getResponse().append(m_gmp.getTalk());
    }

    private void cmdUndo(GtpCommand cmd) throws GtpError
    {
        StringBuffer message = new StringBuffer();
        if (! m_gmp.undo(message))
            throw new GtpError(message.toString());
    }
}

//----------------------------------------------------------------------------
