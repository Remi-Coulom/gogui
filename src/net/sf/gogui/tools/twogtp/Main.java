// Main.java

package net.sf.gogui.tools.twogtp;

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
            opt.checkNoArguments();
            if (opt.contains("help"))
            {
                String helpText =
                    "Usage: gogui-twogtp [options]\n" +
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
                System.out.println("GoGuiTwoGtp " + Version.get());
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

            Program blackProgram = new Program(black, "Black", "B", verbose);
            Program whiteProgram = new Program(white, "White", "W", verbose);
            Program refereeProgram;
            if (referee.equals(""))
                refereeProgram = null;
            else
                refereeProgram = new Program(referee, "Referee", "R", verbose);
            ResultFile resultFile;
            if (sgfFile.equals(""))
                resultFile = null;
            else
                resultFile = new ResultFile(new File(sgfFile + ".dat"),
                                            new File(sgfFile + ".lock"),
                                            force, blackProgram, whiteProgram,
                                            refereeProgram, size, komi,
                                            sgfFile, openings, alternate,
                                            useXml);
            TwoGtp twoGtp
                = new TwoGtp(blackProgram, whiteProgram, refereeProgram,
                             observer, size, komi, games, alternate, sgfFile,
                             verbose, openings, timeSettings, resultFile);
            twoGtp.setMaxMoves(maxMoves);
            if (auto)
                autoPlay(twoGtp, games);
            else
                twoGtp.mainLoop(System.in, System.out);
            if (resultFile != null)
                resultFile.close();
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

    private static void autoPlay(TwoGtp twoGtp,
                                 int numberGames) throws Exception
    {
        if (twoGtp.gamesLeft() == 0)
        {
            System.err.println("Already " + numberGames + " games played");
            return;
        }
        try
        {
            System.in.close();
            while (twoGtp.gamesLeft() > 0)
                twoGtp.autoPlayGame();
        }
        finally
        {
            twoGtp.close();
        }
    }
}
