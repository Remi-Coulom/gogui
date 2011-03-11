// GoGuiSettings.java

package net.sf.gogui.gogui;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Options;
import net.sf.gogui.version.Version;

/** Parse command line options.
    Also modifies the persistent preferences if some command line options
    are set (e.g. komi) and handles some simple options that don't require
    the graphical startup of GoGui (e.g. -help). */
public final class GoGuiSettings
{
    public boolean m_auto;

    public boolean m_register;

    public boolean m_initComputerColor;

    public boolean m_computerBlack;

    public boolean m_computerWhite;

    /** True if no startup is required.
        This happens for the -help and -version options which are already
        handled in the constructor. */
    public boolean m_noStartup;

    public boolean m_verbose;

    public int m_move;

    public File m_file;

    public File m_analyzeCommands;

    public String m_gtpCommand;

    public String m_gtpFile;

    public String m_lookAndFeel;

    public String m_program;

    public String m_time;

    public GoGuiSettings(String args[], Class c) throws ErrorMessage
    {
        m_prefs = Preferences.userNodeForPackage(c);
        String options[] = {
            "analyze-commands:",
            "auto",
            "command:",
            "computer-black",
            "computer-both",
            "computer-none",
            "computer-white",
            "config:",
            "gtpfile:",
            "help",
            "komi:",
            "laf:",
            "move:",
            "program:",
            "register",
            "size:",
            "time:",
            "verbose",
            "version"
        };
        Options opt = Options.parse(args, options);
        if (opt.contains("help"))
        {
            printHelp();
            m_noStartup = true;
            return;
        }
        if (opt.contains("version"))
        {
            m_noStartup = true;
            System.out.println("GoGui " + Version.get());
            return;
        }
        String analyzeCommandsFilename = opt.get("analyze-commands", null);
        if (analyzeCommandsFilename != null)
            m_analyzeCommands = new File(analyzeCommandsFilename);
        m_auto = opt.contains("auto");
        m_initComputerColor = false;
        if (opt.contains("computer-none"))
        {
            m_computerBlack = false;
            m_computerWhite = false;
            m_initComputerColor = true;
        }
        else if (opt.contains("computer-black"))
        {
            m_computerBlack = true;
            m_computerWhite = false;
            m_initComputerColor = true;
        }
        else if (opt.contains("computer-white"))
        {
            m_computerBlack = false;
            m_computerWhite = true;
            m_initComputerColor = true;
        }
        else if (opt.contains("computer-both"))
        {
            m_computerBlack = true;
            m_computerWhite = true;
            m_initComputerColor = true;
        }
        m_program = opt.get("program", null);
        m_register = opt.contains("register");
        if (m_register && m_program == null)
            throw new ErrorMessage(
                     "Option -register can be used only with option -program");
        m_gtpFile = opt.get("gtpfile", "");
        m_gtpCommand = opt.get("command", "");
        if (opt.contains("komi"))
            m_prefs.putDouble("komi", opt.getDouble("komi"));
        m_lookAndFeel = opt.get("laf", null);
        m_move = opt.getInteger("move", -1);
        if (opt.contains("size"))
            m_prefs.putInt("boardsize", opt.getInteger("size"));
        m_time = opt.get("time", null);
        m_verbose = opt.contains("verbose");
        ArrayList<String> arguments = opt.getArguments();
        m_file = null;
        if (arguments.size() == 1)
            m_file = new File(arguments.get(0));
        else if (arguments.size() > 1)
            throw new ErrorMessage("Only one argument allowed");
        validate();
    }

    private final Preferences m_prefs;

    private void printHelp()
    {
        String helpText =
            "Usage: gogui [options] [file]\n" +
            "Graphical user interface for Go programs\n" +
            "using the Go Text Protocol.\n" +
            "\n" +
            "-analyze          Initialize analyze command\n" +
            "-analyze-commands Use analyze commands configuration file\n" +
            "-auto             Auto play games (if computer both)\n" +
            "-command          Send GTP command at startup\n" +
            "-computer-black   Computer plays black\n" +
            "-computer-both    Computer plays both sides\n" +
            "-computer-none    Computer plays no side\n" +
            "-computer-white   Computer plays white\n" +
            "-config           Config file\n" +
            "-gtpfile          Send GTP file at startup\n" +
            "-help             Display this help and exit\n" +
            "-komi             Set komi\n" +
            "-laf              Set Swing look and feel\n" +
            "-move             Load SGF file until move number\n" +
            "-program          Go program to attach\n" +
            "-size             Set board size\n" +
            "-time             Set time limits (min[+min/moves])\n" +
            "-verbose          Print debugging messages\n" +
            "-version          Print version and exit\n";
        System.out.print(helpText);
    }

    private void validate() throws ErrorMessage
    {
        int size = m_prefs.getInt("boardsize", GoPoint.DEFAULT_SIZE);
        if (size < 1 || size > GoPoint.MAX_SIZE)
            throw new ErrorMessage("Invalid board size: " + size);
    }
}
