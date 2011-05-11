// Program.java

package net.sf.gogui.tools.twogtp;

import java.util.ArrayList;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.util.Table;

public class Program
{
    public Program(String command, String defaultName, String logPrefix,
                   boolean verbose) throws GtpError
    {
        m_defaultName = defaultName;
        m_gtp = new GtpClient(command, null, verbose, null);
        m_gtp.setLogPrefix(logPrefix);
        m_synchronizer = new GtpSynchronizer(m_gtp);
        m_gtp.queryProtocolVersion();
        try
        {
            m_name = m_gtp.send("name");
            if (m_name.trim().equals(""))
                m_name = defaultName;
        }
        catch (GtpError e)
        {
            m_name = defaultName;
        }
        try
        {
            m_version = m_gtp.send("version");
        }
        catch (GtpError e)
        {
            m_version = "";
        }
        m_gtp.querySupportedCommands();
        m_gtp.queryInterruptSupport();
    }

    public void close()
    {
        // Some programs don't handle closing input stream well, so
        // we send an explicit quit
        try
        {
            m_gtp.send("quit");
        }
        catch (GtpError e)
        {
        }
        m_gtp.close();
        m_gtp.waitForExit();
    }

    /** Get cputime since program start or last invocation of this
        function. */
    public double getAndClearCpuTime()
    {
        double cpuTime;
        try
        {
            if (m_gtp.isCpuTimeSupported())
                cpuTime = m_gtp.getCpuTime();
            else
                cpuTime = 0;
        }
        catch (GtpError e)
        {
            cpuTime = 0;
        }
        double result = Math.max(0, cpuTime - m_cpuTime);
        m_cpuTime = cpuTime;
        return result;
    }

    /** Get unique label.
        Call setLabel() first. */
    public String getLabel()
    {
        return m_label;
    }

    public String getProgramCommand()
    {
        return m_gtp.getProgramCommand();
    }

    public String getResult()
    {
        try
        {
            return m_gtp.send("final_score");
        }
        catch (GtpError e)
        {
            return "?";
        }
    }

    public String getVersion()
    {
        return m_version;
    }

    public void interruptProgram()
    {
        try
        {
            if (m_gtp.isInterruptSupported())
                m_gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            System.err.println(e);
        }
    }

    public boolean isOutOfSync()
    {
        return m_synchronizer.isOutOfSync();
    }

    public boolean isProgramDead()
    {
        return m_gtp.isProgramDead();
    }

    public boolean isSupported(String command)
    {
        return m_gtp.isSupported(command);
    }

    public String send(String command) throws GtpError
    {
        return m_gtp.send(command);
    }

    public String sendCommandGenmove(GoColor color) throws GtpError
    {
        return send(m_gtp.getCommandGenmove(color));
    }

    public void sendIfSupported(String cmd, String cmdLine)
    {
        if (! m_gtp.isSupported(cmd))
            return;
        try
        {
            m_gtp.send(cmdLine);
        }
        catch (GtpError e)
        {
        }
    }

    public void setIOCallback(GtpClient.IOCallback callback)
    {
        m_gtp.setIOCallback(callback);
    }

    /** Choose a unique label for this program.
        The label will be the program name, if it is the only one with
        this name, otherwise the program name with the version appended
        (or numbers, if the version string is empty or more than 40
        characters).
        @param programs The list of all programs (must include this
        program) */
    public void setLabel(ArrayList<Program> programs)
    {
        boolean isUnique = true;
        for (Program program : programs)
            if (program != this && program.m_name.equals(m_name))
            {
                isUnique = false;
                break;
            }
        if (isUnique)
            m_label = m_name;
        else if (! m_version.trim().equals("")
                 &&  m_version.length() <= 40)
            m_label = m_name + ":" + m_version;
        else
            m_label = m_name + "[" + (programs.indexOf(this) + 1) + "]";
    }

    public void setTableProperties(Table table)
    {
        table.setProperty(m_defaultName, m_name);
        table.setProperty(m_defaultName + "Version", m_version);
        table.setProperty(m_defaultName + "Label", m_label);
        table.setProperty(m_defaultName + "Command", getProgramCommand());
    }

    public void synchronize(ConstGame game) throws GtpError
    {
        try
        {
            ConstNode node = game.getGameInfoNode();
            ConstGameInfo info = game.getGameInfo(node);
            m_synchronizer.synchronize(game.getBoard(), info.getKomi(),
                                       info.getTimeSettings());
        }
        catch (GtpError e)
        {
            throw new GtpError(m_label + ": " + e.getMessage());
        }
    }

    public void synchronizeInit(ConstGame game) throws GtpError
    {
        try
        {
            ConstNode node = game.getGameInfoNode();
            ConstGameInfo info = game.getGameInfo(node);
            m_synchronizer.init(game.getBoard(), info.getKomi(),
                                info.getTimeSettings());
        }
        catch (GtpError e)
        {
            throw new GtpError(m_label + ": " + e.getMessage());
        }
    }

    public void updateAfterGenmove(ConstBoard board)
    {
        m_synchronizer.updateAfterGenmove(board);
    }

    private double m_cpuTime;

    private final String m_defaultName;

    private String m_label;

    private final GtpClient m_gtp;

    private final GtpSynchronizer m_synchronizer;

    private String m_name;

    private String m_version;
}
