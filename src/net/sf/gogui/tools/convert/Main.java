// Main.java

package net.sf.gogui.tools.convert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.gamefile.GameReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.tex.TexWriter;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;
import net.sf.gogui.xml.XmlWriter;

/** Convert SGF and Jago XML Go game files to other formats. */
public final class Main
{
    /** Main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "check",
                "config:",
                "force",
                "format:",
                "help",
                "title:",
                "version",
                "werror"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.contains("version"))
            {
                System.out.println("gogui-convert " + Version.get());
                System.exit(0);
            }
            boolean force = opt.contains("force");
            String title = opt.get("title", "");
            boolean werror = opt.contains("werror");
            boolean checkOnly = opt.contains("check");
            ArrayList<String> arguments = opt.getArguments();
            if (! (arguments.size() == 2
                   || (arguments.size() == 1 && checkOnly)))
            {
                printUsage(System.err);
                System.exit(1);
            }
            File in = new File(arguments.get(0));
            File out = null;
            String format = null;
            if (! checkOnly)
            {
                out = new File(arguments.get(1));
                if (opt.contains("format"))
                    format = opt.get("format");
                else
                    format =
                        FileUtil.getExtension(out).toLowerCase(Locale.ENGLISH);
                if (! format.equals("sgf")
                    && ! format.equals("tex")
                    && ! format.equals("xml"))
                    throw new ErrorMessage("Unknown format");
                if (out.exists() && ! force)
                    throw new ErrorMessage("File \"" + out
                                           + "\" already exists");
            }
            if (! in.exists())
                throw new ErrorMessage("File \"" + in + "\" not found");
            GameReader reader = new GameReader(in);
            ConstGameTree tree = reader.getTree();
            String warnings = reader.getWarnings();
            if (warnings != null)
            {
                System.err.print(warnings);
                if (werror)
                    System.exit(1);
            }
            if (! checkOnly)
            {
                String version = Version.get();
                if (format.equals("xml"))
                    new XmlWriter(new FileOutputStream(out), tree,
                                  "gogui-convert:" + version);
                else if (format.equals("sgf"))
                    new SgfWriter(new FileOutputStream(out), tree,
                                  "gogui-convert", version);
                else if (format.equals("tex"))
                    new TexWriter(title, new FileOutputStream(out), tree);
                else
                    assert false; // checked above
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
        out.print("Usage: gogui-convert infile outfile\n" +
                  "\n" +
                  "-check   only check reading a file\n" +
                  "-config  config file\n" +
                  "-force   overwrite existing files\n" +
                  "-format  output format (sgf,tex,xml)\n" +
                  "-help    display this help and exit\n" +
                  "-title   use title\n" +
                  "-version print version and exit\n" +
                  "-werror  handle read warnings as errors\n");
    }
}
