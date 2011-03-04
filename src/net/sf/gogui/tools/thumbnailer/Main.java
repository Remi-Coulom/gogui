// Main.java

package net.sf.gogui.tools.thumbnailer;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.thumbnail.ThumbnailCreator;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** GoGuiThumbnailer main function. */
public final class Main
{
    /** GoGuiThumbnailer main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                //"check-expire:", // experimental; needs more testing
                "config:",
                //"expire:", // experimental; needs more testing
                "help",
                "scale",
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
                System.out.println("gogui-thumbnailer " + Version.get());
                return;
            }
            /*
            if (opt.contains("expire"))
            {
                int seconds = opt.getInteger("expire", 0, 0);
                ThumbnailUtil.expire(seconds, false);
                return;
            }
            if (opt.contains("check-expire"))
            {
                int seconds = opt.getInteger("expire", 0, 0);
                ThumbnailUtil.expire(seconds, true);
                return;
            } */
            boolean verbose = opt.contains("verbose");
            boolean scale = opt.contains("scale");
            ArrayList<String> arguments = opt.getArguments();
            if (arguments.isEmpty() || arguments.size() > 2)
            {
                printUsage(System.err);
                System.exit(1);
            }
            File input = new File(arguments.get(0));
            File output = null;
            if (arguments.size() == 2)
                output = new File(arguments.get(1));
            int size = opt.getInteger("size", 128, 1);
            ThumbnailCreator thumbnailCreator = new ThumbnailCreator(verbose);
            try
            {
                thumbnailCreator.create(input, output, size, scale);
            }
            catch (ThumbnailCreator.Error e)
            {
                System.err.println(e.getMessage());
                System.exit(1);
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

    private static void printUsage(PrintStream out)
    {
        String helpText =
            "Usage: gogui-thumbnailer [options] input [output]\n" +
            "Options:\n" +
            "-config    config file\n" +
            "-help      Print help and exit\n" +
            "-scale     Scale size for board sizes other than 19x19\n" +
            "-size      Thumbnail size in pixels\n" +
            "-verbose   Print logging messages to stderr\n" +
            "-version   Print version and exit\n";
        out.print(helpText);
    }
}
