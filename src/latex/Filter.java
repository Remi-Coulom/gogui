//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package latex;

//----------------------------------------------------------------------------

import java.io.File;
import javax.swing.filechooser.FileFilter;
import utils.FileUtils;

//----------------------------------------------------------------------------

public class Filter
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
