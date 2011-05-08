// ResultFile.java

package net.sf.gogui.tools.twogtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.go.Komi;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.util.Table;
import net.sf.gogui.xml.XmlWriter;
import net.sf.gogui.version.Version;

public class ResultFile
{
    public ResultFile(boolean force, Program black, Program white,
                      Program referee, int numberGames, int size, Komi komi,
                      String filePrefix, Openings openings, boolean alternate,
                      boolean useXml, int numberThreads) throws ErrorMessage
    {
        m_filePrefix = filePrefix;
        m_alternate = alternate;
        m_numberGames = numberGames;
        m_useXml = useXml;
        m_numberThreads = numberThreads;
        m_lockFile = new File(filePrefix + ".lock");
        acquireLock();
        m_tableFile = new File(filePrefix + ".dat");
        if (force)
        {
            if (m_tableFile.exists() && ! m_tableFile.delete())
                throw new ErrorMessage("Could not delete file '"
                                       + m_tableFile + "'");
        }
        if (m_tableFile.exists())
        {
            m_table = readTable(m_tableFile, numberGames, m_gameExists);
            m_nextGameIndex = 0;
            while (m_gameExists.contains(m_nextGameIndex))
            {
                ++m_nextGameIndex;
                if (numberGames > 0 && m_nextGameIndex > numberGames)
                {
                    m_nextGameIndex = -1;
                    break;
                }
            }
            readGames();
        }
        else
        {
            m_table = createTable(black, white, referee, size, komi, openings);
            m_nextGameIndex = 0;
        }
    }

    public synchronized void addResult(int gameIndex, ConstGame game,
                                       String resultBlack, String resultWhite,
                                       String resultReferee,
                                       boolean alternated, int numberMoves,
                                       boolean error, String errorMessage,
                                       double timeBlack, double timeWhite,
                                       double cpuTimeBlack,
                                       double cpuTimeWhite)
        throws ErrorMessage
    {
        ArrayList<Compare.Placement> moves
            = Compare.getPlacements(game.getTree().getRootConst());
        String duplicate =
            Compare.checkDuplicate(game.getBoard(), moves, m_games,
                                   m_alternate, alternated);
        NumberFormat format = StringUtil.getNumberFormat(1);
        m_table.startRow();
        m_table.set("GAME", Integer.toString(gameIndex));
        m_table.set("RES_B", resultBlack);
        m_table.set("RES_W", resultWhite);
        m_table.set("RES_R", resultReferee);
        m_table.set("ALT", alternated ? "1" : "0");
        m_table.set("DUP", duplicate);
        m_table.set("LEN", numberMoves);
        m_table.set("TIME_B", format.format(timeBlack));
        m_table.set("TIME_W", format.format(timeWhite));
        m_table.set("CPU_B", format.format(cpuTimeBlack));
        m_table.set("CPU_W", format.format(cpuTimeWhite));
        m_table.set("ERR", error ? "1" : "0");
        m_table.set("ERR_MSG", errorMessage);

        // The code does not rely on the table being sorted by game number,
        // but it looks nicer for the user.
        int rowEnd = m_table.getNumberRows();
        int rowBegin = rowEnd - m_numberThreads;
        if (rowBegin < 0)
            rowBegin = 0;
        // If the run was terminated and continued with a different number
        // of threads, there could be a gap between gameIndex and
        // getNumberRows() larger than m_numberThreads
        if (gameIndex < rowBegin)
            rowBegin = gameIndex;
        m_table.sortByIntColumn("GAME", rowBegin, rowEnd);

        File tmpFile = new File(m_tableFile.getAbsolutePath() + ".new");
        try
        {
            m_table.save(tmpFile);
            if (Platform.isWindows())
                // File.renameTo() fails on Windows if target exists
                m_tableFile.delete();
            tmpFile.renameTo(m_tableFile);
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Could not write to: " + m_tableFile);
        }
        File file = getFile(gameIndex);
        try
        {
            OutputStream out = new FileOutputStream(file);
            if (m_useXml)
                new XmlWriter(out, game.getTree(),
                              "gogui-twogtp:" + Version.get());
            else
                new SgfWriter(out, game.getTree(),
                              "gogui-twogtp", Version.get());
            m_games.put(gameIndex, moves);
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage("Could not save " + file + ": "
                                   + e.getMessage());
        }
    }

    public void close()
    {
        try
        {
            m_lockFileChannel.close();
        }
        catch (IOException e)
        {
            System.err.println("Could not close '" + m_lockFile + "'");
        }
        if (! m_lockFile.delete())
            System.err.println("Could not delete '" + m_lockFile + "'");
    }

