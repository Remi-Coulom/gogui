// Main.java

package net.sf.gogui.tools.twogtp;

import java.io.File;
import java.util.ArrayList;
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
        boolean exitError = false;
        try
        {
            String options[] = {
                "alternate",
                "analyze:",
                "auto",
                "black:",
                "compare",
                "config:",
                "debugtocomment",
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
                "threads:",
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
                   "-debugtocomment save stderr of programs in SGF comments\n" +
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
                   "-threads n      number of threads\n" +
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
            if (opt.contains("version"))
            {
                System.out.println("gogui-twogtp " + Version.get());
                System.exit(0);
            }
            boolean force = opt.contains("force");
            if (opt.contains("analyze"))
            {
                String filename = opt.get("analyze");
                new Analyze(filename, force);
                return;
            }
            boolean alternate = opt.contains("alternate");
            boolean auto = opt.contains("auto");
            boolean debugToComment = opt.contains("debugtocomment");
            boolean verbose = opt.contains("verbose");
            String black = opt.get("black", "");
            if (black.equals(""))
                throw new ErrorMessage("No black program set");
            String white = opt.get("white", "");
            if (white.equals(""))
                throw new ErrorMessage("No white program set");
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
            int numberGames = opt.getInteger("games", defaultGames, 0);
            int numberThreads = opt.getInteger("threads", 1, 1);
            if (numberThreads > 1 && ! auto)
                throw new ErrorMessage("Option -threads needs option -auto");
            String sgfFile = opt.get("sgffile", "");
            if (opt.contains("games") && sgfFile.equals(""))
                throw new ErrorMessage("Use option -sgffile with -games");
            Openings openings = null;
            if (opt.contains("openings"))
                openings = new Openings(new File(opt.get("openings")));
            boolean useXml = opt.contains("xml");
            if (auto)
                System.in.close();

            TwoGtp twoGtp[] = new TwoGtp[numberThreads];
            TwoGtpThread thread[] = new TwoGtpThread[numberThreads];
            ResultFile resultFile = null;
            for (int i = 0; i < numberThreads; ++i)
            {
                ArrayList<Program> allPrograms = new ArrayList<Program>();
                Program blackProgram =
                    new Program(black, "Black", "B", verbose);
                allPrograms.add(blackProgram);
                Program whiteProgram =
                    new Program(white, "White", "W", verbose);
                allPrograms.add(whiteProgram);
                Program refereeProgram;
                if (referee.equals(""))
                    refereeProgram = null;
                else
                {
                    refereeProgram =
                        new Program(referee, "Referee", "R", verbose);
                    allPrograms.add(refereeProgram);
                }
                for (Program program : allPrograms)
                    program.setLabel(allPrograms);
                if (! sgfFile.equals("") && resultFile == null)
                    resultFile =
                        new ResultFile(force, blackProgram, whiteProgram,
                                       refereeProgram, numberGames, size,
                                       komi, sgfFile, openings, alternate,
                                       useXml, numberThreads);
                if (i > 0)
                    verbose = false;
                twoGtp[i] = new TwoGtp(blackProgram, whiteProgram,
                                       refereeProgram, observer, size, komi,
                                       numberGames, alternate, sgfFile,
                                       verbose, openings, timeSettings,
                                       resultFile);
                twoGtp[i].setMaxMoves(maxMoves);
                if (debugToComment)
                    twoGtp[i].setDebugToComment(true);
                if (auto)
                {
                    thread[i] = new TwoGtpThread(twoGtp[i]);
                    thread[i].start();
                }
            }
            if (auto)
            {
                for (int i = 0; i < numberThreads; ++i)
                    thread[i].join();
                for (int i = 0; i < numberThreads; ++i)
                    if (thread[i].getException() != null)
                    {
                        StringUtil.printException(thread[i].getException());
                        exitError = true;
                    }
            }
            else
                twoGtp[0].mainLoop(System.in, System.out);
            if (resultFile != null)
                resultFile.close();
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            exitError = true;
        }
        if (exitError)
            System.exit(1);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }
}

class TwoGtpThread
    extends Thread
{
    public TwoGtpThread(TwoGtp twoGtp)
    {
        m_twoGtp = twoGtp;
    }

    public Exception getException()
    {
        return m_exception;
    }

    public void run()
    {
        try
        {
            m_twoGtp.autoPlay();
        }
        catch (Exception e)
        {
            m_exception = e;
        }
        finally
        {
            m_twoGtp.close();
        }
    }

    private Exception m_exception;

    private TwoGtp m_twoGtp;
}