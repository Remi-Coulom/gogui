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
    public static final int PAD = 5;

    public static final int SMALL_PAD = 2;

    public static Border createEmptyBorder()
    {
        if (m_emptyBorder == null)
            m_emptyBorder = BorderFactory.createEmptyBorder(PAD, PAD,
                                                            PAD, PAD);
        return m_emptyBorder;
    }

    public static Border createSmallEmptyBorder()
    {
        if (m_smallEmptyBorder == null)
            m_smallEmptyBorder =
                BorderFactory.createEmptyBorder(SMALL_PAD, SMALL_PAD,
                                                SMALL_PAD, SMALL_PAD);
        return m_smallEmptyBorder;
    }

    public static Box.Filler createFiller()
    {
        Dimension dim = new Dimension(PAD, PAD);
        return new Box.Filler(dim, dim, dim);
    }

    public static int getDefaultMonoFontSize()
    {
        Font font = UIManager.getFont("TextArea.font");        
        if (font == null)
            return 10;
        return font.getSize();
    }
    private static Border m_emptyBorder;

    private static Border m_smallEmptyBorder;
}

//-----------------------------------------------------------------------------
