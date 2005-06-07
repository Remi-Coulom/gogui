//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.util.Vector;
import gui.SimpleDialogs;
import utils.Options;
import utils.Preferences;
import utils.StringUtils;
import version.Version;

//----------------------------------------------------------------------------

/** GoGui main function. */
public class Main
{
    /** GoGui main function. */
    public static void main(String[] args)
    {
        // Setting these Mac system properties here worked with older versions
        // of Mac Java, for newer ones, they have to be set when starting the
        // VM (see options in scripts and mac/Info.plist)
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        System.setProperty("apple.awt.brushMetalLook", "true");
        boolean verbose = false;
        try
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
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GoGui " + Version.get());
                return;
            }
            Preferences prefs = new Preferences();
            String initAnalyze = opt.getString("analyze");
            boolean fastPaint = opt.isSet("fast");
            boolean auto = opt.isSet("auto");
            boolean computerBlack = false;
            boolean computerWhite = true;
            if (opt.isSet("computer-none"))
                computerWhite = false;
            else if (opt.isSet("computer-black"))
            {
                computerBlack = true;
                computerWhite = false;
            }
            else if (opt.isSet("computer-both"))
                computerBlack = true;
            String program = opt.getString("program", null);
            String gtpFile = opt.getString("gtpfile", "");
            String gtpCommand = opt.getString("command", "");
            if (opt.contains("komi"))
                prefs.setDouble("komi", opt.getDouble("komi"));
            int move = opt.getInteger("move", -1);
            if (opt.contains("size"))
                prefs.setInt("boardsize", opt.getInteger("size"));
            String rules = opt.getString("rules", "");
            prefs.setString("rules", rules);
            String time = opt.getString("time", null);
            verbose = opt.isSet("verbose");
            Vector arguments = opt.getArguments();
            String file = null;
            if (arguments.size() == 1)
                file = (String)arguments.get(0);
            else if (arguments.size() > 1)
                throw new Exception("Only one argument allowed.");
            new GoGui(program, prefs, file, move, time, verbose,
                      computerBlack, computerWhite, auto, gtpFile, gtpCommand,
                      initAnalyze, fastPaint);
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtils.printException(t));
            System.exit(-1);
        }
    }
}

//----------------------------------------------------------------------------
