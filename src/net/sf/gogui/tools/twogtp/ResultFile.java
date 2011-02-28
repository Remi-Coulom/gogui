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
    public ResultFile(File file, File lockFile, boolean force, Program black,
                      Program white, Program referee, int size, Komi komi,
                      String filePrefix, Openings openings, boolean alternate,
                      boolean useXml) throws ErrorMessage
    {
        m_filePrefix = filePrefix;
        m_alternate = alternate;
        m_useXml = useXml;
        m_lockFile = lockFile;
        acquireLock();
        m_file = file;
        if (force)
        {
            if (file.exists() && ! file.delete())
                throw new ErrorMessage("Could not delete file '" + file + "'");
        }
        if (file.exists())
        {
            m_table = new Table();
            try
            {
                m_table.read(file);
                int lastRowIndex = m_table.getNumberRows() - 1;
                int gameIndex =
                    Integer.parseInt(m_table.get("GAME", lastRowIndex)) + 1;
                if (gameIndex < 0)
                    throw new ErrorMessage("Invalid file format: " + file);
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
            return;
        }
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
        m_table = new Table(columns);
        black.setTableProperties(m_table);
        white.setTableProperties(m_table);
        if (referee == null)
            m_table.setProperty("Referee", "-");
        else
            referee.setTableProperties(m_table);
        m_table.setProperty("Size", Integer.toString(size));
        m_table.setProperty("Komi", komi.toString());
        if (openings != null)
            m_table.setProperty("Openings",
                                openings.getDirectory() + " ("
                                + openings.getNumber() + " files)");
        m_table.setProperty("Date", StringUtil.getDate());
        m_table.setProperty("Host", Platform.getHostInfo());
        m_table.setProperty("Xml", useXml ? "1" : "0");
        readGames();
    }

    public void addResult(ConstGame game, String resultBlack,
                          String resultWhite, String resultReferee,
                          boolean alternated, int numberMoves, boolean error,
                          String errorMessage, double timeBlack,
                          double timeWhite, double cpuTimeBlack,
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
        m_table.set("GAME", Integer.toString(getGameIndex()));
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
        File tmpFile = new File(m_file.getAbsolutePath() + ".new");
        try
        {
            m_table.save(tmpFile);
            tmpFile.renameTo(m_file);
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Could not write to: " + m_file);
        }
        File file = getFile(getGameIndex());
        try
        {
            OutputStream out = new FileOutputStream(file);
            if (m_useXml)
                new XmlWriter(out, game.getTree(),
                              "gogui-twogtp:" + Version.get());
            else
                new SgfWriter(out, game.getTree(),
                              "gogui-twogtp", Version.get());
            m_games.add(moves);
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage("Could not save " + file + ": "
                                   + e.getMessage());
        }
    }

    public void close()
    {
        if (! m_lockFile.delete())
            System.err.println("Could not delete '" + m_lockFile + "'");
    }

    public int getGameIndex()
    {
        return m_table.getNumberRows();
    }

    private boolean m_alternate;

    private boolean m_useXml;

    private String m_filePrefix;

    private File m_file;

    private File m_lockFile;

    private Table m_table;

    private final ArrayList<ArrayList<Compare.Placement>> m_games
        = new ArrayList<ArrayList<Compare.Placement>>(100);

    private void acquireLock() throws ErrorMessage
    {
        try
        {
            m_lockFile.createNewFile();
            FileChannel channel
                = new RandomAccessFile(m_lockFile, "rw").getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null)
                throw new ErrorMessage("Could not get lock on file '"
                                       + m_lockFile
                            + "': already used by another instance of TwoGtp");
            // We keep the lock until the end of the process and rely on the
            // operating system to release it
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Could not lock file '" + m_lockFile
                                   + "': " + e.getMessage());
        }
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
        for (int n = 0; n < getGameIndex(); ++n)
        {
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
                m_games.add(Compare.getPlacements(root));
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
}
