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
        return m_defaultMonoFontSize;
    }

    public static boolean isNormalSizeMode(JFrame window)
    {
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
        if (m_iconURL != null)
            frame.setIconImage(new ImageIcon(m_iconURL).getImage());
    }

    private static final int m_defaultMonoFontSize =
        getTextAreaFont() == null ? 10 : getTextAreaFont().getSize();

    private static final Border m_emptyBorder =
        BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD);

    private static final Border m_smallEmptyBorder =
        BorderFactory.createEmptyBorder(SMALL_PAD, SMALL_PAD,
                                        SMALL_PAD, SMALL_PAD);

    private static final Dimension m_fillerDimension =
        new Dimension(PAD, PAD);

    private static final Dimension m_smallFillerDimension =
        new Dimension(SMALL_PAD, SMALL_PAD);

    private static URL m_iconURL =
        ClassLoader.getSystemClassLoader().getResource("images/gogui.png");

    private static Font getTextAreaFont()
    {
        return UIManager.getFont("TextArea.font");
    }
}

//----------------------------------------------------------------------------
