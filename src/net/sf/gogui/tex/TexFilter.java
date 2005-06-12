//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.tex;

//----------------------------------------------------------------------------

import java.io.File;
import javax.swing.filechooser.FileFilter;
import net.sf.gogui.utils.FileUtils;

//----------------------------------------------------------------------------

/** File filter for accepting LaTeX files. */
public class TexFilter
    extends FileFilter
{
    public boolean accept(File f)
    {
        if (f.isDirectory())
            return true;
        return FileUtils.hasExtension(f, "tex");
    }

    public String getDescription()
    {
        return "LaTex files (*.tex)";
    }

}

//----------------------------------------------------------------------------
