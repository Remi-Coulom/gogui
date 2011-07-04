// Dummy.java

package net.sf.gogui.tools.dummy;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;
import net.sf.gogui.gtp.GtpCallback;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.version.Version;

/** Dummy Go program for testing GTP controlling programs.
    See the gogui-dummy documentation for information about the extension
    commands. */
public class Dummy
    extends GtpEngine
{
    public Dummy(PrintStream log, boolean useRandomSeed, long randomSeed,
                 int resign)
        throws Exception
    {
        super(log);
        registerCommands();
        setName("gogui-dummy");
        setVersion(Version.get());
        m_random = new Random();
        m_resign = resign;
        if (useRandomSeed)
            m_random.setSeed(randomSeed);
        initSize(GoPoint.DEFAULT_SIZE);
    }

    public void cmdBWBoard(GtpCommand cmd)
    {
        cmd.getResponse().append('\n');
        for (int x = 0; x < m_size; ++x)
        {
            for (int y = 0; y < m_size; ++y)
            {
                cmd.getResponse().append(m_random.nextBoolean() ? 'B' : 'W');
                if (y < m_size - 1)
                    cmd.getResponse().append(' ');
            }
            cmd.getResponse().append('\n');
        }
    }

    public void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAX_SIZE);
        initSize(size);
    }

    public void cmdCrash(GtpCommand cmd)
    {
        System.err.println("Aborting gogui-dummy");
        System.exit(1);
    }

    public void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        initSize(m_size);
    }

    public void cmdEcho(GtpCommand cmd)
    {
        cmd.setResponse(cmd.getArgLine());
    }

    public void cmdEchoErr(GtpCommand cmd)
    {
        System.err.println(cmd.getArgLine());
    }

    public void cmdDelay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(1);
        if (cmd.getNuArg() == 1)
            m_delay = cmd.getIntArg(0, 0, Integer.MAX_VALUE);
        else
            cmd.getResponse().append(m_delay);
    }

    public void cmdFileOpen(GtpCommand cmd) throws GtpError
    {
        try
        {
            File f = new File(cmd.getArg());
            cmd.setResponse("CanonicalPath: " + f.getCanonicalPath() + "\n" +
                            "Exists:        " + f.exists() + "\n" +
                            "CanRead:       " + f.canRead() + "\n" +
                            "CanWrite:      " + f.canWrite() + "\n");
        }
        catch (IOException e)
        {
            throw new GtpError(e.getMessage());
        }
    }

    public void cmdFileSave(GtpCommand cmd) throws GtpError
    {
        File file = new File(cmd.getArg());
        try
        {
            PrintStream out = new PrintStream(file);
            out.println("Hello world!");
            out.close();
        }
        catch (IOException e)
        {
            throw new GtpError(e.getMessage());
        }
    }

    public void cmdEPList(GtpCommand cmd) throws GtpError
    {
        if (cmd.getNuArg() == 1 && cmd.getArg(0).equals("show"))
            cmd.setResponse(GoPoint.toString(m_ePList));
        else
            m_ePList = cmd.getPointListArg(m_size);
    }

    public void cmdGfx(GtpCommand cmd)
    {
        cmd.setResponse("LABEL A4 test\n" +
                        "COLOR green A5 A7 B9\n" +
                        "COLOR #980098 B7 B8\n" +
                        "SQUARE B5 C9\n" +
                        "MARK A6 B6\n" +
                        "TRIANGLE A9\n" +
                        "WHITE A1\n" +
                        "BLACK B1\n" +
                        "CIRCLE c8\n" +
                        "INFLUENCE a7 -1 b7 -0.75 c7 -0.5 d7 -0.25 e7 0"
                        + " f7 0.25 g7 0.5 h7 0.75 j7 1\n" +
                        "VAR b c1 w c2 b c3 b c4 w pass b c5\n" +
                        "TEXT Graphics Demo\n");
    }

    public void cmdGoGuiAnalyzeCommands(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        String response =
            "bwboard/BWBoard/gogui-dummy-bwboard\n" +
            "none/Crash/gogui-dummy-crash\n" +
            "none/Delay/gogui-dummy-delay %o\n" +
            "eplist/EPList/gogui-dummy-eplist\n" +
            "string/File Open/gogui-dummy-file_open %r\n" +
            "none/File Save/gogui-dummy-file_save %w\n" +
            "gfx/Gfx/gogui-dummy-gfx\n" +
            "none/Invalid/gogui-dummy-invalid\n" +
            "none/Live Gfx/gogui-dummy-live_gfx\n" +
            "string/Long Response/gogui-dummy-long_response %s\n" +
            "none/Next Failure/gogui-dummy-next_failure %s\n" +
            "none/Next Success/gogui-dummy-next_success %s\n" +
            "sboard/SBoard/gogui-dummy-sboard\n" +
            "none/Sleep/gogui-dummy-sleep %s\n" +
            "none/Sleep 20s/gogui-dummy-sleep\n";
        cmd.setResponse(response);
    }

    public void cmdGenmove(GtpCommand cmd)
    {
        ++m_numberGenmove;
        if (m_numberGenmove == m_resign)
        {
            cmd.setResponse("resign");
            return;
        }
        int numberPossibleMoves = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                if (! m_alreadyPlayed[x][y])
                    ++numberPossibleMoves;
        GoPoint point = null;
        if (numberPossibleMoves > 0)
        {
            int rand = m_random.nextInt(numberPossibleMoves);
            int index = 0;
            for (int x = 0; x < m_size && point == null; ++x)
                for (int y = 0; y < m_size && point == null; ++y)
                    if (! m_alreadyPlayed[x][y])
                    {
                        if (index == rand)
                            point = GoPoint.get(x, y);
                        ++index;
                    }
        }
        cmd.setResponse(GoPoint.toString(point));
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
    }

    public void cmdInterrupt(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
    }

    public void cmdInvalid(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        printInvalidResponse("This is an invalid GTP response.\n" +
                             "It does not start with a status character.\n");
    }

    public void cmdLiveGfx(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        System.err.println("gogui-gfx: TEXT Live Graphics Demo");
        System.err.println("gogui-gfx: LABEL A4 test");
        sleep(1000);
        System.err.println("gogui-gfx: COLOR green A5 A7 B9");
        sleep(1000);
        System.err.println("gogui-gfx: COLOR #980098 B7 B8");
        sleep(1000);
        System.err.println("gogui-gfx:\n" +
                           "SQUARE B5 C9\n" +
                           "MARK A6 B6\n" +
                           "TRIANGLE A9\n");
        sleep(1000);
        System.err.println("gogui-gfx: WHITE A1");
        sleep(1000);
        System.err.println("gogui-gfx: BLACK B1");
        sleep(1000);
        System.err.println("gogui-gfx: CIRCLE c8");
        sleep(1000);
        System.err.println("gogui-gfx: INFLUENCE a7 -1 b7 -0.75 c7 "
                           + "-0.5 d7 -0.25 e7 0 f7 0.25 g7 0.5 h7 0.75 "
                           + "j7 1");
        sleep(1000);
        System.err.println("gogui-gfx: VAR b c1 w c2 b c3 b c4 w pass "
                           + "b c5");
        sleep(1000);
        System.err.println("gogui-gfx: CLEAR");
    }

    public void cmdLongResponse(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int n = cmd.getIntArg(0, 1, Integer.MAX_VALUE);
        for (int i = 1; i <= n; ++i)
        {
            cmd.getResponse().append(i);
            cmd.getResponse().append("\n");
        }
    }

    public void cmdNextFailure(GtpCommand cmd) throws GtpError
    {
        nextResponseFixed(cmd, false);
    }

    public void cmdNextSuccess(GtpCommand cmd) throws GtpError
    {
        nextResponseFixed(cmd, true);
    }

    public void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_size);
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
    }

    public void cmdSBoard(GtpCommand cmd)
    {
        cmd.getResponse().append('\n');
        for (int x = 0; x < m_size; ++x)
        {
            for (int y = 0; y < m_size; ++y)
            {
                if (x == 1 && y == 1)
                    cmd.getResponse().append("\"a b\"");
                else if (x == 1 && y == 2)
                    cmd.getResponse().append("ab   ");
                else if (x == 1 && y == 3)
                    cmd.getResponse().append("abc  ");
                else if (x == 2 && y == 1)
                    cmd.getResponse().append("abcde");
                else
                    cmd.getResponse().append("\"\"   ");
                if (y < m_size - 1)
                    cmd.getResponse().append(' ');
            }
            cmd.getResponse().append('\n');
        }
    }

    public void cmdSleep(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArgLessEqual(1);
        long millis = 20000;
        if (cmd.getNuArg() == 1)
            millis = (long)(cmd.getDoubleArg(0) * 1000.0);
        long showProgressInterval = Math.max(millis / 100, 1000);
        long steps = millis / showProgressInterval;
        long remaining = millis - steps * showProgressInterval;
        for (long i = 0; i < steps && ! isInterrupted(); ++i)
        {
            System.err.println("gogui-gfx: TEXT " + (100L * i / steps) + " %");
            sleep(showProgressInterval);
        }
        sleep(remaining);
    }

    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        if (m_nextResponseFixed)
        {
            m_nextResponseFixed = false;
            if (! m_nextStatus)
                throw new GtpError(m_nextResponse);
            cmd.setResponse(m_nextResponse);
        }
        else
            super.handleCommand(cmd);
        if (m_delay > 0)
        {
            try
            {
                Thread.sleep(1000L * m_delay);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    private boolean m_nextResponseFixed;

    private boolean m_nextStatus;

    /** Delay every command (seconds) */
    private int m_delay;

    private int m_numberGenmove;

    private final int m_resign;

    private int m_size;

    private boolean[][] m_alreadyPlayed;

    private final Random m_random;

    private String m_nextResponse;

    /** Editable point list for gogui-dummy-eplist command. */
    private PointList m_ePList = new PointList();

    private void initSize(int size)
    {
        m_alreadyPlayed = new boolean[size][size];
        m_size = size;
        m_numberGenmove = 0;
    }

    private void nextResponseFixed(GtpCommand cmd, boolean nextStatus)
    {
        m_nextResponseFixed = true;
        m_nextStatus = nextStatus;
        m_nextResponse = cmd.getArgLine();
    }

    private void registerCommands()
    {
        register("boardsize", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdBoardsize(cmd); } });
        register("clear_board", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdClearBoard(cmd); } });
        register("gogui-dummy-bwboard", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdBWBoard(cmd); } });
        register("gogui-dummy-crash", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdCrash(cmd); } });
        register("gogui-dummy-delay", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdDelay(cmd); } });
        register("gogui-dummy-eplist", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdEPList(cmd); } });
        register("gogui-dummy-file_open", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdFileOpen(cmd); } });
        register("gogui-dummy-file_save", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdFileSave(cmd); } });
        register("gogui-dummy-gfx", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGfx(cmd); } });
        register("gogui-dummy-invalid", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdInvalid(cmd); } });
        register("gogui-dummy-live_gfx", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdLiveGfx(cmd); } });
        register("gogui-dummy-long_response", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdLongResponse(cmd); } });
        register("gogui-dummy-next_failure", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdNextFailure(cmd); } });
        register("gogui-dummy-next_success", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdNextSuccess(cmd); } });
        register("gogui-dummy-sboard", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdSBoard(cmd); } });
        register("gogui-dummy-sleep", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdSleep(cmd); } });
        register("echo", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdEcho(cmd); } });
        register("echo_err", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdEchoErr(cmd); } });
        register("genmove", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGenmove(cmd); } });
        register("gogui-analyze_commands", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdGoGuiAnalyzeCommands(cmd); } });
        register("gogui-interrupt", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdInterrupt(cmd); } });
        register("play", new GtpCallback() {
                public void run(GtpCommand cmd) throws GtpError {
                    cmdPlay(cmd); } });
    }

    private void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
        }
    }
}
