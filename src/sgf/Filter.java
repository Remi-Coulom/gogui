//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package sgf;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import utils.FileUtils;

//----------------------------------------------------------------------------

/** Swing file filter for SGF files. */
public class Filter
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
