//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.twogtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileFilter;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
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
    public Openings(File directory) throws Exception
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
    public GameInformation getGameInformation()
    {
        return m_gameTree.getGameInformation();
    }

    /** Get game tree of currently loaded file. */
    public GameTree getGameTree()
    {
        return m_gameTree;
    }

    /** Get number of opening files in directory. */
    public int getNumber()
    {
        return m_files.length;
    }

    /** Load opening file number i. */
    public void loadFile(int i) throws Exception
    {
        File file = m_files[i];
        FileInputStream fileStream = new FileInputStream(file);
        SgfReader reader =
            new SgfReader(fileStream, file.toString(), null, 0);
        m_gameTree = reader.getGameTree();
        m_currentFile = i;
    }

    private int m_currentFile;

    private final File m_directory;

    private File[] m_files;

    private GameTree m_gameTree;

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

