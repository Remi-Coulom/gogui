// GameWriter.java

package net.sf.gogui.gamefile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.xml.XmlWriter;

public class GameWriter
{
    public GameWriter(GameFile gameFile, ConstGameTree tree,
                      String application, String version) throws ErrorMessage
    {
        OutputStream out;
        try
        {
            out = new FileOutputStream(gameFile.m_file);
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        switch (gameFile.m_format)
        {
        case SGF:
            new SgfWriter(out, tree, application, version);
            break;
        case XML:
            String xmlApplication = application;
            if (xmlApplication != null && version != null)
                xmlApplication = xmlApplication + ":" + version;
            new XmlWriter(out, tree, xmlApplication);
            break;
        }
    }
}
