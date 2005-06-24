//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.PrintStream;
import java.util.Vector;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GtpStatistics main function. */
class Main
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "analyze:",
                "commands:",
                "config:",
                "help",
                "precision:",
                "program:",
                "verbose",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpStatistics " + Version.get());
                return;
            }
            boolean analyze = opt.isSet("analyze");
            String program = "";
            if (! analyze)
            {
                if (! opt.isSet("program"))
                {
                    System.out.println("Need option -program");
                    System.exit(-1);
                }
                program = opt.getString("program");
            }
            boolean verbose = opt.isSet("verbose");
            int precision = opt.getInteger("precision", 4, 0);
            Vector commands = null;
            if (opt.isSet("commands"))
            {
                String commandString = opt.getString("commands");
                String[] commandsArray
                    = StringUtils.split(commandString, ',');
                commands = new Vector(commandsArray.length);
                for (int i = 0; i < commandsArray.length; ++i)
                    commands.add(commandsArray[i].trim());
            }
            Vector arguments = opt.getArguments();
            int size = arguments.size();
            if (analyze)
            {
                new Analyze(opt.getString("analyze"), precision);
            }
            else
            {
                if (size < 1)
                {
                    printUsage(System.err);
                    System.exit(-1);
                }
                GtpStatistics gtpStatistics
                    = new GtpStatistics(program, arguments, commands,
                                        verbose);
                System.exit(gtpStatistics.getResult() ? 0 : -1);
            }
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
        out.print("Usage: java -jar gtpstatistics.jar [options] file.sgf|dir"
                  + " [...]\n" +
                  "\n" +
                  "-config       Config file\n" +
                  "-help         Display this help and exit\n" +
                  "-verbose      Log GTP stream to stderr\n" +
                  "-version      Display this help and exit\n");
    }
}
    
//----------------------------------------------------------------------------
