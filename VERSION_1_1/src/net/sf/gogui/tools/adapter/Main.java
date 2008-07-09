// Main.java

package net.sf.gogui.tools.adapter;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** Adapter main function. */
public final class Main
{
    /** Adapter main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "fillpasses",
                "gtpfile:",
                "help",
                "log:",
                "lowercase",
                "noscore",
                "name:",
                "size:",
                "verbose",
                "version",
                "version1"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.contains("version"))
            {
                if (opt.getArguments().size() > 0)
                    throw new ErrorMessage("No arguments allowed with"
                                           + " option -version");
                System.out.println("gogui-adapter " + Version.get());
                return;
            }
            boolean verbose = opt.contains("verbose");
            boolean noScore = opt.contains("noscore");
            boolean version1 = opt.contains("version1");
            boolean fillPasses = opt.contains("fillpasses");
            boolean lowerCase = opt.contains("lowercase");
            String name = opt.get("name", null);
            String gtpFile = opt.get("gtpfile", null);
            ArrayList<String> arguments = opt.getArguments();
            int size = opt.getInteger("size", 19, 1, GoPoint.MAX_SIZE);
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(1);
            }
            PrintStream log = null;
            if (opt.contains("log"))
            {
                File file = new File(opt.get("log"));
                log = new PrintStream(file);
            }
            String program = arguments.get(0);
            Adapter adapter = new Adapter(program, log, gtpFile, verbose,
                                          noScore, version1, fillPasses,
                                          lowerCase, size);
            if (name != null)
                adapter.setName(name);
            adapter.mainLoop(System.in, System.out);
            adapter.close();
            if (log != null)
                log.close();
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
            "Usage: gogui-adapter program\n" +
            "\n" +
            "-config       config file\n" +
            "-fillpasses   fill non-alternating moves with pass moves\n" +
            "-gtpfile      file with GTP commands to send at startup\n" +
            "-help         print help and exit\n" +
            "-log file     log GTP stream to file\n" +
            "-lowercase    translate move commands to lowercase\n" +
            "-noscore      hide score commands\n" +
            "-resign score resign if estimated score is below threshold\n" +
            "-size         accept only this board size\n" +
            "-verbose      log GTP stream to stderr\n" +
            "-version      print version and exit\n" +
            "-version1     report GTP version 1 in protocol_version\n";
        out.print(helpText);
    }
}
