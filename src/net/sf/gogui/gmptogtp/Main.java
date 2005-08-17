//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gmptogtp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.ProcessUtils;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GmpToGtp main function. */
public class Main
{
    /** GmpToGtp main function. */
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
            Options opt = Options.parse(args, options);
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
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GmpToGtp " + Version.get());
                return;
            }
            if (opt.isSet("list"))
            {
                listDevices();
                return;
            }
            String color = opt.getString("color", "");
            if (! color.equals(""))
            {
                color = color.toLowerCase();
                if (! color.equals("black") && ! color.equals("white"))
                    throw new Exception("invalid color");
            }
            String device = opt.getString("device", "");
            int size = opt.getInteger("size", GoPoint.DEFAULT_SIZE);
            if (size < 1 || size > 22)
                throw new Exception("invalid size");
            int baud = opt.getInteger("baud", 2400);
            if (baud <= 0)
                throw new Exception("invalid baud value");
            boolean verbose = opt.isSet("verbose");
            boolean simple = opt.isSet("simple");
            boolean wait = opt.isSet("wait");
            String program = null;
            ArrayList arguments = opt.getArguments();
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
            if (device.equals(""))
            {
                Runtime runtime = Runtime.getRuntime();
                process = runtime.exec(StringUtils.splitArguments(program));
                Thread stdErrThread = new ProcessUtils.StdErrThread(process);
                stdErrThread.start();
                title = title + program;
                in = process.getInputStream();
                out = process.getOutputStream();
            }
            else
            {
                port = openPort(device, baud, flow);
                title = title + device;
                in = port.getInputStream();
                out = port.getOutputStream();
            }
            int colorIndex = 0;
            if (! color.equals(""))
            {
                if (color.equals("black"))
                    colorIndex =  1;
                else if (color.equals("white"))
                    colorIndex =  2;
            }
            GmpToGtp gmpToGtp = new GmpToGtp(title, in, out, verbose, size,
                                             colorIndex, wait, simple);
            gmpToGtp.mainLoop(System.in, System.out);
        }
        catch (Throwable t)
        {
            StringUtils.printException(t);
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
                StringUtils.printException(e);
            }
            if (process != null)
            {
                process.destroy();
                try
                {
                    process.waitFor();
                }
                catch (InterruptedException e)
                {
                    System.err.println("Interrupted");
                }
            }
            if (port != null)
                port.close();
        }
        System.exit(exitStatus);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
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
}

//----------------------------------------------------------------------------