    public synchronized int getNextGameIndex()
    {
        if (m_nextGameIndex != -1)
            while (m_gameExists.contains(m_nextGameIndex))
            {
                ++m_nextGameIndex;
                if (m_numberGames > 0 && m_nextGameIndex >= m_numberGames)
                {
                    m_nextGameIndex = -1;
                    break;
                }
            }
        if (m_nextGameIndex != -1)
            m_gameExists.add(m_nextGameIndex);
        return m_nextGameIndex;
    }

    private final boolean m_alternate;

    private final boolean m_useXml;

    private final TreeSet<Integer> m_gameExists = new TreeSet<Integer>();

    private int m_nextGameIndex;

    private final int m_numberGames;

    private final int m_numberThreads;

    private final String m_filePrefix;

    private final File m_tableFile;

    private final File m_lockFile;

    private FileChannel m_lockFileChannel;

    private final Table m_table;

    private final TreeMap<Integer, ArrayList<Compare.Placement>> m_games
        = new TreeMap<Integer, ArrayList<Compare.Placement>>();

    private void acquireLock() throws ErrorMessage
    {
        try
        {
            m_lockFile.createNewFile();
            m_lockFileChannel
                = new RandomAccessFile(m_lockFile, "rw").getChannel();
            FileLock lock = m_lockFileChannel.tryLock();
            if (lock == null)
                throw new ErrorMessage("Could not get lock on file '"
                                       + m_lockFile
                            + "': already used by another instance of TwoGtp");
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Could not lock file '" + m_lockFile
                                   + "': " + e.getMessage());
        }
    }

    private Table createTable(Program black, Program white, Program referee,
                              int size, Komi komi, Openings openings)
    {
        ArrayList<String> columns = new ArrayList<String>();
        columns.add("GAME");
        columns.add("RES_B");
        columns.add("RES_W");
        columns.add("RES_R");
        columns.add("ALT");
        columns.add("DUP");
        columns.add("LEN");
        columns.add("TIME_B");
        columns.add("TIME_W");
        columns.add("CPU_B");
        columns.add("CPU_W");
        columns.add("ERR");
        columns.add("ERR_MSG");
        Table table = new Table(columns);
        black.setTableProperties(table);
        white.setTableProperties(table);
        if (referee == null)
            table.setProperty("Referee", "-");
        else
            referee.setTableProperties(table);
        table.setProperty("Size", Integer.toString(size));
        table.setProperty("Komi", komi.toString());
        if (openings != null)
            table.setProperty("Openings",
                                openings.getDirectory() + " ("
                                + openings.getNumber() + " files)");
        table.setProperty("Date", StringUtil.getDate());
        table.setProperty("Host", Platform.getHostInfo());
        table.setProperty("Xml", m_useXml ? "1" : "0");
        return table;
    }

    private File getFile(int gameIndex)
    {
        if (m_useXml)
            return new File(m_filePrefix + "-" + gameIndex + ".xml");
        else
            return new File(m_filePrefix + "-" + gameIndex + ".sgf");
    }

    private void readGames()
    {
        for (int n = 0; n < m_numberGames; ++n)
        {
            if (! m_gameExists.contains(n))
                continue;
            File file = getFile(n);
            if (! file.exists())
            {
                System.err.println("Game " + file + " not found");
                continue;
            }
            if (! file.exists())
                return;
            try
            {
                FileInputStream fileStream = new FileInputStream(file);
                SgfReader reader = new SgfReader(fileStream, file, null, 0);
                ConstNode root = reader.getTree().getRoot();
                m_games.put(n, Compare.getPlacements(root));
            }
            catch (SgfError e)
            {
                System.err.println("Error reading " + file + ": " +
                                   e.getMessage());
            }
            catch (Exception e)
            {
                System.err.println("Error reading " + file + ": " +
                                   e.getMessage());
            }
        }
    }

    private static Table readTable(File file, int numberGames,
                                   TreeSet<Integer> gameExists)
        throws ErrorMessage
    {
        Table table = new Table();
        try
        {
            table.read(file);
            int numberRows = table.getNumberRows();
            if (numberGames > 0 && numberRows >= numberGames)
                throw new ErrorMessage("File " + file + " already contains "
                                       + numberRows + " games");
            for (int i = 0; i < numberRows; ++i)
            {
                int gameIndex = Integer.parseInt(table.get("GAME", i));
                if (gameIndex < 0)
                    throw new ErrorMessage("Invalid file format: " + file);
                gameExists.add(gameIndex);
            }
        }
        catch (NumberFormatException e)
        {
            throw new ErrorMessage("Invalid file format: " + file);
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Read error: " + file);
        }
        return table;
    }
}
