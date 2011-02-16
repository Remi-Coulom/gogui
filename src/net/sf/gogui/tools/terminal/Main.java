// Main.java

package net.sf.gogui.tools.terminal;

import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** Terminal main function. */
public final class Main
{
    /** Terminal main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "color",
                "config:",
                "help",
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
                System.out.println("gogui-terminal " + Version.get());
                return;
            }
            int size = opt.getInteger("size", GoPoint.DEFAULT_SIZE, 1,
                                      GoPoint.MAX_SIZE);
            boolean verbose = opt.contains("verbose");
            boolean color = opt.contains("color");
            ArrayList<String> arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(1);
            }
            String program = arguments.get(0);
            Terminal terminal = new Terminal(program, size, verbose);
            terminal.setColor(color);
            terminal.mainLoop();
            terminal.close();
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

    private static void printUsage(PrintStream out)
    {
        String helpText =
            "Usage: gogui-terminal program\n" +
            "\n" +
            "-color        colorize go board\n" +
            "-config       config file\n" +
            "-help         print help and exit\n" +
            "-size n       board size (default 19)\n" +
            "-verbose      print debug information\n" +
            "-version      print version and exit\n";
        out.print(helpText);
    }
}
