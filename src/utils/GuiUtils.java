//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.awt.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;

//----------------------------------------------------------------------------

public class GuiUtils
{
    public static final int PAD = 5;

    public static final int SMALL_PAD = 2;

    public static Border createEmptyBorder()
    {
        return m_emptyBorder;
    }

    public static Box.Filler createFiller()
    {
        return new Box.Filler(m_fillerDimension, m_fillerDimension,
                              m_fillerDimension);
    }

    public static Border createSmallEmptyBorder()
    {
        return m_smallEmptyBorder;
    }

    public static Box.Filler createSmallFiller()
    {
        return new Box.Filler(m_smallFillerDimension, m_smallFillerDimension,
                              m_smallFillerDimension);
    }

    public static int getDefaultMonoFontSize()
    {
        Font font = UIManager.getFont("TextArea.font");
        if (font == null)
            return 10;
        return font.getSize();
    }

    public static boolean isNormalSizeMode(JFrame window)
    {
        if (! window.isShowing())
            return false;
        int state = window.getExtendedState();
        int mask =
            Frame.MAXIMIZED_BOTH |
            Frame.MAXIMIZED_VERT |
            Frame.MAXIMIZED_HORIZ |
            Frame.ICONIFIED;
        return ((state & mask) == 0);
    }

    public static void setGoIcon(Frame frame)
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource("images/icon.png");
        frame.setIconImage(new ImageIcon(url).getImage());
    }

    private static final Border m_emptyBorder =
        BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD);

    private static final Border m_smallEmptyBorder =
        BorderFactory.createEmptyBorder(SMALL_PAD, SMALL_PAD,
                                        SMALL_PAD, SMALL_PAD);

    private static final Dimension m_fillerDimension =
        new Dimension(PAD, PAD);

    private static final Dimension m_smallFillerDimension =
        new Dimension(SMALL_PAD, SMALL_PAD);
}

//----------------------------------------------------------------------------
