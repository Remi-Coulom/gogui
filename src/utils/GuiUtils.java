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
        URL url = m_iconURL;
        if (url != null)
            frame.setIconImage(new ImageIcon(url).getImage());
    }

    static
    {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        // There are problems on most platforms with larger icons auto-scaled
        // down and transparency issues (Windows, Linux Sun Java 1.5.0)
        // Best solution for now is to take a 16x16 icon with no transparency
        m_iconURL = loader.getResource("images/gogui-16x16-notrans.png");
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

    private static URL m_iconURL;

    private static Font getTextAreaFont()
    {
        return UIManager.getFont("TextArea.font");
    }
}

//----------------------------------------------------------------------------
