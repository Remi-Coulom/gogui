//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.awt.*;
import java.net.*;
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

    public static Box.Filler createFiller()
    {
        Dimension dim = new Dimension(PAD, PAD);
        return new Box.Filler(dim, dim, dim);
    }

    public static Border createSmallEmptyBorder()
    {
        if (m_smallEmptyBorder == null)
            m_smallEmptyBorder =
                BorderFactory.createEmptyBorder(SMALL_PAD, SMALL_PAD,
                                                SMALL_PAD, SMALL_PAD);
        return m_smallEmptyBorder;
    }

    public static Box.Filler createSmallFiller()
    {
        Dimension dim = new Dimension(SMALL_PAD, SMALL_PAD);
        return new Box.Filler(dim, dim, dim);
    }

    public static int getDefaultMonoFontSize()
    {
        Font font = UIManager.getFont("TextArea.font");        
        if (font == null)
            return 10;
        return font.getSize();
    }

    public static void setGoIcon(Frame frame)
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource("images/icon.png");
        frame.setIconImage(new ImageIcon(url).getImage());
    }

    private static Border m_emptyBorder;

    private static Border m_smallEmptyBorder;
}

//-----------------------------------------------------------------------------
