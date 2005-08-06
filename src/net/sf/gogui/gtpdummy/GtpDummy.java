//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpdummy;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Vector;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** Dummy Go program for testing GTP controlling programs.
    See the GtpDummy documentation for information about the extension
    commands.
*/
public class GtpDummy
    extends GtpEngine
{
    public GtpDummy(InputStream in, OutputStream out, PrintStream log,
                    boolean useRandomSeed, long randomSeed)
        throws Exception
    {
        super(in, out, log);
        m_random = new Random();
        if (useRandomSeed)
            m_random.setSeed(randomSeed);
        initSize(19);
        m_thread = Thread.currentThread();
    }

    public void handleCommand(String cmdLine, StringBuffer response)
        throws GtpError
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        if (m_nextResponseFixed
            && ! (cmd.equals("dummy_next_failure")
                  || cmd.equals("dummy_next_success")))
        {
            m_nextResponseFixed = false;
            if (! m_nextStatus)
                throw new GtpError(m_nextResponse);
            response.append(m_nextResponse);
        }
        else if (cmd.equals("boardsize"))
            cmdBoardsize(cmdArray);
        else if (cmd.equals("clear_board"))
            cmdClearBoard();
        else if (cmd.equals("dummy_bwboard"))
            bwBoard(response);
        else if (cmd.equals("dummy_delay"))
            cmdDelay(cmdArray, response);
        else if (cmd.equals("dummy_eplist"))
            cmdEPList(cmdArray, response);
        else if (cmd.equals("dummy_gfx"))
            cmdGfx(cmdArray, response);
        else if (cmd.equals("dummy_invalid"))
            cmdInvalid();
        else if (cmd.equals("dummy_long_response"))
            cmdLongResponse(cmdArray, response);
        else if (cmd.equals("dummy_crash"))
            crash();
        else if (cmd.equals("dummy_next_failure"))
            nextResponseFixed(cmd, cmdLine, false);
        else if (cmd.equals("dummy_next_success"))
            nextResponseFixed(cmd, cmdLine, true);
        else if (cmd.equals("dummy_sleep"))
            sleep(cmdArray, response);
        else if (cmd.equals("echo"))
            echo(cmdLine, response);
        else if (cmd.equals("echo_err"))
            echoErr(cmdLine);
        else if (cmd.equals("genmove"))
            cmdGenmove(response);
        else if (cmd.equals("gogui_interrupt"))
            ;
        else if (cmd.equals("name"))
            response.append("GtpDummy");
        else if (cmd.equals("play"))
            cmdPlay(cmdArray, response);
        else if (cmd.equals("protocol_version"))
            response.append("2");
        else if (cmd.equals("list_commands"))
            response.append("boardsize\n" +
                            "clear_board\n" +
                            "dummy_bwboard\n" +
                            "dummy_crash\n" +
                            "dummy_delay\n" +
                            "dummy_eplist\n" +
                            "dummy_gfx\n" +
                            "dummy_invalid\n" +
                            "dummy_long_response\n" +
                            "dummy_next_success\n" +
                            "dummy_next_failure\n" +
                            "dummy_sleep\n" +
                            "echo\n" +
                            "echo_err\n" +
                            "genmove\n" +
                            "gogui_interrupt\n" +
                            "list_commands\n" +
                            "name\n" +
                            "play\n" +
                            "protocol_version\n" +
                            "quit\n" +
                            "version\n");
        else if (cmd.equals("version"))
            response.append(Version.get());
        else if (cmd.equals("quit"))
            ;
        else
            throw new GtpError("unknown command");
        if (m_delay > 0 && ! cmd.equals("dummy_delay"))
        {
            try
            {
                Thread.sleep(1000L * m_delay);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    public void interruptCommand()
    {
        m_thread.interrupt();
    }

    private boolean m_nextResponseFixed;

    private boolean m_nextStatus;

    /** Delay every command (seconds) */
    private int m_delay;

    private int m_size;

    private boolean[][] m_alreadyPlayed;

    private final Random m_random;

    private String m_nextResponse;

    private final Thread m_thread;

    /** Editable point list for dummy_eplist command. */
    private Vector m_ePList = new Vector();

    private void bwBoard(StringBuffer response)
    {        
        response.append('\n');
        for (int x = 0; x < m_size; ++x)
        {
            for (int y = 0; y < m_size; ++y)
            {
                response.append(m_random.nextBoolean() ? 'B' : 'W');
                if (y < m_size - 1)
                    response.append(' ');
            }
            response.append('\n');
        }                    
    }

    private void cmdBoardsize(String[] cmdArray) throws GtpError
    {
        int size = parseIntegerArgument(cmdArray);
        if (size < 1 || size > 1000)
            throw new GtpError("Invalid size");
        initSize(size);
    }

    private void cmdClearBoard() throws GtpError
    {
        initSize(m_size);
    }

    private void cmdDelay(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        int n;
        try
        {
            n = parseIntegerArgument(cmdArray);
        }
        catch (GtpError e)
        {
            response.delete(0, response.length());
            response.append(m_delay);
            return;
        }
        if (n < 0)
            throw new GtpError("Argument must be positive");
        m_delay = n;
    }
    
    private void cmdEPList(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        if (cmdArray.length == 2 && cmdArray[1].equals("show"))
        {
            response.append(GoPoint.toString(m_ePList));
            return;
        }
        m_ePList = parsePointListArgument(cmdArray, m_size);
    }

    private void cmdGfx(String[] cmdArray, StringBuffer response)
    {
        response.append("LABEL A4 test\n" +
                        "COLOR green A5 A7 B9\n" +
                        "COLOR #980098 B7 B8\n" +
                        "SQUARE B5 C9\n" +
                        "MARK A6 B6\n" +
                        "TRIANGLE A9\n" +
                        "WHITE A1\n" +
                        "BLACK B1\n" +
                        "CIRCLE c8\n");
    }

    private void cmdGenmove(StringBuffer response)
    {
        int numberPossibleMoves = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                if (! m_alreadyPlayed[x][y])
                    ++numberPossibleMoves;
        GoPoint point = null;
        if (numberPossibleMoves > 0)
        {
            int rand = m_random.nextInt(numberPossibleMoves);
            int index = 0;
            for (int x = 0; x < m_size && point == null; ++x)
                for (int y = 0; y < m_size && point == null; ++y)
                    if (! m_alreadyPlayed[x][y])
                    {
                        if (index == rand)
                            point = GoPoint.create(x, y);
                        ++index;
                    }
        }
        response.append(GoPoint.toString(point));
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
    }

    private void cmdInvalid()
    {        
        printInvalidResponse("This is an invalid GTP response.\n" +
                             "It does not start with a status character.\n");
    }

    private void cmdLongResponse(String[] cmdArray, StringBuffer response)
        throws GtpError
    {        
        int n = parseIntegerArgument(cmdArray);
        for (int i = 1; i <= n; ++i)
        {
            response.append(i);
            response.append("\n");
        }
    }

    private void cmdPlay(String[] cmdArray, StringBuffer response)
        throws GtpError
    {
        ColorPointArgument argument
            = parseColorPointArgument(cmdArray, m_size);
        GoPoint point = argument.m_point;
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
    }

    private void crash()
    {        
        System.err.println("Aborting GtpDummy");
        System.exit(-1);
    }

    private void echo(String cmdLine, StringBuffer response)
    {
        int index = cmdLine.indexOf(" ");
        if (index < 0)
            return;
        response.append(cmdLine.substring(index + 1));
    }

    private void echoErr(String cmdLine)
    {
        int index = cmdLine.indexOf(" ");
        if (index < 0)
            return;
        System.err.println(cmdLine.substring(index + 1));
    }

    private void initSize(int size)
    {
        m_alreadyPlayed = new boolean[size][size];
        m_size = size;
    }

    private void nextResponseFixed(String cmd, String cmdLine,
                                   boolean nextStatus)
    {
        m_nextResponseFixed = true;
        m_nextStatus = nextStatus;
        m_nextResponse = cmdLine.substring(cmd.length()).trim();
    }

    private boolean sleep(String[] cmdArray, StringBuffer response)
    {
        long millis = 20000;
        if (cmdArray.length > 1)
        {
            try
            {
                millis = (long)(Double.parseDouble(cmdArray[1]) * 1000.0);
            }
            catch (NumberFormatException e)
            {
                response.append("Invalid argument");
                return false;
            }
        }
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            response.append("Interrupted");
            return false;
        }
        return true;
    }
}

//----------------------------------------------------------------------------
