//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.File;
import java.io.PrintStream;
import java.util.Vector;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.FileUtils;
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
                "final:",
                "force",
                "help",
                "interval:",
                "precision:",
                "program:",
                "size:",
                "output:",
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
            boolean force = opt.isSet("force");
            int precision = opt.getInteger("precision", 3, 0);
            int interval = opt.getInteger("interval", 20, 1);
            int boardSize = opt.getInteger("size", 19, 1);
            Vector commands = null;
            Vector finalCommands = null;
            if (opt.isSet("commands"))
            {
                String commandString = opt.getString("commands");
                String[] commandsArray
                    = StringUtils.split(commandString, ',');
                commands = new Vector(commandsArray.length);
                for (int i = 0; i < commandsArray.length; ++i)
                    commands.add(commandsArray[i].trim());
            }
            if (opt.isSet("final"))
            {
                String commandString = opt.getString("final");
                String[] commandsArray
                    = StringUtils.split(commandString, ',');
                finalCommands = new Vector(commandsArray.length);
                for (int i = 0; i < commandsArray.length; ++i)
                    finalCommands.add(commandsArray[i].trim());
            }
            Vector arguments = opt.getArguments();
            int size = arguments.size();
            if (analyze)
            {
                if (size > 0)
                {
                    printUsage(System.err);
                    System.exit(-1);
                }
                String fileName = opt.getString("analyze");
                String output;
                if (opt.isSet("output"))
                    output = opt.getString("output");
                else
                    output = FileUtils.removeExtension(new File(fileName),
                                                       "dat");
                new Analyze(fileName, output, precision, interval);
            }
            else
            {
                if (size < 1)
                {
                    printUsage(System.err);
                    System.exit(-1);
                }
                GtpStatistics gtpStatistics
                    = new GtpStatistics(program, arguments, boardSize,
                                        commands, finalCommands, verbose,
                                        force);
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
                  "-analyze      Create HTML file from result file\n" +
                  "-config       Config file\n" +
                  "-commands     GTP commands to run (comma separated)\n" +
                  "-final        GTP commands to run on final positions\n" +
                  "              (comma separated)\n" +
                  "-force        Overwrite existing file\n" +
                  "-help         Display this help and exit\n" +
                  "-interval     Move interval size for -analyze\n" +
                  "-output       Filename prefix for output files\n" +
                  "-precision    Floating point precision for -analyze\n" +
                  "-size         Board size of games\n" +
                  "-verbose      Log GTP stream to stderr\n" +
                  "-version      Display this help and exit\n");
    }
}
    
//----------------------------------------------------------------------------
