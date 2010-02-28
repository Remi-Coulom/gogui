// GameFileFilter.java

package net.sf.gogui.gamefile;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import static net.sf.gogui.gamefile.I18n.i18n;
import net.sf.gogui.util.FileUtil;

/** Swing file filter for SGF or Jago XML files. */
public class GameFileFilter
    extends FileFilter
{
    /** Accept function.
        @param file The file to check.
        @return true if file has extension .sgf or .SGF or is a directory */
    public boolean accept(File file)
    {
        if (file.isDirectory())
            return true;
        return (FileUtil.hasExtension(file, "sgf")
                || FileUtil.hasExtension(file, "SGF")
                || FileUtil.hasExtension(file, "xml")
                || FileUtil.hasExtension(file, "XML"));
    }

    public String getDescription()
    {
        return i18n("LB_GOGAME");
    }
}
