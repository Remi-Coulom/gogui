//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpdisplay;

import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GtpDisplay main function. */
public final class Main
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "fast",
                "help",
                "verbose",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpDisplay " + Version.get());
                System.exit(0);
            }
            boolean verbose = opt.isSet("verbose");
            boolean fastPaint = opt.isSet("fast");
            ArrayList arguments = opt.getArguments();
            if (arguments.size() > 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = null;
            if (arguments.size() == 1)
                program = (String)arguments.get(0);
            GtpDisplay gtpDisplay
                = new GtpDisplay(program, verbose, fastPaint);
            gtpDisplay.mainLoop(System.in, System.out);
            gtpDisplay.close();
        }
        catch (Throwable t)
        {
            StringUtils.printException(t);
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void printUsage(PrintStream out)
    {
        String helpText =
            "Usage: java -jar gtpdisplay.jar program\n" +
            "\n" +
            "-config       config file\n" +
            "-fast         fast and simple graphics\n" +
            "-help         print help and exit\n" +
            "-verbose      log GTP stream to stderr\n" +
            "-version      print version and exit\n";
        out.print(helpText);
    }
}

//----------------------------------------------------------------------------
