// Openings.java

package net.sf.gogui.tools.twogtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileFilter;
import java.io.IOException;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;

class Filter
    implements FileFilter
{
    public boolean accept(File f)
    {
        return FileUtil.hasExtension(f, "sgf");
    }
}

/** Access opening SGF files from directory. */
public class Openings
{
    public Openings(File directory) throws ErrorMessage
    {
        if (! directory.isDirectory())
            throw new ErrorMessage(directory + " is not a directory");
        m_directory = directory;
        m_files = directory.listFiles(new Filter());
        if (m_files.length == 0)
            throw new ErrorMessage("No SGF files found in " + directory);
        sortFiles();
        m_currentFile = -1;
    }

    public int getBoardSize()
    {
        return getTree().getBoardSize();
    }

    /** Get name of directory. */
    public String getDirectory()
    {
        return m_directory.toString();
    }

    /** Get name of currently loaded file. */
    public String getFilename()
    {
        return m_files[m_currentFile].toString();
    }

    /** Get game information of currently loaded file. */
    public GameInfo getGameInfo()
    {
        return m_tree.getGameInfo(m_tree.getRoot());
    }

    /** Get game tree of currently loaded file. */
    public GameTree getTree()
    {
        return m_tree;
    }

    /** Get number of opening files in directory. */
    public int getNumber()
    {
        return m_files.length;
    }

    /** Load opening file number i. */
    public void loadFile(int i) throws IOException, SgfError
    {
        File file = m_files[i];
        FileInputStream fileStream = new FileInputStream(file);
        SgfReader reader = new SgfReader(fileStream, file, null, 0);
        m_tree = reader.getTree();
        m_currentFile = i;
    }

    private int m_currentFile;

    private final File m_directory;

    private File[] m_files;

    private GameTree m_tree;

    private void sortFiles()
    {
        for (int i = 0; i < m_files.length - 1; ++i)
            for (int j = i + 1; j < m_files.length; ++j)
                if (m_files[i].compareTo(m_files[j]) > 0)
                {
                    File tmp = m_files[i];
                    m_files[i] = m_files[j];
                    m_files[j] = tmp;
                }
    }
}
