//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.awt.*;
import javax.swing.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class DialogUtils
{
    public static void center(JDialog dialog, Window window)
    {
        Dimension size = dialog.getSize();
        if (window == null)
        {
            GraphicsEnvironment env =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            Point point = env.getCenterPoint();
            int x = point.x - size.width / 2;
            int y = point.y - size.height / 2;
            dialog.setLocation(x, y);
            return;
        }
        Dimension windowSize = window.getSize();
        int x = (windowSize.width - size.width) / 2;
        int y = (windowSize.height - size.height) / 2;
        dialog.setLocation(x, y);
    }
}

//-----------------------------------------------------------------------------

