//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.util.*;
import javax.swing.*;
import gui.SimpleDialogs;
import utils.*;
import version.*;

//----------------------------------------------------------------------------

public class Main
{
    public static void main(String[] args)
    {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        System.setProperty("apple.awt.brushMetalLook", "true");
        try
        {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        }
        catch (Exception e)
        {
        }
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
                "file:",
                "gtpfile:",
                "help",
                "komi:",
                "move:",
                "rules:",
                "size:",
                "time:",
                "verbose",
                "version"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar gogui.jar [options] program\n" +
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
                    "-file filename  load SGF file\n" +
                    "-gtpfile file   send GTP file at startup\n" +
                    "-help           display this help and exit\n" +
                    "-komi value     set komi\n" +
                    "-move n         load SGF file until move number\n" +
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
            String file = opt.getString("file", "");
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
            String program = null;
            if (arguments.size() == 1)
                program = (String)arguments.get(0);
            else if (arguments.size() > 1)
                throw new Exception("Only one program argument allowed.");
            new GoGui(program, prefs, file, move, time, verbose,
                      computerBlack, computerWhite, auto, gtpFile, gtpCommand,
                      initAnalyze);
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtils.printException(t));
            System.exit(-1);
        }
    }
}

//----------------------------------------------------------------------------
