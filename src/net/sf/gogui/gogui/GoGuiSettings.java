//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gui.GameTreePanel;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.Preferences;
import net.sf.gogui.version.Version;

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

    public String m_lookAndFeel;

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
            "laf:",
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
        setDefaults(m_preferences);
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
        m_lookAndFeel = opt.getString("laf", null);
        if ("gtk".equals(m_lookAndFeel))
            m_lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        else if ("motif".equals(m_lookAndFeel))
            m_lookAndFeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        else if ("windows".equals(m_lookAndFeel))
            m_lookAndFeel =
                "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        else if ("plasticxp".equals(m_lookAndFeel))
            m_lookAndFeel = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";
        m_move = opt.getInteger("move", -1);
        if (opt.contains("size"))
            m_preferences.setInt("boardsize", opt.getInteger("size"));
        m_rules = opt.getString("rules", "");
        m_preferences.setString("rules", m_rules);
        m_time = opt.getString("time", null);
        m_verbose = opt.isSet("verbose");
        ArrayList arguments = opt.getArguments();
        m_file = null;
        if (arguments.size() == 1)
            m_file = (String)arguments.get(0);
        else if (arguments.size() > 1)
            throw new ErrorMessage("Only one argument allowed");
        validate();
    }

    private void printHelp()
    {
        String helpText =
            "Usage: java -jar gogui.jar [options] [file]\n" +
            "Graphical user interface for Go programs\n" +
            "using the Go Text Protocol.\n" +
            "\n" +
            "-analyze name   Initialize analyze command\n" +
            "-auto           Auto play games (if computer both)\n" +
            "-command cmd    Send GTP command at startup\n" +
            "-computer-both  Computer plays both sides\n" +
            "-computer-black Computer plays black\n" +
            "-computer-none  Computer plays no side\n" +
            "-config         Config file\n" +
            "-fast           Fast and simple graphics\n" +
            "-gtpfile file   Send GTP file at startup\n" +
            "-help           Display this help and exit\n" +
            "-komi value     Set komi\n" +
            "-laf name       Set Swing look and feel\n" +
            "-move n         Load SGF file until move number\n" +
            "-program cmd    Go program to attach\n" +
            "-rules name     Use rules (chinese|japanese)\n" +
            "-size n         Set board size\n" +
            "-time spec      Set time limits (min[+min/moves])\n" +
            "-verbose        Print debugging messages\n" +
            "-version        Print version and exit\n";
        System.out.print(helpText);
    }   

    private static void setDefaults(Preferences prefs)
    {
        prefs.setBoolDefault("analyze-only-supported-commands", true);
        prefs.setBoolDefault("analyze-sort", true);
        prefs.setBoolDefault("beep-after-move", true);
        prefs.setIntDefault("boardsize", GoPoint.DEFAULT_SIZE);
        prefs.setBoolDefault("comment-font-fixed", false);
        prefs.setIntDefault("gametree-labels", GameTreePanel.LABEL_NUMBER);
        prefs.setIntDefault("gametree-size", GameTreePanel.SIZE_NORMAL);
        prefs.setBoolDefault("gametree-show-subtree-sizes", false);
        prefs.setBoolDefault("gtpshell-highlight", true);
        prefs.setBoolDefault("gtpshell-autonumber", false);
        // JComboBox has problems on the Mac, see section Bugs in
        // documentation
        prefs.setBoolDefault("gtpshell-disable-completions",
                             Platform.isMac());
        prefs.setIntDefault("gtpshell-history-max", 3000);
        prefs.setIntDefault("gtpshell-history-min", 2000);
        prefs.setBoolDefault("gtpshell-timestamp", false);
        prefs.setDoubleDefault("komi", 6.5);
        prefs.setStringDefault("rules", "Chinese");
        prefs.setBoolDefault("show-analyze", false);
        prefs.setBoolDefault("show-gtpshell", false);
        prefs.setBoolDefault("show-gametree", false);
        prefs.setBoolDefault("show-cursor", true);
        prefs.setBoolDefault("show-grid", true);
        prefs.setBoolDefault("show-info-panel", true);
        prefs.setBoolDefault("show-last-move", false);
        prefs.setBoolDefault("show-toolbar", true);
        prefs.setBoolDefault("show-variations", false);
    }

    private void validate() throws ErrorMessage
    {
        int size = m_preferences.getInt("boardsize");
        if (size < 1 || size > GoPoint.MAXSIZE)
            throw new ErrorMessage("Invalid board size: " + size);
    }
}

//----------------------------------------------------------------------------

