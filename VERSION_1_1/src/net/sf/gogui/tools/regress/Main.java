// Main.java

package net.sf.gogui.tools.regress;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** Regress main function. */
public final class Main
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "gtpfile:",
                "help",
                "long",
                "output:",
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
                System.out.println("gogui-regress " + Version.get());
                return;
            }
            boolean verbose = opt.contains("verbose");
            boolean longOutput = opt.contains("long");
            String output = opt.get("output", "");
            File gtpFile = null;
            if (opt.contains("gtpfile"))
                gtpFile = new File(opt.get("gtpfile")).getAbsoluteFile();
            ArrayList<String> arguments = opt.getArguments();
            int size = arguments.size();
            if (size < 2)
            {
                printUsage(System.err);
                System.exit(2);
            }
            String program = arguments.get(0);
            ArrayList<String> tests = new ArrayList<String>(arguments);
            tests.remove(0);
            Regress regress = new Regress(program, tests, output, longOutput,
                                          verbose, gtpFile);
            System.exit(regress.getResult() ? 0 : 1);
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(2);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: gogui-regress [options] program test.tst"
                  + " [...]\n" +
                  "\n" +
                  "-config       Config file\n" +
                  "-gtpfile      GTP file to execute before each test\n" +
                  "-help         Display this help and exit\n" +
                  "-long         Longer output to standard out\n" +
                  "-output       Output directory\n" +
                  "-verbose      Log GTP stream to stderr\n" +
                  "-version      Display this help and exit\n");
    }
}
