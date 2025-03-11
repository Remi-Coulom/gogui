// GameFileFilter.java

package net.sf.gogui.gamefile;

import net.sf.gogui.util.FileUtil;

import javax.swing.filechooser.FileFilter;
import java.io.File;

import static net.sf.gogui.gamefile.I18n.i18n;

/** Swing file filter for Programs list in XML. */
public class ProgramFileFilter
    extends FileFilter
{
    /** Accept function.
        @param file The file to check.
        @return true if file has extension .sgf or .SGF or is a directory */
    public boolean accept(File file)
    {
        if (file.isDirectory())
            return true;
        return (FileUtil.hasExtension(file, "xml") ||
                FileUtil.hasExtension(file, "XML"));
    }

    public String getDescription()
    {
        return i18n("LB_PROGRAM");
    }
}
