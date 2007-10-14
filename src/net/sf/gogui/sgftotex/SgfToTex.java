//----------------------------------------------------------------------------
// SgfToTex.java
//----------------------------------------------------------------------------

package net.sf.gogui.sgftotex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.tex.TexWriter;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** SGF to LaTeX converter. */
public final class SgfToTex
{
    /** Main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "force",
                "help",
                "title:",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.contains("version"))
            {
                System.out.println("SgfToTex " + Version.get());
                System.exit(0);
            }
            boolean force = opt.contains("force");
            String title = opt.get("title", "");
            ArrayList<String> arguments = opt.getArguments();
            InputStream in;
            OutputStream out;
            if (arguments.size() > 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String inFileName = null;
            if (arguments.isEmpty())
            {
                in = System.in;
                out = System.out;
            }
            else
            {
                inFileName = arguments.get(0);
                File inFile = new File(inFileName);
                in = new FileInputStream(inFile);
                String outFileName;
                if (arguments.size() == 1)
                    outFileName =
                        FileUtil.replaceExtension(inFile, "sgf", "tex");
                else
                    outFileName = arguments.get(1);
                File outFile = new File(outFileName);
                if (outFile.exists() && ! force)
                    throw new Exception("File " + outFile
                                        + " already exists");
                out = new FileOutputStream(outFile);
                if (title.equals(""))
                    title =
                        FileUtil.removeExtension(new File(outFile.getName()),
                                                  "tex");
            }
            convert(in, inFileName, out, title);
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private SgfToTex()
    {
    }

    private static void convert(InputStream in, String name, OutputStream out,
                                String title)
        throws SgfError
    {
        SgfReader reader = new SgfReader(in, new File(name), null, 0);
        new TexWriter(title, out, reader.getTree());
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: java -jar sgftotex.jar [file.sgf [file.tex]]\n" +
                  "\n" +
                  "-config  config file\n" +
                  "-force   overwrite existing files\n" +
                  "-help    display this help and exit\n" +
                  "-title   use title\n" +
                  "-version print version and exit\n");
    }
}
