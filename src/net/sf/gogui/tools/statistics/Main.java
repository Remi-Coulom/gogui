// Main.java

package net.sf.gogui.tools.statistics;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** Statistics main function. */
public final class Main
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "analyze:",
                "backward",
                "begin:",
                "commands:",
                "config:",
                "final:",
                "force",
                "help",
                "max:",
                "min:",
                "output:",
                "precision:",
                "program:",
                "quiet",
                "random",
                "setup",
                "size:",
                "verbose",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.contains("version"))
            {
                System.out.println("gogui-statistics " + Version.get());
                return;
            }
            boolean analyze = opt.contains("analyze");
            boolean allowSetup = opt.contains("setup");
            boolean backward = opt.contains("backward");
            boolean random = opt.contains("random");
            String program = "";
            if (! analyze)
            {
                if (! opt.contains("program"))
                {
                    System.out.println("Need option -program");
                    System.exit(1);
                }
                program = opt.get("program");
            }
            boolean verbose = opt.contains("verbose");
            boolean quiet = opt.contains("quiet");
            boolean force = opt.contains("force");
            int min = opt.getInteger("min", 0, 0);
            int max = opt.getInteger("max", Integer.MAX_VALUE, 0);
            int precision = opt.getInteger("precision", 3, 0);
            int boardSize = opt.getInteger("size", GoPoint.DEFAULT_SIZE, 1,
                                           GoPoint.MAX_SIZE);
            ArrayList<String> commands = parseCommands(opt, "commands");
            ArrayList<String> finalCommands = parseCommands(opt, "final");
            ArrayList<String> beginCommands = parseCommands(opt, "begin");
            ArrayList<String> arguments = opt.getArguments();
            int size = arguments.size();
            if (analyze)
            {
                if (size > 0)
                {
                    printUsage(System.err);
                    System.exit(1);
                }
                String fileName = opt.get("analyze");
                String output = opt.get("output");
                new Analyze(fileName, output, precision);
            }
            else
            {
                if (size < 1)
                {
                    printUsage(System.err);
                    System.exit(1);
                }
                File output;
                if (opt.contains("output"))
                    output = new File(opt.get("output"));
                else
                    output = new File("statistics.dat");
                if (output.exists() && ! force)
                    throw new ErrorMessage("File \"" + output +
                                           "\" already exists");
                Statistics statistics = new Statistics();
                statistics.setMin(min);
                statistics.setMax(max);
                statistics.setQuiet(quiet);
                statistics.run(program, arguments, boardSize, commands,
                               beginCommands, finalCommands, verbose,
                               allowSetup, backward, random);
                statistics.saveTable(output);
            }
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static ArrayList<String> parseCommands(Options opt, String option)
        throws ErrorMessage
    {
        ArrayList<String> result = null;
        if (opt.contains(option))
        {
            String string = opt.get(option);
            String[] array = StringUtil.split(string, ',');
            result = new ArrayList<String>(array.length);
            for (int i = 0; i < array.length; ++i)
                result.add(array[i].trim());
        }
        return result;
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: gogui-statistics -program program"
                  + " [options] file.sgf|dir [...]\n" +
                  "\n" +
                  "-analyze      Create HTML file from result file\n" +
                  "-backward     Iterate backward from end position\n" +
                  "-begin        GTP commands to run on begin positions\n" +
                  "-commands     GTP commands to run (comma separated)\n" +
                  "-config       Config file\n" +
                  "-final        GTP commands to run on final positions\n" +
                  "-force        Overwrite existing file\n" +
                  "-help         Display this help and exit\n" +
                  "-max          Only positions with maximum move number\n" +
                  "-min          Only positions with minimum move number\n" +
                  "-output       Filename prefix for output files\n" +
                  "-precision    Floating point precision for -analyze\n" +
                  "-quiet        Don't write logging messages\n" +
                  "-setup        Allow setup stones in root position\n" +
                  "-size         Board size of games\n" +
                  "-verbose      Log GTP stream to stderr\n" +
                  "-version      Display this help and exit\n");
    }
}
