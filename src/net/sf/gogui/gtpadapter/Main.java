//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpadapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** GtpAdapter main function. */
public final class Main
{
    /** GtpAdapter main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "emuhandicap",
                "fillpasses",
                "gtpfile:",
                "help",
                "log:",
                "lowercase",
                "noscore",
                "name:",
                "resign:",
                "size:",
                "verbose",
                "version",
                "version1"
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                return;
            }
            if (opt.isSet("version"))
            {
                if (opt.getArguments().size() > 0)
                    fail("No arguments allowed with option -version");
                System.out.println("GtpAdapter " + Version.get());
                return;
            }
            boolean verbose = opt.isSet("verbose");
            boolean noScore = opt.isSet("noscore");
            boolean version1 = opt.isSet("version1");
            boolean emuHandicap = opt.isSet("emuhandicap");
            boolean fillPasses = opt.isSet("fillpasses");
            String name = opt.getString("name", null);
            String gtpFile = opt.getString("gtpfile", null);
            boolean resign = opt.isSet("resign");
            int resignScore = opt.getInteger("resign");            
            ArrayList arguments = opt.getArguments();
            if (arguments.size() != 1)
            {
                printUsage(System.err);
                fail();
            }
            PrintStream log = null;
            if (opt.isSet("log"))
            {
                File file = new File(opt.getString("log"));
                log = new PrintStream(new FileOutputStream(file));
            }
            String program = (String)arguments.get(0);
            GtpAdapter adapter
                = new GtpAdapter(program, log, gtpFile, verbose, emuHandicap,
                                 noScore, version1);
            if (name != null)
                adapter.setName(name);
            if (fillPasses)
                adapter.setFillPasses();
            if (resign)
                adapter.setResign(resignScore);
            if (opt.isSet("lowercase"))
                adapter.setLowerCase();
            if (opt.isSet("size"))
            {
                int size = opt.getInteger("size", 0, 1, GoPoint.MAXSIZE);
                adapter.setFixedSize(size);
            }
            adapter.mainLoop(System.in, System.out);
            adapter.close();
            if (log != null)
                log.close();
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            fail();
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void fail()
    {
        System.exit(-1);
    }

    private static void fail(String message)
    {
        System.err.println(message);
        fail();
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
            "-lowercase    translate move commands to lowercase\n" +
            "-noscore      hide score commands\n" +
            "-resign score resign if estimated score is below threshold\n" +
            "-size         accept only this board size\n" +
            "-verbose      log GTP stream to stderr\n" +
            "-version      print version and exit\n" +
            "-version1     report GTP version 1 in protocol_version";
        out.print(helpText);
    }
}

//----------------------------------------------------------------------------
