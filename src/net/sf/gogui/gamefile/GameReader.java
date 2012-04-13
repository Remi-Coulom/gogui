// GameReader.java

package net.sf.gogui.gamefile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.ProgressShow;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.xml.XmlReader;

/** Read a game and detect automatically if it is SGF or XML. */
public class GameReader
{
    /** Construct reader and read a game.
        @param progressShow Callback to show progress, can be null */
    public GameReader(File file, ProgressShow progressShow) throws ErrorMessage
    {
        m_gameFile = new GameFile();
        GameFile.Format format = detectFormat(file);
        m_gameFile.m_file = file;
        m_gameFile.m_format = format;
        InputStream in;
        try
        {
            in = new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage("File \"" + file + "\" not found");
        }
        switch (format)
        {
        case XML:
            {
                XmlReader reader = new XmlReader(in, progressShow,
                                                 file.length());
                m_tree = reader.getTree();
                m_warnings = reader.getWarnings();
            }
            break;
        case SGF:
            {
                SgfReader reader =
                    new SgfReader(in, file, progressShow, file.length());
                m_tree = reader.getTree();
                m_warnings = reader.getWarnings();
            }
            break;
        }
    }

    public GameReader(File file) throws ErrorMessage
    {
        this(file, null);
    }

    public GameFile getFile()
    {
        return m_gameFile;
    }

    public GameTree getTree()
    {
        return m_tree;
    }

    public String getWarnings()
    {
        return m_warnings;
    }

    private GameFile m_gameFile;

    private String m_warnings;

    private GameTree m_tree;

    private static GameFile.Format detectFormat(File file)
    {
        String extension = FileUtil.getExtension(file);
        if (extension != null)
        {
            extension = extension.toLowerCase(Locale.ENGLISH);
            if (extension.equals("sgf"))
                return GameFile.Format.SGF;
            if (extension.equals("xml"))
                return GameFile.Format.XML;
        }
        FileReader reader = null;
        try
        {
            reader = new FileReader(file);
            char[] buffer = new char[5];
            int n = reader.read(buffer, 0, 5);
            if (n == 5 && new String(buffer).equals("<?xml"))
                return GameFile.Format.XML;
        }
        catch (IOException e)
        {
        }
        finally
        {
            try
            {
                if (reader != null)
                    reader.close();
            }
            catch (IOException e)
            {
            }
        }
        return GameFile.Format.SGF;
    }
}
