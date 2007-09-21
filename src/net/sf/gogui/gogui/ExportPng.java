//----------------------------------------------------------------------------
// ExportPng.java
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import net.sf.gogui.boardpainter.BoardPainter;
import net.sf.gogui.boardpainter.BoardPainterUtil;
import net.sf.gogui.boardpainter.ConstField;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gui.ConstGuiBoard;
import net.sf.gogui.gui.FileDialogs;
import net.sf.gogui.gui.MessageDialogs;

public final class ExportPng
{
    public static void run(Component parent, ConstGuiBoard guiBoard,
                           Preferences prefs, MessageDialogs messageDialogs)
    {
        String value = prefs.get(PREF_WIDTH, "512");
        boolean done = false;
        int width = 0;
        while (! done)
        {
            value =
                (String)JOptionPane.showInputDialog(parent, "Image Width",
                                                    "Image Width",
                                                    JOptionPane.PLAIN_MESSAGE,
                                                    null, null, value);
            if (value == null)
                return;
            try
            {
                width = Integer.parseInt(value);
                if (width > 0)
                    done = true;
            }
            catch (NumberFormatException e)
            {
            }
            if (! done)
            {
                messageDialogs.showError(parent, "Invalid value",
                                         "The image width needs to be a number"
                                         +" greater than zero.",
                                         false);
                continue;
            }
        }
        prefs.put(PREF_WIDTH, Integer.toString(width));
        File file
            = FileDialogs.showSave(parent, "Export PNG Image", messageDialogs);
        if (file == null)
            return;
        BoardPainter painter = new BoardPainter();
        int size = guiBoard.getBoardSize();
        ConstField[][] fields = new ConstField[size][size];
        for (int x = 0; x < size; ++x)
            for (int y = 0; y < size; ++y)
                fields[x][y] = guiBoard.getFieldConst(GoPoint.get(x, y));
        BufferedImage image
            = BoardPainterUtil.getImage(painter, fields, width, width);
        try
        {
            BoardPainterUtil.writeImage(image, file, null);
        }
        catch (IOException e)
        {
            messageDialogs.showError(parent, "Writing image failed",
                                     e.getMessage());
        }
    }

    private static final String PREF_WIDTH = "gogui-export-png-width";

    /** Make constructor unavailable; class is for namespace only. */
    private ExportPng()
    {
    }
}
