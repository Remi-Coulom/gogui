//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gmptogtp;

import java.io.*;
import java.util.*;
import javax.comm.*;
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
                "config:",
                "device:",
                "flow:",
                "help",
                "list",
                "simple",
                "size:",
                "verbose",
                "wait",
                "version"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar gmptogtp.jar [options]\n" +
                    "\n" +
                    "-baud    speed of serial device (default 2400)\n" +
                    "-color   color (black|white)\n" +
                    "-config  config file\n" +
                    "-device  serial device file\n" +
                    "-flow    flow control (none|rtscts(default)|xonxoff)\n" +
                    "-help    display this help and exit\n" +
                    "-list    list serial devices and exit\n" +
                    "-simple  use simple version of the protocol\n" +
                    "-size    board size\n" +
                    "-verbose print logging messages\n" +
                    "-version print version and exit\n" +
                    "-wait    wait for first newgame command\n";
                System.out.print(helpText);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GmpToGtp " + Version.get());
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
            boolean simple = opt.isSet("simple");
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
            String title = "Go Modem ";
            String flow = opt.getString("flow", "rtscts");                
            if (! device.equals(""))
            {
                port = openPort(device, baud, flow);
                title = title + device;
                in = port.getInputStream();
                out = port.getOutputStream();
            }
            else
            {
                Runtime runtime = Runtime.getRuntime();
                process = runtime.exec(StringUtils.tokenize(program));
                Thread stdErrThread = new ProcessUtils.StdErrThread(process);
                stdErrThread.start();
                title = title + program;
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
            GmpToGtp gmpToGtp = new GmpToGtp(title, in, out, verbose, size,
                                             colorIndex, wait, simple);
            gmpToGtp.mainLoop();
        }
        catch (AssertionError e)
        {
            e.printStackTrace();
            exitStatus = -1;
        }
        catch (Throwable t)
        {
            System.err.println(StringUtils.formatException(t));
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
            catch (IOException e)
            {
                System.err.println(StringUtils.formatException(e));
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

    private static SerialPort openPort(String device, int baud, String flow)
        throws Error, NoSuchPortException, PortInUseException,
               UnsupportedCommOperationException
    {
        CommPortIdentifier portId =
            CommPortIdentifier.getPortIdentifier(device);
        SerialPort port = (SerialPort)portId.open("GmpToGtp", 5000);
        port.setSerialPortParams(baud, SerialPort.DATABITS_8,
                                 SerialPort.STOPBITS_1,
                                 SerialPort.PARITY_NONE);
        if (flow.equals("rtscts"))
            port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                                    SerialPort.FLOWCONTROL_RTSCTS_OUT);
        else if (flow.equals("xonxoff"))
            port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN |
                                    SerialPort.FLOWCONTROL_XONXOFF_OUT);
        else if (flow.equals("none"))
            port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        else
            throw new Error("Unknown flow control mode \"" + flow + "\"");
        return port;
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
