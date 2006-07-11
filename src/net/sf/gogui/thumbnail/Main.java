//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.thumbnail;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** SgfThumbnail main function. */
public final class Main
{
    /** SgfThumbnail main function. */
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
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("SgfThumbnail " + Version.get());
                System.exit(0);
            }
            boolean verbose = opt.isSet("verbose");
            ArrayList arguments = opt.getArguments();
            if (arguments.size() == 0 || arguments.size() > 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            File input = new File((String)arguments.get(0));
            File output = null;
            if (arguments.size() == 2)
                output = new File((String)arguments.get(1));
            int size = opt.getInteger("size", 128, 1);
            Thumbnail thumbnail = new Thumbnail(verbose);
            if (! thumbnail.create(input, output, size, true))
            {
                System.err.println(thumbnail.getLastError());
                System.exit(-1);
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
        String helpText =
            "Usage: java -jar sgfthumbnail.jar [options] input [output]\n" +
            "Options:\n" +
            "-help         Print help and exit\n" +
            "-verbose      Print logging messages to stderr\n" +
            "-version      Print version and exit\n";
        out.print(helpText);
    }
}

//----------------------------------------------------------------------------
