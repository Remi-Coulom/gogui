//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package twogtp;

import java.io.*;
import java.lang.*;
import java.util.*;
import game.*;
import go.*;
import sgf.*;
import utils.FileUtils;

//----------------------------------------------------------------------------

class Filter
    implements FileFilter
{
    public boolean accept(File f)
    {
        return FileUtils.hasExtension(f, "sgf");
    }
}

//----------------------------------------------------------------------------

public class Openings
{
    public Openings(File directory) throws Exception
    {
        if (! directory.isDirectory())
            throw new Exception(directory + " is not a directory");
        m_files = directory.listFiles(new Filter());
        sortFiles();
        m_currentFile = -1;
    }

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

    public int getNumber()
    {
        return m_files.length;
    }

    /** Load opening file number i. */
    public void loadFile(int i) throws Exception
    {
        File file = m_files[i];
        sgf.Reader reader =
            new sgf.Reader(new FileReader(file), file.toString());
        m_gameTree = reader.getGameTree();
        m_currentFile = i;
    }

    private int m_currentFile;

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

//----------------------------------------------------------------------------
