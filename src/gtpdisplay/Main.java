//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtpdisplay;

import java.awt.*;
import java.io.*;
import java.util.*;
import game.*;
import go.*;
import gtp.*;
import gui.*;
import utils.*;
import version.*;

//----------------------------------------------------------------------------

public class Main
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "help",
                "size:",
                "verbose",
                "version",
                "version2"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
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
            boolean version2 = opt.isSet("version2");
            int size = opt.getInteger("size", -1);
            Vector arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            GtpDisplay gtpDisplay =
                new GtpDisplay(System.in, System.out, program, verbose);
            gtpDisplay.mainLoop();
            gtpDisplay.close();
        }
        catch (Throwable t)
        {
            StringUtils.printException(t);
            System.exit(-1);
        }
    }

    private static void printUsage(PrintStream out)
    {
        String helpText =
            "Usage: java -jar gtpdisplay.jar program\n" +
            "\n" +
            "-config       config file\n" +
            "-help         print help and exit\n" +
            "-size         accept only this board size\n" +
            "-verbose      log GTP stream to stderr\n" +
            "-version      print version and exit\n";
        out.print(helpText);
    }
}

//----------------------------------------------------------------------------
