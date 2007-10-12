//----------------------------------------------------------------------------
// Main.java
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

import java.io.File;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Komi;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** TwoGtp main function. */
public final class Main
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
                "komi:",
                "maxmoves:",
                "observer:",
                "openings:",
                "referee:",
                "sgffile:",
                "size:",
                "time:",
                "verbose",
                "version",
                "white:",
                "xml"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
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
                    "-maxmoves       move limit\n" +
                    "-observer       command for observer program\n" +
                    "-openings       directory with opening sgf files\n" +
                    "-referee        command for referee program\n" +
                    "-sgffile        filename prefix\n" +
                    "-size           board size for autoplay (default 19)\n" +
                    "-time spec      set time limits (min[+min/moves])\n" +
                    "-verbose        log GTP streams to stderr\n" +
                    "-version        print version and exit\n" +
                    "-white          command for white program\n" +
                    "-xml            save games in XML format\n";
                System.out.print(helpText);
                System.exit(0);
            }
            boolean compare = opt.contains("compare");
            if (compare)
            {
                Compare.compare(opt.getArguments());
                System.exit(0);
            }
            boolean force = opt.contains("force");
            if (opt.contains("version"))
            {
                System.out.println("TwoGtp " + Version.get());
                System.exit(0);
            }
            if (opt.contains("analyze"))
            {
                String filename = opt.get("analyze");
                new Analyze(filename, force);
                return;
            }
            boolean alternate = opt.contains("alternate");
            boolean auto = opt.contains("auto");
            boolean verbose = opt.contains("verbose");
            String black = opt.get("black", "");
            String white = opt.get("white", "");
            String referee = opt.get("referee", "");
            String observer = opt.get("observer", "");
            int size = opt.getInteger("size", GoPoint.DEFAULT_SIZE, 1,
                                      GoPoint.MAX_SIZE);
            Komi komi = new Komi(6.5);
            if (opt.contains("komi"))
                komi = Komi.parseKomi(opt.get("komi"));
            int maxMoves = opt.getInteger("maxmoves", 1000, -1);
            TimeSettings timeSettings = null;
            if (opt.contains("time"))
                timeSettings = TimeSettings.parse(opt.get("time"));
            int defaultGames = (auto ? 1 : 0);
            int games = opt.getInteger("games", defaultGames, 0);
            String sgfFile = opt.get("sgffile", "");
            if (opt.contains("games") && sgfFile.equals(""))
                throw new ErrorMessage("Use option -sgffile with -games");
            Openings openings = null;
            if (opt.contains("openings"))
                openings = new Openings(new File(opt.get("openings")));
            boolean useXml = opt.contains("xml");
            TwoGtp twoGtp
                = new TwoGtp(black, white, referee, observer, size, komi,
                             games, alternate, sgfFile, force, verbose,
                             openings, timeSettings, useXml);
            twoGtp.setMaxMoves(maxMoves);
            if (auto)
            {
                if (twoGtp.gamesLeft() == 0)
                    System.err.println("Already " + games + " games played");
                twoGtp.autoPlay();
            }
            else
                twoGtp.mainLoop(System.in, System.out);
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }
}
