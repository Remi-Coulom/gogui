//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

//-----------------------------------------------------------------------------

public class GuiUtils
{
    public static int PAD = 5;

    public static int SMALL_PAD = 2;

    public static Border createEmptyBorder()
    {
        int pad = PAD;
        return BorderFactory.createEmptyBorder(pad, pad, pad, pad);
    }

    public static Border createSmallEmptyBorder()
    {
        int pad = SMALL_PAD;
        return BorderFactory.createEmptyBorder(pad, pad, pad, pad);
    }
}

//-----------------------------------------------------------------------------
