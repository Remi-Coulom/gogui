//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtpadapter;

import java.io.*;
import java.util.*;
import game.*;
import go.*;
import gtp.*;
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
                "emuhandicap",
                "emuloadsgf",
                "fillpasses",
                "gtpfile:",
                "help",
                "log:",
                "noscore",
                "name:",
                "resign:",
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
                return;
            }
            if (opt.isSet("version"))
            {
                System.out.println("GtpAdapter " + Version.get());
                return;
            }
            boolean verbose = opt.isSet("verbose");
            boolean noScore = opt.isSet("noscore");
            boolean version2 = opt.isSet("version2");
            boolean emuHandicap = opt.isSet("emuhandicap");
            boolean emuLoadsgf = opt.isSet("emuloadsgf");
            boolean fillPasses = opt.isSet("fillpasses");
            String name = opt.getString("name", null);
            String gtpFile = opt.getString("gtpfile", null);
            int size = opt.getInteger("size", -1);
            boolean resign = opt.isSet("resign");
            int resignScore = opt.getInteger("resign");            
            Vector arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            PrintStream log = null;
            if (opt.isSet("log"))
            {
                File file = new File(opt.getString("log"));
                log = new PrintStream(new FileOutputStream(file));
            }
            String program = (String)arguments.get(0);
            GtpAdapter gtpAdapter =
                new GtpAdapter(System.in, System.out, program, log, version2,
                               size, name, noScore, emuHandicap, emuLoadsgf,
                               resign, resignScore, gtpFile, verbose,
                               fillPasses);
            gtpAdapter.mainLoop();
            gtpAdapter.close();
            if (log != null)
                log.close();
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
            "Usage: java -jar gtpadapter.jar program\n" +
            "\n" +
            "-config       config file\n" +
            "-emuhandicap  emulate free handicap commands\n" +
            "-emuloadsgf   emulate loadsgf commands\n" +
            "-fillpasses   fill non-alternating moves with pass moves\n" +
            "-gtpfile      file with GTP commands to send at startup\n" +
            "-help         print help and exit\n" +
            "-log file     log GTP stream to file\n" +
            "-noscore      hide score commands\n" +
            "-resign score resign if estimated score is below threshold\n" +
            "-size         accept only this board size\n" +
            "-verbose      log GTP stream to stderr\n" +
            "-version      print version and exit\n" +
            "-version2     translate GTP version 2 for version 1 programs\n";
        out.print(helpText);
    }
}

//----------------------------------------------------------------------------
