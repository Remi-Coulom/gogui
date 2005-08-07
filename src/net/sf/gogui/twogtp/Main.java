//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

import java.io.File;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Options;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** TwoGtp main function. */
public class Main
{
    /** TwoGtp main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "alternate",
                "analyze:",
                "auto",
                "black:",
                "compare",
                "config:",
                "force",
                "games:",
                "help",
                "loadsgf",
                "komi:",
                "observer:",
                "openings:",
                "referee:",
                "sgffile:",
                "size:",
                "verbose",
                "version",
                "white:"
            };
            Options opt = Options.parse(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar twogtp.jar [options]\n" +
                    "\n" +
                    "-alternate      alternate colors\n" +
                    "-analyze file   analyze result file\n" +
                    "-auto           autoplay games\n" +
                    "-black          command for black program\n" +
                    "-compare        compare list of sgf files\n" +
                    "-config         config file\n" +
                    "-force          overwrite existing files\n" +
                    "-games          number of games (0=unlimited)\n" +
                    "-help           display this help and exit\n" +
                    "-komi           komi\n" +
                    "-loadsgf        use loadsgf command for openings\n" +
                    "-observer       command for observer program\n" +
                    "-openings       directory with opening sgf files\n" +
                    "-referee        command for referee program\n" +
                    "-sgffile        filename prefix\n" +
                    "-size           board size for autoplay (default 19)\n" +
                    "-verbose        log GTP streams to stderr\n" +
                    "-version        print version and exit\n" +
                    "-white          command for white program\n";
                System.out.print(helpText);
                System.exit(0);
            }
            boolean compare = opt.isSet("compare");
            if (compare)
            {
                Compare.compare(opt.getArguments());
                System.exit(0);
            }
            boolean force = opt.isSet("force");
            if (opt.isSet("version"))
            {
                System.out.println("TwoGtp " + Version.get());
                System.exit(0);
            }
            if (opt.contains("analyze"))
            {
                String filename = opt.getString("analyze");
                new Analyze(filename, force);
                return;
            }                
            boolean alternate = opt.isSet("alternate");
            boolean auto = opt.isSet("auto");
            boolean verbose = opt.isSet("verbose");
            String black = opt.getString("black", "");
            String white = opt.getString("white", "");
            String referee = opt.getString("referee", "");
            String observer = opt.getString("observer", "");
            int size = opt.getInteger("size", 19, 1, GoPoint.MAXSIZE);
            double komi = 6.5;
            boolean isKomiFixed = opt.isSet("komi");
            if (isKomiFixed)
                komi = opt.getDouble("komi");
            int defaultGames = (auto ? 1 : 0);
            int games = opt.getInteger("games", defaultGames, 0);
            String sgfFile = opt.getString("sgffile", "");
            if (opt.isSet("games") && sgfFile.equals(""))
                throw new ErrorMessage("Use option -sgffile with -games.");
            Openings openings = null;
            if (opt.isSet("openings"))
                openings = new Openings(new File(opt.getString("openings")));
            boolean loadsgf = opt.isSet("loadsgf");
            if (loadsgf && openings == null)
                throw new ErrorMessage("Use option -loadsgf with -openings.");
            if (loadsgf && ! auto)
                throw new ErrorMessage("Option -loadsgf can only be used with"
                                       + " -auto");
            TwoGtp twoGtp =
                new TwoGtp(System.in, System.out, black, white, referee,
                           observer, size, komi, isKomiFixed, games,
                           alternate, sgfFile, force, verbose, openings,
                           loadsgf);
            if (auto)
            {
                if (twoGtp.gamesLeft() == 0)
                    System.err.println("Already " + games + " games played.");
                twoGtp.autoPlay();
            }
            else
                twoGtp.mainLoop();
            twoGtp.close();
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
}
