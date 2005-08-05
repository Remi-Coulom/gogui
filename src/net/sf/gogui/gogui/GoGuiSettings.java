//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.Preferences;
import net.sf.gogui.version.Version;
import java.util.Vector;

//----------------------------------------------------------------------------

/** Parse command line options.
    Also modifies the persistent preferences if some command line options
    are set (e.g. komi) and handles some simple options that don't require
    the graphical startup of GoGui (e.g. -help).
*/
public final class GoGuiSettings
{
    public boolean m_auto;

    public boolean m_computerBlack;

    public boolean m_computerWhite;

    public boolean m_fastPaint;

    /** True if no startup is required.
        This happens for the -help and -version options which are already
        handled in the constructor.
    */
    public boolean m_noStartup;

    public boolean m_verbose;

    public int m_move;

    public Preferences m_preferences;

    public String m_file;

    public String m_gtpCommand;

    public String m_gtpFile;

    public String m_initAnalyze;

    public String m_program;

    public String m_rules;

    public String m_time;

    public GoGuiSettings(String args[]) throws ErrorMessage
    {
        String options[] = {
            "analyze:",
            "auto",
            "command:",
            "computer-black",
            "computer-both",
            "computer-none",
            "config:",
            "gtpfile:",
            "fast",
            "help",
            "komi:",
            "move:",
            "program:",
            "rules:",
            "size:",
            "time:",
            "verbose",
            "version"
        };
        Options opt = Options.parse(args, options);
        if (opt.isSet("help"))
        {
            printHelp();
            m_noStartup = true;
            return;
        }
        if (opt.isSet("version"))
        {
            m_noStartup = true;
            System.out.println("GoGui " + Version.get());
            return;
        }
        m_preferences = new Preferences();
        m_initAnalyze = opt.getString("analyze");
        m_fastPaint = opt.isSet("fast");
        m_auto = opt.isSet("auto");
        m_computerBlack = false;
        m_computerWhite = true;
        if (opt.isSet("computer-none"))
            m_computerWhite = false;
        else if (opt.isSet("computer-black"))
        {
            m_computerBlack = true;
            m_computerWhite = false;
        }
        else if (opt.isSet("computer-both"))
            m_computerBlack = true;
        m_program = opt.getString("program", null);
        m_gtpFile = opt.getString("gtpfile", "");
        m_gtpCommand = opt.getString("command", "");
        if (opt.contains("komi"))
            m_preferences.setDouble("komi", opt.getDouble("komi"));
        m_move = opt.getInteger("move", -1);
        if (opt.contains("size"))
            m_preferences.setInt("boardsize", opt.getInteger("size"));
        m_rules = opt.getString("rules", "");
        m_preferences.setString("rules", m_rules);
        m_time = opt.getString("time", null);
        m_verbose = opt.isSet("verbose");
        Vector arguments = opt.getArguments();
        m_file = null;
        if (arguments.size() == 1)
            m_file = (String)arguments.get(0);
        else if (arguments.size() > 1)
            throw new ErrorMessage("Only one argument allowed.");
        validate();
    }

    private void printHelp()
    {
        String helpText =
            "Usage: java -jar gogui.jar [options] [file]\n" +
            "Graphical user interface for Go programs\n" +
            "using the Go Text Protocol.\n" +
            "\n" +
            "-analyze name   initialize analyze command\n" +
            "-auto           auto play games (if computer both)\n" +
            "-command cmd    send GTP command at startup\n" +
            "-computer-both  computer plays both sides\n" +
            "-computer-black computer plays black\n" +
            "-computer-none  computer plays no side\n" +
            "-config         config file\n" +
            "-fast           fast and simple graphics\n" +
            "-gtpfile file   send GTP file at startup\n" +
            "-help           display this help and exit\n" +
            "-komi value     set komi\n" +
            "-move n         load SGF file until move number\n" +
            "-program cmd    Go program to attach\n" +
            "-rules name     use rules (chinese|japanese)\n" +
            "-size n         set board size\n" +
            "-time spec      set time limits (min[+min/moves])\n" +
            "-verbose        print debugging messages\n" +
            "-version        print version and exit\n";
        System.out.print(helpText);
    }   

    private void validate() throws ErrorMessage
    {
        int size = m_preferences.getInt("boardsize");
        if (size < 1 || size > GoPoint.MAXSIZE)
            throw new ErrorMessage("Invalid board size: " + size);
    }
}

//----------------------------------------------------------------------------

