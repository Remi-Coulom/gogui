//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gmptogtp;

import java.io.*;
import java.util.*;
import gmp.*;
import gtp.GtpServer;
import utils.*;
import version.*;

//----------------------------------------------------------------------------

/** GTP to GMP adapter. */
public class GmpToGtp
    extends GtpServer
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
    }

    public boolean handleCommand(String command, StringBuffer response)
    {
        boolean status = true;
        if (command.equals("quit"))
            return true;
        else if (command.startsWith("black"))
            return play(true, command, response);
        else if (command.startsWith("gmp_text"))
            return sendTalk(command, response);
        else if (command.startsWith("gmp_queue"))
            return queue(response);
        else if (command.startsWith("gogui_title"))
            response.append(m_title);
        else if (command.startsWith("gogui_interrupt"))
            ;
        else if (command.startsWith("white"))
            return play(false, command, response);
        else if (command.startsWith("undo"))
            return undo(response);
        else if (command.startsWith("genmove_black"))
            return genmove(true, response);
        else if (command.startsWith("genmove_white"))
            return genmove(false, response);
        else if (command.startsWith("boardsize"))
            return boardsize(command, response);
        else if (command.equals("name"))
            response.append("GmpToGtp");
        else if (command.equals("version"))
            response.append(Version.get());
        else if (command.equals("protocol_version"))
            response.append("1");
        else if (command.equals("help"))
            response.append("boardsize\n" +
                            "black\n" +
                            "genmove_black\n" +
                            "genmove_white\n" +
                            "genmove_white\n" +
                            "gmp_text\n" +
                            "gmp_queue\n" +
                            "gogui_interrupt\n" +
                            "gogui_title\n" +
                            "name\n" +
                            "undo\n" +
                            "version\n" +
                            "white\n" +
                            "quit\n");
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

    private boolean m_simple;

    private boolean m_wait;

    private int m_colorIndex;

    private String m_title;

    private Gmp m_gmp;

    private boolean boardsize(String command, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(command);
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (! (m_wait && m_firstGame) && ! (m_simple && m_colorIndex == 1))
            return m_gmp.newGame(argument.m_integer, response);
        m_firstGame = false;
        return m_gmp.waitNewGame(argument.m_integer, response);
    }

    private boolean genmove(boolean isBlack, StringBuffer response)
    {
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

    private boolean play(boolean isBlack, String command,
                         StringBuffer response)
    {
        String[] tokens = StringUtils.tokenize(command);
        if (tokens.length < 2)
        {
            response.append("Missing argument");
            return false;
        }
        try
        {
            String arg = tokens[1].toUpperCase();
            if (arg.length() < 2)
            {
                response.append("Invalid argument");
                return false;
            }
            int x = -1;
            int y = -1;
            if (! arg.equals("PASS"))
            {
                char xChar = arg.charAt(0);
                if (xChar >= 'J')
                    --xChar;
                x = (xChar - 'A');
                y = Integer.parseInt(arg.substring(1)) - 1;
                
            }
            return m_gmp.play(isBlack, x, y, response);
        }
        catch (NumberFormatException e)
        {
            response.append("Invalid argument");
            return false;
        }
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
