//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gmp;

import java.io.*;
import java.util.*;
import javax.comm.*;
import utils.*;
import gtp.GtpServer;

//-----------------------------------------------------------------------------

public class GmpToGtp
    extends GtpServer
{
    public GmpToGtp(InputStream in, OutputStream out, boolean verbose,
                    int size, int colorIndex, boolean wait)
    {
        super(System.in, System.out);
        m_verbose = verbose;
        m_wait = wait;
        m_gmp = new Gmp(in, out, size, colorIndex);
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
            return queue(command, response);
        else if (command.startsWith("white"))
            return play(false, command, response);
        else if (command.startsWith("undo"))
            return undo(command, response);
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
                            "gmp_queue\n" +
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
        int exitStatus = 0;
        Process process = null;
        SerialPort port = null;
        InputStream in = null;
        OutputStream out = null;
        try
        {
            String options[] = {
                "baud:",
                "color:",
                "device:",
                "help",
                "list",
                "size:",
                "verbose",
                "wait"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -cp gogui.jar gmp.GmpToGtp [options]\n" +
                    "\n" +
                    "-baud    speed of serial device (default 2400)\n" +
                    "-color   color (black|white)\n" +
                    "-device  serial device file\n" +
                    "-help    display this help and exit\n" +
                    "-list    list serial devices and exit\n" +
                    "-size    board size\n" +
                    "-verbose print logging messages" +
                    "-wait    wait for first newgame command\n";
                System.out.print(helpText);
                System.exit(0);
            }
            if (opt.isSet("list"))
            {
                listDevices();
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
            int baud = opt.getInteger("baud", 2400);
            if (baud <= 0)
                throw new Exception("invalid baud value");
            boolean verbose = opt.isSet("verbose");
            boolean wait = opt.isSet("wait");
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
            if (! device.equals(""))
            {
                CommPortIdentifier portId =
                    CommPortIdentifier.getPortIdentifier(device);
                port = (SerialPort)portId.open("GmpToGtp", 5000);
                port.setSerialPortParams(baud, SerialPort.DATABITS_8,
                                         SerialPort.STOPBITS_1,
                                         SerialPort.PARITY_NONE);
                in = port.getInputStream();
                out = port.getOutputStream();
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
                                             colorIndex, wait);
            gmpToGtp.mainLoop();
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            t.printStackTrace();
            exitStatus = -1;
        }
        finally
        {
            try
            {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            }
            catch (IOException ignore)
            {
            }
            if (process != null)
            {
                process.destroy();
                try
                {
                    process.waitFor();
                }
                catch(InterruptedException e)
                {
                    System.err.println("Interrupted");
                }
            }
            if (port != null)
                port.close();
        }
        System.exit(exitStatus);
    }

    private boolean m_firstGame = true;

    private boolean m_verbose;

    private boolean m_wait;

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
            if (! m_firstGame || ! m_wait)
                return m_gmp.newGame(arg, response);
            m_firstGame = false;
            return m_gmp.waitNewGame(arg, response);
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
        int x = 'A' + move.m_x;
        if (x >= 'I')
            ++x;
        response.append((char)(x));
        response.append(move.m_y + 1);
        return true;
    }

    private static void listDevices()
    {
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements())
        {
            CommPortIdentifier portId =
                (CommPortIdentifier)portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL)
                System.out.println(portId.getName());
        }
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
            response.append("Needs integer argument");
            return false;
        }
    }

    private boolean queue(String command, StringBuffer response)
    {
        return m_gmp.queue(response);
    }

    private boolean sendTalk(String command, StringBuffer response)
    {
        int index = command.indexOf(' ');
        if (index > 0)
            m_gmp.sendTalk(command.substring(index + 1));
        return true;
    }

    private boolean undo(String command, StringBuffer response)
    {
        return m_gmp.undo(response);
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
