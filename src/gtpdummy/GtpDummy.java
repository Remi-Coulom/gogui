//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtpdummy;

import java.io.*;
import go.*;
import gtp.*;
import utils.Options;
import utils.StringUtils;
import version.*;

//----------------------------------------------------------------------------

/** Dummy Go program for testing GTP controlling programs. */
public class GtpDummy
    extends GtpServer
{
    public GtpDummy(InputStream in, OutputStream out, PrintStream log)
        throws Exception
    {
        super(in, out, log);
        initSize(19);
        m_thread = Thread.currentThread();
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (m_nextResponseFixed)
        {
            status = m_nextStatus;
            response.append(m_nextResponse);
            m_nextResponseFixed = false;
        }
        else if (cmd.equals("black"))
            status = play(cmdArray, response);
        else if (cmd.equals("dummy_bwboard"))
            bwBoard(response);
        else if (cmd.equals("dummy_crash"))
            crash();
        else if (cmd.equals("dummy_next_failure"))
            nextResponseFixed(cmd, cmdLine, false);
        else if (cmd.equals("dummy_next_success"))
            nextResponseFixed(cmd, cmdLine, true);
        else if (cmd.equals("echo"))
            echo(cmdLine, response);
        else if (cmd.equals("echo_err"))
            echoErr(cmdLine);
        else if (cmd.equals("genmove_black"))
            status = genmove(response);
        else if (cmd.equals("boardsize"))
            status = boardsize(cmdArray, response);
        else if (cmd.equals("gogui_interrupt"))
            ;
        else if (cmd.equals("genmove_white"))
            status = genmove(response);
        else if (cmd.equals("name"))
            response.append("GtpDummy");
        else if (cmd.equals("protocol_version"))
            response.append("1");
        else if (cmd.equals("dummy_sleep"))
            status = sleep(cmdArray, response);
        else if (cmd.equals("quit"))
            ;
        else if (cmd.equals("version"))
            response.append(Version.get());
        else if (cmd.equals("white"))
            status = play(cmdArray, response);
        else if (cmd.equals("help"))
            response.append("black\n" +
                            "dummy_bwboard\n" +
                            "dummy_crash\n" +
                            "dummy_next_success\n" +
                            "dummy_next_failure\n" +
                            "dummy_sleep\n" +
                            "echo\n" +
                            "echo_err\n" +
                            "genmove_black\n" +
                            "genmove_white\n" +
                            "gogui_interrupt\n" +
                            "help\n" +
                            "white\n" +
                            "name\n" +
                            "protocol_version\n" +
                            "quit\n" +
                            "version\n" +
                            "white\n");
        else
        {
            response.append("unknown command");
            status = false;
        }
        return status;
    }

    public void interruptCommand()
    {
        m_thread.interrupt();
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "help",
                "log:",
                "version"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar gtpdummy.jar [options]\n" +
                    "\n" +
                    "-config       config file\n" +
                    "-help         display this help and exit\n" +
                    "-log file     log GTP stream to file\n" +
                    "-version      print version and exit\n";
                System.out.print(helpText);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpDummy " + Version.get());
                System.exit(0);
            }
            PrintStream log = null;
            if (opt.isSet("log"))
            {
                File file = new File(opt.getString("log"));
                log = new PrintStream(new FileOutputStream(file));
            }
            GtpDummy gtpDummy = new GtpDummy(System.in, System.out, log);
            gtpDummy.mainLoop();
            if (log != null)
                log.close();
        }
        catch (Error e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            System.err.println(StringUtils.formatException(t));
            System.exit(-1);
        }
    }
    
    private boolean m_nextResponseFixed;

    private boolean m_nextStatus;

    private int m_size;

    private boolean[][] m_alreadyPlayed;

    private String m_nextResponse;

    private Thread m_thread;

    private boolean boardsize(String[] cmdArray, StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (argument.m_integer < 1 || argument.m_integer > 1000)
        {
            response.append("Invalid size");
            return false;
        }
        initSize(argument.m_integer);
        return true;
    }

    private void bwBoard(StringBuffer response)
    {        
        response.append("\n");
        for (int x = 0; x < m_size; ++x)
        {
            for (int y = 0; y < m_size; ++y)
                response.append(Math.random() > 0.5 ? "B " : "W ");
            response.append("\n");
        }                    
    }

    private void crash()
    {        
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

    private boolean genmove(StringBuffer response)
    {
        int numberPossibleMoves = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                if (! m_alreadyPlayed[x][y])
                    ++numberPossibleMoves;
        Point point = null;
        if (numberPossibleMoves > 0)
        {
            int rand = (int)(Math.random() * numberPossibleMoves);
            int index = 0;
            for (int x = 0; x < m_size && point == null; ++x)
                for (int y = 0; y < m_size && point == null; ++y)
                    if (! m_alreadyPlayed[x][y])
                    {
                        if (index == rand)
                            point = new Point(x, y);
                        ++index;
                    }
        }
        response.append(Point.toString(point));
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
        return true;
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

    private boolean play(String[] cmdArray, StringBuffer response)
    {
        if (cmdArray.length < 2)
        {
            response.append("Missing argument");
            return false;
        }
        Point point = null;
        try
        {
            point = Gtp.parsePoint(cmdArray[1], m_size);
        }
        catch (Gtp.Error e)
        {
            response.append(e.getMessage());
            return false;
        }
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
        return true;
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
