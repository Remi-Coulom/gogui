//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import net.sf.gogui.utils.FileUtil;

//----------------------------------------------------------------------------

/** Swing file filter for SGF files. */
public class SgfFilter
    extends FileFilter
{
    /** Accept function.
        @param file The file to check.
        @return true if file has extension .sgf or .SGF or is a directory
    */
    public boolean accept(File file)
    {
        if (file.isDirectory())
            return true;
        return FileUtil.hasExtension(file, "sgf")
            || FileUtil.hasExtension(file, "SGF");
    }

    public String getDescription()
    {
        return "Go Games (*.sgf,*.SGF)";
    }
}

//----------------------------------------------------------------------------
