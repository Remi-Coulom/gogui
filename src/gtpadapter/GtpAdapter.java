//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gtpadapter;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import go.*;
import gtp.*;
import utils.*;
import version.*;

//-----------------------------------------------------------------------------

public class GtpAdapter
    extends GtpServer
{
    public GtpAdapter(InputStream in, OutputStream out, String program,
                      PrintStream log, boolean version2, int size, String name,
                      boolean noScore)
        throws Exception
    {
        super(in, out, log);
        if (program.equals(""))
            throw new Exception("No program set.");
        m_gtp = new Gtp(program, false, null);
        m_gtp.queryProtocolVersion();
        m_gtp.querySupportedCommands();
        if (version2 && m_gtp.getProtocolVersion() != 1)
            throw new Exception("Program is not GTP version 1.");
        m_version2 = version2;
        m_noScore = noScore;
        m_size = size;
        m_name = name;
    }

    public void close()
    {
        m_gtp.close();
        m_gtp.waitForExit();
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (cmd.equals("boardsize"))
            status = cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board") && m_version2)
            status = send("boardsize " + m_boardSize, response);
        else if (cmd.equals("final_score") && m_noScore)
            status = cmdUnknown(response);
        else if (cmd.equals("final_status_list") && m_noScore)
            status = cmdUnknown(response);
        else if (cmd.equals("genmove") && m_version2)
            status = translateColorCommand(cmdArray, "genmove_", response);
        else if (cmd.equals("help"))
            status = cmdListCommands(response);
        else if (cmd.equals("list_commands"))
            status = cmdListCommands(response);
        else if (cmd.equals("name") && m_name != null)
            status = cmdName(response);
        else if (cmd.equals("play") && m_version2)
            status = translateColorCommand(cmdArray, "", response);
        else if (cmd.equals("protocol_version") && m_version2)
            response.append("2");
        else if (cmd.equals("version") && m_name != null)
            status = cmdVersion(response);
        else
            status = send(cmdLine, response);
        return status;
    }

    public void interruptProgram(Gtp gtp)
    {
        try
        {
            if (gtp.isInterruptSupported())
                gtp.sendInterrupt();
        }
        catch (Gtp.Error e)
        {
            System.err.println(e);
        }
    }

    public void interruptCommand()
    {
        interruptProgram(m_gtp);
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "help",
                "log:",
                "noscore",
                "name:",
                "size:",
                "version",
                "version2"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpAdapter " + Version.get());
                System.exit(0);
            }
            boolean noScore = opt.isSet("noscore");
            boolean version2 = opt.isSet("version2");
            String name = opt.getString("name", null);
            int size = opt.getInteger("size", -1);
            Vector arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            PrintStream log = null;
            if (opt.isSet("log"))
            {
                File file = new File(opt.getString("log"));
                log = new PrintStream(new FileOutputStream(file));
            }
            String program = (String)arguments.get(0);
            GtpAdapter gtpAdapter =
                new GtpAdapter(System.in, System.out, program, log, version2,
                               size, name, noScore);
            gtpAdapter.mainLoop();
            gtpAdapter.close();
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
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            System.exit(-1);
        }
    }

    private boolean m_noScore;

    private boolean m_version2;

    /** Only accept this board size.
        A value of -1 means accept any size.
    */
    private int m_size;

    private int m_boardSize = 19;

    private String m_name;

    private Gtp m_gtp;

    private boolean cmdBoardsize(String cmdArray[], StringBuffer response)
    {
        if (cmdArray.length != 2)
        {
            response.append("Need argument");
            return false;
        }
        try
        {
            int boardSize = Integer.parseInt(cmdArray[1]);
            if (boardSize < 1)
            {
                response.append("Invalid board size");
                return false;
            }
            if (m_size > 0 && boardSize != m_size)
            {
                response.append("Boardsize must be " + m_size);
                return false;
            }
            m_boardSize = boardSize;
            if (! m_version2)
                return send("boardsize " + boardSize, response);
        }
        catch (NumberFormatException e)
        {
            response.append("Need integer argument");
            return false;
        }
        return true;
    }

    private boolean cmdListCommands(StringBuffer response)
    {
        Vector commands = m_gtp.getSupportedCommands();
        for (int i = 0; i < commands.size(); ++i)
        {
            String cmd = (String)commands.get(i);
            if (m_version2)
                if (cmd.equals("boardsize")
                    || cmd.equals("black")
                    || cmd.equals("genmove_black")
                    || cmd.equals("genmove_white")
                    || cmd.equals("help")
                    || cmd.equals("protocol_version")
                    || cmd.equals("white"))
                    continue;
            if (m_noScore)
                if (cmd.equals("final_score")
                    || (cmd.equals("final_status_list")))
                    continue;
            response.append(cmd);
            response.append("\n");
        }
        if (m_version2)
        {
            response.append("boardsize\n");
            response.append("clear_board\n");
            response.append("genmove\n");
            response.append("list_commands\n");
            response.append("play\n");
            response.append("protocol_version\n");
        }
        return true;
    }

    private boolean cmdName(StringBuffer response)
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index < 0)
            response.append(m_name);
        else
            response.append(m_name.substring(0, index));
        return true;
    }

    private boolean cmdUnknown(StringBuffer response)
    {
        response.append("Unknown command");
        return false;
    }

    private boolean cmdVersion(StringBuffer response)
    {
        assert(m_name != null);
        int index = m_name.indexOf(':');
        if (index >= 0)
            response.append(m_name.substring(index + 1));
        return true;
    }

    private static void printUsage(PrintStream out)
    {
        String helpText =
            "Usage: java -jar gtpadapter.jar program\n" +
            "\n" +
            "-config      config file\n" +
            "-log file    log GTP stream to file\n" +
            "-size        accept only this board size\n" +
            "-version     print version and exit\n" +
            "-version2    translate GTP version 2 for version 1 programs\n";
        out.print(helpText);
    }

    private boolean send(String cmd, StringBuffer response)
    {
        try
        {
            response.append(m_gtp.sendCommand(cmd));
            return true;
        }
        catch (Gtp.Error error)
        {
            response.append(error.getMessage());
            return false;
        }
    }

    private boolean translateColorCommand(String cmdArray[], String cmdPrefix,
                                          StringBuffer response)
    {
        if (cmdArray.length < 2)
        {
            response.append("Need argument");
            return false;
        }
        StringBuffer args = new StringBuffer();
        for (int i = 2; i < cmdArray.length; ++i)
        {
            args.append(" ");
            args.append(cmdArray[i]);
        }
        String color = cmdArray[1].toLowerCase();
        if (color.equals("white") || color.equals("w"))
            return send(cmdPrefix + "white" + args, response);
        if (color.equals("black") || color.equals("b"))
            return send(cmdPrefix + "black" + args, response);
        response.append("Invalid argument");
        return false;
    }
}

//-----------------------------------------------------------------------------
