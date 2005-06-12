//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import net.sf.gogui.utils.FileUtils;

//----------------------------------------------------------------------------

/** Swing file filter for SGF files. */
public class SgfFilter
    extends FileFilter
{
    public boolean accept(File f)
    {
        if (f.isDirectory())
            return true;
        return FileUtils.hasExtension(f, "sgf");
    }

    public String getDescription()
    {
        return "Go games (*.sgf)";
    }
}

//----------------------------------------------------------------------------
