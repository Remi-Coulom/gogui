//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package sgf;

//-----------------------------------------------------------------------------

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;
import utils.FileUtils;

//-----------------------------------------------------------------------------

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

//-----------------------------------------------------------------------------
