//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gmp;

import java.io.*;
import java.util.*;
import utils.*;
import gtp.GtpServer;

//-----------------------------------------------------------------------------

public class GmpToGtp
    extends GtpServer
{
    public GmpToGtp(InputStream in, OutputStream out, boolean verbose,
                    int size, int colorIndex)
    {
        super(System.in, System.out);
        m_verbose = verbose;
        m_gmp = new Gmp(in, out, size, colorIndex);
    }

    public boolean handleCommand(String command, StringBuffer response)
    {
        boolean status = true;
        if (command.equals("quit"))
            setQuit();
        else if (command.startsWith("black"))
            return play(true, command, response);
        else if (command.startsWith("gmp_text"))
            return sendText(command, response);
        else if (command.startsWith("white"))
            return play(true, command, response);
        else if (command.startsWith("undo"))
            ;
        else if (command.startsWith("genmove_black"))
            return genmove(true, response);
        else if (command.startsWith("genmove_white"))
            return genmove(false, response);
        else if (command.startsWith("boardsize"))
            return boardsize(command, response);
        else if (command.equals("name"))
            response.append("GmpToGtp");
        else if (command.equals("version"))
            ;
        else if (command.equals("protocol_version"))
            response.append("1");
        else if (command.equals("help"))
            response.append("boardsize\n" +
                            "black\n" +
                            "genmove_black\n" +
                            "genmove_white\n" +
                            "genmove_white\n" +
                            "gmp_text\n" +
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

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "color:",
                "device:",
                "help",
                "size:",
                "verbose"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -cp gogui.jar gmp.GmpToGtp [options]\n" +
                    "\n" +
                    "-color   color (black|white)\n" +
                    "-device  serial device file\n" +
                    "-help    display this help and exit\n" +
                    "-size    board size\n" +
                    "-verbose print logging messages";
                System.out.print(helpText);
                System.exit(0);
            }
            String color = opt.getString("color", "");
            if (! color.equals(""))
            {
                color = color.toLowerCase();
                if (! color.equals("black") && ! color.equals("white"))
                    throw new Exception("invalid color");
            }
            String device = opt.getString("device", "");
            int size = opt.getInteger("size", 19);
            if (size < 1 || size > 22)
                throw new Exception("invalid size");
            boolean verbose = opt.isSet("verbose");
            String program = null;
            Vector arguments = opt.getArguments();
            if (arguments.size() == 1)
                program = (String)arguments.get(0);
            else if (arguments.size() > 1)
            {
                System.err.println("Only one program argument allowed.");
                System.exit(-1);
            }
            else if (device.equals(""))
            {
                System.err.println("Missing program argument.");
                System.exit(-1);
            }
            InputStream in;
            OutputStream out;
            Process process = null;
            if (! device.equals(""))
            {
                File file = new File(device);
                in = new FileInputStream(file);
                out = new FileOutputStream(((FileInputStream)in).getFD());
            }
            else
            {
                Runtime runtime = Runtime.getRuntime();
                process = runtime.exec(StringUtils.getCmdArray(program));
                Thread stdErrThread = new StdErrThread(process);
                stdErrThread.start();
                in = process.getInputStream();
                out = process.getOutputStream();
            }
            int colorIndex = 0;
            if (color != null)
            {
                if (color.equals("black"))
                    colorIndex =  1;
                else if (color.equals("white"))
                    colorIndex =  2;
            }
            GmpToGtp gmpToGtp = new GmpToGtp(in, out, verbose, size,
                                             colorIndex);
            gmpToGtp.mainLoop();
            if (process != null)
            {
                process.destroy();
                process.waitFor();
            }
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean m_verbose;

    private Gmp m_gmp;

    private boolean boardsize(String command, StringBuffer response)
    {
        String[] tokens = StringUtils.split(command, ' ');
        if (tokens.length < 2)
        {
            response.append("Missing argument");
            return false;
        }
        try
        {
            int arg = Integer.parseInt(tokens[1]);
            return m_gmp.newGame(arg, response);
        }
        catch (NumberFormatException e)
        {
            response.append("Needs integer argument");
            return false;
        }
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
        response.append((char)('A' + move.m_x));
        response.append(move.m_y + 1);
        return true;
    }

    private boolean play(boolean isBlack, String command,
                         StringBuffer response)
    {
        String[] tokens = StringUtils.split(command, ' ');
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
                x = (arg.charAt(0) - 'A');
                y = Integer.parseInt(arg.substring(1));
                
            }
            return m_gmp.play(isBlack, x, y, response);
        }
        catch (NumberFormatException e)
        {
            response.append("Needs integer argument");
            return false;
        }
    }

    private boolean sendText(String command, StringBuffer response)
    {
        int index = command.indexOf(' ');
        if (index > 0)
            if (! m_gmp.sendText(command.substring(index + 1)))
            {
                response.append("I/O error");
                return false;
            }
        return true;
    }
}

class StdErrThread
    extends Thread
{
    public StdErrThread(Process process)
    {
        m_err = new InputStreamReader(process.getErrorStream());
    }
    
    public void run()
    {
        try
        {
            char buf[] = new char[32768];
            while (true)
            {
                int len = m_err.read(buf);
                if (len < 0)
                    return;
                if (len > 0)
                {
                    char c[] = new char[len];
                    for (int i = 0; i < len; ++i)
                        c[i] = buf[i];
                }
                sleep(100);
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            System.err.println(msg);
        }
    }
    
    private Reader m_err;
}

//-----------------------------------------------------------------------------
